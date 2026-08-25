package com.eit.automation.actions;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.eit.automation.annotations.Action;
import io.qameta.allure.Step;

public class InputActions {

	private WebDriver driver;
	private WebDriverWait wait;
	private WaitActions waitActions;

	// Logging configuration
	private boolean detailedLogging = false;

	public InputActions(WebDriver driver, WebDriverWait wait, WaitActions waitActions) {
		this.driver = driver;
		this.wait = wait;
		this.waitActions = waitActions;
	}

	/**
	 * Type text into input field
	 */
	@Action(keys = { "type", "enter", "input" })
	@Step("Typing text '{1}' into element: {0}")
	public void typeText(String xpath, String text) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Typing text '" + text + "' into: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			element.clear();
			element.sendKeys(text);
		} catch (Exception e) {
			throw new RuntimeException("Failed to type text into element: " + xpath, e);
		}
	}

	/**
	 * Type text into WebElement (PageFactory support)
	 */
	@Action(keys = { "type", "enter", "input" })
	@Step("Typing text '{1}' into element: {0}")
	public void typeText(WebElement element, String text) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Typing text '" + text + "' into PageFactory element");
		try {
			wait.until(ExpectedConditions.visibilityOf(element));
			element.clear();
			element.sendKeys(text);
		} catch (Exception e) {
			throw new RuntimeException("Failed to type text into PageFactory element", e);
		}
	}

	/**
	 * Type text slowly (character by character)
	 */
	public void typeTextSlowly(String xpath, String text, long delayMs) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Typing text slowly '" + text + "' into: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			element.clear();

			for (char c : text.toCharArray()) {
				element.sendKeys(String.valueOf(c));
				waitActions.waitFor(delayMs);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to type text slowly: " + xpath, e);
		}
	}

	/**
	 * Type text using JavaScript
	 */
	public void typeTextWithJavaScript(String xpath, String text) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Typing JS text '" + text + "' into: " + xpath);
		try {
			WebElement element = driver.findElement(By.xpath(xpath));
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("arguments[0].value = arguments[1];", element, text);
		} catch (Exception e) {
			throw new RuntimeException("Failed to type with JavaScript: " + xpath, e);
		}
	}

	/**
	 * Clear input field
	 */
	public void clearField(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Clearing field: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			element.clear();
		} catch (Exception e) {
			throw new RuntimeException("Failed to clear field: " + xpath, e);
		}
	}

	/**
	 * Clear field using backspace
	 */
	public void clearFieldWithBackspace(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Clearing field with backspace: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			String value = element.getAttribute("value");
			if (value != null) {
				for (int i = 0; i < value.length(); i++) {
					element.sendKeys(Keys.BACK_SPACE);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to clear with backspace: " + xpath, e);
		}
	}

	/**
	 * Select from dropdown by visible text
	 */
	public void selectByVisibleText(String xpath, String text) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Selecting '" + text + "' from: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			Select dropdown = new Select(element);
			dropdown.selectByVisibleText(text);
		} catch (Exception e) {
			throw new RuntimeException("Failed to select by text '" + text + "': " + xpath, e);
		}
	}

	/**
	 * Select from dropdown by value
	 */
	public void selectByValue(String xpath, String value) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Selecting value '" + value + "' from: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			Select dropdown = new Select(element);
			dropdown.selectByValue(value);
		} catch (Exception e) {
			throw new RuntimeException("Failed to select by value '" + value + "': " + xpath, e);
		}
	}

	/**
	 * Select from dropdown by index
	 */
	public void selectByIndex(String xpath, int index) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Selecting index " + index + " from: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			Select dropdown = new Select(element);
			dropdown.selectByIndex(index);
		} catch (Exception e) {
			throw new RuntimeException("Failed to select by index " + index + ": " + xpath, e);
		}
	}

	/**
	 * Press Enter key
	 */
	public void pressEnter(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Pressing Enter on: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			element.sendKeys(Keys.ENTER);
		} catch (Exception e) {
			throw new RuntimeException("Failed to press Enter: " + xpath, e);
		}
	}

	/**
	 * Press Tab key
	 */
	public void pressTab(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Pressing Tab on: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			element.sendKeys(Keys.TAB);
		} catch (Exception e) {
			throw new RuntimeException("Failed to press Tab: " + xpath, e);
		}
	}

	/**
	 * Type and press Enter
	 */
	public void typeAndEnter(String xpath, String text) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Typing '" + text + "' and pressing Enter on: " + xpath);
		try {
			// Don't call typeText directly to avoid double spinner wait
			WebElement element = waitActions.waitForElementVisible(xpath);
			element.clear();
			element.sendKeys(text);
			element.sendKeys(Keys.ENTER);
		} catch (Exception e) {
			throw new RuntimeException("Failed to type and enter: " + xpath, e);
		}
	}

	/**
	 * Check checkbox
	 */
	public void checkCheckbox(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Checking checkbox: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			if (!element.isSelected()) {
				element.click();
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to check checkbox: " + xpath, e);
		}
	}

	/**
	 * Uncheck checkbox
	 */
	public void uncheckCheckbox(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Unchecking checkbox: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			if (element.isSelected()) {
				element.click();
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to uncheck checkbox: " + xpath, e);
		}
	}

	/**
	 * Select radio button
	 */
	public void selectRadioButton(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Selecting radio button: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			if (!element.isSelected()) {
				element.click();
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to select radio button: " + xpath, e);
		}
	}

	/**
	 * Press Escape key
	 */
	public void pressEscape(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Pressing Escape on: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			element.sendKeys(Keys.ESCAPE);
		} catch (Exception e) {
			throw new RuntimeException("Failed to press Escape: " + xpath, e);
		}
	}

	/**
	 * Press any key
	 */
	public void pressKey(String xpath, Keys key) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Pressing key " + key.name() + " on: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			element.sendKeys(key);
		} catch (Exception e) {
			throw new RuntimeException("Failed to press key: " + xpath, e);
		}
	}

	/**
	 * Send keyboard keys (for special key combinations)
	 */
	public void sendKeys(String xpath, CharSequence... keys) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Sending keys to: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			element.sendKeys(keys);
		} catch (Exception e) {
			throw new RuntimeException("Failed to send keys: " + xpath, e);
		}
	}

	private void log(String message) {
		if (detailedLogging) {
			System.out.println("[INPUT] " + message);
		}
	}
}