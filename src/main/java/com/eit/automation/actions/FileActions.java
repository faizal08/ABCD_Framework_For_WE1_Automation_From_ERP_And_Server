package com.eit.automation.actions;

import java.io.File;
import java.io.IOException;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;

public class FileActions {

	private WebDriver driver;
	private WebDriverWait wait;
	private WaitActions waitActions;
	// Logging configuration
	private boolean detailedLogging = false;

	public FileActions(WebDriver driver, WebDriverWait wait, WaitActions waitActions) {
		this.driver = driver;
		this.wait = wait;
		this.waitActions = waitActions;
	}

	/**
	 * Upload file using input element
	 */
	public void uploadFile(String filePath, String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Uploading file: " + filePath);
		try {
			// Verify file exists
			File file = new File(filePath);
			if (!file.exists()) {
				throw new RuntimeException("File not found: " + filePath);
			}

			// Get absolute path
			String absolutePath = file.getAbsolutePath();

			// Find file input element
			WebElement fileInput = waitActions.waitForElementPresent(xpath);

			// Send file path to input element
			fileInput.sendKeys(absolutePath);

			log("✓ File uploaded: " + absolutePath);

		} catch (Exception e) {
			throw new RuntimeException("Failed to upload file: " + filePath, e);
		}
	}

	/**
	 * Wait for file upload to complete by checking for success indicator
	 */
	public void waitForUploadComplete(String successIndicatorXpath) {
		log("Waiting for upload completion: " + successIndicatorXpath);
		try {
			waitActions.waitForElementVisible(successIndicatorXpath);
			log("✓ File upload completed successfully");
		} catch (Exception e) {
			throw new RuntimeException("File upload did not complete or success indicator not found", e);
		}
	}

	/**
	 * Wait for upload progress bar to disappear
	 */
	public void waitForUploadProgressComplete(String progressBarXpath) {
		log("Waiting for upload progress: " + progressBarXpath);
		try {
			waitActions.waitForElementToDisappear(progressBarXpath);
			log("✓ Upload progress completed");
		} catch (Exception e) {
			System.out.println("Warning: Progress bar check timeout or not found");
		}
	}

	/**
	 * Download file by clicking download link/button
	 */
	public void downloadFile(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Downloading file via: " + xpath);
		try {
			WebElement downloadButton = waitActions.waitForElementClickable(xpath);
			downloadButton.click();

			// Wait a bit for download to start
			waitActions.waitFor(2000);

			log("✓ Download initiated");
		} catch (Exception e) {
			throw new RuntimeException("Failed to download file", e);
		}
	}

