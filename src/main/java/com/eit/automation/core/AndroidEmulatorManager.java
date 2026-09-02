package com.eit.automation.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Self-Healing Android Emulator Manager (CLI Engine)
 * Package: com.eit.automation.core
 *
 * Implements full self-healing workflow:
 * 1. ADB Health Check (sys.boot_completed & responsiveness)
 * 2. Kill frozen process & Cold boot wipe recovery
 * 3. Fallback AVD recreation via avdmanager (Pixel 4 / API 33) if unrecoverable
 * 4. Supports Headless/RAM execution for Server runs (-no-window)
 */
public class AndroidEmulatorManager {

    private static final String DEFAULT_SYS_IMAGE = "system-images;android-33;google_apis;x86_64";
    private static final String DEFAULT_DEVICE_PROFILE = "pixel_4";
    private static final int BOOT_TIMEOUT_SECONDS = 300;

    /**
     * Main self-healing entry point. Ensures the target emulator is booted and ready.
     * Example usage: ensureEmulatorHealthy("WE1_User_Device", 5554)
     *
     * @param avdName AVD name (e.g., WE1_User_Device, WE1_Driver_Device, WE1_Store_Device)
     * @param port    Port number (e.g., 5554, 5556, 5558)
     */
    public static synchronized boolean ensureEmulatorHealthy(String avdName, int port) {
        String deviceId = "emulator-" + port;
        System.out.println("🔍 [Self-Healing] Checking health for " + avdName + " (" + deviceId + ")...");

        // Step 1: Check ADB Status & Boot State
        if (isEmulatorHealthy(deviceId)) {
            System.out.println("✅ " + avdName + " (" + deviceId + ") is healthy and ready.");
            return true;
        }

        System.out.println("⚠️ " + avdName + " (" + deviceId + ") is NOT healthy/running. Initiating Recovery Flow...");

        // Step 2: Kill frozen process & attempt Cold Boot / Wipe
        killEmulatorProcess(deviceId, port);
        boolean recovered = startEmulatorWithColdBoot(avdName, port);

        if (recovered && waitForBootCompletion(deviceId, BOOT_TIMEOUT_SECONDS)) {
            System.out.println("✅ Recovery Successful! " + avdName + " recovered via Cold Boot.");
            return true;
        }

        // Step 3: Reconstruction Phase (Delete -> Recreate -> Start Fresh)
        System.err.println("❌ Cold boot failed to recover " + avdName + ". Initiating complete AVD Re-creation...");
        killEmulatorProcess(deviceId, port);

        deleteAVD(avdName);
        boolean created = createFreshAVD(avdName, DEFAULT_SYS_IMAGE, DEFAULT_DEVICE_PROFILE);

        if (!created) {
            System.err.println("❌ Failed to recreate AVD: " + avdName);
            return false;
        }

        boolean freshStarted = startEmulatorWithColdBoot(avdName, port);
        if (freshStarted && waitForBootCompletion(deviceId, BOOT_TIMEOUT_SECONDS)) {
            System.out.println("🚀 [Self-Healing Complete] Fresh " + avdName + " initialized and operational!");
            return true;
        }

        System.err.println("❌ [Fatal] Could not bring " + avdName + " to a healthy state after full rebuild.");
        return false;
    }

    /**
     * Verifies whether ADB detects the device and 'sys.boot_completed' equals 1.
     */
    public static boolean isEmulatorHealthy(String deviceId) {
        String adbPath = getAndroidToolPath("adb");
        String output = executeCommand(adbPath + " -s " + deviceId + " shell getprop sys.boot_completed");
        return output.trim().equals("1");
    }

    /**
     * Launches emulator with '-no-snapshot-load -wipe-data' on specified port.
     * Appends '-no-window' and '-no-audio' if running in headless mode.
     */
    private static boolean startEmulatorWithColdBoot(String avdName, int port) {
        try {
            System.out.println("🔄 Launching " + avdName + " on port " + port + " (Cold Boot / Wipe Data)...");

            // Read headless setting directly from Main.config properties (with fallback)
            boolean isHeadless = false;
            if (Main.config != null) {
                String headlessProp = Main.config.getProperty("headless", "false");
                isHeadless = Boolean.parseBoolean(headlessProp);
            }

            List<String> command = new ArrayList<>();
            command.add(getAndroidToolPath("emulator"));
            command.add("-avd");
            command.add(avdName);
            command.add("-port");
            command.add(String.valueOf(port));
            command.add("-no-snapshot-load");
            command.add("-wipe-data");

            // Server / ERP Mode: Run headless inside RAM
            if (isHeadless) {
                System.out.println("🌐 Headless mode detected. Running " + avdName + " with -no-window flag.");
                command.add("-no-window");
                command.add("-no-audio");
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Background thread to drain stdout to avoid OS buffer deadlock
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    while (reader.readLine() != null) {
                        // Consuming process output
                    }
                } catch (Exception ignored) {}
            }).start();

