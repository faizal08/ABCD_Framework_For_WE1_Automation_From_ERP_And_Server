package com.eit.automation.actions;

import java.time.Duration;
import java.util.NoSuchElementException;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.eit.automation.annotations.Action;
import io.qameta.allure.Step;

public class ClickActions {

	private WebDriver driver;
	private WebDriverWait wait;
	private WaitActions waitActions;
	private boolean detailedLogging = true; // Enabled for debugging

	public ClickActions(WebDriver driver, WebDriverWait wait, WaitActions waitActions) {
		this.driver = driver;
		this.wait = wait;
		this.waitActions = waitActions;
	}

	/**
	 * Click element with retry mechanism
	 */
	@Action(keys = { "click_simple" }) // Optional: mapped if needed, simplified key
	public void clickElementWithRetry(String xpath) {
		int maxAttempts = 3;
		// ... (implementation hidden for brevity in this snippet check)
		clickElementWithRetry(xpath, null);
	}

	/**
	 * Click WebElement (PageFactory support)
	 */
	@Action(keys = { "click", "click_simple", "select" })
	@Step("Clicking PageFactory element")
	public void clickElement(WebElement element) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();

		try {
			scrollToElement(element);
			wait.until(ExpectedConditions.elementToBeClickable(element)).click();
		} catch (Exception e) {
			// Simple retry fallback for JS click
			try {
				clickWithJavaScript(element);
			} catch (Exception jsEx) {
				throw new RuntimeException("Failed to click PageFactory element", e);
			}
		}
	}

	@Action(keys = { "click_simple" })
	public void clickElementWithRetryLegacy(String xpath) {
		int maxAttempts = 3;
		Exception lastException = null;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				WebElement element = waitActions.waitForElementClickable(xpath);

				// Scroll element into view
				scrollToElement(element);

				// Try regular click first
				element.click();
				return;
			} catch (Exception e) {
				lastException = e;
				System.out.println("Click attempt " + attempt + " failed, retrying...");

				if (attempt < maxAttempts) {
					waitActions.waitFor(500);

					// Try JavaScript click on retry
					try {
						WebElement element = driver.findElement(By.xpath(xpath));
						clickWithJavaScript(element);
						return;
					} catch (Exception jsException) {
						// Continue to next attempt
					}
				}
			}
		}

		throw new RuntimeException("Failed to click element after " + maxAttempts + " attempts: " + xpath,
				lastException);
	}

	@Action(keys = { "click", "select" })
	@Step("Clicking element: {0} with value: {1}")
	public void clickElementWithRetry(String xpath, String value) {
		boolean isXPath = xpath.startsWith("/") || xpath.startsWith("(") || xpath.contains("//");
		int maxAttempts = 3;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				if (detailedLogging)
					System.out.println("🔄 Attempt " + attempt + ": clicking -> " + xpath);

				// ===== STEP 1: ALWAYS WAIT FOR LOADING SPINNER =====
				waitActions.waitForLoadingSpinner();

				WebElement element;

				try {
					// ===== STEP 2: SMART ELEMENT DISCOVERY (LAST VISIBLE) =====
					java.util.List<WebElement> candidates = driver.findElements(By.xpath(xpath));
					int visibleCount = 0;
					element = null; // Reset

					// Iterate through all candidates
					for (int i = 0; i < candidates.size(); i++) {
						WebElement cand = candidates.get(i);
						boolean isDisplayed = cand.isDisplayed();

						if (detailedLogging) {
							try {
								String outerHtml = cand.getAttribute("outerHTML");
								System.out.println("   🔍 Click Candidate #" + (i + 1) + " | Visible: " + isDisplayed
										+ " | Tag: " + cand.getTagName() + " | Text: " + cand.getText());
								System.out.println("      HTML: " + outerHtml);
							} catch (Exception ignore) {
							}
						}

						if (isDisplayed) {
							visibleCount++;
							element = cand; // Update target to the latest visible element (LAST one)
						}
					}

					if (element == null) {
						// If none are currently visible, fall back to standard wait
						try {
							element = waitActions.waitForElementVisible(xpath);
						} catch (Exception e) {
							// If still not found/visible, try presence as last resort
							try {
								element = waitActions.waitForElementPresent(xpath);
							} catch (Exception ex) {
								if (attempt == maxAttempts)
									throw ex;
								continue; // Retry loop
							}
							if (detailedLogging)
								System.out.println("   ⚠️ No visible element found, using presence: " + xpath);
						}
					} else {
						if (detailedLogging)
							System.out.println("   🎯 Selected Click Candidate #" + visibleCount + " (Last Visible)");
					}

					if (detailedLogging)
						System.out.println("   ✅ Element selected for interaction");

					// ===== STEP 3: SPECIAL HANDLING FOR GRID BUTTONS =====
					if (isGridButton(xpath)) {
						handleGridButtonClick(xpath, element);
						return;
					}

					// ===== STEP 4: HANDLE HIDDEN CHECKBOXES/FILE INPUTS =====
					String type = element.getAttribute("type");
					if (isHiddenCheckbox(element, type)) {
						handleHiddenInputClick(element, type);
						return;
					}

					// ===== STEP 5: FIND CLICKABLE PARENT FOR <p> ELEMENTS =====
					if (xpath.matches(".*p\\[.*text\\(\\).*\\]$")) {
						element = findClickableParent(element);
					}

					// ===== STEP 6: SCROLL INTO VIEW =====
					scrollToElement(element);

					// ===== STEP 7: WAIT FOR SPINNER AGAIN =====
					waitActions.waitForLoadingSpinner();

					// ===== STEP 8: SPECIAL HANDLING FOR NG-SELECT/SELECT =====
					// try {
					// if (xpath.contains("ng-select") ||
					// "select".equalsIgnoreCase(element.getTagName())) {
					// if ("select".equalsIgnoreCase(element.getTagName()) && value != null &&
					// !value.isEmpty()) {
					// Select select = new Select(element);
					// select.selectByVisibleText(value);
					// return;
					// }
					// // For ng-select
					// if (xpath.contains("ng-select")) {
					// JavascriptExecutor js = (JavascriptExecutor) driver;
					// js.executeScript("arguments[0].dispatchEvent(new Event('mousedown'));",
					// element);
					// return;
					// }
					// }
					// } catch (Exception e) {
					// System.out.println("Exception in clickElementWithRetry: " + e.getMessage());
					// throw new RuntimeException("Failed to Select" + e.getMessage());
					// }

					// ===== STEP 9: SPECIAL HANDLING FOR SUBMIT BUTTONS =====
					if ("button".equalsIgnoreCase(element.getTagName())
							&& "submit".equalsIgnoreCase(element.getAttribute("type"))) {
						if (detailedLogging)
							System.out
									.println("   🔘 Detected submit button, forcing JavaScript click for reliability");
						clickWithJavaScript(element);
						return;
					}

					// ===== STEP 10: TRY STANDARD CLICK =====
					try {
						wait.until(ExpectedConditions.elementToBeClickable(element)).click();
						if (detailedLogging)
							System.out.println("✅ Clicked using Selenium");
						return;
					} catch (ElementClickInterceptedException e) {
						if (detailedLogging)
							System.err.println("⚠️ Click intercepted: " + e.getMessage());
						checkForBlockingElements();
						clickWithJavaScript(element);
						if (detailedLogging)
							System.out.println("✅ Clicked using JavaScript fallback");
						return;
					} catch (Exception clickEx) {
						if (detailedLogging)
							System.err.println("⚠️ Standard click failed, trying JavaScript...");
						clickWithJavaScript(element);
						if (detailedLogging)
							System.out.println("✅ Clicked using JavaScript fallback");
						return;
					}

				} catch (Exception findEx) {
					if (!findEx.getMessage().contains("Failed to Select")) {
						if (detailedLogging)
							System.err.println("❌ Element not found or click failed: " + findEx.getMessage());

						// ===== AG-GRID SCROLL RETRY =====
						try {
							if (detailedLogging)
								System.out.println("   🔄 Trying Ag-Grid scroll search...");
							// Pass 'false' to skip fallback to standard wait, since we already tried
							// standard wait in Step 2/9
							element = waitActions.waitForElementVisibleInGrid(xpath, false);
							scrollToElement(element);
							element.click();
							if (detailedLogging)
								System.out.println("   ✅ Clicked after grid scroll");
							return;
						} catch (Exception gridEx) {
							if (detailedLogging)
								System.out.println("   ⚠️ Grid scroll search failed");

						}

						// ===== TEXT FALLBACK =====
						if (isXPath && xpath.contains("text()=")) {
							try {
								String altLocator = xpath.replace("text()=",
										"contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '")
										.replace("']", "']").toLowerCase();
								WebElement fallback = wait
										.until(ExpectedConditions.elementToBeClickable(By.xpath(altLocator)));
								fallback.click();
								if (detailedLogging)
									System.out.println("✅ Clicked using case-insensitive text fallback");
								return;
							} catch (Exception fallbackEx) {
								if (detailedLogging)
									System.err.println("❌ Text fallback failed: " + fallbackEx.getMessage());

								throw new RuntimeException("Failed to click element: " + xpath);

							}
						}

						if (attempt == maxAttempts) {
							throw new RuntimeException(
									"❌ Failed after " + maxAttempts + " attempts: " + findEx.getMessage());
						}
					} else {
						throw new RuntimeException("Failed to select element: " + xpath + findEx.getMessage());
					}
				}

				// ===== STEP 10: TRY ACTIONS API =====
				try {
					waitActions.waitForLoadingSpinner();
					WebElement element1 = driver.findElement(By.xpath(xpath));
					new Actions(driver).moveToElement(element1).pause(Duration.ofMillis(300)).click().perform();
					waitActions.waitFor(500 * attempt);

					if (detailedLogging)
						System.out.println("✅ Clicked using Actions API");
					return;

				} catch (Exception actionsEx) {
					if (detailedLogging)
						System.err.println("⚠️ Actions click failed: " + actionsEx.getMessage());
					throw new RuntimeException("Failed to click element: " + xpath + actionsEx.getMessage());
				}

				// Wait before retrying

			} catch (Exception e) {
				throw new RuntimeException("❌ Thread interrupted during retry wait", e);
			}
		}
	}

	public void selectElementWithRetry(String xpath, String value) {
		boolean isXPath = xpath.startsWith("/") || xpath.startsWith("(") || xpath.contains("//");
		int maxAttempts = 3;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				if (detailedLogging)
					System.out.println("🔄 Attempt " + attempt + ": clicking -> " + xpath);

				// ===== STEP 1: ALWAYS WAIT FOR LOADING SPINNER =====
				waitActions.waitForLoadingSpinner();

				WebElement element;

				try {
					// ===== STEP 2: WAIT FOR ELEMENT PRESENCE =====
					element = waitActions.waitForElementPresent(xpath);
					if (detailedLogging)
						System.out.println("   ✅ Element found");
				} catch (Exception e) {
					throw new RuntimeException("Failed to find element: " + xpath + e.getMessage());
				}
				// ===== STEP 5: FIND CLICKABLE PARENT FOR <p> ELEMENTS =====
				if (xpath.matches(".*p\\[.*text\\(\\).*\\]$")) {
					element = findClickableParent(element);
				}

				// ===== STEP 6: SCROLL INTO VIEW =====
				scrollToElement(element);

				// ===== STEP 7: WAIT FOR SPINNER AGAIN =====
				waitActions.waitForLoadingSpinner();

				if ("select".equalsIgnoreCase(element.getTagName()) && value != null && !value.isEmpty()) {
					Select select = new Select(element);
					select.selectByVisibleText(value);
					return;
				} else if (xpath.contains("ng-select")) {
					JavascriptExecutor js = (JavascriptExecutor) driver;
					js.executeScript("arguments[0].dispatchEvent(new Event('mousedown'));", element);
					return;
				} else {
					// Fallback for custom dropdown items (spans, divs, lis, etc.)
					// If the element is not a <select>, we assume the action is to just click it
					// (e.g. selecting an option from a list)

					// Smart handling for duplicated elements (hidden vs visible)
					// Verify if multiple elements exist and pick the visible one
					java.util.List<WebElement> candidates = driver.findElements(By.xpath(xpath));
					WebElement targetElement = null;
					int visibleCount = 0;

					// Iterate through all candidates
					for (int i = 0; i < candidates.size(); i++) {
						WebElement cand = candidates.get(i);
						boolean isDisplayed = cand.isDisplayed();

						if (detailedLogging) {
							try {
								String outerHtml = cand.getAttribute("outerHTML");
								System.out.println("   🔍 Candidate #" + (i + 1) + " | Visible: " + isDisplayed
										+ " | Tag: " + cand.getTagName() + " | Text: " + cand.getText());
								System.out.println("      HTML: " + outerHtml);
							} catch (Exception ignore) {
							}
						}

						if (isDisplayed) {
							visibleCount++;
							targetElement = cand; // Update target to the latest visible element (LAST one)
						}
					}

					if (targetElement == null) {
						// If none are currently visible, fall back to standard wait
						try {
							targetElement = waitActions.waitForElementVisible(xpath);
						} catch (Exception e) {
							targetElement = element;
							if (detailedLogging)
								System.out.println("   ⚠️ No visible element found for: " + xpath);
						}
					} else {
						if (detailedLogging)
							System.out.println("   🎯 Selected Candidate #" + visibleCount + " (Last Visible)");
					}

					try {
						wait.until(ExpectedConditions.elementToBeClickable(targetElement)).click();
					} catch (Exception clickEx) {
						// Fallback to JS click if standard click fails
						JavascriptExecutor js = (JavascriptExecutor) driver;
						js.executeScript("arguments[0].click();", targetElement);
					}

					// Small wait to ensure selection registers
					waitActions.waitFor(500);

					return;
				}
			} catch (Exception e) {
				System.out.println("Exception in clickElementWithRetry: " + e.getMessage());
				throw new RuntimeException("Failed to Select" + e.getMessage());
			}

		}
	}

	/**
	 * Check if locator is for a grid button
	 */
	private boolean isGridButton(String locator) {
		return locator.contains("@role='row'") && locator.contains("@role='button'");
	}

	/**
	 * Special handling for grid buttons (row buttons)
	 */
	private void handleGridButtonClick(String locator, WebElement element) {
		if (detailedLogging)
			System.out.println("   🔘 Detected grid button, using special handling");

		String rowId = "unknown";
		if (locator.contains("@row-id='")) {
			rowId = locator.replaceAll(".*@row-id='([^']+)'.*", "$1");
		}
		if (detailedLogging)
			System.out.println("   📍 Row ID: " + rowId);

		scrollToElement(element);
		waitActions.waitFor(300);
		waitActions.waitForLoadingSpinner();

		try {
			WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
			element = shortWait.until(ExpectedConditions.elementToBeClickable(By.xpath(locator)));
			element.click();
			if (detailedLogging)
				System.out.println("✅ Grid button clicked (standard)");
		} catch (ElementClickInterceptedException e) {
			if (detailedLogging)
				System.err.println("⚠️ Grid button blocked by overlay, using JavaScript");
			checkForBlockingElements();
			clickWithJavaScript(element);
			if (detailedLogging)
				System.out.println("✅ Grid button clicked (JavaScript)");
		} catch (Exception e) {
			if (detailedLogging)
				System.err.println("⚠️ Grid button click failed, using JavaScript");
			clickWithJavaScript(element);
			if (detailedLogging)
				System.out.println("✅ Grid button clicked (JavaScript)");
		}

		waitActions.waitFor(500);
		waitActions.waitForLoadingSpinner();
	}

	/**
	 * Check if element is a hidden checkbox, radio, or file input
	 */
	private boolean isHiddenCheckbox(WebElement element, String type) {
		try {
			if (!element.isDisplayed()) {
				return "checkbox".equals(type) || "radio".equals(type) || "file".equals(type);
			}
		} catch (Exception e) {
			// Ignore
		}
		return false;
	}

	/**
	 * Handle hidden input clicks (checkbox, radio, file)
	 */
	private void handleHiddenInputClick(WebElement element, String type) {
		if (detailedLogging)
			System.out.println("   ☑️ Detected hidden " + type + " input, using special handling");

		String inputId = element.getAttribute("id");

		if (inputId != null && !inputId.isEmpty()) {
			try {
				WebElement label = driver.findElement(By.xpath("//label[@for='" + inputId + "']"));
				label.click();
				if (detailedLogging)
					System.out.println("✅ Clicked label for hidden input");
				return;
			} catch (Exception e) {
				if (detailedLogging)
					System.out.println("⚠️ Label not found, using JavaScript");
			}
		}

		// Fallback for non-file types (file input click via JS is often blocked)
		if (!"file".equals(type)) {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript(
					"arguments[0].checked = !arguments[0].checked; arguments[0].dispatchEvent(new Event('change'));",
					element);
			if (detailedLogging)
				System.out.println("✅ Toggled input using JavaScript");
		} else {
			// Try JS click for file input as last resort
			clickWithJavaScript(element);
		}
	}

	/**
	 * Find clickable parent element
	 */
	private WebElement findClickableParent(WebElement element) {
		try {
			if (detailedLogging)
				System.out.println("   🔍 Searching for clickable parent");

			JavascriptExecutor js = (JavascriptExecutor) driver;
			WebElement clickableParent = (WebElement) js.executeScript("let el = arguments[0];" + "let maxLevels = 5;"
					+ "let level = 0;" + "while (el && el !== document.body && level < maxLevels) {"
					+ "  let tag = el.tagName.toLowerCase();" + "  if (tag === 'button' || tag === 'a' || "
					+ "      el.onclick || " + "      el.classList.contains('btn') || "
					+ "      el.classList.contains('button') || " + "      el.getAttribute('role') === 'button' || "
					+ "      window.getComputedStyle(el).cursor === 'pointer') {" + "    return el;" + "  }"
					+ "  el = el.parentElement;" + "  level++;" + "}" + "return arguments[0];", element);

			if (clickableParent != null && !clickableParent.equals(element)) {
				String parentTag = clickableParent.getTagName();
				if (detailedLogging)
					System.out.println("   ✅ Found clickable parent: <" + parentTag + ">");
				return clickableParent;
			}
		} catch (Exception e) {
			if (detailedLogging)
				System.err.println("   ⚠️ Error finding clickable parent: " + e.getMessage());
		}

		return element;
	}

	/**
	 * Check for elements blocking clicks
	 */
	private void checkForBlockingElements() {
		try {
			String[] blockingSelectors = { "#cover-spin", ".loading-overlay", ".spinner", ".modal-backdrop",
					"[class*='overlay']" };

			for (String selector : blockingSelectors) {
				try {
					WebElement blocker = driver.findElement(By.cssSelector(selector));
					String display = blocker.getCssValue("display");
					if (!display.equals("none") && blocker.isDisplayed()) {
						if (detailedLogging)
							System.err.println(
									"   🚫 Blocking element found: " + selector + " (display=" + display + ")");
					}
				} catch (NoSuchElementException e) {
					// No blocker with this selector
				}
			}
		} catch (Exception e) {
			// Ignore errors in checking
		}
	}

	/**
	 * Click using JavaScript executor
	 */
	public void clickWithJavaScript(WebElement element) {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].click();", element);
		} catch (Exception e) {
			throw new RuntimeException("Failed to click with JavaScript", e);
		}
	}

	/**
	 * Click using JavaScript by xpath
	 */
	public void clickWithJavaScript(String xpath) {
		try {
			WebElement element = driver.findElement(By.xpath(xpath));
			clickWithJavaScript(element);
		} catch (Exception e) {
			throw new RuntimeException("Failed to click with JavaScript: " + xpath, e);
		}
	}

	/**
	 * Double click on element
	 */
	public void doubleClick(String xpath) {
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			scrollToElement(element);
			Actions actions = new Actions(driver);
			actions.doubleClick(element).perform();
		} catch (Exception e) {
			throw new RuntimeException("Failed to double click: " + xpath, e);
		}
	}

	/**
	 * Right click on element (context click)
	 */
	public void rightClick(String xpath) {
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			scrollToElement(element);
			Actions actions = new Actions(driver);
			actions.contextClick(element).perform();
		} catch (Exception e) {
			throw new RuntimeException("Failed to right click: " + xpath, e);
		}
	}

	/**
	 * Scroll to element
	 */
	private void scrollToElement(WebElement element) {
		try {
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
			waitActions.waitFor(300); // Small wait after scroll
		} catch (Exception e) {
			// Scroll failed, continue anyway
		}
	}

	/**
	 * Scroll to element by xpath (public method)
	 */
	public void scrollToElement(String xpath) {
		try {
			WebElement element = driver.findElement(By.xpath(xpath));
			scrollToElement(element);
		} catch (Exception e) {
			throw new RuntimeException("Failed to scroll to element: " + xpath, e);
		}
	}

	/**
	 * Hover over element
	 */
	public void hoverElement(String xpath) {
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			scrollToElement(element);
			Actions actions = new Actions(driver);
			actions.moveToElement(element).perform();
			waitActions.waitFor(300); // Small wait after hover
		} catch (Exception e) {
			throw new RuntimeException("Failed to hover: " + xpath, e);
		}
	}

	/**
	 * Click and hold element
	 */
	public void clickAndHold(String xpath) {
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			Actions actions = new Actions(driver);
			actions.clickAndHold(element).perform();
		} catch (Exception e) {
			throw new RuntimeException("Failed to click and hold: " + xpath, e);
		}
	}

	/**
	 * Release held element
	 */
	public void release(String xpath) {
		try {
			WebElement element = driver.findElement(By.xpath(xpath));
			Actions actions = new Actions(driver);
			actions.release(element).perform();
		} catch (Exception e) {
			throw new RuntimeException("Failed to release: " + xpath, e);
		}
	}

	/**
	 * Drag and drop element
	 */
	public void dragAndDrop(String sourceXpath, String targetXpath) {
		try {
			WebElement source = waitActions.waitForElementVisible(sourceXpath);
			WebElement target = waitActions.waitForElementVisible(targetXpath);
			Actions actions = new Actions(driver);
			actions.dragAndDrop(source, target).perform();
		} catch (Exception e) {
			throw new RuntimeException("Failed to drag and drop from " + sourceXpath + " to " + targetXpath, e);
		}
	}

	/**
	 * Click at specific offset from element
	 */
	public void clickAtOffset(String xpath, int xOffset, int yOffset) {
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			Actions actions = new Actions(driver);
			actions.moveToElement(element, xOffset, yOffset).click().perform();
		} catch (Exception e) {
			throw new RuntimeException("Failed to click at offset: " + xpath, e);
		}
	}

	/**
	 * Move to element with offset
	 */
	public void moveToElementWithOffset(String xpath, int xOffset, int yOffset) {
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			Actions actions = new Actions(driver);
			actions.moveToElement(element, xOffset, yOffset).perform();
		} catch (Exception e) {
			throw new RuntimeException("Failed to move to element with offset: " + xpath, e);
		}
	}
}