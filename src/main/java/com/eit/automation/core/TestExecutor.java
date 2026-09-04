package com.eit.automation.core;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.eit.automation.parser.StepParser;
import com.eit.automation.utils.XvfbManager;
import org.openqa.selenium.*;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.interactions.Pause;

import java.util.Collections;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.HidesKeyboard;
import org.openqa.selenium.remote.DesiredCapabilities;
import java.net.URL;
import com.eit.automation.actions.MobileActions;
import java.util.ArrayList;

import com.eit.automation.actions.ClickActions;
import com.eit.automation.actions.FileActions;
import com.eit.automation.actions.InputActions;
import com.eit.automation.actions.ToastActions;
import com.eit.automation.actions.ScrollActions;
import com.eit.automation.actions.AutoItActions;
import com.eit.automation.actions.VerificationActions;
import com.eit.automation.actions.WaitActions;
import com.eit.automation.parser.TestStep;
import com.eit.automation.utils.ReportGenerator;
import com.eit.automation.utils.DatabaseUtils;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestExecutor {

	private WebDriver driver;
	private WebDriverWait wait;

	private Map<String, WebDriver> driverPool = new HashMap<>();
	private Map<String, WebDriverWait> waitPool = new HashMap<>();
	private String currentSessionRole = "web";

	private static AppiumDriverLocalService appiumService;

	private boolean isHybridFlow = false;
	private WaitActions waitActions;
	private ClickActions clickActions;
	private InputActions inputActions;
	private VerificationActions verificationActions;
	private FileActions fileActions;
	private ToastActions toastActions;
	private ScrollActions scrollActions;
	private AutoItActions autoItActions;

	private MobileActions mobileActions;
	private ActionRegistry actionRegistry;
	private PageObjectManager pageObjectManager;

	private ReportGenerator reportGenerator;
	private Properties config;
	private String excelName;

	private boolean isCleanupMode = false;
	private boolean detailedLogging = true;
	private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

	private int totalStepsExecuted = 0;
	private int passedSteps = 0;
	private int failedSteps = 0;

	private KeywordHandler keywordHandler;

	public TestExecutor() {
		log("");
		log("╔════════════════════════════════════════════════════════════════════════════════╗");
		log("║                        INITIALIZING UNIVERSAL EXECUTOR                         ║");
		log("╚════════════════════════════════════════════════════════════════════════════════╝");
	}

	public TestExecutor(ReportGenerator reportGenerator, Properties config) {
		this.reportGenerator = reportGenerator;
		this.config = config;

		String fullPath = config.getProperty("excel.name");
		this.excelName = (fullPath != null) ? fullPath.split("\\.")[0] : "Unknown_Excel";

		log("");
		log("╔════════════════════════════════════════════════════════════════════════════════╗");
		log("║                        INITIALIZING UNIVERSAL EXECUTOR                         ║");
		log("╚════════════════════════════════════════════════════════════════════════════════╝");

		log("✓ Report generator configured");
		log("");
	}

	public void setHybridFlow(boolean isHybrid) {
		this.isHybridFlow = isHybrid;
	}

	public void initializeWebDriver(String role) {
		log("→ Setting up Chrome Browser for Role: " + role.toUpperCase());

		io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();
		log("  • WebDriverManager: Synchronized ChromeDriver");

		ChromeOptions options = new ChromeOptions();

		// Security, autofill, and password manager pop-up suppressions
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("credentials_enable_service", false);
		prefs.put("profile.password_manager_enabled", false);
		prefs.put("autofill.profile_enabled", false);
		prefs.put("profile.password_manager_leak_detection", false);
		options.setExperimentalOption("prefs", prefs);

		options.addArguments("--disable-notifications");
		options.addArguments("--disable-features=SafeBrowsingPasswordCheck");
		options.setExperimentalOption("detach", true);
		options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

		// Source-based execution mode (ERP Headless vs Excel Local)
		String source = System.getProperty("source", "excel").toLowerCase();
		if ("erp".equals(source)) {
			log("🌐 [TestExecutor] ERP Execution Mode: Configuring Headless Chrome for CI/CD...");
			options.addArguments("--headless=new");
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-dev-shm-usage"); // Prevents shared memory crashes in Linux containers
			options.addArguments("--disable-gpu");
			options.addArguments("--window-size=1920,1080");
			options.addArguments("--remote-allow-origins=*");
		} else {
			log("🖥️ [TestExecutor] Excel Local Mode: Launching Interactive Browser Window...");
			options.addArguments("--start-maximized");
		}

		WebDriver newDriver = new ChromeDriver(options);
		WebDriverWait newWait = new WebDriverWait(newDriver, Duration.ofSeconds(30));

		driverPool.put(role, newDriver);
		waitPool.put(role, newWait);

		this.driver = newDriver;
		this.wait = newWait;
		this.currentSessionRole = role;

		refreshActionHandlers();
		log("✓ Web Session [" + role + "] is active and ready");
	}

	public void setupWebDriver() {
		log("🌐 Initializing Chrome Browser Framework Context...");
		initializeWebDriver("web");
		log("✓ Web Session [web] successfully registered in universal context pools.");
	}

	private void ensureAppiumServerRunning() {
		try {
			String appiumUrlStr = config.getProperty("appium.url", "http://127.0.0.1:4723/");
			URL appiumUrl = new URL(appiumUrlStr);
			int port = appiumUrl.getPort() != -1 ? appiumUrl.getPort() : 4723;
			String host = appiumUrl.getHost();

			// Check if Appium Server is already listening on the port
			try (java.net.Socket socket = new java.net.Socket(host, port)) {
				log("⚡ Appium Server is already running on port " + port);
				return;
			} catch (Exception notRunning) {
				log("🚀 Appium Server is not running. Starting programmatic Appium Service on port " + port + "...");
			}

			// Programmatically start Appium Service
			AppiumServiceBuilder builder = new AppiumServiceBuilder()
					.withIPAddress(host)
					.usingPort(port)
					.withArgument(GeneralServerFlag.RELAXED_SECURITY)
					.withTimeout(Duration.ofMinutes(2));

			appiumService = AppiumDriverLocalService.buildService(builder);
			appiumService.start();

			if (appiumService.isRunning()) {
				log("✅ Appium Server started successfully at: " + appiumService.getUrl());
			} else {
				throw new RuntimeException("❌ Failed to start programmatic Appium Server!");
			}

		} catch (Exception e) {
			log("❌ Error initializing programmatic Appium Server: " + e.getMessage());
			throw new RuntimeException("Could not launch Appium Server automatically", e);
		}
	}

	public void setupMobileDriver(String role) {
		String cleanRole = role.toLowerCase().trim();
		log("📱 Initializing Mobile Emulator for Role: [" + cleanRole.toUpperCase() + "]");

		String source = System.getProperty("source", "excel").toLowerCase();

		// -----------------------------------------------------------------
		// ERP SERVER RUN ORCHESTRATION: Mobile & Virtual Display Services
		// -----------------------------------------------------------------
		if ("erp".equals(source)) {
			log("🌐 [ERP Mode] Initializing Mobile & Virtual Display Services...");

			// 1. Force Visible property for AndroidEmulatorManager (Temporary for visual debugging)
			if (config != null) {
				config.setProperty("headless", "false");
			}

			// 2. Start Virtual X11 Display & Video Recording if running on Linux
			XvfbManager.startXvfb(":99", "1920x1080x24");
			String videoPath = "target/recordings/ERP_Execution_" + cleanRole + "_" + System.currentTimeMillis() + ".mp4";
			XvfbManager.startRecording(videoPath, "1920x1080", 24);

			// 3. Resolve AVD Name and Port dynamically from Config (with standard fallbacks)
			String avdName = config.getProperty(cleanRole + ".avd.name", "Pixel_4_API_33");

			// Extract port from targetUdid (e.g. "emulator-5554" -> 5554)
			String targetUdid = config.getProperty(cleanRole + ".device.id", "emulator-5554");
			int port = 5554;
			if (targetUdid.contains("-")) {
				try {
					port = Integer.parseInt(targetUdid.split("-")[1]);
				} catch (Exception ignored) {}
			}

			// 4. Trigger Self-Healing CLI boot (Visible UI mode)
			boolean isHealthy = AndroidEmulatorManager.ensureEmulatorHealthy(avdName, port);
			if (!isHealthy) {
				throw new RuntimeException("❌ [Fatal] Could not spin up healthy Android Emulator for AVD: " + avdName);
			}
		} else {
			log("🖥️ [Excel Local Mode] Connecting to active visual emulator session...");
		}

		// -----------------------------------------------------------------
		// PROGRAMMATIC APPIUM SERVER AUTO-START CHECK
		// -----------------------------------------------------------------
		ensureAppiumServerRunning();

		// -----------------------------------------------------------------
		// APPIUM DRIVER INITIALIZATION (Identical for ERP & Excel Modes)
		// -----------------------------------------------------------------
		try {
			io.appium.java_client.android.options.UiAutomator2Options options = new io.appium.java_client.android.options.UiAutomator2Options();

			options.setPlatformName("Android");
			options.setAutomationName("UiAutomator2");

			String targetUdid = config.getProperty(cleanRole + ".device.id");
			String targetApk = config.getProperty(cleanRole + ".apk.path");
			String targetPackage = config.getProperty(cleanRole + ".app.package");
			String targetActivity = config.getProperty(cleanRole + ".app.activity");

			if (targetUdid == null || targetApk == null) {
				throw new IllegalArgumentException("❌ Configuration properties missing for role prefix: [" + cleanRole + "]. Please verify your config.properties file entries.");
			}

			options.setUdid(targetUdid);
			options.setApp(targetApk);
			options.setAppPackage(targetPackage);
			options.setAppActivity(targetActivity);

			options.setNoReset(false);
			options.setCapability("fullReset", false);
			options.setCapability("shouldTerminateApp", false);
			options.setCapability("dontStopAppOnReset", true);
			options.setNewCommandTimeout(Duration.ofMinutes(10));
			options.setCapability("autoDismissAlerts", true);
			options.setCapability("appium:waitForIdleTimeout", 0);

			// Disable legacy IME keyboard reset to prevent session crash on wiped/fresh emulators
			options.setCapability("appium:unicodeKeyboard", false);
			options.setCapability("appium:resetKeyboard", false);

			// Allow up to 5 minutes for slow AVD boots to stabilize before timing out
			options.setCapability("appium:avdLaunchTimeout", 300000);
			options.setCapability("appium:avdReadyTimeout", 300000);

			// -----------------------------------------------------------------
			// ADB & UIAUTOMATOR2 INSTALLATION TIMEOUT FIXES
			// -----------------------------------------------------------------
			options.setCapability("appium:uiautomator2ServerInstallTimeout", 60000);
			options.setCapability("appium:adbExecTimeout", 60000);
			options.setCapability("appium:uiautomator2ServerLaunchTimeout", 60000);

			options.setCapability("skipDeviceInitialization", false);
			options.setCapability("skipServerInstallation", false);

			URL url = new URL(config.getProperty("appium.url"));

			// Configure Java HTTP Client timeouts to prevent java.net.ConnectException / ClosedChannelException
			org.openqa.selenium.remote.http.ClientConfig clientConfig = org.openqa.selenium.remote.http.ClientConfig.defaultConfig()
					.baseUrl(url)
					.readTimeout(Duration.ofMinutes(5))
					.connectionTimeout(Duration.ofMinutes(5));

			AndroidDriver mobileDriver = new AndroidDriver(clientConfig, options);

			log("⏳ Waiting for app package [" + cleanRole + "] to launch...");
			WebDriverWait launchWait = new WebDriverWait(mobileDriver, Duration.ofSeconds(30));
			launchWait.until(d -> ((AndroidDriver) d).getCurrentPackage() != null);

			driverPool.put(cleanRole, mobileDriver);
			WebDriverWait mobileWait = new WebDriverWait(mobileDriver, Duration.ofSeconds(30));
			waitPool.put(cleanRole, mobileWait);

			if (this.driver == null) {
				this.wait = mobileWait;
				this.driver = mobileDriver;
				this.currentSessionRole = cleanRole;
			}

			refreshActionHandlers();
			log("✅ Mobile session started and stabilized for role: [" + cleanRole + "] on device: " + targetUdid);

		} catch (Exception e) {
			log("❌ Failed to start Mobile session for " + cleanRole + ": " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private void refreshActionHandlers() {
		this.waitActions = new WaitActions(driver, wait);
		this.clickActions = new ClickActions(driver, wait, waitActions);
		this.inputActions = new InputActions(driver, wait, waitActions);
		this.verificationActions = new VerificationActions(driver, wait, waitActions);

		if (!(driver instanceof io.appium.java_client.AppiumDriver)) {
			this.fileActions = new FileActions(driver, wait, waitActions);
			this.toastActions = new ToastActions(driver, wait, waitActions);
			this.scrollActions = new ScrollActions(driver, wait, waitActions);
			this.autoItActions = new AutoItActions(driver, wait, waitActions);
			this.mobileActions = null;
			log("  • Handlers refreshed for WEB context");
		} else {
			this.mobileActions = new MobileActions((io.appium.java_client.AppiumDriver) driver, wait);
			this.fileActions = null;
			this.toastActions = null;
			this.scrollActions = null;
			this.autoItActions = null;
			log("  • Handlers refreshed for MOBILE context");
		}

		// Initialize the KeywordHandler instance with all active dependencies
		this.keywordHandler = new KeywordHandler(
				driver, wait, config,
				waitActions, clickActions, inputActions, verificationActions,
				fileActions, toastActions, scrollActions, autoItActions,
				mobileActions, pageObjectManager, isCleanupMode, this
		);
	}

	public boolean run(String sheetName, List<TestStep> steps, String testCaseName) {
		long testStartTime = System.currentTimeMillis();

		boolean currentSheetIsHybrid = false;
		if (steps != null) {
			for (TestStep step : steps) {
				if (step.getAction() != null) {
					String action = step.getAction().toLowerCase().trim();
					String value = step.getValue() != null ? step.getValue().toLowerCase().trim() : "";

					if (action.equals("switch_to") || action.equals("switchsession")) {
						if (!value.isEmpty() && !value.equals("web")) {
							currentSheetIsHybrid = true;
							break;
						}
					}
				}
			}
		}

		if (currentSheetIsHybrid) {
			this.isHybridFlow = true;
		}

		WebDriver webDriverInstance = driverPool.get("web");
		if (webDriverInstance != null) {
			try {
				Thread.sleep(500);

				if (this.isHybridFlow) {
					log("📱 Hybrid Flow Context Active (Locked)! Retaining split-screen layout on left side (X=0)...");
					webDriverInstance.manage().window().setSize(new org.openqa.selenium.Dimension(960, 1080));
					webDriverInstance.manage().window().setPosition(new org.openqa.selenium.Point(0, 0));
				} else {
					log("🖥️ Web-Only Flow Detected via Context Analysis! Maximizing browser workspace...");
					webDriverInstance.manage().window().maximize();
				}
			} catch (Exception e) {
				log("⚠️ Warning: Failed to apply dynamic browser window configuration changes: " + e.getMessage());
			}
		}

		totalStepsExecuted = 0;
		passedSteps = 0;
		failedSteps = 0;

		log("");
		log("╔════════════════════════════════════════════════════════════════════════════════╗");
		log("║  TEST CASE: " + padRight(testCaseName, 66) + "║");
		log("║  Total Steps: " + padRight(String.valueOf(steps != null ? steps.size() : 0), 63) + "║");
		log("║  Start Time: " + padRight(getCurrentTime(), 64) + "║");
		log("╚════════════════════════════════════════════════════════════════════════════════╝");
		log("");

		if (steps == null || steps.isEmpty()) {
			log("⚠️ No steps found to execute for test case: " + testCaseName);
			return true;
		}

		try {
			if (reportGenerator != null) {
				reportGenerator.startTestCase(testCaseName);
			}

			for (int i = 0; i < steps.size(); i++) {
				TestStep step = steps.get(i);
				int stepNumber = i + 1;
				String action = step.getAction() != null ? step.getAction().toLowerCase().trim() : "";

				String cleanTcName = testCaseName.replaceAll("[^a-zA-Z0-9]", "_");

				if (action.equals("switch_to") || action.equals("switchsession")) {
					logStepHeader(stepNumber, steps.size(), step);
					try {
						String targetRole = (step.getValue() != null) ? step.getValue().toLowerCase().trim() : "";

						if (targetRole.isEmpty()) {
							throw new IllegalArgumentException("Switch action encountered but target session role value is empty!");
						}

						switchSession(targetRole);

						this.driver = driverPool.get(targetRole);
						this.wait = waitPool.get(targetRole);

						refreshActionHandlers();

						if (this.driver instanceof io.appium.java_client.AppiumDriver) {
							log("  ⏳ Stabilizing Mobile Session Viewport...");
							Thread.sleep(3000);
						}

						passedSteps++;
						totalStepsExecuted++;

						String screenshotFileName = "SWITCH_Step" + stepNumber + "_" + cleanTcName + ".jpg";
						String screenshotFullPath = reportGenerator.getReportDir() + File.separator + "screenshots" + File.separator + screenshotFileName;

						String savedPath = null;
						if (this.driver != null) {
							savedPath = ScreenshotHelper.capture(this.driver, screenshotFullPath);
						}

						if (reportGenerator != null) {
							reportGenerator.logStep(stepNumber, step, "PASSED", "Successfully switched context to: [" + targetRole.toUpperCase() + "]", this.driver, screenshotFileName);
						}
						continue;

					} catch (Exception e) {
						failedSteps++;
						totalStepsExecuted++;
						log("❌ Session switch execution context failed: " + e.getMessage());

						String screenshotFileName = "FAIL_Switch_" + cleanTcName + "_Step" + stepNumber + ".jpg";
						String screenshotFullPath = reportGenerator.getReportDir() + File.separator + "screenshots" + File.separator + screenshotFileName;

						String savedPath = null;
						if (this.driver != null) {
							savedPath = ScreenshotHelper.capture(this.driver, screenshotFullPath);
						}

						if (reportGenerator != null) {
							reportGenerator.logStep(stepNumber, step, "FAILED", "Switch failed: " + e.getMessage(), this.driver, screenshotFileName);
						}
						break;
					}
				}

				if (driverPool.containsKey("web")) {
					updateBrowserOverlay(sheetName, testCaseName, stepNumber, step);
				}

				logStepHeader(stepNumber, steps.size(), step);
				long stepStartTime = System.currentTimeMillis();

				try {
					executeStep(step);
					passedSteps++;
					totalStepsExecuted++;
					long stepDuration = System.currentTimeMillis() - stepStartTime;
					logStepSuccess(stepNumber, stepDuration);

					String screenshotFileName = "STEP_" + stepNumber + "_" + cleanTcName + ".jpg";
					String screenshotFullPath = reportGenerator.getReportDir() + File.separator + "screenshots" + File.separator + screenshotFileName;

					String savedPath = null;
					if (this.driver != null) {
						savedPath = ScreenshotHelper.capture(this.driver, screenshotFullPath);
					}

					if (reportGenerator != null) {
						reportGenerator.logStep(stepNumber, step, "PASSED", "", this.driver, screenshotFileName);
					}

				} catch (Exception e) {
					failedSteps++;
					totalStepsExecuted++;
					long stepDuration = System.currentTimeMillis() - stepStartTime;
					logStepFailure(stepNumber, stepDuration, e);

					String screenshotFileName = "FAIL_" + cleanTcName + "_Step" + stepNumber + ".jpg";
					String screenshotFullPath = reportGenerator.getReportDir() + File.separator + "screenshots" + File.separator + screenshotFileName;

					String savedPath = null;
					if (this.driver != null) {
						savedPath = ScreenshotHelper.capture(this.driver, screenshotFullPath);
					}

					if (reportGenerator != null) {
						StringBuilder errorDetails = new StringBuilder();
						errorDetails.append("❌ MUST FIX: ").append(e.getMessage() != null ? e.getMessage() : "Unknown Error").append("\n");

						if (e.getCause() != null) {
							String causeMsg = e.getCause().getMessage();
							if (causeMsg != null) {
								int buildInfoIndex = causeMsg.indexOf("Build info:");
								if (buildInfoIndex > 0) {
									causeMsg = causeMsg.substring(0, buildInfoIndex).trim();
								}
								errorDetails.append("ℹ️ CAUSE: ").append(causeMsg).append("\n");
							}
						}
						errorDetails.append("⚠️ TYPE: ").append(e.getClass().getSimpleName());

						reportGenerator.logStep(stepNumber, step, "FAILED", errorDetails.toString(), this.driver, screenshotFileName);
					}

					log("❌ Aborting current test case due to failure...");
					log("");
					break;
				}
			}

			if (reportGenerator != null) {
				reportGenerator.endTestCase(failedSteps == 0);
			}

			long testDuration = System.currentTimeMillis() - testStartTime;
			logTestSummary(testCaseName, testDuration);

			boolean isSuccess = (failedSteps == 0);
			if (!isSuccess) {
				log("⚠️ Test failed. Purging crashed driver sessions while retaining Appium server...");
				purgeDeadSessions();
			}

			return isSuccess;

		} catch (Exception e) {
			long testDuration = System.currentTimeMillis() - testStartTime;
			logCriticalFailure(testCaseName, testDuration, e);

			if (reportGenerator != null) {
				reportGenerator.endTestCase(false);
			}

			log("❌ Critical exception encountered. Purging crashed driver sessions while retaining Appium server...");
			purgeDeadSessions();

			return false;
		}
	}

	public boolean run(List<TestStep> steps) {
		return run("Default", steps, "Unnamed Test Case");
	}

	public void purgeDeadSessions() {
		if (driverPool != null && !driverPool.isEmpty()) {
			driverPool.entrySet().removeIf(entry -> {
				try {
					WebDriver d = entry.getValue();
					if (d instanceof HasCapabilities) {
						((HasCapabilities) d).getCapabilities();
					} else if (d instanceof RemoteWebDriver) {
						((RemoteWebDriver) d).getCapabilities();
					} else {
						// Fallback check to verify session responsiveness
						d.getWindowHandle();
					}
					return false;
				} catch (Exception ex) {
					log("⚠️ Purged crashed driver session [" + entry.getKey() + "] from pool.");
					try {
						entry.getValue().quit();
					} catch (Exception ignored) {}
					return true;
				}
			});
		}
	}

	public void switchSession(String role) {
		if (role == null || role.trim().isEmpty()) {
			throw new IllegalArgumentException("❌ Cannot switch session context: Role target name is completely blank!");
		}

		String targetRole = role.toLowerCase().trim();
		log("🔄 SWITCHING CONTEXT: Moving focus to [" + targetRole.toUpperCase() + "]");

		if (!driverPool.containsKey(targetRole)) {
			log("⚠️ Session [" + targetRole + "] not found in runtime pool. Attempting auto-initialization...");

			if (targetRole.equals("web")) {
				setupWebDriver();
			} else {
				setupMobileDriver(targetRole);
			}
		}

		if (driverPool.containsKey(targetRole)) {
			this.driver = driverPool.get(targetRole);
			this.wait = waitPool.get(targetRole);
			this.currentSessionRole = targetRole;

			refreshActionHandlers();

			log("✅ Context switched successfully to [" + targetRole.toUpperCase() + "]");
		} else {
			log("❌ CRITICAL ERROR: Failed to initialize target session context [" + targetRole + "]");
			throw new RuntimeException("Could not switch to session: " + targetRole);
		}
	}

	public org.openqa.selenium.By getDynamicLocator(String targetFromExcel) {
		if (targetFromExcel == null || targetFromExcel.trim().isEmpty()) {
			return null;
		}

		String cleanTarget = targetFromExcel.trim();

		if (cleanTarget.startsWith("id=")) {
			return org.openqa.selenium.By.id(cleanTarget.replace("id=", "").trim());
		}
		else if (cleanTarget.startsWith("accessibility=")) {
			return io.appium.java_client.AppiumBy.accessibilityId(cleanTarget.replace("accessibility=", "").trim());
		}
		else if (cleanTarget.startsWith("automator=")) {
			return io.appium.java_client.AppiumBy.androidUIAutomator(cleanTarget.replace("automator=", "").trim());
		}
		else {
			return org.openqa.selenium.By.xpath(cleanTarget);
		}
	}

	private void executeStep(TestStep step) throws Exception {
		if (keywordHandler != null) {
			keywordHandler.executeStep(step);
		} else {
			throw new RuntimeException("KeywordHandler is uninitialized.");
		}
	}

	// =================================================================
	// PROGRAMMATIC APPIUM SERVICE TEARDOWN & CLEANUP
	// =================================================================

	public void close() {
		log("");
		log("╔════════════════════════════════════════════════════════════════════════════════╗");
		log("║                       CLOSING ALL ACTIVE SESSIONS                              ║");
		log("╚════════════════════════════════════════════════════════════════════════════════╝");

		if (driverPool != null && !driverPool.isEmpty()) {
			driverPool.forEach((role, sessionDriver) -> {
				try {
					if (sessionDriver != null) {
						sessionDriver.quit();
						log("✓ Session [" + role.toUpperCase() + "] closed successfully");
					}
				} catch (Exception e) {
					log("⚠ Error closing session [" + role.toUpperCase() + "]: " + e.getMessage());
				}
			});
			driverPool.clear();
			waitPool.clear();
			driver = null;
			wait = null;
		} else if (driver != null) {
			try {
				driver.quit();
				log("✓ Driver closed");
			} catch (Exception ignored) {}
		}

		// 2. Stop ERP Mode Virtual Display & Video Recording Services
		String source = System.getProperty("source", "excel").toLowerCase();
		if ("erp".equals(source)) {
			log("🌐 [ERP Mode] Cleaning up virtual display & video recording processes...");
			try {
				XvfbManager.stopRecording();
				XvfbManager.stopXvfb();
				log("✓ Xvfb display and recording services stopped successfully.");
			} catch (Exception e) {
				log("⚠ Error stopping Xvfb/Recording: " + e.getMessage());
			}
		}

		stopAppiumServer();
		log("");
	}

	public static void stopAppiumServer() {
		if (appiumService != null && appiumService.isRunning()) {
			appiumService.stop();
			System.out.println("🛑 Appium Server stopped successfully.");
		}
	}

	public WebDriver getDriver() {
		return driver;
	}

	private void log(String message) {
		if (detailedLogging) {
			System.out.println("[" + getCurrentTime() + "] " + message);
		}
	}

	private void logStepHeader(int stepNumber, int totalSteps, TestStep step) {
		log("");
		log("┌────────────────────────────────────────────────────────────────────────────────┐");
		log("│ STEP " + stepNumber + "/" + totalSteps + " │ " + step.getAction().toUpperCase()
				+ " ".repeat(Math.max(1, 68 - step.getAction().length() - String.valueOf(stepNumber).length()
				- String.valueOf(totalSteps).length()))
				+ "│");
		log("├────────────────────────────────────────────────────────────────────────────────┤");

		String value = step.getValue() != null ? step.getValue() : "";
		if (value.length() > 70)
			value = value.substring(0, 67) + "...";
		if (!value.isEmpty()) {
			log("│ Value: " + value + " ".repeat(Math.max(1, 73 - value.length())) + "│");
		}

		String xpath = step.getXpath() != null ? step.getXpath() : "";
		if (xpath.length() > 70)
			xpath = xpath.substring(0, 67) + "...";
		if (!xpath.isEmpty()) {
			log("│ Locator: " + xpath + " ".repeat(Math.max(1, 71 - xpath.length())) + "│");
		}

		log("└────────────────────────────────────────────────────────────────────────────────┘");
	}

	private void logStepSuccess(int stepNumber, long duration) {
		log("");
		log("  ✅ STEP " + stepNumber + " PASSED [" + duration + "ms]");
		log("");
	}

	private void logStepFailure(int stepNumber, long duration, Exception e) {
		log("");
		System.err.println("  ❌ STEP " + stepNumber + " FAILED [" + duration + "ms]");
		System.err.println("  ┌─ Error Details ─────────────────────────────────────────────────────────");
		System.err.println("  │ Must Fix: " + (e.getMessage() != null ? e.getMessage() : "Unknown Error"));
		if (e.getCause() != null) {
			System.err.println("  │ Cause: " + e.getCause().getMessage());
		}
		System.err.println("  └─────────────────────────────────────────────────────────────────────────");
		log("");
	}

	private void logTestSummary(String testName, long duration) {
		log("");
		log("╔════════════════════════════════════════════════════════════════════════════════╗");
		log("║                           TEST CASE SUMMARY                                    ║");
		log("╠════════════════════════════════════════════════════════════════════════════════╣");
		log("║  Test Case: " + padRight(testName, 66) + "║");
		log("║  Total Steps: " + padRight(String.valueOf(totalStepsExecuted), 63) + "║");
		log("║  Passed: " + padRight(String.valueOf(passedSteps), 68) + "║");
		log("║  Failed: " + padRight(String.valueOf(failedSteps), 68) + "║");
		log("║  Duration: " + padRight(formatDuration(duration), 66) + "║");
		log("║  End Time: " + padRight(getCurrentTime(), 66) + "║");
		log("╠════════════════════════════════════════════════════════════════════════════════╣");
		if (failedSteps == 0) {
			log("║  STATUS: ✓ ALL STEPS PASSED                                                   ║");
		} else {
			log("║  STATUS: ✗ " + failedSteps + " STEP(S) FAILED                                            ║");
		}
		log("╚════════════════════════════════════════════════════════════════════════════════╝");
		log("");
		log("→ All sessions (Web/Mobile) remain active for inspection");
		log("→ Call executor.close() to terminate all sessions");
		log("");
	}

	private void logCriticalFailure(String testName, long duration, Exception e) {
		log("");
		log("╔════════════════════════════════════════════════════════════════════════════════╗");
		log("║                         CRITICAL TEST FAILURE                                  ║");
		log("╠════════════════════════════════════════════════════════════════════════════════╣");
		log("║  Test Case: " + padRight(testName, 66) + "║");
		log("║  Duration: " + padRight(formatDuration(duration), 67) + "║");
		log("║  Error: " + padRight(e.getClass().getSimpleName(), 70) + "║");
		log("╚════════════════════════════════════════════════════════════════════════════════╝");
		System.err.println("Full Stack Trace:");
		e.printStackTrace();
	}

	private String getCurrentTime() {
		return LocalDateTime.now().format(timeFormatter);
	}

	private String formatDuration(long milliseconds) {
		long seconds = milliseconds / 1000;
		long ms = milliseconds % 1000;
		if (seconds > 60) {
			long minutes = seconds / 60;
			seconds = seconds % 60;
			return String.format("%dm %ds %dms", minutes, seconds, ms);
		} else {
			return String.format("%ds %dms", seconds, ms);
		}
	}

	public void setCleanupMode(boolean mode) {
		this.isCleanupMode = mode;
		if (keywordHandler != null) {
			keywordHandler.setCleanupMode(mode);
		}
	}

	private void showCleanupOverlay() {
		try {
			if (driver == null) return;
			org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;

			String cleanupScript =
					"var cleanOverlay = document.getElementById('cleanup-screen');" +
							"if(!cleanOverlay) {" +
							"  cleanOverlay = document.createElement('div');" +
							"  cleanOverlay.id = 'cleanup-screen';" +
							"  cleanOverlay.style.cssText = 'position:fixed; top:52px; left:0; width:100%; height:100%; " +
							"                               background:#0b0b0f; z-index:999998; " +
							"                               display:flex; flex-direction:column; align-items:center; " +
							"                               justify-content:center; color:white; " +
							"                               font-family:\"Segoe UI\", Tahoma, sans-serif;';" +
							"  cleanOverlay.innerHTML = " +
							"    '<div style=\"display:flex; flex-direction:column; align-items:center; margin-bottom:40px;\">' + " +
							"    '  <div style=\"font-weight:900; font-size:60px; letter-spacing:4px; color:#fff; margin-bottom:0;\">ABCD</div>' + " +
							"    '  <div style=\"font-size:14px; color:#00d4ff; font-weight:bold; text-transform:uppercase; letter-spacing:2px;\">Test Data Cleanup</div>' + " +
							"    '</div>' + " +
							"    '<div style=\"position:relative; margin-bottom:30px;\">' + " +
							"    '  <div style=\"font-size:100px; filter: drop-shadow(0 0 15px #00d4ff); animation: pulse 2s infinite;\">🗄️</div>' + " +
							"    '</div>' + " +
							"    '<div style=\"text-align:center; border: 1px solid #1a1a24; padding: 40px 60px; border-radius:15px; " +
							"                 background: #12121a; box-shadow: 0 15px 40px rgba(0,0,0,0.7);\">' + " +
							"    '  <h2 style=\"color:#ff4444; margin:0; font-size:18px; letter-spacing:1px; font-weight:bold;\">CLEANUP IN PROGRESS</h2>' + " +
							"    '  <div style=\"width:250px; background:#1a1a1a; height:4px; margin:25px auto; border-radius:10px; overflow:hidden;\">' + " +
							"    '    <div style=\"width:40%; background:#00d4ff; height:100%; animation: scanLine 1.5s infinite ease-in-out;\"></div>' + " +
							"    '  </div>' + " +
							"    '  <p style=\"font-size:15px; color:#999; margin:0; line-height:1.8;\">' + " +
							"    '    System is securely removing test data records from the database.<br>' + " +
							"    '    <span style=\"color:#fff; font-weight:bold; background: rgba(255,68,68,0.2); padding: 2px 6px; border-radius:3px;\">DO NOT CLOSE THE BROWSER</span><br>' + " +
							"    '    <span style=\"color:#fff; font-weight:bold; background: rgba(255,68,68,0.2); padding: 2px 6px; border-radius:3px;\">DO NOT STOP TEST EXECUTION</span>' + " +
							"    '  </p>' + " +
							"    '</div>' + " +
							"    '<style>' + " +
							"    '  @keyframes pulse { 0% { transform: scale(1); opacity: 0.8; } 50% { transform: scale(1.08); opacity: 1; } 100% { transform: scale(1); opacity: 0.8; } }' + " +
							"    '  @keyframes scanLine { 0% { width: 0%; margin-left: 0%; } 50% { width: 50%; margin-left: 25%; } 100% { width: 0%; margin-left: 100%; } }' + " +
							"    '</style>';" +
							"  document.body.appendChild(cleanOverlay);" +
							"  document.body.style.overflow = 'hidden';" +
							"}";

			js.executeScript(cleanupScript);
		} catch (Exception ignored) {}
	}

	private void updateBrowserOverlay(String sheet, String testCaseName, int stepNum, TestStep step) {
		try {
			WebDriver webDisplay = driverPool.get("web");
			if (webDisplay == null) return;

			org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) webDisplay;

			String action = step.getAction().toUpperCase();
			String roleName = "UNKNOWN";

			for (java.util.Map.Entry<String, WebDriver> entry : driverPool.entrySet()) {
				if (entry.getValue().equals(this.driver)) {
					roleName = entry.getKey().toUpperCase();
					break;
				}
			}

			String icon = (this.driver instanceof io.appium.java_client.AppiumDriver) ? "📱 " : "💻 ";
			String highlightedRole = "<span style='background:#00d4ff; color:#000; padding:2px 8px; border-radius:4px; font-weight:900; margin-right:5px; box-shadow: 0 0 5px rgba(0,212,255,0.5);'>" + roleName + "</span>";
			String platformLabel = icon + highlightedRole + ": ";

			String rawDetail = "";
			if (step.getValue() != null && !step.getValue().isEmpty()) {
				rawDetail = step.getValue();
			} else if (step.getXpath() != null && !step.getXpath().isEmpty()) {
				String rawXpath = step.getXpath();
				if (rawXpath.startsWith("accessibility=")) {
					rawDetail = "Accessibility ID: " + rawXpath.replace("accessibility=", "");
				} else if (rawXpath.startsWith("id=")) {
					rawDetail = "Resource ID: " + rawXpath.replace("id=", "");
				} else if (rawXpath.startsWith("automator=")) {
					rawDetail = "UIAutomator Engine";
				} else {
					rawDetail = rawXpath;
				}
			}

			String detail = platformLabel + rawDetail;

			String script =
					"var overlay = document.getElementById('automation-overlay');" +
							"if(!overlay) {" +
							"  document.body.style.marginTop = '50px';" +
							"  overlay = document.createElement('div');" +
							"  overlay.id = 'automation-overlay';" +
							"  overlay.style.cssText = 'position:fixed; top:0; left:0; width:100%; height:52px; " +
							"                           background:rgba(15, 15, 20, 0.98); color:#00d4ff; " +
							"                           padding:0 20px; z-index:999999; " +
							"                           font-family:Segoe UI, Tahoma, sans-serif; " +
							"                           border-bottom:3px solid #00d4ff; " +
							"                           display:grid; grid-template-columns: auto auto auto 1fr; " +
							"                           align-items:center; gap:25px; box-shadow:0 4px 12px rgba(0,0,0,0.5); " +
							"                           pointer-events:none; opacity:1.0;';" +
							"  overlay.innerHTML = " +
							"    '<div id=\"brand-container\" style=\"display:flex; flex-direction:column; align-items:center; line-height:1; min-width:80px\">' + " +
							"    '  <span style=\"font-weight:900; font-size:18px; letter-spacing:1px; color:#fff\">ABCD</span>' + " +
							"    '  <span style=\"font-size:9px; color:#00d4ff; font-weight:bold; margin-top:2px; text-transform:uppercase\">Test Suite</span>' + " +
							"    '</div>' + " +
							"    '<div id=\"overlay-timer\" style=\"color:#fff; background:#222; padding:4px 12px; border-radius:20px; border:1px solid #444; font-weight:bold; min-width:75px; text-align:center; font-size:13px\">⏱️ 00:00</div>' + " +
							"    '<div id=\"overlay-left\" style=\"font-size:13px; white-space:nowrap;\"></div>' + " +
							"    '<div id=\"overlay-right\" style=\"text-align:right; font-size:13px; white-space:nowrap;\"></div>';" +
							"  document.body.appendChild(overlay);" +
							"}" +
							"document.getElementById('overlay-left').innerHTML = \"<span style='color:#666'>📄</span> \" + arguments[0] + \" <span style='color:#444;margin:0 5px'>|</span> <span style='color:#666'>🧪</span> \" + arguments[1];" +
							"document.getElementById('overlay-right').innerHTML = \"<b style='color:#00d4ff'>🔢 STEP \" + arguments[2] + \":</b> <span style='color:#fff; background:#333; padding:3px 8px; border-radius:4px; margin:0 5px'>\" + arguments[3] + \"</span> <span style='color:#bbb; font-size:11px'>\" + arguments[4] + \"</span>\";" +
							"if (!window.automationStartTime) { window.automationStartTime = Date.now(); }" +
							"if (!window.automationInterval) {" +
							"  window.automationInterval = setInterval(function() {" +
							"    var timerElem = document.getElementById('overlay-timer');" +
							"    if(timerElem) {" +
							"      var diff = Math.floor((Date.now() - window.automationStartTime) / 1000);" +
							"      var mins = Math.floor(diff / 60); var secs = diff % 60;" +
							"      timerElem.innerHTML = '⏱️ ' + (mins < 10 ? '0' + mins : mins) + ':' + (secs < 10 ? '0' + secs : secs);" +
							"    }" +
							"  }, 1000);" +
							"}";

			js.executeScript(script, this.excelName, testCaseName, stepNum, action, detail);
		} catch (Exception ignored) {}
	}

	public Map<String, WebDriver> getDriverPool() {
		return this.driverPool;
	}

	public WebDriverWait getWait() {
		return this.wait;
	}

	public void setWait(WebDriverWait wait) {
		this.wait = wait;
		if (currentSessionRole != null) {
			waitPool.put(currentSessionRole, wait);
		}
	}

	public void setDriver(WebDriver driver) {
		this.driver = driver;
		if (currentSessionRole != null) {
			driverPool.put(currentSessionRole, driver);
			refreshActionHandlers();
		}
	}

	private String padRight(String s, int n) {
		String val = (s == null) ? "" : s;
		return String.format("%-" + n + "s", val);
	}

	public void setDetailedLogging(boolean enabled) {
		this.detailedLogging = enabled;
	}

	public int getTotalStepsExecuted() { return totalStepsExecuted; }
	public int getPassedSteps() { return passedSteps; }
	public int getFailedSteps() { return failedSteps; }
}