	/**
	 * Verify file was downloaded (checks default download directory)
	 */
	public boolean isFileDownloaded(String fileName, long timeoutSeconds) {
		log("Verifying download: " + fileName);
		String downloadPath = System.getProperty("user.home") + "/Downloads/";
		File file = new File(downloadPath + fileName);

		long startTime = System.currentTimeMillis();
		long timeout = timeoutSeconds * 1000;

		while (System.currentTimeMillis() - startTime < timeout) {
			if (file.exists() && file.length() > 0) {
				log("✓ File downloaded successfully: " + fileName);
				return true;
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		log("✗ File download check timed out");
		return false;
	}

	/**
	 * Clear uploaded file (if clearable)
	 */
	public void clearUploadedFile(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Clearing uploaded file: " + xpath);
		try {
			WebElement fileInput = driver.findElement(By.xpath(xpath));
			fileInput.clear();
			log("✓ Uploaded file cleared");
		} catch (Exception e) {
			System.out.println("Warning: Could not clear file input - " + e.getMessage());
		}
	}

	/**
	 * Get uploaded file name
	 */
	public String getUploadedFileName(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Getting uploaded file name: " + xpath);
		try {
			WebElement fileInput = driver.findElement(By.xpath(xpath));
			String fileName = fileInput.getAttribute("value");
			if (fileName != null && !fileName.isEmpty()) {
				// Extract just the file name from full path
				if (fileName.contains("\\")) {
					fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
				} else if (fileName.contains("/")) {
					fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
				}
			}
			return fileName;
		} catch (Exception e) {
			throw new RuntimeException("Failed to get uploaded file name: " + xpath, e);
		}
	}

	/**
	 * Verify file input is empty
	 */
	public boolean isFileInputEmpty(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		try {
			WebElement fileInput = driver.findElement(By.xpath(xpath));
			String value = fileInput.getAttribute("value");
			return value == null || value.trim().isEmpty();
		} catch (Exception e) {
			return true;
		}
	}

	/**
	 * Upload file using Java Robot class (Native System Dialog)
	 * Useful when standard sendKeys() doesn't work
	 */
	public void uploadFileWithRobot(String filePath) {
		log("Uploading file using Robot: " + filePath);
		try {
			// Verify file exists
			File file = new File(filePath);
			if (!file.exists()) {
				throw new RuntimeException("File not found: " + filePath);
			}

			// Copy file path to clipboard
			String absolutePath = file.getAbsolutePath();
			StringSelection stringSelection = new StringSelection(absolutePath);
			java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);

			// Initialize Robot
			Robot robot = new Robot();
			robot.delay(1500); // Reduced from 4000ms to 1500ms for faster execution

			// Press Ctrl+V
			robot.keyPress(KeyEvent.VK_CONTROL);
			robot.keyPress(KeyEvent.VK_V);
			robot.delay(200);
			robot.keyRelease(KeyEvent.VK_V);
			robot.keyRelease(KeyEvent.VK_CONTROL);

			robot.delay(500);

			// Press Enter to confirm
			robot.keyPress(KeyEvent.VK_ENTER);
			robot.delay(200);
			robot.keyRelease(KeyEvent.VK_ENTER);

			log("✓ File path pasted and confirmed via Robot");

		} catch (Exception e) {
			throw new RuntimeException("Failed to upload file with Robot: " + filePath, e);
		}
	}

	/**
	 * Wait for file download to complete
	 */
	public boolean waitForDownloadComplete(String fileName, long timeoutSeconds) {
		return isFileDownloaded(fileName, timeoutSeconds);
	}

	/**
	 * Delete downloaded file
	 */
	public boolean deleteDownloadedFile(String fileName) {
		log("Deleting downloaded file: " + fileName);
		try {
			String downloadPath = System.getProperty("user.home") + "/Downloads/";
			File file = new File(downloadPath + fileName);
			if (file.exists()) {
				boolean deleted = file.delete();
				if (deleted) {
					log("✓ Downloaded file deleted: " + fileName);
				}
				return deleted;
			}
			return false;
		} catch (Exception e) {
			System.out.println("Warning: Could not delete file - " + e.getMessage());
			return false;
		}
	}

	/**
	 * Check if file exists in downloads folder
	 */
	public boolean fileExistsInDownloads(String fileName) {
		String downloadPath = System.getProperty("user.home") + "/Downloads/";
		File file = new File(downloadPath + fileName);
		return file.exists();
	}

	/**
	 * Get file size in downloads folder
	 */
	public long getDownloadedFileSize(String fileName) {
		String downloadPath = System.getProperty("user.home") + "/Downloads/";
		File file = new File(downloadPath + fileName);
		if (file.exists()) {
			return file.length();
		}
		return 0;
	}

	/**
	 * Upload multiple files (for multi-file upload inputs)
	 */
	public void uploadMultipleFiles(String[] filePaths, String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Uploading multiple files");
		try {
			StringBuilder allPaths = new StringBuilder();
			for (int i = 0; i < filePaths.length; i++) {
				File file = new File(filePaths[i]);
				if (!file.exists()) {
					throw new RuntimeException("File not found: " + filePaths[i]);
				}
				allPaths.append(file.getAbsolutePath());
				if (i < filePaths.length - 1) {
					allPaths.append("\n"); // Separate multiple files with newline
				}
			}

			WebElement fileInput = waitActions.waitForElementPresent(xpath);
			fileInput.sendKeys(allPaths.toString());

			log("✓ Multiple files uploaded: " + filePaths.length + " files");
		} catch (Exception e) {
			throw new RuntimeException("Failed to upload multiple files", e);
		}
	}

	private void log(String message) {
		if (detailedLogging) {
			System.out.println("[FILE] " + message);
		}
	}
}