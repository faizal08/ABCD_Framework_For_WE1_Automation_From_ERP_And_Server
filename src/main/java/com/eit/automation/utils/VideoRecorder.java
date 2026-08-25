package com.eit.automation.utils;

import com.eit.automation.core.ScreenshotHelper;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VideoRecorder {
    private AWTSequenceEncoder encoder;
    private ScheduledExecutorService worker;
    private Robot robot;
    private File videoFile;
    private volatile boolean isRecording = false;
    private SeekableByteChannel out; // Resource management channel

    // Target dimensions: 1024x576 (Ultra-lightweight HD format, perfect for Selenium UI steps)
    private static final int TARGET_WIDTH = 1024;
    private static final int TARGET_HEIGHT = 576;

    // Frame capture rate: 250 ms = 4 FPS (Cuts raw file size by more than half)
    private static final int CAPTURE_INTERVAL_MS = 250;

    /**
     * NO-ARGUMENT CONSTRUCTOR
     */
    public VideoRecorder() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            System.err.println("❌ Robot capture not supported: " + e.getMessage());
        }
    }

    /**
     * Initializes and starts a new MP4 recording session.
     */
    public void startRecording(String directory, String fileName) throws Exception {
        // Force .mp4 extension
        if (!fileName.toLowerCase().endsWith(".mp4")) {
            fileName = fileName.replaceAll("\\.[^.]+$", "") + ".mp4";
        }

        File dir = new File(directory);
        if (!dir.exists()) dir.mkdirs();

        this.videoFile = new File(dir, fileName);
        this.out = NIOUtils.writableChannel(videoFile);

        // Rational.R(4, 1) = 4 FPS (Ultra low overhead, perfectly smooth for UI text)
        this.encoder = new AWTSequenceEncoder(out, Rational.R(8, 1));
        this.isRecording = true;

        // Use Daemon Thread so background recording won't hang JVM on failure
        worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        // Capture frame every 250ms (4 FPS)
        worker.scheduleAtFixedRate(this::captureFrame, 0, CAPTURE_INTERVAL_MS, TimeUnit.MILLISECONDS);

        System.out.println("🎥 Lightweight MP4 Recording started (4 FPS, 1024x576): " + videoFile.getName());
    }

    /**
     * Captures a desktop frame, downscales rapidly using Graphics2D, and encodes to JCodec.
     */
    private void captureFrame() {
        if (!isRecording || encoder == null || robot == null) return;
        try {
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenFullImage = robot.createScreenCapture(screenRect);

            // Fast downscaling via Graphics2D
            BufferedImage resizedImage = fastResizeImage(screenFullImage, TARGET_WIDTH, TARGET_HEIGHT);

            // JCodec fix: Force dimensions to even numbers
            BufferedImage formattedImage = ensureEvenDimensions(resizedImage);

            synchronized (this) {
                if (encoder != null && isRecording) {
                    encoder.encodeImage(formattedImage);
                }
            }
        } catch (Exception e) {
            // Ignore capture errors to keep Selenium running smoothly
        }
    }

    /**
     * High-speed image resizing using Graphics2D with Bilinear interpolation.
     */
    private BufferedImage fastResizeImage(BufferedImage src, int targetW, int targetH) {
        BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(src, 0, 0, targetW, targetH, null);
        g2d.dispose();
        return resized;
    }

    /**
     * Fixes JCodec image distortion/crashes by ensuring width & height are even numbers.
     */
    private BufferedImage ensureEvenDimensions(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        boolean needsWidthFix = (width % 2 != 0);
        boolean needsHeightFix = (height % 2 != 0);

        if (!needsWidthFix && !needsHeightFix) {
            return img;
        }

        int newWidth = needsWidthFix ? width - 1 : width;
        int newHeight = needsHeightFix ? height - 1 : height;

        return img.getSubimage(0, 0, newWidth, newHeight);
    }

    /**
     * Stops recording, finalizes JCodec MP4, attempts FFmpeg compression if available,
     * registers the artifact for Cloudinary upload, and returns the path.
     */
    public String stopRecording(String videoKey) throws Exception {
        this.isRecording = false;

        if (worker != null) {
            worker.shutdown();
            try {
                if (!worker.awaitTermination(2, TimeUnit.SECONDS)) {
                    worker.shutdownNow();
                }
            } catch (InterruptedException e) {
                worker.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Finalize JCodec MP4 header and close channel safely
        synchronized (this) {
            if (encoder != null) {
                try {
                    encoder.finish();
                } catch (Exception e) {
                    System.err.println("⚠️ Warning during encoder finish: " + e.getMessage());
                }
                encoder = null;
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    System.err.println("⚠️ Warning closing channel: " + e.getMessage());
                }
                out = null;
            }
        }

        String savedFilePath = (videoFile != null && videoFile.exists()) ? videoFile.getAbsolutePath() : "";

        if (!savedFilePath.isEmpty()) {
            // Optional FFmpeg post-compression check (If FFmpeg is on machine PATH)
            savedFilePath = compressWithFFmpegIfAvailable(savedFilePath);

            File finalFile = new File(savedFilePath);
            System.out.println("✅ Final MP4 Video saved locally (" + (finalFile.length() / (1024 * 1024)) + " MB): " + savedFilePath);

            String cleanKey = (videoKey != null && !videoKey.trim().isEmpty()) ? new File(videoKey).getName() : finalFile.getName();

            ScreenshotHelper.registerArtifact(cleanKey, savedFilePath);
        } else {
            System.err.println("⚠️ Video file was not created properly!");
        }

        return savedFilePath;
    }

    /**
     * Programmatically compresses the MP4 file using FFmpeg if installed on system PATH.
     */
    private String compressWithFFmpegIfAvailable(String inputPath) {
        String compressedPath = inputPath.replace(".mp4", "_compressed.mp4");

        // Command: ffmpeg -y -i input.mp4 -vcodec libx264 -crf 28 compressed.mp4
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", inputPath,
                "-vcodec", "libx264",
                "-crf", "28",
                "-preset", "ultrafast",
                compressedPath
        );

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                File compressedFile = new File(compressedPath);
                File originalFile = new File(inputPath);

                if (compressedFile.exists() && compressedFile.length() < originalFile.length()) {
                    originalFile.delete(); // Delete uncompressed raw file
                    System.out.println("⚡ FFmpeg successfully compressed video to " + (compressedFile.length() / (1024 * 1024)) + " MB");
                    return compressedPath;
                }
            }
        } catch (Exception e) {
            // FFmpeg not installed or timeout occurred; silently fall back to optimized JCodec output
        }

        return inputPath;
    }

    public String stopRecording() throws Exception {
        String defaultKey = (videoFile != null) ? videoFile.getName() : "test_execution_video.mp4";
        return stopRecording(defaultKey);
    }

    public File getVideoFile() {
        return videoFile;
    }

    public boolean isRecording() {
        return isRecording;
    }
}