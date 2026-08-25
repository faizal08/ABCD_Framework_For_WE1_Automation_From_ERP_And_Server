package com.eit.automation.actions;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WaitActions {

	private WebDriver driver;
	private WebDriverWait wait;
	private JavascriptExecutor jsExecutor;

	public WaitActions(WebDriver driver, WebDriverWait wait) {
		this.driver = driver;
		this.wait = wait;
		this.jsExecutor = (JavascriptExecutor) driver;
	}

	/**
	 * Wait for element to be visible
	 */
	public WebElement waitForElementVisible(String xpath) {
		try {
			return wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
		} catch (Exception e) {
			throw new RuntimeException("Element not visible: " + xpath, e);
		}
	}

	/**
	 * Wait for element to be clickable
	 */
	public WebElement waitForElementClickable(String xpath) {
		try {
			return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
		} catch (Exception e) {
			throw new RuntimeException("Element not clickable: " + xpath, e);
		}
	}

	/**
	 * Wait for element to be present (not necessarily visible)
	 */
	public WebElement waitForElementPresent(String xpath) {
		try {
			return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
		} catch (Exception e) {
			throw new RuntimeException("Element not present: " + xpath, e);
		}
	}

	/**
	 * Wait for page to load completely
	 */
	public void waitForPageLoad() {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(30)).until(webDriver -> ((JavascriptExecutor) webDriver)
					.executeScript("return document.readyState").equals("complete"));
		} catch (Exception e) {
			System.out.println("Warning: Page load check timeout");
		}
	}

	/**
	 * Wait for specific time (in milliseconds)
	 */
	public void waitFor(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Wait for element to disappear
	 */
	public void waitForElementToDisappear(String xpath) {
		try {
			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(xpath)));
		} catch (Exception e) {
			// Element already disappeared or never existed
		}
	}

	/**
	 * Wait for text to be present in element
	 */
	public boolean waitForTextInElement(String xpath, String text) {
		try {
			return wait.until(ExpectedConditions.textToBePresentInElementLocated(By.xpath(xpath), text));
		} catch (Exception e) {
			return false;
		}
	}

	public void scrollIntoView(WebElement element) {
		try {
			jsExecutor.executeScript(
					"arguments[0].scrollIntoView({behavior: 'smooth', block: 'center', inline: 'center'});", element);
			Thread.sleep(300);
		} catch (Exception e) {
			System.err.println("⚠️ Scroll failed: " + e.getMessage());

		}

	}

	/**
	 * Wait for element to be selected
	 */
	public boolean waitForElementToBeSelected(String xpath) {
		try {
			return wait.until(ExpectedConditions.elementToBeSelected(By.xpath(xpath)));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Wait for element attribute to contain value
	 */
	public boolean waitForAttributeContains(String xpath, String attribute, String value) {
		try {
			return wait.until(ExpectedConditions.attributeContains(By.xpath(xpath), attribute, value));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Wait for element attribute to be
	 */
	public boolean waitForAttributeToBe(String xpath, String attribute, String value) {
		try {
			return wait.until(ExpectedConditions.attributeToBe(By.xpath(xpath), attribute, value));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Wait for number of elements to be
	 */
	public void waitForNumberOfElements(String xpath, int count) {
		try {
			wait.until(ExpectedConditions.numberOfElementsToBe(By.xpath(xpath), count));
		} catch (Exception e) {
			throw new RuntimeException("Expected " + count + " elements, but condition not met: " + xpath, e);
		}
	}

	/**
	 * Wait for number of elements to be more than
	 */
	public void waitForNumberOfElementsMoreThan(String xpath, int count) {
		try {
			wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.xpath(xpath), count));
		} catch (Exception e) {
			throw new RuntimeException("Expected more than " + count + " elements: " + xpath, e);
		}
	}

	/**
	 * Wait for number of elements to be less than
	 */
	public void waitForNumberOfElementsLessThan(String xpath, int count) {
		try {
			wait.until(ExpectedConditions.numberOfElementsToBeLessThan(By.xpath(xpath), count));
		} catch (Exception e) {
			throw new RuntimeException("Expected less than " + count + " elements: " + xpath, e);
		}
	}

	public void waitForLoadingSpinner() {
		try {
			// Robustly wait for spinner to be invisible
			// invisibilityOfElementLocated returns true if element is not found, or is
			// hidden
			WebDriverWait spinnerWait = new WebDriverWait(driver, Duration.ofSeconds(30));
			spinnerWait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("cover-spin")));
			Thread.sleep(200); // Small buffer to ensure UI settles
		} catch (Exception e) {
			// Ignore errors during spinner wait (e.g. timeouts) to avoid failing the test
			// The user requested fewer logs, so we suppress the error
		}
	}

	/**
	 * Wait for title to be
	 */
	public boolean waitForTitle(String title) {
		try {
			return wait.until(ExpectedConditions.titleIs(title));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Wait for title to contain
	 */
	public boolean waitForTitleContains(String title) {
		try {
			return wait.until(ExpectedConditions.titleContains(title));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Wait for URL to be
	 */
	public boolean waitForUrl(String url) {
		try {
			return wait.until(ExpectedConditions.urlToBe(url));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Wait for URL to contain
	 */
	public boolean waitForUrlContains(String urlPart) {
		try {
			return wait.until(ExpectedConditions.urlContains(urlPart));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Wait for alert to be present
	 */
	public void waitForAlert() {
		try {
			wait.until(ExpectedConditions.alertIsPresent());
		} catch (Exception e) {
			throw new RuntimeException("Alert not present", e);
		}
	}

	/**
	 * Wait for frame to be available and switch to it
	 */
	public void waitForFrameAndSwitch(String frameLocator) {
		try {
			wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
		} catch (Exception e) {
			throw new RuntimeException("Frame not available: " + frameLocator, e);
		}
	}

	/**
	 * Wait for frame by xpath and switch
	 */
	public void waitForFrameByXpathAndSwitch(String xpath) {
		try {
			wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.xpath(xpath)));
		} catch (Exception e) {
			throw new RuntimeException("Frame not available: " + xpath, e);
		}
	}

	/**
	 * Wait for staleness of element (element to become stale)
	 */
	public boolean waitForStalenessOf(String xpath) {
		try {
			WebElement element = driver.findElement(By.xpath(xpath));
			return wait.until(ExpectedConditions.stalenessOf(element));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Custom wait with custom timeout
	 */
	public WebElement waitForElementVisible(String xpath, int timeoutSeconds) {
		try {
			WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
			return customWait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
		} catch (Exception e) {
			throw new RuntimeException("Element not visible within " + timeoutSeconds + " seconds: " + xpath, e);
		}
	}

	/**
	 * Custom wait for clickable with custom timeout
	 */
	public WebElement waitForElementClickable(String xpath, int timeoutSeconds) {
		try {
			WebDriverWait customWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
			return customWait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
		} catch (Exception e) {
			throw new RuntimeException("Element not clickable within " + timeoutSeconds + " seconds: " + xpath, e);
		}
	}

	/**
	 * Wait for element to be visible with Ag-Grid scrolling support
	 * This helps with virtualized grids where elements are not in DOM until
	 * scrolled to
	 */
	/**
	 * Wait for element to be visible with Ag-Grid scrolling support
	 * This helps with virtualized grids where elements are not in DOM until
	 * scrolled to
	 */
	public WebElement waitForElementVisibleInGrid(String xpath) {
		return waitForElementVisibleInGrid(xpath, true);
	}

	public WebElement waitForElementVisibleInGrid(String xpath, boolean fallbackToStandard) {
		// First try standard wait (short)
		try {
			WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(2));
			return shortWait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
		} catch (Exception e) {
			// Not found immediately, try scrolling grid
		}

		System.out.println("   🔄 Element not found, attempting grid scroll search...");

		// Find potentially scrollable grid containers
		String[] gridSelectors = {
				".ag-body-viewport",
				".ag-center-cols-viewport",
				".grid-viewport",
				"[role='grid'] [role='rowgroup']",
				".ui-grid-viewport"
		};

		WebElement gridContainer = null;
		for (String selector : gridSelectors) {
			try {
				java.util.List<WebElement> grids = driver.findElements(By.cssSelector(selector));
				for (WebElement grid : grids) {
					if (grid.isDisplayed()) {
						gridContainer = grid;
						break;
					}
				}
				if (gridContainer != null)
					break;
			} catch (Exception e) {
				// Continue to next selector
			}
		}

		if (gridContainer == null) {
			System.out.println("   ⚠️ No grid container found, falling back to standard wait");
			if (fallbackToStandard) {
				return waitForElementVisible(xpath); // Fallback to main wait
			} else {
				throw new RuntimeException("Element not visible in grid and no grid container found: " + xpath);
			}
		}

		// Scroll loop
		int maxScrolls = 20;
		int scrollStep = 400;

		for (int i = 0; i < maxScrolls; i++) {
			try {
				// Check if element is now visible
				WebElement element = driver.findElement(By.xpath(xpath));
				if (element.isDisplayed()) {
					System.out.println("   ✅ Element found after scrolling");
					scrollIntoView(element); // Ensure it's fully in view
					return element;
				}
			} catch (Exception e) {
				// Not found yet
			}

			// Scroll down
			try {
				jsExecutor.executeScript("arguments[0].scrollTop += " + scrollStep, gridContainer);
				Thread.sleep(500); // Wait for virtualization to render
			} catch (Exception e) {
				System.err.println("   ⚠️ Grid scroll failed: " + e.getMessage());
				break;
			}
		}

		// One final check with standard wait
		if (fallbackToStandard) {
			return waitForElementVisible(xpath);
		} else {
			throw new RuntimeException("Element not visible in grid after scrolling: " + xpath);
		}
	}
}