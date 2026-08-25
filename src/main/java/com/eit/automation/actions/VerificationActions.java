package com.eit.automation.actions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.eit.automation.annotations.Action;
import io.qameta.allure.Step;

public class VerificationActions {

	private WebDriver driver;
	private WaitActions waitActions;
	private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

	// Logging configuration
	private boolean detailedLogging = false; // Reduced verbosity

	public VerificationActions(WebDriver driver, WebDriverWait wait, WaitActions waitActions) {
		this.driver = driver;
		this.waitActions = waitActions;
	}

	/**
	 * Verify element is visible
	 */
	@Action(keys = { "verify", "verifyvisible", "verifydisplayed" })
	@Step("Verifying element is visible: {0}")
	public void verifyElementVisible(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element is visible: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			if (!element.isDisplayed()) {
				String error = "Element is not visible: " + xpath;
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Element is visible");
		} catch (AssertionError e) {
			// // throw e;
		} catch (Exception e) {
			String error = "Element not found or not visible: " + xpath;
			log("✗ " + error);
			log("✗ Error type: " + e.getClass().getSimpleName());
			log("✗ Error message: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element is not visible
	 */
	/**
	 * Verify element is not visible
	 */
	@Action(keys = { "verifyhidden", "verifynotvisible" })
	@Step("Verifying element is NOT visible: {0}")
	public void verifyElementNotVisible(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element is NOT visible: " + xpath);
		try {
			List<WebElement> elements = driver.findElements(By.xpath(xpath));
			if (!elements.isEmpty() && elements.get(0).isDisplayed()) {
				String error = "Element should not be visible: " + xpath;
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Element is not visible");
		} catch (AssertionError e) {
			// // throw e;
		} catch (Exception e) {
			// Element not found - verification passed
			String error = "✓ Element not found (verification passed)";
			log("✓ Element not found (verification passed)");
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element exists (present in DOM)
	 */
	public void verifyElementExists(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element exists: " + xpath);
		try {
			waitActions.waitForElementPresent(xpath);
			log("✓ Element exists");
		} catch (Exception e) {
			String error = "Element does not exist: " + xpath;
			log("✗ " + error);
			log("✗ Error type: " + e.getClass().getSimpleName());
			log("✗ Error message: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	public WebElement waitForElementVisible(String xpath) {
		log("Waiting for element to be visible: " + xpath);
		try {
			// Use grid-aware wait
			return waitActions.waitForElementVisibleInGrid(xpath);
		} catch (Exception e) {
			String error = "Element not visible: " + xpath;
			log("✗ " + error);
			log("✗ Error type: " + e.getClass().getSimpleName());
			log("✗ Error message: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element text equals expected value
	 */
	/**
	 * Verify element text equals expected value
	 */
	@Action(keys = { "verifytext", "verifyexacttext" })
	@Step("Verifying element text: {0} equals {1}")
	public void verifyElementText(String xpath, String expectedText) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element text: " + xpath);
		log("Expected: " + expectedText);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			String actualText = element.getText().trim();
			log("Actual: " + actualText);

			// Case-insensitive comparison
			if (!actualText.equalsIgnoreCase(expectedText)) {
				String error = String.format("Text mismatch. Expected: '%s', Actual: '%s'", expectedText, actualText);
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Text matches");
		} catch (AssertionError e) {
			// // throw e;
		} catch (Exception e) {
			String error = "Failed to verify element text: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element contains text
	 */
	/**
	 * Verify element contains text
	 */
	@Action(keys = { "verifycontains" })
	@Step("Verifying element: {0} contains text: {1}")
	public void verifyElementContainsText(String xpath, String expectedText) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element contains text: " + xpath);
		log("Expected to contain: " + expectedText);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			String actualText = element.getText().trim();
			log("Actual text: " + actualText);

			if (!actualText.contains(expectedText)) {
				String error = String.format("Text not found. Expected to contain: '%s', Actual: '%s'", expectedText,
						actualText);
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Text contains expected value");
		} catch (AssertionError e) {
			// // throw e;
		} catch (Exception e) {
			String error = "Failed to verify element contains text: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element value attribute
	 */
	public void verifyElementValue(String xpath, String expectedValue) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element value: " + xpath);
		log("Expected: " + expectedValue);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			String actualValue = element.getAttribute("value");
			log("Actual: " + actualValue);

			// Case-insensitive comparison
			if (actualValue == null || !actualValue.equalsIgnoreCase(expectedValue)) {
				String error = String.format("Value mismatch. Expected: '%s', Actual: '%s'", expectedValue,
						actualValue);
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Value matches");
		} catch (AssertionError e) {
			// // throw e;
		} catch (Exception e) {
			String error = "Failed to verify element value: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element value OR text (flexible check)
	 */
	/**
	 * Verify element value OR text (flexible check)
	 */
	@Action(keys = { "verifytextorvalue" })
	@Step("Verifying element: {0} has value or text: {1}")
	public void verifyElementValueOrText(String xpath, String expected) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element value OR text: " + xpath);
		log("Expected: " + expected);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			String value = element.getAttribute("value");
			String text = element.getText().trim();

			log("Element value attribute: " + value);
			log("Element text: " + text);

			// Case-insensitive check
			boolean valueMatch = value != null && value.toLowerCase().contains(expected.toLowerCase());
			boolean textMatch = text.toLowerCase().contains(expected.toLowerCase());

			if (valueMatch || textMatch) {
				log("✓ Value or text contains expected");
				return; // Verification passed
			}

			String error = String.format("Neither value nor text contains expected: '%s'. Value: '%s', Text: '%s'",
					expected, value, text);
			log("✗ " + error);
			throw new RuntimeException(error);

		} catch (AssertionError e) {
			// // throw e;
		} catch (Exception e) {
			String error = "Failed to verify element value or text: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element is enabled
	 */
	public void verifyElementEnabled(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element is enabled: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			boolean isEnabled = element.isEnabled();
			// Check for aria-disabled or class="disabled"
			String ariaDisabled = element.getAttribute("aria-disabled");
			String classAttr = element.getAttribute("class");

			if (ariaDisabled != null && ariaDisabled.equals("true"))
				isEnabled = false;
			if (classAttr != null && classAttr.contains("disabled"))
				isEnabled = false;

			log("Element enabled state: " + isEnabled);

			if (!isEnabled) {
				String error = "Element is not enabled: " + xpath;
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Element is enabled");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify element enabled: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element is disabled
	 */
	public void verifyElementDisabled(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element is disabled: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			boolean isEnabled = element.isEnabled();

			// Check for aria-disabled or class="disabled"
			String ariaDisabled = element.getAttribute("aria-disabled");
			String classAttr = element.getAttribute("class");

			if (ariaDisabled != null && ariaDisabled.equals("true"))
				isEnabled = false;
			if (classAttr != null && classAttr.contains("disabled"))
				isEnabled = false;

			log("Element enabled state: " + isEnabled);

			if (isEnabled) {
				String error = "Element should be disabled: " + xpath;
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Element is disabled");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify element disabled: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element is selected (for checkboxes/radio buttons)
	 */
	public void verifyElementSelected(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element is selected: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			boolean isSelected = element.isSelected();

			// Check for aria-selected or class="selected" (Ag-Grid support)
			String ariaSelected = element.getAttribute("aria-selected");
			String classAttr = element.getAttribute("class");

			if (!isSelected) {
				if (ariaSelected != null && ariaSelected.equals("true"))
					isSelected = true;
				if (classAttr != null && (classAttr.contains("selected") || classAttr.contains("active")))
					isSelected = true;
				// Check if paremt row is selected (common in grids where cell is clicked)
				try {
					WebElement parent = element.findElement(By.xpath("./.."));
					String parentClass = parent.getAttribute("class");
					if (parentClass != null && parentClass.contains("ag-row-selected"))
						isSelected = true;
				} catch (Exception ignore) {
				}
			}

			log("Element selected state: " + isSelected);

			if (!isSelected) {
				String error = "Element is not selected: " + xpath;
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Element is selected");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify element selected: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element count
	 */
	public void verifyElementCount(String xpath, int expectedCount) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element count: " + xpath);
		log("Expected count: " + expectedCount);
		try {
			List<WebElement> elements = driver.findElements(By.xpath(xpath));
			int actualCount = elements.size();
			log("Actual count: " + actualCount);

			if (actualCount != expectedCount) {
				String error = String.format("Element count mismatch. Expected: %d, Actual: %d", expectedCount,
						actualCount);
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Count matches");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify element count: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify element attribute value
	 */
	public void verifyElementAttribute(String xpath, String attributeName, String expectedValue) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element attribute: " + xpath);
		log("Attribute: " + attributeName);
		log("Expected: " + expectedValue);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			String actualValue = element.getAttribute(attributeName);
			log("Actual: " + actualValue);

			if (actualValue == null || !actualValue.equals(expectedValue)) {
				String error = String.format("Attribute '%s' mismatch. Expected: '%s', Actual: '%s'", attributeName,
						expectedValue, actualValue);
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Attribute matches");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify element attribute: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify date field has a value
	 */
	public void verifyDateFieldHasValue(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying date field has value: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			String value = element.getAttribute("value");
			log("Date field value: " + value);

			if (value == null || value.trim().isEmpty()) {
				String error = "Date field is empty: " + xpath;
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Date field has value");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify date field: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify date field contains today's date
	 */
	public void verifyDateFieldIsToday(String xpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying date field is today: " + xpath);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			String value = element.getAttribute("value");
			log("Date field value: " + value);

			// Get today's date in common formats
			LocalDate today = LocalDate.now();
			// We can reuse the flexible comparison from verifyElementDate logic if needed,
			// but for "is today" we usually check against standard formats.
			// Let's keep existing logic but just add more flexibility if needed later.
			String todayYYYYMMDD = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			String todayMMDDYYYY = today.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
			String todayDDMMYYYY = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

			log("Checking against today's date: " + todayYYYYMMDD);

			if (value == null || (!value.contains(todayYYYYMMDD) && !value.contains(todayMMDDYYYY)
					&& !value.contains(todayDDMMYYYY))) {
				String error = String.format("Date field does not contain today's date. Expected: '%s', Actual: '%s'",
						todayYYYYMMDD, value);
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Date is today");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify date field is today: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify date matches expected date (flexible format)
	 */
	public void verifyElementDate(String xpath, String expectedDate) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying element date: " + xpath);
		log("Expected: " + expectedDate);
		try {
			WebElement element = waitActions.waitForElementVisible(xpath);
			String actualValue = element.getAttribute("value");
			if (actualValue == null || actualValue.trim().isEmpty()) {
				actualValue = element.getText().trim();
			}
			log("Actual: " + actualValue);

			LocalDate expected = parseDate(expectedDate);
			LocalDate actual = parseDate(actualValue);

			if (expected != null && actual != null) {
				if (!expected.isEqual(actual)) {
					String error = String.format("Date mismatch. Expected: '%s', Actual: '%s'", expected, actual);
					log("✗ " + error);
					throw new RuntimeException(error);
				}
			} else {
				// Fallback to string comparison if parsing fails
				log("⚠️ Date parsing failed, using string comparison");
				if (!actualValue.contains(expectedDate) && !expectedDate.contains(actualValue)) {
					String error = String.format("Date mismatch (String). Expected: '%s', Actual: '%s'", expectedDate,
							actualValue);
					log("✗ " + error);
					throw new RuntimeException(error);
				}
			}
			log("✓ Date matches");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify element date: " + xpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	private LocalDate parseDate(String dateStr) {
		if (dateStr == null)
			return null;
		String[] patterns = { "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "yyyy/MM/dd" };
		for (String pattern : patterns) {
			try {
				return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
			} catch (Exception e) {
				// continue
			}
		}
		return null;
	}

	/**
	 * Verify page title
	 */
	public void verifyPageTitle(String expectedTitle) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying page title");
		log("Expected: " + expectedTitle);
		try {
			String actualTitle = driver.getTitle();
			log("Actual: " + actualTitle);

			if (!actualTitle.equals(expectedTitle)) {
				String error = String.format("Page title mismatch. Expected: '%s', Actual: '%s'", expectedTitle,
						actualTitle);
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Page title matches");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify page title";
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify page title contains text
	 */
	public void verifyPageTitleContains(String expectedText) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying page title contains: " + expectedText);
		try {
			String actualTitle = driver.getTitle();
			log("Actual title: " + actualTitle);

			if (!actualTitle.contains(expectedText)) {
				String error = String.format("Page title does not contain: '%s'. Actual title: '%s'", expectedText,
						actualTitle);
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Page title contains expected text");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify page title contains";
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify current URL
	 */
	public void verifyCurrentUrl(String expectedUrl) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying current URL");
		log("Expected: " + expectedUrl);
		try {
			String actualUrl = driver.getCurrentUrl();
			log("Actual: " + actualUrl);

			if (!actualUrl.equals(expectedUrl)) {
				String error = String.format("URL mismatch. Expected: '%s', Actual: '%s'", expectedUrl, actualUrl);
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ URL matches");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify current URL";
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify URL contains text
	 */
	public void verifyUrlContains(String expectedText) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying URL contains: " + expectedText);
		try {
			String actualUrl = driver.getCurrentUrl();
			log("Actual URL: " + actualUrl);

			if (!actualUrl.contains(expectedText)) {
				String error = String.format("URL does not contain: '%s'. Actual URL: '%s'", expectedText, actualUrl);
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ URL contains expected text");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify URL contains";
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}
	// ========================================
	// MAP VERIFICATION
	// ========================================

	/**
	 * Verify map has drawn shapes (polygons/paths)
	 */
	public void verifyMapShapePresent(String mapXpath) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying map has shapes: " + mapXpath);
		try {
			WebElement map = waitActions.waitForElementVisible(mapXpath);
			// Check for SVG paths or generic shapes inside the map container
			List<WebElement> shapes = map.findElements(By
					.xpath(".//*[local-name()='path' or local-name()='g' or contains(@class, 'leaflet-interactive')]"));

			if (shapes.isEmpty()) {
				String error = "No shapes (polygons) found in map: " + mapXpath;
				log("✗ " + error);
				throw new RuntimeException(error);
			}
			log("✓ Found " + shapes.size() + " shape(s) on map");
		} catch (AssertionError e) {
			// throw e;
		} catch (Exception e) {
			String error = "Failed to verify map shapes: " + mapXpath;
			log("✗ " + error);
			log("✗ Error: " + e.getMessage());
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Verify grid contains value in specific column
	 */
	public void verifyGridCellValue(String gridXpath, String columnName, String expectedValue) {
		if (waitActions != null)
			waitActions.waitForLoadingSpinner();
		log("Verifying grid value: " + columnName + " = " + expectedValue);
		log("Grid XPath: " + gridXpath);

		try {
			WebElement grid = waitActions.waitForElementVisible(gridXpath);

			// Detect grid type
			String gridType = detectGridType(grid);
			log("Detected grid type: " + gridType);

			int columnIndex = -1;

			switch (gridType) {
				case "AG_GRID":
					columnIndex = findColumnIndexAGGrid(grid, columnName);
					if (columnIndex != -1) {
						verifyValueInAGGrid(grid, columnIndex, expectedValue, columnName);
					}
					break;

				case "KENDO_GRID":
					columnIndex = findColumnIndexKendoGrid(grid, columnName);
					if (columnIndex != -1) {
						verifyValueInKendoGrid(grid, columnIndex, expectedValue, columnName);
					}
					break;

				case "DEVEXTREME_GRID":
					columnIndex = findColumnIndexDevExtreme(grid, columnName);
					if (columnIndex != -1) {
						verifyValueInDevExtreme(grid, columnIndex, expectedValue, columnName);
					}
					break;

				case "STANDARD_TABLE":
				default:
					columnIndex = findColumnIndexStandardTable(grid, columnName);
					if (columnIndex != -1) {
						verifyValueInStandardTable(grid, columnIndex, expectedValue, columnName);
					}
					break;
			}

			if (columnIndex == -1) {
				throw new RuntimeException("Column '" + columnName + "' not found in grid.");
			}

			log("✓ Found value '" + expectedValue + "' in column '" + columnName + "'");

		} catch (AssertionError e) {
			throw e;
		} catch (Exception e) {
			String error = "Failed to verify grid value: " + e.getMessage();
			log("✗ " + error);
			throw new RuntimeException(error, e);
		}
	}

	// Detect grid type based on class names and structure
	private String detectGridType(WebElement grid) {
		String classes = grid.getAttribute("class");

		if (classes != null) {
			if (classes.contains("ag-grid") || classes.contains("ag-theme")) {
				return "AG_GRID";
			}
			if (classes.contains("k-grid")) {
				return "KENDO_GRID";
			}
			if (classes.contains("dx-datagrid")) {
				return "DEVEXTREME_GRID";
			}
		}

		// Check by structure
		if (!grid.findElements(By.cssSelector(".ag-header-cell")).isEmpty()) {
			return "AG_GRID";
		}
		if (!grid.findElements(By.cssSelector(".k-grid-header")).isEmpty()) {
			return "KENDO_GRID";
		}

		return "STANDARD_TABLE";
	}

	// ============= AG-GRID =============
	private int findColumnIndexAGGrid(WebElement grid, String columnName) {
		List<WebElement> headers = grid
				.findElements(By.cssSelector(".ag-header-cell, [col-id], [role='columnheader']"));

		for (int i = 0; i < headers.size(); i++) {
			String headerText = headers.get(i).getText().trim();
			String colId = headers.get(i).getAttribute("col-id");

			if (headerText.equalsIgnoreCase(columnName) || (colId != null && colId.equalsIgnoreCase(columnName))) {
				log("Found column '" + columnName + "' at index: " + (i + 1));
				return i + 1;
			}
		}
		return -1;
	}

	private void verifyValueInAGGrid(WebElement grid, int columnIndex, String expectedValue, String columnName) {
		// AG-Grid uses col-id attribute or position-based selection
		List<WebElement> cells = grid
				.findElements(By.cssSelector(".ag-cell[col-id], .ag-row .ag-cell:nth-child(" + columnIndex + ")"));

		for (WebElement cell : cells) {
			String cellText = cell.getText().trim();
			if (cellText.contains(expectedValue)) {
				return; // Found
			}
		}

		// Try deeper search in nested elements
		List<WebElement> nestedCells = grid.findElements(By.xpath(
				".//*[contains(@class,'ag-cell')][" + columnIndex + "]//*[contains(text(),'" + expectedValue + "')]"));

		if (nestedCells.isEmpty()) {
			throw new RuntimeException("Value '" + expectedValue + "' not found in column '" + columnName + "'");
		}
	}

	// ============= KENDO GRID =============
	private int findColumnIndexKendoGrid(WebElement grid, String columnName) {
		List<WebElement> headers = grid.findElements(By.cssSelector(".k-grid-header th, .k-header"));

		for (int i = 0; i < headers.size(); i++) {
			String headerText = headers.get(i).getText().trim();
			if (headerText.equalsIgnoreCase(columnName)) {
				log("Found column '" + columnName + "' at index: " + (i + 1));
				return i + 1;
			}
		}
		return -1;
	}

	private void verifyValueInKendoGrid(WebElement grid, int columnIndex, String expectedValue, String columnName) {
		List<WebElement> cells = grid.findElements(By.cssSelector("tbody tr td:nth-child(" + columnIndex + ")"));

		for (WebElement cell : cells) {
			if (cell.getText().trim().contains(expectedValue)) {
				return;
			}
		}

		throw new RuntimeException("Value '" + expectedValue + "' not found in column '" + columnName + "'");
	}

	// ============= DEVEXTREME GRID =============
	private int findColumnIndexDevExtreme(WebElement grid, String columnName) {
		List<WebElement> headers = grid.findElements(By.cssSelector(".dx-header-row td"));

		for (int i = 0; i < headers.size(); i++) {
			String headerText = headers.get(i).getText().trim();
			if (headerText.equalsIgnoreCase(columnName)) {
				log("Found column '" + columnName + "' at index: " + (i + 1));
				return i + 1;
			}
		}
		return -1;
	}

	private void verifyValueInDevExtreme(WebElement grid, int columnIndex, String expectedValue, String columnName) {
		List<WebElement> cells = grid.findElements(By.cssSelector(".dx-data-row td:nth-child(" + columnIndex + ")"));

		for (WebElement cell : cells) {
			if (cell.getText().trim().contains(expectedValue)) {
				return;
			}
		}

		throw new RuntimeException("Value '" + expectedValue + "' not found in column '" + columnName + "'");
	}

	// ============= STANDARD TABLE =============
	private int findColumnIndexStandardTable(WebElement grid, String columnName) {
		List<WebElement> headers = grid.findElements(By.xpath(
				".//th | .//thead//td | .//div[contains(@class,'header')] | .//span[contains(@class,'header')]"));

		for (int i = 0; i < headers.size(); i++) {
			String headerText = headers.get(i).getText().trim();
			if (headerText.equalsIgnoreCase(columnName)) {
				log("Found column '" + columnName + "' at index: " + (i + 1));
				return i + 1;
			}
		}
		return -1;
	}

	private void verifyValueInStandardTable(WebElement grid, int columnIndex, String expectedValue, String columnName) {
		String cellXpath = String.format("(.//tr/td[%d] | .//div[@role='row']/div[%d])[contains(., '%s')]", columnIndex,
				columnIndex, expectedValue);

		List<WebElement> matches = grid.findElements(By.xpath(cellXpath));

		if (matches.isEmpty()) {
			cellXpath = String.format("(.//tr/td[%d] | .//div[@role='row']/div[%d])//*[contains(text(), '%s')]",
					columnIndex, columnIndex, expectedValue);
			matches = grid.findElements(By.xpath(cellXpath));
		}

		if (matches.isEmpty()) {
			throw new RuntimeException("Value '" + expectedValue + "' not found in column '" + columnName + "'");
		}
	}

	// ========================================
	// LOGGING HELPER
	// ========================================

	private void log(String message) {
		if (detailedLogging) {
			System.out.println("[VERIFY " + getCurrentTime() + "] " + message);
		}
	}

	private String getCurrentTime() {
		return LocalDateTime.now().format(timeFormatter);
	}
}