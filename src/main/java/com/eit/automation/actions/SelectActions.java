package com.eit.automation.actions;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Handles dropdown and selection actions
 * Package: com.eit.automation.actions
 */
public class SelectActions {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    private WaitActions waitActions;
    // Logging configuration
    private boolean detailedLogging = false;

    public SelectActions(WebDriver driver, WebDriverWait wait, WaitActions waitActions) {
        this.driver = driver;
        this.wait = wait;
        this.jsExecutor = (JavascriptExecutor) driver;
        this.waitActions = waitActions;
    }

    /**
     * Select from jqWidgets combobox/dropdown by visible text
     */
    public void selectJqxCombobox(String xpath, String optionText) {
        try {
            log("Selecting from jqx combobox: '" + optionText + "'");

            waitActions.waitForLoadingSpinner();

            // Step 1: Find the combobox container
            WebElement combobox = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));

            // Step 2: Get the combobox ID
            String comboboxId = findJqxComboboxId(combobox);

            if (comboboxId != null && !comboboxId.isEmpty()) {
                log("Found jqx combobox ID: " + comboboxId);

                // Method 1: Use jqWidgets API to select by text
                boolean success = (boolean) jsExecutor.executeScript(
                        "try {" +
                                "  var combo = $('#" + comboboxId + "');" +
                                "  if (combo.length && typeof combo.jqxComboBox === 'function') {" +
                                "    combo.jqxComboBox('selectItem', '" + optionText + "');" +
                                "    return true;" +
                                "  }" +
                                "  return false;" +
                                "} catch(e) {" +
                                "  console.error('jqx select error:', e);" +
                                "  return false;" +
                                "}");

                if (success) {
                    log("✓ Selected using jqWidgets API");
                    Thread.sleep(500);
                    waitActions.waitForLoadingSpinner();
                    return;
                }
            }

            // Fallback: Manual selection
            log("Falling back to manual selection...");
            selectJqxComboboxManual(xpath, optionText);

        } catch (Exception e) {
            throw new RuntimeException("Failed to select from jqx combobox: " + e.getMessage());
        }
    }

    /**
     * Manually select from jqx combobox (fallback method)
     */
    private void selectJqxComboboxManual(String comboboxXpath, String optionText) {
        try {
            log("Using manual selection method...");

            // Step 1: Click the combobox or arrow to open dropdown
            WebElement combobox = driver.findElement(By.xpath(comboboxXpath));

            // Try to find the arrow button
            try {
                WebElement arrow = combobox.findElement(By.xpath(".//div[contains(@class, 'jqx-combobox-arrow')]"));
                jsExecutor.executeScript("arguments[0].click();", arrow);
                log("✓ Opened dropdown");
            } catch (Exception e) {
                // Click the combobox itself
                jsExecutor.executeScript("arguments[0].click();", combobox);
                log("✓ Clicked combobox");
            }

            Thread.sleep(500);
            waitActions.waitForLoadingSpinner();

            // Step 2: Find and click the option
            // Try multiple XPath strategies
            String[] optionXpaths = {
                    "//div[contains(@class, 'jqx-listitem') and contains(., '" + optionText + "')]",
                    "//span[contains(@class, 'jqx-listitem-element') and text()='" + optionText + "']",
                    "//div[@role='option' and contains(., '" + optionText + "')]",
                    "//*[contains(@class, 'jqx-listitem') and normalize-space()='" + optionText + "']"
            };

            boolean optionFound = false;
            for (String optionXpath : optionXpaths) {
                try {
                    WebElement option = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(optionXpath)));

                    // Scroll into view
                    waitActions.scrollIntoView(option);
                    Thread.sleep(200);

                    // Click using multiple methods
                    try {
                        option.click();
                    } catch (Exception e) {
                        jsExecutor.executeScript("arguments[0].click();", option);
                    }

                    log("✓ Clicked option: " + optionText);
                    optionFound = true;
                    break;

                } catch (Exception e) {
                    // Try next XPath
                    continue;
                }
            }

            if (!optionFound) {
                throw new RuntimeException("Option not found: " + optionText);
            }

            Thread.sleep(500);
            waitActions.waitForLoadingSpinner();

            log("✓ Manual selection completed");

        } catch (Exception e) {
            throw new RuntimeException("Manual selection failed: " + e.getMessage());
        }
    }

    /**
     * Find jqx combobox ID from element or its children
     */
    private String findJqxComboboxId(WebElement element) {
        try {
            // Check element itself
            String id = element.getAttribute("id");
            if (id != null && !id.isEmpty()) {
                // Verify it's a jqx combobox
                Object isJqx = jsExecutor.executeScript(
                        "return $('#" + id + "').length && typeof $('#" + id + "').jqxComboBox === 'function';");
                if (isJqx != null && (boolean) isJqx) {
                    return id;
                }
            }

            // Check children with class containing 'jqx-combobox'
            List<WebElement> children = element.findElements(By.xpath(".//*[contains(@class, 'jqx-combobox')]"));
            for (WebElement child : children) {
                id = child.getAttribute("id");
                if (id != null && !id.isEmpty()) {
                    Object isJqx = jsExecutor.executeScript(
                            "return $('#" + id + "').length && typeof $('#" + id + "').jqxComboBox === 'function';");
                    if (isJqx != null && (boolean) isJqx) {
                        return id;
                    }
                }
            }

            return null;

        } catch (Exception e) {
            if (detailedLogging) {
                System.err.println("Warning: Error finding jqx combobox ID: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Select from standard HTML select dropdown
     */
    public void selectStandardDropdown(String xpath, String optionText) {
        try {
            log("Selecting from standard dropdown: '" + optionText + "'");

            waitActions.waitForLoadingSpinner();

            WebElement selectElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));

            // Use Selenium's Select class
            org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(selectElement);

            // Try by visible text
            try {
                select.selectByVisibleText(optionText);
                log("✓ Selected by visible text");
            } catch (Exception e) {
                // Try by partial text
                List<WebElement> options = select.getOptions();
                for (WebElement option : options) {
                    if (option.getText().contains(optionText)) {
                        option.click();
                        log("✓ Selected by partial text match");
                        break;
                    }
                }
            }

            Thread.sleep(300);
            waitActions.waitForLoadingSpinner();

        } catch (Exception e) {
            throw new RuntimeException("Failed to select from dropdown: " + e.getMessage());
        }
    }

    /**
     * Smart select - auto-detects dropdown type
     */
    public void smartSelect(String xpath, String optionText) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
            String tagName = element.getTagName().toLowerCase();
            String className = element.getAttribute("class");

            // Detect dropdown type
            if (tagName.equals("select")) {
                log("Detected: Standard HTML select");
                selectStandardDropdown(xpath, optionText);
            } else if (className != null && className.contains("jqx")) {
                log("Detected: jqWidgets combobox");
                selectJqxCombobox(xpath, optionText);
            } else {
                log("Detected: Custom dropdown");
                selectJqxComboboxManual(xpath, optionText);
            }

        } catch (Exception e) {
            throw new RuntimeException("Smart select failed: " + e.getMessage());
        }
    }

    private void log(String message) {
        if (detailedLogging) {
            System.out.println("[SELECT] " + message);
        }
    }
}