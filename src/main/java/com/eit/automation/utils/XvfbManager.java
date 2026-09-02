package com.eit.automation.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages Virtual X11 Display (Xvfb) and FFmpeg Video Screen Recording
 * for Headless Browser/Mobile Execution across Linux (Docker/CI) and Windows (Local).
 * Package: com.eit.automation.utils
 */
public class XvfbManager {

    private static Process xvfbProcess;
    private static Process ffmpegProcess;

    private static final String DEFAULT_DISPLAY = ":99";
    private static final String DEFAULT_RESOLUTION = "1920x1080x24";
    private static String activeDisplay = DEFAULT_DISPLAY;

    private static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("aix");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Starts Xvfb Server on default display :99 (1920x1080x24) - Linux only.
     */
    public static synchronized boolean startXvfb() {
        return startXvfb(DEFAULT_DISPLAY, DEFAULT_RESOLUTION);
    }

    /**
     * Starts Xvfb Server on specified display port and resolution (Linux only).
     * Example: startXvfb(":99", "1920x1080x24")
     */
    public static synchronized boolean startXvfb(String displayNum, String resolution) {
        if (!isLinux()) {
            System.out.println("ℹ️ OS is not Linux (" + System.getProperty("os.name") + "). Skipping Xvfb virtual display initialization.");
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
     * Starts FFmpeg screen recording.
     * Uses x11grab on Linux (capturing Xvfb) and gdigrab on Windows (capturing desktop).
     *
     * @param outputVideoFilePath Path where MP4 video will be written.
     * @param videoWidthHeight    Resolution string e.g. "1920x1080"
     * @param fps                 Frames per second (e.g., 24 or 30)
     */
    public static synchronized boolean startRecording(String outputVideoFilePath, String videoWidthHeight, int fps) {
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

            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-y"); // Overwrite existing file

            if (isLinux()) {
                // Linux / Docker CI/CD (Grabs Xvfb virtual screen buffer)
                System.out.println("🐧 Linux OS detected: Using x11grab engine on display " + activeDisplay);
                command.add("-video_size"); command.add(videoWidthHeight);
                command.add("-framerate");  command.add(String.valueOf(fps));
                command.add("-f");          command.add("x11grab");
                command.add("-i");          command.add(activeDisplay + ".0");
            } else if (isWindows()) {
                // Windows OS (Grabs desktop screen via gdigrab engine)
                System.out.println("🪟 Windows OS detected: Using gdigrab desktop engine");
                command.add("-f");          command.add("gdigrab");
                command.add("-framerate");  command.add(String.valueOf(fps));
                command.add("-i");          command.add("desktop");
            } else {
                System.err.println("❌ Unsupported Operating System for FFmpeg screen capture.");
                return false;
            }

            // Common Encoding Options for H.264 MP4 Output
            command.add("-c:v");     command.add("libx264");
            command.add("-preset");  command.add("ultrafast");
            command.add("-pix_fmt"); command.add("yuv420p");
            command.add(outputVideoFilePath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            ffmpegProcess = pb.start();

            // Background thread to drain process output stream (prevents OS buffer locking)
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpegProcess.getInputStream()))) {
                    while (reader.readLine() != null) {
                        // Consuming stdout/stderr buffer continuously
                    }
                } catch (Exception ignored) {}
            }).start();

            Thread.sleep(1000); // Warmup FFmpeg

            if (ffmpegProcess.isAlive()) {
                System.out.println("🎥 FFmpeg video recording started successfully.");
                return true;
            } else {
                System.err.println("❌ FFmpeg failed to start recording. Verify FFmpeg is installed and accessible in PATH.");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Error starting FFmpeg recording: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stops FFmpeg recording gracefully by sending 'q' signal to write MP4 atom headers properly.
     */
    public static synchronized void stopRecording() {
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            System.out.println("🛑 Stopping FFmpeg video recording...");
            try {
                // Graceful quit signal for FFmpeg to finalize MP4 container headers
                OutputStream os = ffmpegProcess.getOutputStream();
                os.write("q\n".getBytes());
                os.flush();

                boolean exited = ffmpegProcess.waitFor(5, TimeUnit.SECONDS);
                if (!exited) {
                    ffmpegProcess.destroyForcibly();
                }
                System.out.println("✅ FFmpeg recording stopped and video finalized.");
            } catch (Exception e) {
                System.err.println("⚠️ Error while gracefully stopping FFmpeg: " + e.getMessage());
                ffmpegProcess.destroyForcibly();
            } finally {
                ffmpegProcess = null;
            }
        }
    }

    /**
     * Stops Xvfb virtual display process (if running on Linux).
     */
    public static synchronized void stopXvfb() {
        stopRecording(); // Always ensure video recording is stopped first

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