            return true;
        } catch (Exception e) {
            System.err.println("❌ Failed to start emulator command: " + e.getMessage());
            return false;
        }
    }

    /**
     * Polls ADB until sys.boot_completed returns '1' or timeout expires.
     */
    private static boolean waitForBootCompletion(String deviceId, int timeoutSeconds) {
        System.out.println("⏳ Waiting for " + deviceId + " to finish booting (Max " + timeoutSeconds + "s)...");
        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < (timeoutSeconds * 1000L)) {
            if (isEmulatorHealthy(deviceId)) {
                return true;
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {}
        }
        return false;
    }

    /**
     * Kills emulator process on specific port via ADB and OS process termination.
     */
    public static void killEmulatorProcess(String deviceId, int port) {
        System.out.println("🛑 Killing process for " + deviceId + "...");
        String adbPath = getAndroidToolPath("adb");

        executeCommand(adbPath + " -s " + deviceId + " emu kill");

        // OS level safety kill by port
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            executeCommand("cmd /c \"for /f \"tokens=5\" %a in ('netstat -aon ^| findstr :" + port + "') do taskkill /F /PID %a\"");
        } else {
            executeCommand("fuser -k " + port + "/tcp");
        }

        executeCommand(adbPath + " kill-server");
        executeCommand(adbPath + " start-server");
    }

    /**
     * Deletes the AVD using avdmanager.
     */
    private static boolean deleteAVD(String avdName) {
        System.out.println("🗑️ Deleting corrupted AVD: " + avdName + "...");
        String cmd = getAndroidToolPath("avdmanager") + " delete avd -n " + avdName;
        String result = executeCommand(cmd);
        return !result.toLowerCase().contains("error");
    }

    /**
     * Re-creates the AVD with Pixel 4 profile and API 33 Google APIs system image.
     */
    private static boolean createFreshAVD(String avdName, String systemImage, String deviceProfile) {
        System.out.println("🏗️ Creating fresh AVD: " + avdName + " (Pixel 4, API 33)...");

        String avdManagerCmd = getAndroidToolPath("avdmanager");
        String cmd = "echo no | " + avdManagerCmd +
                " create avd -n " + avdName +
                " -k \"" + systemImage + "\"" +
                " -d \"" + deviceProfile + "\"" +
                " --force";

        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            cmd = "cmd /c \"echo no | " + avdManagerCmd +
                    " create avd -n " + avdName +
                    " -k \"" + systemImage + "\"" +
                    " -d \"" + deviceProfile + "\"" +
                    " --force\"";
        }

        String result = executeCommand(cmd);
        return result.toLowerCase().contains("created") || !result.toLowerCase().contains("error");
    }

    /**
     * Resolves absolute binary paths using ANDROID_HOME or ANDROID_SDK_ROOT.
     */
    private static String getAndroidToolPath(String tool) {
        String sdkRoot = System.getenv("ANDROID_HOME");
        if (sdkRoot == null || sdkRoot.isEmpty()) {
            sdkRoot = System.getenv("ANDROID_SDK_ROOT");
        }

        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        String extension = isWin ? ".bat" : "";
        String exeExt = isWin ? ".exe" : "";

        if (sdkRoot != null && !sdkRoot.isEmpty()) {
            if (tool.equals("emulator")) {
                return new File(sdkRoot, "emulator/emulator" + exeExt).getAbsolutePath();
            } else if (tool.equals("avdmanager")) {
                return new File(sdkRoot, "cmdline-tools/latest/bin/avdmanager" + extension).getAbsolutePath();
            } else if (tool.equals("adb")) {
                return new File(sdkRoot, "platform-tools/adb" + exeExt).getAbsolutePath();
            }
        }
        return tool; // Fallback to system PATH if SDK variables are not set
    }

    /**
     * Helper to run CLI commands and return execution log.
     */
    private static String executeCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                pb = new ProcessBuilder("bash", "-c", command);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            output.append("Execution Exception: ").append(e.getMessage());
        }
        return output.toString();
    }
}