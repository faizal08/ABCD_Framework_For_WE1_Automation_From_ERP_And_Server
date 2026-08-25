package com.eit.automation.utils;

import com.eit.automation.core.ScreenshotHelper;
import com.eit.automation.parser.TestStep;
import lombok.Setter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Generates incremental HTML test execution reports with screenshots and videos.
 * Aligned with Option C: Local fast writes during execution, Post-Suite Batch Upload via Cloudinary.
 * Package: com.eit.automation.utils
 */
public class ReportGenerator {

	private String reportDir;
	private String screenshotDir;
	private String reportFilePath;
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat timeFormat;
	private Date startTime;
	private Date endTime;

	private int totalTests = 0;
	private int passedTests = 0;
	private int failedTests = 0;

	@Setter
	private String excelFileName = "Unknown File";

	private List<TestCaseResult> testResults;
	private TestCaseResult currentTestCase;

	private boolean autoUpdateEnabled = true;

	public ReportGenerator() {
		this(true);
	}

	public ReportGenerator(boolean autoUpdate) {
		this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		this.timeFormat = new SimpleDateFormat("HH:mm:ss");
		this.testResults = new ArrayList<>();
		this.autoUpdateEnabled = autoUpdate;
		initializeReport();
	}

	/**
	 * Initialize report directories and files
	 */
	private void initializeReport() {
		try {
			String timestamp = dateFormat.format(new Date());

			reportDir = "test-reports/report_" + timestamp;
			screenshotDir = reportDir + "/screenshots";

			Files.createDirectories(Paths.get(screenshotDir));

			reportFilePath = reportDir + "/TestReport.html";

			System.out.println("📊 Report initialized at: " + reportFilePath);

		} catch (IOException e) {
			System.err.println("❌ Failed to initialize report: " + e.getMessage());
		}
	}

	/**
	 * Start test execution
	 */
	public void startTestExecution() {
		startTime = new Date();
		System.out.println("🚀 Test execution started at: " + dateFormat.format(startTime));

		if (autoUpdateEnabled) {
			generateReport();
		}
	}

	/**
	 * End test execution
	 */
	public void endTestExecution() {
		endTime = new Date();
		System.out.println("🏁 Test execution ended at: " + dateFormat.format(endTime));

		generateReport();
	}

	/**
	 * Start a new test case
	 */
	public void startTestCase(String testCaseName) {
		totalTests++;
		currentTestCase = new TestCaseResult(testCaseName);
		currentTestCase.startTime = new Date();
		System.out.println("\n📝 Starting Test Case: " + testCaseName);
	}

	/**
	 * End current test case and auto-update report
	 */
	public void endTestCase(boolean passed) {
		if (currentTestCase != null) {
			currentTestCase.endTime = new Date();
			currentTestCase.passed = passed;

			if (passed) {
				passedTests++;
				currentTestCase.status = "PASSED";
				System.out.println("✅ Test Case PASSED: " + currentTestCase.name);
			} else {
				failedTests++;
				currentTestCase.status = "FAILED";
				System.out.println("❌ Test Case FAILED: " + currentTestCase.name);
			}

			testResults.add(currentTestCase);
			currentTestCase = null;

			if (autoUpdateEnabled) {
				generateReport();
				System.out.println("📊 Report updated: " + reportFilePath);
			}
		}
	}

