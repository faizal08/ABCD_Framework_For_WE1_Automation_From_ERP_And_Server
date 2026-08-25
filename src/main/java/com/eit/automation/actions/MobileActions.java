package com.eit.automation.actions;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.PerformsTouchActions;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.appium.java_client.HidesKeyboard;
import java.time.Duration;
import java.util.Collections;

public class MobileActions {

    private AppiumDriver driver;
    private WebDriverWait wait;

    public MobileActions(AppiumDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    /**
     * Standard click for Mobile (handles Accessibility IDs or XPaths)
     */
    public void tap(String locator) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(parseLocator(locator)));
        element.click();
    }

    /**
     * Swipe logic: Up, Down, Left, Right
     */
    /**
     * Swipe logic: Up, Down, Left, Right using modern W3C Actions API
     */
    public void swipe(String direction) {
        Dimension size = driver.manage().window().getSize();
        int startX, startY, endX, endY;

        switch (direction.toLowerCase()) {
            case "up":
                startX = size.width / 2;
                startY = (int) (size.height * 0.75);
                endX = size.width / 2;
                endY = (int) (size.height * 0.25);
                break;
            case "down":
                startX = size.width / 2;
                startY = (int) (size.height * 0.25);
                endX = size.width / 2;
                endY = (int) (size.height * 0.75);
                break;
            case "left":
                // To scroll content rightwards into view, drag finger right-to-left
                startX = (int) (size.width * 0.85);
                startY = size.height / 2;
                endX = (int) (size.width * 0.15);
                endY = size.height / 2;
                break;
            case "right":
                // To scroll content leftwards into view, drag finger left-to-right
                startX = (int) (size.width * 0.15);
                startY = size.height / 2;
                endX = (int) (size.width * 0.85);
                endY = size.height / 2;
                break;
            default:
                System.out.println("  ⚠️ Warning: Unsupported swipe direction requested: " + direction);
                return;
        }
        executePhysicalSwipeSequence(startX, startY, endX, endY);
    }

    // 2. ELEMENT-BOUNDED SWIPE (New Overload for element-specific scrolling)
    public void swipeOnElement(WebElement element, String direction) {
        Point location = element.getLocation();
        Dimension size = element.getSize();

        int startX, startY, endX, endY;
        int centerOfElementX = location.getX() + (size.width / 2);
        int centerOfElementY = location.getY() + (size.height / 2);

        switch (direction.toLowerCase()) {
            case "up":
                startX = centerOfElementX;
                startY = location.getY() + (int) (size.height * 0.75);
                endX = centerOfElementX;
                endY = location.getY() + (int) (size.height * 0.25);
                break;
            case "down":
                startX = centerOfElementX;
                startY = location.getY() + (int) (size.height * 0.25);
                endX = centerOfElementX;
                endY = location.getY() + (int) (size.height * 0.75);
                break;
            case "left":
                startX = location.getX() + (int) (size.width * 0.85);
                startY = centerOfElementY;
                endX = location.getX() + (int) (size.width * 0.15);
                endY = centerOfElementY;
                break;
            case "right":
                startX = location.getX() + (int) (size.width * 0.15);
                startY = centerOfElementY;
                endX = location.getX() + (int) (size.width * 0.85);
                endY = centerOfElementY;
                break;
            default:
                System.out.println("  ⚠️ Warning: Unsupported swipe direction requested: " + direction);
                return;
        }
        executePhysicalSwipeSequence(startX, startY, endX, endY);
    }

    // Helper structure to execute the generated input chain
    private void executePhysicalSwipeSequence(int startX, int startY, int endX, int endY) {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "swipeFinger");
        org.openqa.selenium.interactions.Sequence swipeSeq = new org.openqa.selenium.interactions.Sequence(finger, 1);

        swipeSeq.addAction(finger.createPointerMove(Duration.ZERO, org.openqa.selenium.interactions.PointerInput.Origin.viewport(), startX, startY));
        swipeSeq.addAction(finger.createPointerDown(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipeSeq.addAction(finger.createPointerMove(Duration.ofMillis(600), org.openqa.selenium.interactions.PointerInput.Origin.viewport(), endX, endY));
        swipeSeq.addAction(finger.createPointerUp(org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));

        try {
            driver.perform(Collections.singletonList(swipeSeq));
            try { Thread.sleep(400); } catch (InterruptedException e) {}
        } catch (Exception e) {
            System.out.println("  ❌ Failed to execute swipe sequence: " + e.getMessage());
        }
    }
    /**
     * Hide keyboard - essential for mobile forms
     */
    public void hideKeyboard() {
        try {
            if (driver instanceof HidesKeyboard) {
                ((HidesKeyboard) driver).hideKeyboard();
            }
        } catch (Exception e) {
            // Ignore if keyboard is already hidden or not supported
        }
    }

    public void pushFileToDevice(String localPath, String devicePath) {
        try {
            java.io.File file = new java.io.File(localPath);

            // 🚀 CRITICAL SANITIZATION: Force forward slashes for Android's file system
            String sanitizedDevicePath = devicePath.replace("\\", "/");

            // 1. Ensure driver is cast cleanly and push the file to the device
            ((io.appium.java_client.android.AndroidDriver) this.driver).pushFile(sanitizedDevicePath, file);
            System.out.println("✅ [MobileActions] Successfully pushed test image to mobile device: " + sanitizedDevicePath);

            // 🚀 OFFICIAL GALLERY FIX: Uses the driver's native 'mobile: broadcast' method
            // This triggers the intent cleanly to tell Android to parse and show the file in the Gallery.
            java.util.Map<String, Object> intentArgs = new java.util.HashMap<>();
            intentArgs.put("action", "android.intent.action.MEDIA_SCANNER_SCAN_FILE");
            intentArgs.put("data", "file://" + sanitizedDevicePath);

            ((io.appium.java_client.android.AndroidDriver) this.driver).executeScript("mobile: broadcast", intentArgs);
            System.out.println("🔄 [MobileActions] Native Media Scanner broadcast triggered successfully via 'mobile: broadcast'.");

        } catch (Exception e) {
            System.err.println("❌ [MobileActions] Failed to push file to device: " + e.getMessage());
            // Crucial for automation: rethrow the exception so the TestExecutor knows the setup step failed
            throw new RuntimeException(e);
        }
    }
    /**
     * Helper to distinguish between XPath and Mobile Accessibility IDs
     */
    private By parseLocator(String locator) {
        if (locator.startsWith("//") || locator.startsWith("(//")) {
            return By.xpath(locator);
        }
        // Default to Accessibility ID for mobile-friendly strings
        return By.id(locator);
    }
}
