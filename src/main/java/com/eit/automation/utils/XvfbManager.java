package com.eit.automation.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Manages Virtual X11 Display (Xvfb) and FFmpeg Video Screen Recording
 * for Headless Browser Execution in Linux/Docker environments.
 * Package: com.eit.automation.utils
 */
public class XvfbManager {

    private static Process xvfbProcess;
    private static Process ffmpegProcess;

    private static final String DEFAULT_DISPLAY = ":99";
    private static final String DEFAULT_RESOLUTION = "1920x1080x24";
    private static String activeDisplay = DEFAULT_DISPLAY;

    private static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("nix") ||
                System.getProperty("os.name").toLowerCase().contains("nux") ||
                System.getProperty("os.name").toLowerCase().contains("aix");
    }

    /**
     * Starts Xvfb Server on default display :99 (1920x1080x24)
     */
    public static synchronized boolean startXvfb() {
        return startXvfb(DEFAULT_DISPLAY, DEFAULT_RESOLUTION);
    }

    /**
     * Starts Xvfb Server on specified display port and resolution.
     * Example: startXvfb(":99", "1920x1080x24")
     */
    public static synchronized boolean startXvfb(String displayNum, String resolution) {
        if (!isLinux()) {
            System.out.println("ℹ️ OS is not Linux. Skipping Xvfb virtual display initialization.");
            return false;
        }

        if (xvfbProcess != null && xvfbProcess.isAlive()) {
            System.out.println("⚠️ Xvfb is already running on display: " + activeDisplay);
            return true;
        }

        try {
            activeDisplay = displayNum;
            System.out.println("🖥️ Starting Xvfb virtual display on " + displayNum + " (" + resolution + ")...");

            ProcessBuilder pb = new ProcessBuilder(
                    "Xvfb", displayNum,
                    "-screen", "0", resolution,
                    "-ac", "+extension", "RANDR"
            );
            pb.redirectErrorStream(true);
            xvfbProcess = pb.start();

            // Allow Xvfb time to allocate framebuffer memory
            Thread.sleep(1000);

            if (xvfbProcess.isAlive()) {
                System.out.println("✅ Xvfb started successfully on display " + displayNum);
                return true;
            } else {
                System.err.println("❌ Xvfb process terminated unexpectedly during launch.");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to launch Xvfb: " + e.getMessage());
            return false;
        }
    }

    /**
     * Starts FFmpeg screen recording of the active Xvfb virtual display.
     *
     * @param outputVideoFilePath Path where MP4 video will be written.
     * @param videoWidthHeight    Resolution string e.g. "1920x1080"
     * @param fps                 Frames per second (e.g., 24 or 30)
     */
    public static synchronized boolean startRecording(String outputVideoFilePath, String videoWidthHeight, int fps) {
        if (!isLinux()) {
            System.out.println("ℹ️ Skipping FFmpeg X11 capture (Not running on Linux/X11).");
            return false;
        }

        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            System.out.println("⚠️ FFmpeg recording is already active.");
            return true;
        }

        try {
            File outputFile = new File(outputVideoFilePath);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }

            System.out.println("🎬 Starting FFmpeg video recording -> " + outputVideoFilePath);

            // FFmpeg command using x11grab on Xvfb display
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",                           // Overwrite existing output file
                    "-video_size", videoWidthHeight, // e.g. "1920x1080"
                    "-framerate", String.valueOf(fps),
                    "-f", "x11grab",                // X11 Screen Capture engine
                    "-i", activeDisplay + ".0",     // Input Display (e.g. :99.0)
                    "-c:v", "libx264",              // Standard MP4 H.264 codec
                    "-preset", "ultrafast",         // Low CPU footprint during test run
                    "-pix_fmt", "yuv420p",          // High browser and cloud preview compatibility
                    outputVideoFilePath
            );

            pb.redirectErrorStream(true);
            ffmpegProcess = pb.start();

            // Background thread to drain process output buffer
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
                    while (reader.readLine() != null) {
                        // Consuming stdout/stderr to prevent buffer overflow freeze
                    }
                } catch (Exception ignored) {}
            }).start();

            Thread.sleep(1000); // Warmup FFmpeg

            if (ffmpegProcess.isAlive()) {
                System.out.println("🎥 FFmpeg video recording started successfully.");
                return true;
            } else {
                System.err.println("❌ FFmpeg failed to start recording.");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Error starting FFmpeg recording: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stops FFmpeg recording gracefully by sending 'q' signal to write MP4 atom headers.
     */
    public static synchronized void stopRecording() {
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            System.out.println("🛑 Stopping FFmpeg video recording...");
            try {
                // Graceful quit signal for FFmpeg to finalize MP4 file index
                OutputStream os = ffmpegProcess.getOutputStream();
                os.write("q\n".getBytes());
                os.flush();

                boolean exited = ffmpegProcess.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    ffmpegProcess.destroyForcibly();
                }
                System.out.println("✅ FFmpeg recording stopped and finalized.");
            } catch (Exception e) {
                System.err.println("⚠️ Error while gracefully stopping FFmpeg: " + e.getMessage());
                ffmpegProcess.destroyForcibly();
            } finally {
                ffmpegProcess = null;
            }
        }
    }

    /**
     * Stops Xvfb virtual display process.
     */
    public static synchronized void stopXvfb() {
        stopRecording(); // Always stop recording first if running

        if (xvfbProcess != null && xvfbProcess.isAlive()) {
            System.out.println("🔌 Shutting down Xvfb virtual display (" + activeDisplay + ")...");
            xvfbProcess.destroy();
            try {
                xvfbProcess.waitFor(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                xvfbProcess.destroyForcibly();
            }
            System.out.println("✅ Xvfb stopped.");
            xvfbProcess = null;
        }
    }
}
