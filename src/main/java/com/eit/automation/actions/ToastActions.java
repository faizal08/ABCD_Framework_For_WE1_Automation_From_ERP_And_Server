package com.eit.automation.actions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ToastActions {

    private WebDriver driver;
    private WebDriverWait wait;
    private WaitActions waitActions;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public ToastActions(WebDriver driver, WebDriverWait wait, WaitActions waitActions) {
        this.driver = driver;
        this.wait = wait;
        this.waitActions = waitActions;
    }

    /*
     * =========================
     * VERIFY TOAST WITH XPATH
     * =========================
     */
    public void verifyToastMessage(String expectedMessage, String xpath) {
        log("Verifying toast message");
        log("XPath: " + xpath);
        log("Expected: " + expectedMessage);

        // Ensure any loading spinner from previous action is gone
        if (waitActions != null) {
            waitActions.waitForLoadingSpinner();
        }

        try {
            // Increased timeout to 15 seconds to handle network latency
            WebDriverWait toastWait = new WebDriverWait(driver, Duration.ofSeconds(15));
            WebElement toast = toastWait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));

            String actualText = toast.getText().trim();
            log("Actual: " + actualText);

            if (expectedMessage != null && !actualText.contains(expectedMessage)) {
                String error = "Toast text mismatch. Expected to contain: '" +
                        expectedMessage + "', Actual: '" + actualText + "'";
                log("✗ " + error);
                throw new AssertionError(error);
            }

            log("✓ Toast message verified");

        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            String error = "Toast not found or not visible: " + xpath;
            log("✗ " + error);
            log("✗ Error type: " + e.getClass().getSimpleName());
            log("✗ Error message: " + e.getMessage());
            throw new RuntimeException(error, e);
        }
    }

    /*
     * =========================
     * VERIFY TOAST BY TEXT
     * =========================
     */
    public void verifyToastMessageByText(String expectedMessage) {
        log("Verifying toast message by text");
        log("Expected: " + expectedMessage);

        // Ensure any loading spinner from previous action is gone
        if (waitActions != null) {
            waitActions.waitForLoadingSpinner();
        }

        String xpath = "//*[contains(@class,'toast') or contains(@class,'alert') or contains(@class,'growl')]" +
                "[contains(.,'" + expectedMessage + "')]";

        try {
            WebDriverWait toastWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement toast = toastWait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));

            String actualText = toast.getText().trim();
            log("Actual: " + actualText);

            log("✓ Toast message verified");

        } catch (Exception e) {
            String error = "Toast message not found: " + expectedMessage;
            log("✗ " + error);
            log("✗ Error type: " + e.getClass().getSimpleName());
            log("✗ Error message: " + e.getMessage());
            throw new RuntimeException(error, e);
        }
    }

    /*
     * =========================
     * VERIFY SUCCESS TOAST
     * =========================
     */
    public void verifySuccessToast(String expectedMessage) {
        log("Verifying SUCCESS toast");
        verifyToastMessageByText(expectedMessage);
    }

    /*
     * =========================
     * VERIFY ERROR TOAST
     * =========================
     */
    public void verifyErrorToast(String expectedMessage) {
        log("Verifying ERROR toast");
        verifyToastMessageByText(expectedMessage);
    }

    /*
     * =========================
     * WAIT FOR TOAST LIFE CYCLE
     * =========================
     */
    public void waitForToastToAppearAndDisappear(String xpath) {
        log("Waiting for toast appear & disappear");
        log("XPath: " + xpath);

        try {
            WebDriverWait toastWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            toastWait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
            log("✓ Toast appeared");

            toastWait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(xpath)));
            log("✓ Toast disappeared");

        } catch (Exception e) {
            log("⚠ Toast lifecycle not fully observed");
            log("⚠ Reason: " + e.getMessage());
        }
    }

    /*
     * =========================
     * LOGGING (MATCH VERIFY)
     * =========================
     */
    private void log(String message) {
        System.out.println("[VERIFY " + getCurrentTime() + "] " + message);
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(timeFormatter);
    }
}