	public void logStep(int stepNumber, TestStep step, String status, String message, WebDriver driver, String screenshotFileName) {
		if (currentTestCase == null) {
			return;
		}

		StepResult stepResult = new StepResult();
		stepResult.lineNumber = step.getLineNumber();
		stepResult.stepNumber = stepNumber;
		stepResult.action = step.getAction();
		stepResult.value = step.getValue();
		stepResult.xpath = step.getXpath();
		stepResult.status = status;
		stepResult.message = message;
		stepResult.timestamp = timeFormat.format(new Date());
		stepResult.description = step.getOriginalSentence();

		// Handle Screenshot Registration & Extension Matching (.png -> .jpg)
		if (screenshotFileName != null && !screenshotFileName.trim().isEmpty()) {

			// 1. Force filename extension to .jpg to match ScreenshotHelper's JPEG compression output
			String cleanJpgName = screenshotFileName;
			if (cleanJpgName.toLowerCase().endsWith(".png")) {
				cleanJpgName = cleanJpgName.substring(0, cleanJpgName.length() - 4) + ".jpg";
			} else if (!cleanJpgName.toLowerCase().endsWith(".jpg") && !cleanJpgName.toLowerCase().endsWith(".jpeg")) {
				cleanJpgName = cleanJpgName + ".jpg";
			}

			String relativePath = cleanJpgName.contains("/") || cleanJpgName.contains("\\")
					? cleanJpgName
					: "screenshots/" + cleanJpgName;

			File fileOnDisk = new File(reportDir + File.separator + relativePath);
			if (!fileOnDisk.exists()) {
				fileOnDisk = new File(reportDir + File.separator + cleanJpgName);
			}

			if (fileOnDisk.exists()) {
				ScreenshotHelper.registerArtifact(screenshotFileName, fileOnDisk.getAbsolutePath());
				ScreenshotHelper.registerArtifact(cleanJpgName, fileOnDisk.getAbsolutePath());
				ScreenshotHelper.registerArtifact(relativePath, fileOnDisk.getAbsolutePath());
			}

			stepResult.screenshotPath = relativePath;
		} else if (driver != null) {
			// Fallback capture
			stepResult.screenshotPath = captureScreenshot(driver,
					currentTestCase.name + "_step_" + stepNumber + "_" + status);
		}

		currentTestCase.steps.add(stepResult);

		String emoji = status.equals("PASSED") ? "✅" : status.equals("FAILED") ? "❌" : "ℹ️";
		System.out.println("  " + emoji + " Step " + stepNumber + ": " + step.getAction() + " - " + status);
	}

