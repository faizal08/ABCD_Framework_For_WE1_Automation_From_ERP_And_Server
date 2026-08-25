package com.eit.automation.actions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AutoItActions {

    private WebDriver driver;
    private WebDriverWait wait;
    private WaitActions waitActions;
    private boolean detailedLogging = true;

    public AutoItActions(WebDriver driver, WebDriverWait wait, WaitActions waitActions) {
        this.driver = driver;
        this.wait = wait;
        this.waitActions = waitActions;
    }

    /**
     * wrapper function for AutoIT execution
     * 
     * @param scriptPath - Absolute path to the .exe file
     */
    public void executeScript(String scriptPath) {
        executeScript(scriptPath, "");
    }

    /**
     * wrapper function for AutoIT execution with arguments
     * 
     * @param scriptPath - Absolute path to the .exe file
     */
    public void executeScript(String scriptPath, String args) {
        log("Executing AutoIT script: " + scriptPath + " with args: " + args);

        File file = new File(scriptPath);
        if (!file.exists()) {
            throw new RuntimeException("AutoIT script not found at: " + scriptPath);
        }

        try {
            String command = scriptPath + " " + args;
            Process process = Runtime.getRuntime().exec(command);

            // Wait for the process to complete with a timeout (e.g., 60 seconds)
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroy();
                throw new RuntimeException("AutoIT script timed out: " + scriptPath);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("AutoIT script exited with error code: " + exitCode);
            }

            log("✓ AutoIT script executed successfully");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute AutoIT script: " + scriptPath, e);
        }
    }

    private void log(String message) {
        if (detailedLogging) {
            System.out.println("[AutoIT] " + message);
        }
    }
}
