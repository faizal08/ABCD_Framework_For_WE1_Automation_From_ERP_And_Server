//package com.eit.automation.core;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.time.Duration;
//import java.util.Date;
//import java.util.List;
//import java.util.NoSuchElementException;
//
//import org.openqa.selenium.By;
//import org.openqa.selenium.ElementClickInterceptedException;
//import org.openqa.selenium.JavascriptExecutor;
//import org.openqa.selenium.StaleElementReferenceException;
//import org.openqa.selenium.TimeoutException;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.WebElement;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.chrome.ChromeOptions;
//import org.openqa.selenium.interactions.Actions;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.WebDriverWait;
//
//import com.eit.automation.parser.TestStep;
//
//public class TestExecutorold {
//	WebDriver driver;
//	WebDriverWait wait;
//	Actions actions;
//	JavascriptExecutor jsExecutor;
//
//	public TestExecutorold() {
//		driver = new ChromeDriver(new ChromeOptions().addArguments("--start-maximized"));
//		wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // Increased timeout to 30s
//		actions = new Actions(driver);
//		jsExecutor = (JavascriptExecutor) driver;
//	}
//
//	public boolean run(List<TestStep> steps) {
//		try {
//			for (int i = 0; i < steps.size(); i++) {
//				TestStep step = steps.get(i);
//				System.out.println("? Step " + (i + 1) + ": Action=" + step.action + " | Value=" + step.value
//						+ " | Given XPath=" + step.xpath);
//				System.out.println("🔹 Parsed Step:");
//				System.out.println("   Action: " + step.action);
//				System.out.println("   Value: " + step.value);
//				System.out.println("   XPath: " + step.xpath);
//				if (step.value.equals("") && step.xpath.equals(""))
//					continue;
//				String finalXpath = (step.xpath != null && !step.xpath.isEmpty()) ? step.xpath
//						: generateXPathFromValue(step.value, step.action);
//				System.out.println(finalXpath + "finalXpath");
//				switch (step.action.toLowerCase()) {
//				case "openurl", "Navigate":
//					System.out.println("🌐 Opening URL: " + step.value);
//					driver.get(step.value);
//					waitForPageLoad();
//					break;
//
//				case "click":
//					if (isValidDate(step.value)) {
//						break;
//					}
//					clickElementWithRetry(finalXpath);
//					break;
//
//				case "select":
//					if (isValidDate(step.value)) {
//						typeText(finalXpath, step.value);
//					}
//					clickElementWithRetry(finalXpath);
//					break;
//
//				case "type":
//				case "enter":
//					typeText(finalXpath, step.value);
//					break;
//
//				case "verify":
//					verifyElementVisible(finalXpath);
//					break;
//
//				case "verifytext", "Verifyelement":
//					verifyTextWithHoverFallback(finalXpath, step.value);
//					break;
//
//				case "hover":
//					hoverElement(finalXpath);
//					break;
//
//				default:
//					throw new RuntimeException("Unknown action: " + step.action);
//				}
//			}
//		} catch (Exception e) {
//			System.out.println("❌ Test failed: " + e.getMessage());
//			ScreenshotHelper.capture(driver, "failed_step.png");
//			return false;
//		}
//		return true;
//	}
//
//	public static boolean isValidDate(String dateStr) {
//		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
//		sdf.setLenient(false); // strict parsing
//		try {
//			Date date = sdf.parse(dateStr);
//			return true;
//		} catch (ParseException e) {
//			return false;
//		}
//	}
//
//	private void waitForPageLoad() {
//		try {
//			wait.until(webDriver -> jsExecutor.executeScript("return document.readyState").equals("complete"));
//		} catch (Exception e) {
//			System.out.println("⚠️ Page load wait interrupted: " + e.getMessage());
//		}
//	}
//
//	private WebElement findElement(String xpath) {
//		try {
//			return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
//		} catch (Exception e) {
//			System.out.println("⚠️ Initial XPath lookup failed: " + xpath);
//		}
//
//		if (xpath.contains("text()")) {
//			try {
//				String modifiedXpath = xpath
//						.replace("text()=",
//								"translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')=")
//						.replace("'", "translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'),'")
//						.toLowerCase();
//				return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(modifiedXpath)));
//			} catch (Exception e) {
//				System.out.println("⚠️ Case-insensitive text matching failed");
//			}
//		}
//
//		if (xpath.startsWith("//P") || xpath.startsWith("//p")) {
//			try {
//				String lowerP = xpath.replace("//P", "//p");
//				return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(lowerP)));
//			} catch (Exception e) {
//				System.out.println("⚠️ Lowercase p element search failed");
//			}
//
//			try {
//				String textValue = xpath.split("'")[1];
//				String anyElementXpath = "//*[contains(text(), '" + textValue + "')]";
//				return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(anyElementXpath)));
//			} catch (Exception e) {
//				System.out.println("⚠️ Any element text search failed");
//			}
//		}
//
//		try {
//			String textValue = xpath.split("'")[1];
//			WebElement fallbackElement = (WebElement) jsExecutor.executeScript(
//					"return Array.from(document.querySelectorAll('*')).find(el => el.textContent.trim() === '"
//							+ textValue + "');");
//			if (fallbackElement != null) {
//				return fallbackElement;
//			}
//			throw new RuntimeException("❌ All attempts to find element failed for XPath: " + xpath);
//		} catch (Exception e) {
//			throw new RuntimeException("❌ All attempts to find element failed for XPath: " + xpath);
//		}
//	}
//
//	private void clickElementWithRetry(String locator) {
//		clickElementWithRetry(locator, 3);
////		click(locator);
//	}
//
//	/**
//	 * Generate XPath from value when XPath is not provided Creates smart XPath
//	 * based on the text/value and action type
//	 */
//	private String generateXPathFromValue(String value, String action) {
//		if (value == null || value.isEmpty()) {
//			throw new IllegalArgumentException("Cannot generate XPath from empty value");
//		}
//
//		String xpath = "";
//
//		switch (action.toLowerCase()) {
//		case "click":
//			// Generate XPath for clickable elements
//			xpath = String.format("//*[normalize-space()='%1$s' or " + "@value='%1$s' or " + "@placeholder='%1$s' or "
//					+ "@title='%1$s' or " + "contains(text(), '%1$s')]", value);
//
//			System.out.println("   🔍 Looking for clickable element with text/value: '" + value + "'");
//			break;
//
//		case "type":
//		case "enter":
//			// Generate XPath for input fields
//			xpath = String.format("//input[@placeholder='%1$s' or " + "@name='%1$s' or " + "@id='%1$s' or "
//					+ "contains(@aria-label, '%1$s')] | " + "//textarea[@placeholder='%1$s' or " + "@name='%1$s' or "
//					+ "@id='%1$s']", value);
//
//			System.out.println("   🔍 Looking for input field with placeholder/name: '" + value + "'");
//			break;
//
//		case "select":
//			// Generate XPath for dropdowns/selects
//			xpath = String.format("//select[@name='%1$s' or @id='%1$s'] | "
//					+ "//div[contains(@class, 'select') and contains(., '%1$s')] | "
//					+ "//*[@role='combobox' and contains(., '%1$s')]", value);
//
//			System.out.println("   🔍 Looking for dropdown with name: '" + value + "'");
//			break;
//
//		case "verify":
//		case "verifytext":
//			// Generate XPath for verification
//			xpath = String.format("//*[contains(text(), '%s') or " + "contains(@value, '%s') or " + "@title='%s']",
//					value, value, value);
//
//			System.out.println("   🔍 Looking for element containing text: '" + value + "'");
//			break;
//
//		case "hover":
//			// Generate XPath for hover
//			xpath = String.format("//*[normalize-space()='%s' or " + "contains(text(), '%s') or " + "@title='%s']",
//					value, value, value);
//
//			System.out.println("   🔍 Looking for element to hover: '" + value + "'");
//			break;
//
//		default:
//			// Generic XPath
//			xpath = String.format("//*[normalize-space()='%1$s' or " + "@value='%1$s' or " + "@placeholder='%1$s' or "
//					+ "@name='%1$s' or " + "@id='%1$s' or " + "contains(., '%1$s')]", value);
//
//			System.out.println("   🔍 Looking for any element matching: '" + value + "'");
//		}
//
//		return xpath;
//	}
//
////	private void clickElementWithRetry(String locator, int maxAttempts) {
////		By by = (locator.startsWith("/") || locator.startsWith("(") || locator.contains("//")) ? By.xpath(locator)
////				: By.cssSelector(locator);
////
////		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
////			try {
////				// 1. FORCE CLEAR OVERLAYS (The "Unstuck" Command)
////				jsExecutor.executeScript(
////						"var overlays = document.querySelectorAll('.spinner, .loading, .overlay, .modal-backdrop');"
////								+ "overlays.forEach(el => el.remove());" + "document.body.style.overflow = 'auto';" // Restore
////																													// scrolling
////																													// if
////																													// a
////																													// modal
////																													// locked
////																													// it
////				);
////
////				// 2. Find and Scroll
////				WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
////				jsExecutor.executeScript("arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});",
////						element);
////
////				// 3. Handle ng-select dropdowns specifically
////				if (locator.contains("ng-select") || element.getAttribute("class").contains("ng-select")) {
////					jsExecutor.executeScript("arguments[0].dispatchEvent(new Event('mousedown'));", element);
////					return;
////				}
////
////				// 4. THE ULTIMATE CLICK STRATEGY
////				try {
////					// Try real user click first (triggers all JS listeners)
////					wait.until(ExpectedConditions.elementToBeClickable(element)).click();
////					System.out.println("✅ Clicked: " + locator);
////					return;
////				} catch (Exception e) {
////					System.err.println("⚠️ Standard click failed: " + e.getMessage());
////					// Fallback to JS Click (Bypasses "Element Intercepted" errors)
////					jsExecutor.executeScript("arguments[0].click();", element);
////					System.out.println("✅ JS Fallback Clicked: " + locator);
////					return;
////				}
////
////			} catch (Exception e) {
////				System.err.println("❌ Attempt " + attempt + " failed for [" + locator + "]: " + e.getMessage());
////				if (attempt == maxAttempts)
////					throw new RuntimeException("Final Click Failure", e);
////
////				// Wait for DOM to settle after API hits
////				try {
////					Thread.sleep(1000 * attempt);
////				} catch (InterruptedException ignored) {
////				}
////			}
////		}
////	}
//	/**
//	 * Enhanced click with automatic grid button detection and spinner handling
//	 */
//	private void clickElementWithRetry(String locator, int maxAttempts) {
//		boolean isXPath = locator.startsWith("/") || locator.startsWith("(") || locator.contains("//");
//
//		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
//			try {
//				System.out.println("🔄 Attempt " + attempt + ": clicking -> " + locator);
//
//				// ===== STEP 1: ALWAYS WAIT FOR LOADING SPINNER =====
//				waitForLoadingSpinner();
//
//				WebElement element;
//
//				try {
//					By by = isXPath ? By.xpath(locator) : By.cssSelector(locator);
//
//					// ===== STEP 2: WAIT FOR ELEMENT PRESENCE =====
//					element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
//					System.out.println("   ✅ Element found");
//
//					// ===== STEP 3: SPECIAL HANDLING FOR GRID BUTTONS =====
//					if (isGridButton(locator)) {
//						handleGridButtonClick(locator, element);
//						return;
//					}
//
//					// ===== STEP 4: HANDLE HIDDEN CHECKBOXES =====
//					if (isHiddenCheckbox(element)) {
//						handleCheckboxClick(element);
//						return;
//					}
//
//					// ===== STEP 5: FIND CLICKABLE PARENT FOR <p> ELEMENTS =====
//					if (locator.matches(".*p\\[.*text\\(\\).*\\]$")) {
//						element = findClickableParent(element);
//					}
//
//					// ===== STEP 6: SCROLL INTO VIEW =====
//					scrollIntoView(element);
//
//					// ===== STEP 7: WAIT FOR SPINNER AGAIN =====
//					waitForLoadingSpinner();
//
//					// ===== STEP 8: SPECIAL HANDLING FOR NG-SELECT =====
//					if (locator.contains("ng-select")) {
//						jsExecutor.executeScript("arguments[0].dispatchEvent(new Event('mousedown'));", element);
//						System.out.println("✅ Clicked ng-select");
//						return;
//					}
//
//					// ===== STEP 9: TRY STANDARD CLICK =====
//					try {
//						wait.until(ExpectedConditions.elementToBeClickable(element)).click();
//						System.out.println("✅ Clicked using Selenium");
//						return;
//					} catch (ElementClickInterceptedException e) {
//						System.err.println("⚠️ Click intercepted: " + e.getMessage());
//						// Check what's blocking
//						checkForBlockingElements();
//						// Try JavaScript fallback
//						jsExecutor.executeScript("arguments[0].click();", element);
//						System.out.println("✅ Clicked using JavaScript fallback");
//						return;
//					} catch (Exception clickEx) {
//						System.err.println("⚠️ Standard click failed, trying JavaScript...");
//						jsExecutor.executeScript("arguments[0].click();", element);
//						System.out.println("✅ Clicked using JavaScript fallback");
//						return;
//					}
//
//				} catch (Exception findEx) {
//					System.err.println("❌ Element not found or click failed: " + findEx.getMessage());
//
//					// ===== TEXT FALLBACK =====
//					if (isXPath && locator.contains("text()=")) {
//						try {
//							String altLocator = locator.replace("text()=",
//									"contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '")
//									.replace("']", "']").toLowerCase();
//							WebElement fallback = wait
//									.until(ExpectedConditions.elementToBeClickable(By.xpath(altLocator)));
//							fallback.click();
//							System.out.println("✅ Clicked using case-insensitive text fallback");
//							return;
//						} catch (Exception fallbackEx) {
//							System.err.println("❌ Text fallback failed: " + fallbackEx.getMessage());
//						}
//					}
//
//					if (attempt == maxAttempts) {
//						throw new RuntimeException(
//								"❌ Failed after " + maxAttempts + " attempts: " + findEx.getMessage());
//					}
//				}
//
//				// ===== STEP 10: TRY ACTIONS API =====
//				try {
//					waitForLoadingSpinner();
//					WebElement element1 = driver.findElement(isXPath ? By.xpath(locator) : By.cssSelector(locator));
//					new Actions(driver).moveToElement(element1).pause(Duration.ofMillis(300)).click().perform();
//					System.out.println("✅ Clicked using Actions API");
//					return;
//				} catch (Exception actionsEx) {
//					System.err.println("⚠️ Actions click failed: " + actionsEx.getMessage());
//				}
//
//				// Wait before retrying
//				Thread.sleep(500 * attempt);
//
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//				throw new RuntimeException("❌ Thread interrupted during retry wait", e);
//			}
//		}
//	}
////	private void clickElementWithRetry(String locator, int maxAttempts) {
////		boolean isXPath = locator.startsWith("/") || locator.startsWith("(") || locator.contains("//");
////
////		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
////			try {
////				System.out.println("🔄 Attempt " + attempt + ": clicking -> " + locator);
////
////				// ===== STEP 1: ALWAYS WAIT FOR LOADING SPINNER =====
////				waitForLoadingSpinner();
////
////				WebElement element;
////
////				try {
////					By by = isXPath ? By.xpath(locator) : By.cssSelector(locator);
////
////					// ===== STEP 2: WAIT FOR ELEMENT PRESENCE =====
////					element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
////					System.out.println("   ✅ Element found");
////
////					// ===== STEP 3: SPECIAL HANDLING FOR GRID BUTTONS =====
////					if (isGridButton(locator)) {
////						handleGridButtonClick(locator, element);
////						return;
////					}
////
////					// ===== STEP 4: HANDLE HIDDEN CHECKBOXES =====
////					if (isHiddenCheckbox(element)) {
////						handleCheckboxClick(element);
////						return;
////					}
////
////					// ===== STEP 5: FIND CLICKABLE PARENT FOR <p> ELEMENTS =====
////					if (locator.matches(".*p\\[.*text\\(\\).*\\]$")) {
////						element = findClickableParent(element);
////					}
////
////					// ===== STEP 6: SCROLL INTO VIEW =====
////					scrollIntoView(element);
////
////					// ===== STEP 7: WAIT FOR SPINNER AGAIN =====
////					waitForLoadingSpinner();
////
////					// ===== STEP 8: SPECIAL HANDLING FOR NG-SELECT =====
////					if (locator.contains("ng-select")) {
////						jsExecutor.executeScript("arguments[0].dispatchEvent(new Event('mousedown'));", element);
////						System.out.println("✅ Clicked ng-select");
////						return;
////					}
////
////					// ===== STEP 9: TRY STANDARD CLICK =====
////					try {
////						wait.until(ExpectedConditions.elementToBeClickable(element)).click();
////						System.out.println("✅ Clicked using Selenium");
////						return;
////					} catch (ElementClickInterceptedException e) {
////						System.err.println("⚠️ Click intercepted: " + e.getMessage());
////						// Check what's blocking
////						checkForBlockingElements();
////						// Try JavaScript fallback
////						jsExecutor.executeScript("arguments[0].click();", element);
////						System.out.println("✅ Clicked using JavaScript fallback");
////						return;
////					} catch (Exception clickEx) {
////						System.err.println("⚠️ Standard click failed, trying JavaScript...");
////						jsExecutor.executeScript("arguments[0].click();", element);
////						System.out.println("✅ Clicked using JavaScript fallback");
////						return;
////					}
////
////				} catch (Exception findEx) {
////					System.err.println("❌ Element not found or click failed: " + findEx.getMessage());
////
////					// ===== TEXT FALLBACK =====
////					if (isXPath && locator.contains("text()=")) {
////						try {
////							String altLocator = locator.replace("text()=",
////									"contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '")
////									.replace("']", "']").toLowerCase();
////							WebElement fallback = wait
////									.until(ExpectedConditions.elementToBeClickable(By.xpath(altLocator)));
////							fallback.click();
////							System.out.println("✅ Clicked using case-insensitive text fallback");
////							return;
////						} catch (Exception fallbackEx) {
////							System.err.println("❌ Text fallback failed: " + fallbackEx.getMessage());
////						}
////					}
////
////					if (attempt == maxAttempts) {
////						throw new RuntimeException(
////								"❌ Failed after " + maxAttempts + " attempts: " + findEx.getMessage());
////					}
////				}
////
////				// ===== STEP 10: TRY ACTIONS API =====
////				try {
////					waitForLoadingSpinner();
////					WebElement element1 = driver.findElement(isXPath ? By.xpath(locator) : By.cssSelector(locator));
////					new Actions(driver).moveToElement(element1).pause(Duration.ofMillis(300)).click().perform();
////					System.out.println("✅ Clicked using Actions API");
////					return;
////				} catch (Exception actionsEx) {
////					System.err.println("⚠️ Actions click failed: " + actionsEx.getMessage());
////				}
////
////				// Wait before retrying
////				Thread.sleep(500 * attempt);
////
////			} catch (InterruptedException e) {
////				Thread.currentThread().interrupt();
////				throw new RuntimeException("❌ Thread interrupted during retry wait", e);
////			}
////		}
////	}
//
//	// ===== HELPER METHODS =====
//
//	/**
//	 * Check if locator is for a grid button
//	 */
//	private boolean isGridButton(String locator) {
//		return locator.contains("@role='row'") && locator.contains("@role='button'");
//	}
//
//	/**
//	 * Special handling for grid buttons (row buttons)
//	 */
//	private void handleGridButtonClick(String locator, WebElement element) throws InterruptedException {
//		System.out.println("   🔘 Detected grid button, using special handling");
//
//		// Extract row ID for better logging
//		String rowId = "unknown";
//		if (locator.contains("@row-id='")) {
//			rowId = locator.replaceAll(".*@row-id='([^']+)'.*", "$1");
//		}
//		System.out.println("   📍 Row ID: " + rowId);
//
//		// Scroll the row into view first
//		scrollIntoView(element);
//		Thread.sleep(300);
//
//		// Wait for spinner again (grid might reload after scroll)
//		waitForLoadingSpinner();
//
//		// Check if button is actually clickable (no overlays)
//		try {
//			WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
//			element = shortWait.until(ExpectedConditions.elementToBeClickable(By.xpath(locator)));
//			element.click();
//			System.out.println("✅ Grid button clicked (standard)");
//		} catch (ElementClickInterceptedException e) {
//			System.err.println("⚠️ Grid button blocked by overlay, using JavaScript");
//			checkForBlockingElements();
//			jsExecutor.executeScript("arguments[0].click();", element);
//			System.out.println("✅ Grid button clicked (JavaScript)");
//		} catch (Exception e) {
//			System.err.println("⚠️ Grid button click failed, using JavaScript");
//			jsExecutor.executeScript("arguments[0].click();", element);
//			System.out.println("✅ Grid button clicked (JavaScript)");
//		}
//
//		// Wait for any post-click loading
//		Thread.sleep(500);
//		waitForLoadingSpinner();
//	}
//
//	/**
//	 * Check if element is a hidden checkbox
//	 */
//	private boolean isHiddenCheckbox(WebElement element) {
//		try {
//			String tagName = element.getTagName().toLowerCase();
//			if (tagName.equals("input")) {
//				String type = element.getAttribute("type");
//				return ("checkbox".equals(type) || "radio".equals(type)) && !element.isDisplayed();
//			}
//		} catch (Exception e) {
//			// Ignore
//		}
//		return false;
//	}
//
//	/**
//	 * Handle hidden checkbox clicks
//	 */
//	private void handleCheckboxClick(WebElement element) {
//		System.out.println("   ☑️ Detected hidden checkbox, using special handling");
//
//		String inputId = element.getAttribute("id");
//
//		if (inputId != null && !inputId.isEmpty()) {
//			// Try to find and click label
//			try {
//				WebElement label = driver.findElement(By.xpath("//label[@for='" + inputId + "']"));
//				label.click();
//				System.out.println("✅ Clicked label for checkbox");
//				return;
//			} catch (Exception e) {
//				System.out.println("⚠️ Label not found, using JavaScript");
//			}
//		}
//
//		// Fallback: Use JavaScript to toggle
//		jsExecutor.executeScript(
//				"arguments[0].checked = !arguments[0].checked; " + "arguments[0].dispatchEvent(new Event('change'));",
//				element);
//		System.out.println("✅ Toggled checkbox using JavaScript");
//	}
//
//	/**
//	 * Find clickable parent element
//	 */
//	private WebElement findClickableParent(WebElement element) {
//		try {
//			System.out.println("   🔍 Searching for clickable parent");
//
//			WebElement clickableParent = (WebElement) jsExecutor.executeScript("let el = arguments[0];"
//					+ "let maxLevels = 5;" + "let level = 0;"
//					+ "while (el && el !== document.body && level < maxLevels) {"
//					+ "  let tag = el.tagName.toLowerCase();" + "  if (tag === 'button' || tag === 'a' || "
//					+ "      el.onclick || " + "      el.classList.contains('btn') || "
//					+ "      el.classList.contains('button') || " + "      el.getAttribute('role') === 'button' || "
//					+ "      window.getComputedStyle(el).cursor === 'pointer') {" + "    return el;" + "  }"
//					+ "  el = el.parentElement;" + "  level++;" + "}" + "return arguments[0];", element);
//
//			if (clickableParent != null && !clickableParent.equals(element)) {
//				String parentTag = clickableParent.getTagName();
//				System.out.println("   ✅ Found clickable parent: <" + parentTag + ">");
//				return clickableParent;
//			}
//		} catch (Exception e) {
//			System.err.println("   ⚠️ Error finding clickable parent: " + e.getMessage());
//		}
//
//		return element;
//	}
//
//	/**
//	 * Scroll element into view
//	 */
//	private void scrollIntoView(WebElement element) {
//		try {
//			jsExecutor.executeScript(
//					"arguments[0].scrollIntoView({behavior: 'smooth', block: 'center', inline: 'center'});", element);
//			Thread.sleep(300);
//		} catch (Exception e) {
//			System.err.println("⚠️ Scroll failed: " + e.getMessage());
//		}
//	}
//
//	/**
//	 * Check for elements blocking clicks
//	 */
//	private void checkForBlockingElements() {
//		try {
//			// Check for common blocking overlays
//			String[] blockingSelectors = { "#cover-spin", ".loading-overlay", ".spinner", ".modal-backdrop",
//					"[class*='overlay']" };
//
//			for (String selector : blockingSelectors) {
//				try {
//					WebElement blocker = driver.findElement(By.cssSelector(selector));
//					String display = blocker.getCssValue("display");
//					if (!display.equals("none") && blocker.isDisplayed()) {
//						System.err.println("   🚫 Blocking element found: " + selector + " (display=" + display + ")");
//					}
//				} catch (NoSuchElementException e) {
//					// No blocker with this selector
//				}
//			}
//		} catch (Exception e) {
//			// Ignore errors in checking
//		}
//	}
//
//	/**
//	 * Wait for loading spinner/overlay to disappear
//	 */
//	private void waitForLoadingSpinner() {
//		try {
//			// Specifically wait for cover-spin (your main spinner)
//			try {
//				WebElement spinner = driver.findElement(By.id("cover-spin"));
//
//				WebDriverWait spinnerWait = new WebDriverWait(driver, Duration.ofSeconds(30));
//				spinnerWait.until(driver -> {
//					try {
//						WebElement spin = driver.findElement(By.id("cover-spin"));
//						String display = spin.getCssValue("display");
//						return display.equals("none");
//					} catch (NoSuchElementException e) {
//						return true;
//					}
//				});
//
//				Thread.sleep(300); // Small buffer after spinner disappears
//
//			} catch (NoSuchElementException e) {
//				// No spinner found - good, page is ready
//			}
//
//		} catch (TimeoutException e) {
//			System.err.println("⚠️ Spinner timeout - continuing anyway");
//		} catch (Exception e) {
//			System.err.println("⚠️ Spinner wait error: " + e.getMessage());
//		}
//	}
//
//	private void handleSelectAction(String xpath, String value) {
//		// First click to open dropdown
//		clickElementWithRetry(xpath);
//
//		// Then select the option (similar to your ng-select handling)
//		String optionXpath = String.format("//div[@role='option' and contains(., '%s')]", value);
//		clickElementWithRetry(optionXpath);
//	}
//
//	private void typeText(String xpath, String text) {
//		if (xpath.contains("filepond") || xpath.contains("@type='file'")) {
//			handleFileUpload(xpath, text);
//		} else if (xpath.contains("ng-select") || xpath.contains("@role='option'")) {
//			handleNgSelect(xpath, text);
//		} else {
//			try {
//				WebElement input = findElement(xpath);
//				makeElementVisible(input);
//				wait.until(ExpectedConditions.elementToBeClickable(input));
//				input.clear();
//				input.sendKeys(text);
//				System.out.println("✅ Successfully typed text: " + text + " into element: " + xpath);
//			} catch (Exception e) {
//				System.out.println("⚠️ Failed to type text. Dumping available options...");
//				try {
//					List<WebElement> options = driver.findElements(By.xpath("//div[@role='option']"));
//					for (int i = 0; i < options.size(); i++) {
//						System.out.println("Option " + (i + 1) + ": " + options.get(i).getText());
//					}
//					String pageSource = driver.getPageSource().substring(0,
//							Math.min(driver.getPageSource().length(), 1000));
//					System.out.println("📄 Page source snippet: " + pageSource);
//				} catch (Exception logEx) {
//					System.out.println("⚠️ Failed to log options: " + logEx.getMessage());
//				}
//				throw new RuntimeException(
//						"❌ Failed to type text into element with XPath: " + xpath + " - " + e.getMessage());
//			}
//		}
//	}
//
//	private void handleNgSelect(String xpath, String text) {
//		try {
//			System.out.println("🔘 Handling ng-select for: " + text);
//			// If XPath targets an option, find the parent ng-select container
//			String ngSelectXpath = xpath.contains("@role='option'")
//					? "//div[contains(@class, 'ng-select') or contains(@class, 'dropdown')]" // Target ng-select
//																								// container
//					: xpath;
//			WebElement ngSelect = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(ngSelectXpath)));
//			jsExecutor.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", ngSelect);
//			makeElementVisible(ngSelect);
//
//			// Retry clicking to open dropdown
//			final int maxAttempts = 3;
//			for (int attempt = 1; attempt <= maxAttempts; attempt++) {
//				try {
//					System.out.println("🔄 Attempt " + attempt + " to open dropdown");
//					ngSelect.click();
//					Thread.sleep(1000); // Wait for dropdown to open
//					break;
//				} catch (Exception e) {
//					if (attempt == maxAttempts) {
//						throw new RuntimeException(
//								"❌ Failed to open dropdown after " + maxAttempts + " attempts: " + e.getMessage());
//					}
//					Thread.sleep(1000);
//				}
//			}
//
//			// Log available options before typing
//			System.out.println("🔍 Available dropdown options before typing:");
//			try {
//				List<WebElement> options = driver.findElements(By.xpath("//div[@role='option']"));
//				for (int i = 0; i < options.size(); i++) {
//					System.out.println("Option " + (i + 1) + ": " + options.get(i).getText());
//				}
//				if (options.isEmpty()) {
//					System.out.println("⚠️ No options found in dropdown");
//				}
//			} catch (Exception e) {
//				System.out.println("⚠️ Failed to log options before typing: " + e.getMessage());
//			}
//
//			// Find and type into the combobox
//			WebElement searchInput = wait
//					.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@role='combobox']")));
//			searchInput.sendKeys(text);
//			Thread.sleep(1000); // Wait for options to filter
//
//			// Log available options after typing
//			System.out.println("🔍 Available dropdown options after typing '" + text + "':");
//			try {
//				List<WebElement> options = driver.findElements(By.xpath("//div[@role='option']"));
//				for (int i = 0; i < options.size(); i++) {
//					System.out.println("Option " + (i + 1) + ": " + options.get(i).getText());
//				}
//				if (options.isEmpty()) {
//					System.out.println("⚠️ No options found after typing");
//				}
//			} catch (Exception e) {
//				System.out.println("⚠️ Failed to log options after typing: " + e.getMessage());
//			}
//
//			// Select the option with flexible text matching
//			String optionXpath = "//div[@role='option' and contains(translate(., 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '"
//					+ text.toLowerCase() + "')]";
//			WebElement option = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(optionXpath)));
//			makeElementVisible(option);
//			option.click();
//			System.out.println("✅ Successfully selected option '" + text + "' in ng-select");
//		} catch (Exception e) {
//			System.out.println("⚠️ Standard ng-select handling failed, trying JavaScript fallback");
//			// Log available options for debugging
//			try {
//				List<WebElement> options = driver.findElements(By.xpath("//div[@role='option']"));
//				for (int i = 0; i < options.size(); i++) {
//					System.out.println("Option " + (i + 1) + ": " + options.get(i).getText());
//				}
//				if (options.isEmpty()) {
//					System.out.println("⚠️ No options found in JavaScript fallback");
//				}
//				String pageSource = driver.getPageSource().substring(0,
//						Math.min(driver.getPageSource().length(), 1000));
//				System.out.println("📄 Page source snippet: " + pageSource);
//			} catch (Exception logEx) {
//				System.out.println("⚠️ Failed to log options in fallback: " + logEx.getMessage());
//			}
//			// JavaScript fallback
//			try {
//				WebElement ngSelect = driver.findElement(By.xpath(xpath.contains("@role='option'")
//						? "//div[contains(@class, 'ng-select') or contains(@class, 'dropdown')]"
//						: xpath));
//				jsExecutor.executeScript("arguments[0].dispatchEvent(new Event('mousedown'));"
//						+ "var input = arguments[0].querySelector('input') || document.querySelector('input[role=\"combobox\"]');"
//						+ "if (input) {" + "  input.value = arguments[1];"
//						+ "  input.dispatchEvent(new Event('change'));" + "  input.dispatchEvent(new Event('keydown'));"
//						+ "  input.dispatchEvent(new Event('keyup'));" + "}"
//						+ "var option = Array.from(document.querySelectorAll('.ng-option, [role=\"option\"]')).find("
//						+ "  el => el.textContent.toLowerCase().includes(arguments[1].toLowerCase())" + ");"
//						+ "if (option) option.click();", ngSelect, text);
//				System.out.println("✅ JavaScript fallback succeeded for: " + text);
//			} catch (Exception ex) {
//				throw new RuntimeException("❌ Failed to handle ng-select: " + ex.getMessage());
//			}
//		}
//	}
//
//	private void handleFileUpload(String xpath, String filePath) {
//		Path absolutePath = Paths.get(filePath).toAbsolutePath();
//		if (!absolutePath.toFile().exists()) {
//			throw new RuntimeException("❌ File not found: " + absolutePath);
//		}
//
//		String systemIndependentPath = absolutePath.toString();
//
//		try {
//			WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
//			makeElementVisible(fileInput);
//			fileInput.sendKeys(systemIndependentPath);
//			System.out.println("✅ File uploaded successfully: " + systemIndependentPath);
//			return;
//		} catch (Exception e) {
//			System.out.println("⚠️ Standard upload failed, trying JavaScript approach");
//		}
//
//		try {
//			WebElement fileInput = driver.findElement(By.xpath(xpath));
//			makeElementVisible(fileInput);
//			jsExecutor.executeScript("arguments[0].style.display='block'; arguments[0].style.visibility='visible';",
//					fileInput);
//			fileInput.sendKeys(systemIndependentPath);
//		} catch (Exception ex) {
//			tryAlternativeFileUploadLocators(systemIndependentPath);
//		}
//	}
//
//	private void makeElementVisible(WebElement element) {
//		jsExecutor.executeScript("arguments[0].style.display='block';" + "arguments[0].style.visibility='visible';"
//				+ "arguments[0].style.width='100px';" + "arguments[0].style.height='100px';"
//				+ "arguments[0].style.opacity=1;", element);
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			Thread.currentThread().interrupt();
//		}
//	}
//
//	private void tryAlternativeFileUploadLocators(String filePath) {
//		String[] alternativeXPaths = { "//input[@type='file']", "//input[contains(@class, 'file')]",
//				"//input[contains(@id, 'upload')]", "//input[contains(@name, 'file')]",
//				"//div[contains(@class, 'upload')]//input[@type='file']" };
//
//		for (String altXpath : alternativeXPaths) {
//			try {
//				WebElement fileInput = driver.findElement(By.xpath(altXpath));
//				makeElementVisible(fileInput);
//				fileInput.sendKeys(filePath);
//				System.out.println("✅ File uploaded using alternative locator: " + altXpath);
//				return;
//			} catch (Exception e) {
//				System.out.println("⚠️ Attempt failed with XPath: " + altXpath);
//			}
//		}
//		throw new RuntimeException("❌ All file upload attempts failed for: " + filePath);
//	}
//
//	private void verifyElementVisible(String xpath) {
//		try {
//			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
//			System.out.println("✅ Verified visibility of element: " + xpath);
//		} catch (Exception e) {
//			throw new RuntimeException("❌ Verification failed: Element not visible or not found for XPath: " + xpath);
//		}
//	}
//
////	@Step("Click on element: {target}")
////	private void click(String target) {
////		By locator = getBy(target);
////		driver.findElement(locator).click();
////	}
//
////	private By getBy(String target) {
////		By locator;
////		if (target.startsWith("id=")) {
////			locator = By.id(target.substring(3));
////		} else if (target.startsWith("name=")) {
////			locator = By.name(target.substring(5));
////		} else if (target.startsWith("class=")) {
////			locator = By.className(target.substring(6));
////		} else if (target.startsWith("xpath=")) {
////			locator = By.xpath(target.substring(6));
////		} else if (target.startsWith("css=")) {
////			locator = By.cssSelector(target.substring(4));
////		} else if (target.startsWith("linktext=")) {
////			locator = By.linkText(target.substring(9));
////		} else {
////			throw new IllegalArgumentException("Unsupported locator format: " + target);
////		}
////		return locator;
////	}
//
//	private boolean verifyTextWithHoverFallback(String xpath, String expectedText) {
//		try {
//			if (xpath.contains("jqx-grid")) {
//				return verifyJqxGridText(expectedText, xpath);
//			}
//
//			// ===== NEW: Special handling for input/textarea elements =====
//			WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
//			String tagName = element.getTagName().toLowerCase();
//
//			System.out.println("🔍 Verifying '" + expectedText + "' in <" + tagName + ">");
//
//			// For input/textarea, check 'value' attribute instead of text
//			if (tagName.equals("input") || tagName.equals("textarea") || tagName.equals("select")) {
//				System.out.println("   Checking 'value' attribute...");
//
//				try {
//					boolean found = wait.until(driver -> {
//						WebElement el = driver.findElement(By.xpath(xpath));
//						String value = el.getAttribute("value");
//						String text = el.getText();
//
//						// Debug: Print what we found
//						System.out.println("   Current value attribute: '" + (value != null ? value : "null") + "'");
//						System.out.println("   Current text content: '" + (text != null ? text : "null") + "'");
//
//						// Check both value attribute and text content
//						boolean valueMatch = value != null && value.contains(expectedText);
//						boolean textMatch = text != null && text.contains(expectedText);
//
//						return valueMatch || textMatch;
//					});
//
//					if (found) {
//						System.out.println("✅ Verified '" + expectedText + "' in " + tagName);
//						return true;
//					}
//				} catch (TimeoutException e) {
//					// Print final state before failing
//					String finalValue = element.getAttribute("value");
//					String finalText = element.getText();
//					System.out.println("❌ Verification timed out after 30s");
//					System.out.println("   Expected: '" + expectedText + "'");
//					System.out.println("   Actual value: '" + (finalValue != null ? finalValue : "null") + "'");
//					System.out.println("   Actual text: '" + (finalText != null ? finalText : "null") + "'");
//
//					// Don't throw yet - try hover fallback below
//					System.out.println("⚠️ Trying hover fallback...");
//				}
//			} else {
//				// For other elements (div, span, p, etc.), check text content normally
//				boolean found = wait
//						.until(ExpectedConditions.textToBePresentInElementLocated(By.xpath(xpath), expectedText));
//				if (found) {
//					System.out.println("✅ Verified text '" + expectedText + "' in element: " + xpath);
//					return true;
//				}
//			}
//		} catch (Exception e) {
//			System.out.println("⚠️ Initial text verification failed: " + e.getMessage());
//		}
//
//		// ===== HOVER FALLBACK (your existing logic) =====
//		System.out.println("🔄 Starting hover fallback...");
//		try {
////	        List<WebElement> allElements = driver.findElements(By.xpath("//*"));
//			List<WebElement> potentialElements = driver.findElements(
//					By.xpath("//*[contains(text(), '" + expectedText + "')] | //input[@value='" + expectedText + "']"));
//
//			for (WebElement element : potentialElements) {
//				try {
//					if (element.getSize().getHeight() < 5 || element.getSize().getWidth() < 5) {
//						continue;
//					}
//
//					System.out.println("🔄 Hovering over element at (" + element.getLocation().x + ","
//							+ element.getLocation().y + ")");
//
//					actions.moveToElement(element).perform();
//					Thread.sleep(300);
//
//					try {
//						// Check if text appeared after hover
//						if (wait.until(
//								ExpectedConditions.textToBePresentInElementLocated(By.xpath(xpath), expectedText))) {
//							System.out.println("✅ Text appeared after hovering!");
//							return true;
//						}
//					} catch (Exception ex) {
//						continue;
//					}
//				} catch (Exception e) {
//					continue;
//				}
//			}
//		} catch (Exception e) {
//			System.out.println("⚠️ Hover search failed: " + e.getMessage());
//		}
//
//		throw new RuntimeException("❌ Text verification failed after all attempts for: " + expectedText);
//	}
//
//	private boolean verifyJqxGridText(String expectedText, String originalXpath) {
//		System.out.println("🔍 Starting jqx-grid verification for text: '" + expectedText + "'");
//
//		try {
//			WebElement grid = wait.until(
//					ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@class,'jqx-grid')]")));
//
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//			}
//
//			String[] xpathVariations = { originalXpath,
//					"//div[contains(@class,'jqx-grid-cell') and contains(.,'" + expectedText + "')]",
//					"//div[contains(@class,'jqx-grid-cell')]//*[contains(text(),'" + expectedText + "')]",
//					"//div[contains(@class,'jqx-grid-content')]//div[contains(text(),'" + expectedText + "')]",
//					"//div[@class='jqx-grid-cell' and contains(.,'" + expectedText + "')]",
//					"//*[contains(@class,'jqx-grid')]//*[contains(text(),'" + expectedText + "')]",
//					"//div[contains(@class,'jqx-grid-cell')][contains(.,'" + expectedText + "')]",
//					"//div[@role='gridcell' and contains(.,'" + expectedText + "')]",
//					"//div[contains(@aria-label,'" + expectedText + "')]",
//					"//div[contains(text(),'" + expectedText + "') and ancestor::div[contains(@class,'jqx-grid')]]" };
//
//			for (String xpath : xpathVariations) {
//				try {
//					System.out.println("🔎 Trying XPath: " + xpath);
//					WebElement cell = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
//
//					jsExecutor.executeScript(
//							"arguments[0].scrollIntoView({behavior: 'smooth', block: 'center', inline: 'center'});",
//							cell);
//
//					jsExecutor.executeScript("arguments[0].style.border='3px solid red'", cell);
//					Thread.sleep(300);
//					jsExecutor.executeScript("arguments[0].style.border=''", cell);
//
//					if (cell.isDisplayed()) {
//						String actualText = cell.getText().trim();
//						System.out.println("📋 Found cell with text: '" + actualText + "'");
//						if (actualText.contains(expectedText)) {
//							System.out.println("✅ Found matching cell with expected text");
//							return true;
//						}
//					}
//				} catch (Exception e) {
//					System.out.println("⚠️ Attempt failed with XPath: " + xpath + " - " + e.getMessage());
//				}
//			}
//
//			System.out.println("🔄 Trying JavaScript grid search");
//			String script = "var expected = arguments[0];" + "var found = false;"
//					+ "var cells = document.querySelectorAll('.jqx-grid-cell, [role=\"gridcell\"]');"
//					+ "Array.from(cells).forEach(cell => {" + "  var cellText = cell.textContent.trim();"
//					+ "  if (cellText.includes(expected)) {"
//					+ "    cell.scrollIntoView({behavior: 'smooth', block: 'center'});"
//					+ "    cell.style.border='2px solid green';" + "    setTimeout(() => cell.style.border='', 500);"
//					+ "    found = true;" + "  }" + "});" + "return found;";
//
//			Boolean found = (Boolean) jsExecutor.executeScript(script, expectedText);
//			if (found) {
//				System.out.println("✅ JavaScript verification succeeded");
//				return true;
//			}
//
//			System.out.println("⚠️ Text not found, dumping grid structure:");
//			String gridHtml = grid.getAttribute("outerHTML");
//			System.out.println(gridHtml.substring(0, Math.min(gridHtml.length(), 1000)) + "...");
//
//			System.out.println("⚠️ Dumping first 100 grid cells:");
//			List<WebElement> cells = driver.findElements(
//					By.xpath("(//div[contains(@class,'jqx-grid-cell') or @role='gridcell'])[position() <= 100]"));
//
//			for (int i = 0; i < cells.size(); i++) {
//				try {
//					String cellText = cells.get(i).getText().trim();
//					if (!cellText.isEmpty()) {
//						System.out.println("Cell " + (i + 1) + ": '" + cellText + "'");
//						if (cellText.contains(expectedText)) {
//							System.out.println("💡 Found potential match at position " + (i + 1));
//						}
//					}
//				} catch (StaleElementReferenceException e) {
//					System.out.println("Cell " + (i + 1) + ": [stale element]");
//				}
//			}
//
//		} catch (Exception e) {
//			System.out.println("❌ jqx-grid verification error: " + e.getMessage());
//			e.printStackTrace();
//		}
//
//		throw new RuntimeException("❌ Text '" + expectedText + "' not found in jqx-grid after all attempts");
//	}
//
//	private void hoverElement(String xpath) {
//		try {
//			WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
//			actions.moveToElement(element).perform();
//			Thread.sleep(500);
//		} catch (Exception e) {
//			throw new RuntimeException("⚠️ Hover failed: " + e.getMessage());
//		}
//	}
//
//	public void close() {
//		if (driver != null) {
//			driver.quit();
//		}
//	}
//}