	/**
	 * Captures screenshot locally on disk (~3-5ms) and registers it for Post-Suite Batch Upload.
	 * ZERO external network overhead during test step execution.
	 */
	private String captureScreenshot(WebDriver driver, String screenshotName) {
		try {
			TakesScreenshot ts = (TakesScreenshot) driver;
			File srcFile = ts.getScreenshotAs(OutputType.FILE);

			String timestamp = new SimpleDateFormat("HHmmss").format(new Date());
			String fileName = screenshotName.replaceAll("[^a-zA-Z0-9_-]", "_") + "_" + timestamp + ".png";
			String destPath = screenshotDir + "/" + fileName;

			File localFile = new File(destPath);
			Files.copy(srcFile.toPath(), localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

			String absolutePath = localFile.getAbsolutePath();
			String relativePath = "screenshots/" + fileName;

			// Register both key patterns for Option C Post-Suite Batch Upload
			ScreenshotHelper.registerArtifact(fileName, absolutePath);
			ScreenshotHelper.registerArtifact(relativePath, absolutePath);

			// Return relative local path for initial local HTML rendering
			return relativePath;

		} catch (Exception e) {
			System.err.println("⚠️ Failed to capture screenshot: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Returns the current report directory path
	 */
	public String getReportDir() {
		return this.reportDir;
	}

	public void addVideoToTestCase(String videoPathOrUrl) {
		if (videoPathOrUrl != null && !videoPathOrUrl.startsWith("http")) {
			File videoFile = new File(videoPathOrUrl);

			// Convert absolute path to simple relative filename if stored in reportDir
			String relativeVideoPath = videoFile.getName();

			if (videoFile.exists()) {
				ScreenshotHelper.registerArtifact(videoPathOrUrl, videoFile.getAbsolutePath());
				ScreenshotHelper.registerArtifact(relativeVideoPath, videoFile.getAbsolutePath());
			}

			videoPathOrUrl = relativeVideoPath;
		}

		if (currentTestCase != null) {
			currentTestCase.videoPath = videoPathOrUrl;
		} else if (!testResults.isEmpty()) {
			testResults.get(testResults.size() - 1).videoPath = videoPathOrUrl;
		}

		if (autoUpdateEnabled) {
			generateReport();
		}
	}
	/**
	 * POST-SUITE HOOK (OPTION C)
	 * Replaces local screenshot and video paths in test results with Cloudinary URLs after batch upload.
	 *
	 * @param uploadedUrls Map of Key (Local Path / Filename) -> Cloudinary Hosted URL
	 */
	public void updateMediaPaths(Map<String, String> uploadedUrls) {
		if (uploadedUrls == null || uploadedUrls.isEmpty()) {
			return;
		}

		for (TestCaseResult tcr : testResults) {
			// Update Video Path if uploaded
			if (tcr.videoPath != null) {
				String cloudUrl = matchUploadedUrl(tcr.videoPath, uploadedUrls);
				if (cloudUrl != null) {
					tcr.videoPath = cloudUrl;
				}
			}

			// Update Step Screenshots if uploaded
			for (StepResult step : tcr.steps) {
				if (step.screenshotPath != null) {
					String cloudUrl = matchUploadedUrl(step.screenshotPath, uploadedUrls);
					if (cloudUrl != null) {
						step.screenshotPath = cloudUrl;
					}
				}
			}
		}

		System.out.println("🔄 Updated report with " + uploadedUrls.size() + " Cloudinary URLs.");
		generateReport(); // Regenerate final report with cloud links
	}

	/**
	 * Helper to match media paths against Cloudinary keys flexibly
	 */
	private String matchUploadedUrl(String localPath, Map<String, String> uploadedUrls) {
		if (uploadedUrls.containsKey(localPath)) {
			return uploadedUrls.get(localPath);
		}
		String fileName = new File(localPath).getName();
		if (uploadedUrls.containsKey(fileName)) {
			return uploadedUrls.get(fileName);
		}
		for (Map.Entry<String, String> entry : uploadedUrls.entrySet()) {
			if (entry.getKey().endsWith(fileName) || localPath.endsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * Generate/Update HTML report
	 */
	public synchronized void generateReport() {
		try (FileWriter writer = new FileWriter(reportFilePath)) {

			writeHtmlHeader(writer);
			writeExecutionSummary(writer);
			writeTestCaseResults(writer);
			writeHtmlFooter(writer);

		} catch (IOException e) {
			System.err.println("❌ Failed to generate report: " + e.getMessage());
		}
	}

	/**
	 * Write HTML header
	 */
	private void writeHtmlHeader(FileWriter writer) throws IOException {
		writer.write("<!DOCTYPE html>\n");
		writer.write("<html lang='en'>\n");
		writer.write("<head>\n");
		writer.write("    <meta charset='UTF-8'>\n");
		writer.write("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
		writer.write("    <title>Test Execution Report - Live</title>\n");
		writer.write("    <style>\n");
		writer.write(getCSS());
		writer.write("    </style>\n");
		writer.write("</head>\n");
		writer.write("<body>\n");
		writer.write("    <div class='container'>\n");
		writer.write("        <h1>🧪 Test Execution Report - Live Updates</h1>\n");

		String statusBadge = (endTime == null)
				? "<span class='live-badge'>🔴 RUNNING</span>"
				: "<span class='completed-badge'>✅ COMPLETED</span>";

		writer.write("        <div class='status-badge'>" + statusBadge + "</div>\n");
	}

	/**
	 * Write execution summary
	 */
	private void writeExecutionSummary(FileWriter writer) throws IOException {
		SimpleDateFormat datePart = new SimpleDateFormat("dd MMM yyyy");
		SimpleDateFormat timePart = new SimpleDateFormat("hh:mm:ss a");

		long duration = 0;
		if (endTime != null) {
			duration = (endTime.getTime() - startTime.getTime()) / 1000;
		} else if (startTime != null) {
			duration = (new java.util.Date().getTime() - startTime.getTime()) / 1000;
		}

		double successRate = totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0;

		writer.write("        <div class='summary'>\n");
		writer.write("            <h2 style='margin-bottom: 20px;'>📊 Execution Summary</h2>\n");
		writer.write("            <div class='summary-grid' style='display: flex; flex-wrap: nowrap; gap: 10px; justify-content: space-between;'>\n");

		// START TIME
		writer.write("                <div class='summary-item' style='flex: 1; min-width: 0;'>\n");
		writer.write("                    <div class='summary-label'>Start Time</div>\n");
		writer.write("                    <div class='summary-value' style='white-space: nowrap; font-size: 0.95rem;'>" + (startTime != null ? datePart.format(startTime) : "-") + "</div>\n");
		writer.write("                    <div class='time-subtext' style='white-space: nowrap; font-size: 0.8rem;'>" + (startTime != null ? timePart.format(startTime) : "-") + "</div>\n");
		writer.write("                </div>\n");

		// END TIME / STATUS
		if (endTime != null) {
			writer.write("                <div class='summary-item' style='flex: 1; min-width: 0;'>\n");
			writer.write("                    <div class='summary-label'>End Time</div>\n");
			writer.write("                    <div class='summary-value' style='white-space: nowrap; font-size: 0.95rem;'>" + datePart.format(endTime) + "</div>\n");
			writer.write("                    <div class='time-subtext' style='white-space: nowrap; font-size: 0.8rem;'>" + timePart.format(endTime) + "</div>\n");
			writer.write("                </div>\n");
		} else {
			writer.write("                <div class='summary-item running' style='flex: 1;'>\n");
			writer.write("                    <div class='summary-label'>Status</div>\n");
			writer.write("                    <div class='summary-value'>⏳ Running</div>\n");
			writer.write("                </div>\n");
		}

		// DURATION
		writer.write("                <div class='summary-item' style='flex: 0.7;'>\n");
		writer.write("                    <div class='summary-label'>Duration</div>\n");
		writer.write("                    <div class='summary-value'>" + formatDuration(duration) + "</div>\n");
		writer.write("                </div>\n");

		// TOTAL
		writer.write("                <div class='summary-item' style='flex: 0.6;'>\n");
		writer.write("                    <div class='summary-label'>Total</div>\n");
		writer.write("                    <div class='summary-value'>" + totalTests + "</div>\n");
		writer.write("                </div>\n");

		// PASSED
		writer.write("                <div class='summary-item passed' style='flex: 0.6;'>\n");
		writer.write("                    <div class='summary-label'>Passed</div>\n");
		writer.write("                    <div class='summary-value'>" + passedTests + "</div>\n");
		writer.write("                </div>\n");

		// FAILED
		writer.write("                <div class='summary-item failed' style='flex: 0.6;'>\n");
		writer.write("                    <div class='summary-label'>Failed</div>\n");
		writer.write("                    <div class='summary-value'>" + failedTests + "</div>\n");
		writer.write("                </div>\n");

		// SUCCESS RATE
		writer.write("                <div class='summary-item' style='flex: 1;'>\n");
		writer.write("                    <div class='summary-label'>Success</div>\n");
		writer.write("                    <div class='summary-value' style='color: " + (successRate == 100 ? "#28a745" : "#dc3545") + "; white-space: nowrap;'>" + String.format("%.2f%%", successRate) + "</div>\n");
		writer.write("                </div>\n");

		writer.write("            </div>\n");
		writer.write("        </div>\n");
	}

	/**
	 * Write test case results
	 */
	private void writeTestCaseResults(FileWriter writer) throws IOException {
		writer.write("        <div class='test-cases'>\n");
		writer.write("            <h2>🧪 Test Cases (" + escapeHtml(excelFileName) + ") (" + testResults.size() + " / " + totalTests + ")</h2>\n");

		for (int i = 0; i < testResults.size(); i++) {
			TestCaseResult tcr = testResults.get(i);

			long tcDuration = 0;
			if (tcr.endTime != null && tcr.startTime != null) {
				tcDuration = (tcr.endTime.getTime() - tcr.startTime.getTime()) / 1000;
			}

			String statusClass = tcr.passed ? "passed" : "failed";
			String statusIcon = tcr.passed ? "✅" : "❌";

			writer.write("            <div class='test-case " + statusClass + "'>\n");
			writer.write("                <div class='test-case-header' onclick='toggleSteps(" + i + ")'>\n");
			writer.write("                    <div class='test-case-title'>\n");
			writer.write("                        <span class='status-icon'>" + statusIcon + "</span>\n");
			writer.write("                        <span class='test-name'>" + escapeHtml(tcr.name) + "</span>\n");

			// VIDEO LINK INTEGRATION
			if (tcr.videoPath != null) {
				writer.write("    <a href='" + tcr.videoPath + "' target='_blank' onclick='event.stopPropagation();' " +
						"style='margin-left:15px; text-decoration:none; background:#f39c12; color:white; " +
						"padding:2px 10px; border-radius:15px; font-size:12px; font-weight:bold;'>🎥 Watch Video</a>\n");
			}

			writer.write("                        <span class='test-status " + statusClass + "'>" + tcr.status + "</span>\n");
			writer.write("                    </div>\n");

			writer.write("                    <div class='test-case-info'>\n");
			writer.write("                        <span>Steps: " + tcr.steps.size() + "</span>\n");
			writer.write("                        <span>Duration: " + formatDuration(tcDuration) + "</span>\n");
			writer.write("                        <span class='toggle-icon' id='toggle-" + i + "'>▼</span>\n");
			writer.write("                    </div>\n");
			writer.write("                </div>\n");

			// Steps table
			writer.write("                <div class='test-steps' id='steps-" + i + "'>\n");
			writer.write("                    <table class='steps-table'>\n");
			writer.write("                        <thead>\n");
			writer.write("                            <tr>\n");
			writer.write("                                <th>LineNumber</th>\n");
			writer.write("                                <th>Step</th>\n");
			writer.write("                                <th>Original Description</th>\n");
			writer.write("                                <th>Action</th>\n");
			writer.write("                                <th>Value</th>\n");
			writer.write("                                <th>XPath</th>\n");
			writer.write("                                <th>Status</th>\n");
			writer.write("                                <th>Time</th>\n");
			writer.write("                                <th>Screenshot</th>\n");
			writer.write("                            </tr>\n");
			writer.write("                        </thead>\n");
			writer.write("                        <tbody>\n");

			for (StepResult step : tcr.steps) {
				String stepStatusClass = step.status.equals("PASSED") ? "step-passed"
						: step.status.equals("FAILED") ? "step-failed" : "step-info";
				String stepIcon = step.status.equals("PASSED") ? "✅" : step.status.equals("FAILED") ? "❌" : "ℹ️";

				writer.write("                            <tr class='" + stepStatusClass + "'>\n");
				writer.write("                                <td>" + step.lineNumber + "</td>\n");
				writer.write("                                <td>" + step.stepNumber + "</td>\n");
				writer.write("                                <td class='description'>" + escapeHtml(step.description) + "</td>\n");
				writer.write("                                <td>" + escapeHtml(step.action) + "</td>\n");
				writer.write("                                <td>" + escapeHtml(step.value) + "</td>\n");
				writer.write("                                <td class='xpath'>" + escapeHtml(step.xpath) + "</td>\n");
				writer.write("                                <td><span class='step-status'>" + stepIcon + " " + step.status + "</span></td>\n");
				writer.write("                                <td>" + step.timestamp + "</td>\n");
				writer.write("                                <td>\n");

				if (step.screenshotPath != null) {
					writer.write("                                    <a href='" + step.screenshotPath + "' target='_blank' class='screenshot-link'>\n");
					writer.write("                                        <img src='" + step.screenshotPath + "' alt='Screenshot' class='thumbnail'>\n");
					writer.write("                                    </a>\n");
				}

				writer.write("                                </td>\n");
				writer.write("                            </tr>\n");

				if (step.message != null && !step.message.isEmpty()) {
					writer.write("                            <tr>\n");
					writer.write("                                <td colspan='9'><div class='error-details'>" + escapeHtml(step.message) + "</div></td>\n");
					writer.write("                            </tr>\n");
				}
			}

			writer.write("                        </tbody>\n");
			writer.write("                    </table>\n");
			writer.write("                </div>\n");
			writer.write("            </div>\n");
		}

		writer.write("        </div>\n");
	}

	private void writeHtmlFooter(FileWriter writer) throws IOException {
		writer.write("    </div>\n");
		writer.write("    <script>\n");
		writer.write(getJavaScript());
		writer.write("    </script>\n");
		writer.write("</body>\n");
		writer.write("</html>\n");
	}

	/**
	 * Get CSS styles
	 */
	private String getCSS() {
		return """
                 * {
                     margin: 0;
                     padding: 0;
                     box-sizing: border-box;
                 }

                 body {
                     font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                     background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                     padding: 20px;
                     color: #333;
                 }

                 .container {
                     max-width: 1400px;
                     margin: 0 auto;
                     background: white;
                     border-radius: 15px;
                     box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                     padding: 30px;
                 }

                 h1 {
                     color: #667eea;
                     margin-bottom: 20px;
                     font-size: 2.5em;
                     text-align: center;
                     text-shadow: 2px 2px 4px rgba(0,0,0,0.1);
                 }

                 .status-badge {
                     text-align: center;
                     margin-bottom: 30px;
                 }

                 .live-badge {
                     display: inline-block;
                     padding: 10px 25px;
                     background: linear-gradient(45deg, #ff6b6b, #ee5a6f);
                     color: white;
                     border-radius: 25px;
                     font-weight: bold;
                     font-size: 1.1em;
                     animation: pulse 2s infinite;
                     box-shadow: 0 5px 15px rgba(255, 107, 107, 0.4);
                 }

                 .completed-badge {
                     display: inline-block;
                     padding: 10px 25px;
                     background: linear-gradient(45deg, #4CAF50, #45a049);
                     color: white;
                     border-radius: 25px;
                     font-weight: bold;
                     font-size: 1.1em;
                     box-shadow: 0 5px 15px rgba(76, 175, 80, 0.4);
                 }

                 @keyframes pulse {
                     0%, 100% { opacity: 1; }
                     50% { opacity: 0.7; }
                 }

                 h2 {
                     color: #555;
                     margin-bottom: 20px;
                     font-size: 1.8em;
                     border-bottom: 3px solid #667eea;
                     padding-bottom: 10px;
                 }

                 .summary {
                     background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                     padding: 25px;
                     border-radius: 10px;
                     margin-bottom: 30px;
                     box-shadow: 0 5px 15px rgba(0,0,0,0.1);
                 }

                 .summary-grid {
                     display: flex;
                     flex-direction: row;
                     align-items: stretch;
                     width: 100%;
                 }

                 .summary-item {
                     background: #fff;
                     padding: 10px 5px;
                     border-radius: 6px;
                     box-shadow: 0 2px 4px rgba(0,0,0,0.05);
                     text-align: center;
                     border: 1px solid #eee;
                 }

                 .summary-item:hover {
                     transform: translateY(-5px);
                 }

                 .summary-item.passed {
                     border-left: 5px solid #4CAF50;
                 }

                 .summary-item.failed {
                     border-left: 5px solid #f44336;
                 }

                 .summary-item.running {
                     border-left: 5px solid #ff9800;
                     animation: pulse 2s infinite;
                 }

                 .summary-label {
                     font-size: 0.9em;
                     color: #777;
                     margin-bottom: 10px;
                     font-weight: 600;
                     text-transform: uppercase;
                 }

                 .summary-value {
                     display: block;
                     font-weight: 700;
                     white-space: nowrap;
                     overflow: hidden;
                 }

                 .time-subtext {
                     color: #888;
                     margin-top: 2px;
                 }

                 .test-cases {
                     margin-top: 30px;
                 }

                 .test-case {
                     background: white;
                     border: 2px solid #e0e0e0;
                     border-radius: 10px;
                     margin-bottom: 20px;
                     overflow: hidden;
                     transition: all 0.3s;
                 }

                 .test-case:hover {
                     box-shadow: 0 5px 20px rgba(0,0,0,0.15);
                 }

                 .test-case.passed {
                     border-left: 5px solid #4CAF50;
                 }

                 .test-case.failed {
                     border-left: 5px solid #f44336;
                 }

                 .test-case-header {
                     padding: 20px;
                     cursor: pointer;
                     background: #f9f9f9;
                     transition: background 0.3s;
                 }

                 .test-case-header:hover {
                     background: #f0f0f0;
                 }

                 .test-case-title {
                     display: flex;
                     align-items: center;
                     gap: 15px;
                     margin-bottom: 10px;
                 }

                 .status-icon {
                     font-size: 1.5em;
                 }

                 .test-name {
                     font-size: 1.3em;
                     font-weight: bold;
                     flex: 1;
                     color: #333;
                 }

                 .test-status {
                     padding: 5px 15px;
                     border-radius: 20px;
                     font-size: 0.9em;
                     font-weight: bold;
                 }

                 .test-status.passed {
                     background: #4CAF50;
                     color: white;
                 }

                 .test-status.failed {
                     background: #f44336;
                     color: white;
                 }

                 .test-case-info {
                     display: flex;
                     gap: 20px;
                     color: #666;
                     font-size: 0.95em;
                 }

                 .toggle-icon {
                     margin-left: auto;
                     transition: transform 0.3s;
                 }

                 .toggle-icon.rotated {
                     transform: rotate(180deg);
                 }

                 .test-steps {
                     padding: 20px;
                     background: #fafafa;
                     display: none;
                     overflow-x: auto;
                 }

                 .steps-table {
                     width: 100%;
                     border-collapse: collapse;
                     background: white;
                     box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                 }

                 .steps-table th {
                     background: #667eea;
                     color: white;
                     padding: 12px;
                     text-align: left;
                     font-weight: 600;
                 }

                 .steps-table td {
                     padding: 12px;
                     border-bottom: 1px solid #e0e0e0;
                     vertical-align: middle;
                 }

                 .steps-table tr:hover {
                     background: #f5f5f5;
                 }

                 .step-passed {
                     background: #f1f8f4;
                 }

                 .step-failed {
                     background: #fef1f1;
                 }

                 .step-info {
                     background: #f8f9fa;
                 }

                 .step-status {
                     padding: 3px 10px;
                     border-radius: 15px;
                     font-size: 0.85em;
                     font-weight: bold;
                     white-space: nowrap;
                 }

                 .xpath {
                     font-family: 'Courier New', monospace;
                     font-size: 0.85em;
                     color: #666;
                     word-wrap: break-word;
                     overflow-wrap: break-word;
                     white-space: normal;
                     min-width: 200px;
                 }

                 .description {
                     min-width: 250px;
                     font-size: 0.9em;
                     color: #444;
                 }

                 .error-details {
                     background: #fff0f0;
                     color: #d9534f;
                     font-family: 'Consolas', 'Monaco', monospace;
                     font-size: 0.9em;
                     white-space: pre-wrap;
                     padding: 10px;
                     border-left: 4px solid #d9534f;
                     margin-top: 5px;
                     display: block;
                 }

                 .screenshot-link {
                     display: inline-block;
                     text-decoration: none;
                 }

                 .thumbnail {
                     width: 80px;
                     height: 60px;
                     object-fit: cover;
                     border-radius: 5px;
                     border: 2px solid #ddd;
                     transition: transform 0.3s;
                     cursor: pointer;
                 }

                 .thumbnail:hover {
                     transform: scale(2.5);
                     z-index: 1000;
                     box-shadow: 0 5px 20px rgba(0,0,0,0.3);
                 }
                 """;
	}

	/**
	 * Get JavaScript
	 */
	private String getJavaScript() {
		return """
             function toggleSteps(index) {
                 const stepsDiv = document.getElementById('steps-' + index);
                 const toggleIcon = document.getElementById('toggle-' + index);

                 if (stepsDiv.style.display === 'none' || stepsDiv.style.display === '') {
                     stepsDiv.style.display = 'block';
                     toggleIcon.classList.add('rotated');
                 } else {
                     stepsDiv.style.display = 'none';
                     toggleIcon.classList.remove('rotated');
                 }
             }

             window.addEventListener('load', function() {
                 const testCases = document.querySelectorAll('.test-case');
                 if (testCases.length > 0) {
                     const lastTestCase = testCases[testCases.length - 1];
                     lastTestCase.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                 }
             });
             """;
	}

	/**
	 * Format duration
	 */
	private String formatDuration(long seconds) {
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long secs = seconds % 60;

		if (hours > 0) {
			return String.format("%dh %dm %ds", hours, minutes, secs);
		} else if (minutes > 0) {
			return String.format("%dm %ds", minutes, secs);
		} else {
			return String.format("%ds", secs);
		}
	}

	/**
	 * Escape HTML
	 */
	private String escapeHtml(String text) {
		if (text == null)
			return "";
		return text.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	/**
	 * Truncate text
	 */
	private String truncate(String text, int length) {
		if (text == null)
			return "";
		if (text.length() <= length)
			return text;
		return text.substring(0, length) + "...";
	}

	/**
	 * Getter for ERP result synchronization
	 */


	public List<TestCaseResult> getTestResults() {
		return this.testResults;
	}

	public String getReportPath() {
		return reportFilePath;
	}

	public void setReportFilePath(String reportFilePath) {
		this.reportFilePath = reportFilePath;
	}

	/**
	 * Starts tracking a new Test Case result.
	 */


	// ==========================================
	// Inner Classes
	// ==========================================

	public static class TestCaseResult {
		private String name;
		private String status;
		private String userStory;      // Holds the Backlog ID (e.g., US-2026-00855)
		private String backlogSubject; // Holds the Backlog Subject (e.g., UserDriverWebIntialilzer)
		private boolean passed;
		private Date startTime;
		private Date endTime;
		private List<StepResult> steps = new ArrayList<>();
		private String videoPath;

		public TestCaseResult(String name) {
			this.name = name;
		}

		// --- Backlog & User Story Getters/Setters ---
		public String getUserStory() {
			return userStory;
		}

		public void setUserStory(String userStory) {
			this.userStory = userStory;
		}

		public String getBacklogSubject() {
			return backlogSubject;
		}

		public void setBacklogSubject(String backlogSubject) {
			this.backlogSubject = backlogSubject;
		}

		// --- General Getters & Setters ---
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public boolean isPassed() {
			return passed;
		}

		public void setPassed(boolean passed) {
			this.passed = passed;
		}

		public Date getStartTime() {
			return startTime;
		}

		public Date getEndTime() {
			return endTime;
		}

		public void setEndTime(Date endTime) {
			this.endTime = endTime;
		}

		public List<StepResult> getSteps() {
			return steps;
		}

		public String getVideoPath() {
			return videoPath;
		}

		public void setVideoPath(String videoPath) {
			this.videoPath = videoPath;
		}

		public void addStep(StepResult step) {
			this.steps.add(step);
		}
	}

	public static class StepResult {
		private int lineNumber;
		private int stepNumber;
		private String action;
		private String value;
		private String xpath;
		private String status;
		private String message;
		private String timestamp;
		private String screenshotPath;
		private String description;

		// --- Getters & Setters ---
		public int getLineNumber() { return lineNumber; }
		public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

		public int getStepNumber() { return stepNumber; }
		public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }

		public String getAction() { return action; }
		public void setAction(String action) { this.action = action; }

		public String getValue() { return value; }
		public void setValue(String value) { this.value = value; }

		public String getXpath() { return xpath; }
		public void setXpath(String xpath) { this.xpath = xpath; }

		public String getStatus() { return status; }
		public void setStatus(String status) { this.status = status; }

		public String getMessage() { return message; }
		public void setMessage(String message) { this.message = message; }

		public String getTimestamp() { return timestamp; }
		public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

		public String getScreenshotPath() { return screenshotPath; }
		public void setScreenshotPath(String screenshotPath) { this.screenshotPath = screenshotPath; }

		public String getDescription() { return description; }
		public void setDescription(String description) { this.description = description; }
	}

	// ==========================================
// Getters for ERP Result Synchronization
// ==========================================

	public Date getStartTime() {
		return this.startTime;
	}

	public Date getEndTime() {
		return this.endTime;
	}

	public int getTotalTests() {
		return this.totalTests;
	}

	public int getPassedTests() {
		return this.passedTests;
	}

	public int getFailedTests() {
		return this.failedTests;
	}

	/**
	 * Calculates execution duration formatted as string (e.g. "1m 39s" or "42s")
	 */
	public String getFormattedDuration() {
		long durationSeconds = 0;
		if (endTime != null && startTime != null) {
			durationSeconds = (endTime.getTime() - startTime.getTime()) / 1000;
		} else if (startTime != null) {
			durationSeconds = (new Date().getTime() - startTime.getTime()) / 1000;
		}
		return formatDuration(durationSeconds);
	}

	/**
	 * Calculates success percentage formatted to 2 decimal places (e.g. "100.00%")
	 */
	public String getFormattedSuccessRate() {
		double successRate = totalTests > 0 ? (passedTests * 100.0 / totalTests) : 0.0;
		return String.format("%.2f%%", successRate);
	}
}
