package com.eit.automation.core;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import com.eit.automation.actions.*;
import com.eit.automation.parser.StepParser;
import com.eit.automation.parser.TestStep;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.interactions.Pause;
import com.eit.automation.utils.DatabaseUtils;

public class KeywordHandler {

    private WebDriver driver;
    private WebDriverWait wait;
    private Properties config;

    private WaitActions waitActions;
    private ClickActions clickActions;
    private InputActions inputActions;
    private VerificationActions verificationActions;
    private FileActions fileActions;
    private ToastActions toastActions;
    private ScrollActions scrollActions;
    private AutoItActions autoItActions;
    private MobileActions mobileActions;
    private PageObjectManager pageObjectManager;
    private boolean isCleanupMode;
    private TestExecutor testExecutor;

    public KeywordHandler(WebDriver driver, WebDriverWait wait, Properties config,
                          WaitActions waitActions, ClickActions clickActions, InputActions inputActions,
                          VerificationActions verificationActions, FileActions fileActions, ToastActions toastActions,
                          ScrollActions scrollActions, AutoItActions autoItActions, MobileActions mobileActions,
                          PageObjectManager pageObjectManager, boolean isCleanupMode, TestExecutor testExecutor) {
        this.driver = driver;
        this.wait = wait;
        this.config = config;
        this.waitActions = waitActions;
        this.clickActions = clickActions;
        this.inputActions = inputActions;
        this.verificationActions = verificationActions;
        this.fileActions = fileActions;
        this.toastActions = toastActions;
        this.scrollActions = scrollActions;
        this.autoItActions = autoItActions;
        this.mobileActions = mobileActions;
        this.pageObjectManager = pageObjectManager;
        this.isCleanupMode = isCleanupMode;
        this.testExecutor = testExecutor;
    }

    public void setCleanupMode(boolean cleanupMode) {
        this.isCleanupMode = cleanupMode;
    }

    public void executeStep(TestStep step) throws Exception {
        if (step == null) return;

        if (step.getXpath() != null) {
            String xp = step.getXpath().trim();
            if (!xp.contains("automator=")) {
                if (xp.startsWith("\"") && xp.endsWith("\"") && xp.length() > 1) {
                    xp = xp.substring(1, xp.length() - 1).trim();
                }
                step.setXpath(xp);
            } else {
                step.setXpath(xp);
            }
        }
        if (step.getValue() != null) {
            String val = step.getValue().trim();
            if (!val.contains("automator=")) {
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1) {
                    val = val.substring(1, val.length() - 1).trim();
                }
                step.setValue(val);
            } else {
                step.setValue(val);
            }
        }
        if (step.getContext() != null) {
            String ctx = step.getContext().trim();
            if (!ctx.contains("automator=")) {
                if (ctx.startsWith("\"") && ctx.endsWith("\"") && ctx.length() > 1) {
                    ctx = ctx.substring(1, ctx.length() - 1).trim();
                }
                step.setContext(ctx);
            } else {
                step.setContext(ctx);
            }
        }

        if (step.getAction() != null && step.getAction().equalsIgnoreCase("uploadfile")) {
            String fileTarget = null;
            String locatorKeyTarget = null;

            List<String> textPool = new ArrayList<>();
            if (step.getValue() != null && !step.getValue().isEmpty()) textPool.add(step.getValue());
            if (step.getXpath() != null && !step.getXpath().isEmpty()) textPool.add(step.getXpath());
            if (step.getContext() != null && !step.getContext().isEmpty()) textPool.add(step.getContext());

            for (String text : textPool) {
                if (text.startsWith("//") || text.startsWith("(") ||
                        text.startsWith("accessibility=") || text.startsWith("id=") || text.startsWith("automator=")) {
                    locatorKeyTarget = text;
                }
                else if (text.contains("/") || text.contains("\\")) {
                    fileTarget = text;
                }
                else {
                    locatorKeyTarget = text;
                }
            }

            if (locatorKeyTarget != null) {
                step.setXpath(locatorKeyTarget);
            }
            if (fileTarget != null) {
                step.setValue(fileTarget);
            }
        }

        if (step.getXpath() != null && !step.getXpath().isEmpty()
                && !step.getXpath().startsWith("//") && !step.getXpath().startsWith("(")
                && !step.getXpath().startsWith("accessibility=") && !step.getXpath().startsWith("id=") && !step.getXpath().startsWith("automator=")) {

            String resolvedXpath = LocatorMapper.getXPath(step.getXpath());
            if (resolvedXpath != null && !resolvedXpath.isEmpty()) {
                step.setXpath(resolvedXpath);
            }
        }

        if (step.getValue() != null && !step.getValue().isEmpty()
                && !step.getValue().contains("/") && !step.getValue().contains("\\")
                && !step.getValue().startsWith("//") && !step.getValue().startsWith("(")
                && !step.getValue().startsWith("accessibility=") && !step.getValue().startsWith("id=") && !step.getValue().startsWith("automator=")) {
            String resolvedValue = LocatorMapper.getXPath(step.getValue());
            if (resolvedValue != null && !resolvedValue.isEmpty()) {
                step.setValue(resolvedValue);
            }
        }

        if (step.getXpath() != null && step.getXpath().contains("{") && step.getXpath().contains("}")) {
            String processedXpath = StepParser.replaceSavedVariablesOnly(step.getXpath());
            step.setXpath(processedXpath);
        }

        String action = step.getAction().toLowerCase();
        String value = step.getValue();
        String xpath = step.getXpath();
        String context = step.getContext();

        System.out.println("  ⚙ Action: " + action.toUpperCase());

        if ((xpath == null || xpath.isEmpty()) && (value != null && !value.isEmpty())) {
            if (pageObjectManager != null) {
                WebElement element = pageObjectManager.findElementByName(value);
                if (element != null) {
                    System.out.println("  → Found PageFactory Element: " + value);
                }
            }
        }

        if (xpath == null || xpath.isEmpty()) {
            if (value != null && !value.isEmpty()) {

                boolean isDirectXPath = value.startsWith("//") || value.startsWith("(");

                if (isDirectXPath) {
                    xpath = value;
                }
                else if (!(driver instanceof io.appium.java_client.AppiumDriver) &&
                        !action.startsWith("verifytoast") &&
                        !action.equals("robotupload") &&
                        !action.equalsIgnoreCase("drawpolygon")) {

                    xpath = generateXPathFromValue(value, context);
                }
                else if (driver instanceof io.appium.java_client.AppiumDriver) {
                    xpath = value;
                }
            }
        }

        if ((xpath == null || xpath.isEmpty()) && (value == null || value.isEmpty())) {
            // Both empty
        }

        step.setXpath(xpath);
        step.setValue(value);

        switch (action) {
            case "openurl":
            case "navigate":
                if (driver instanceof io.appium.java_client.AppiumDriver) {
                    System.out.println("  → Mobile Context: App is already launched via Capabilities");
                } else {
                    System.out.println("  → URL: " + value);
                    driver.get(value);
                    waitActions.waitForPageLoad();
                    System.out.println("  ✓ Page loaded: " + driver.getCurrentUrl());
                }
                break;

            case "scrolltobottom":
                scrollActions.scrollToBottom();
                break;

            case "scrolltotop":
                scrollActions.scrollToTop();
                break;

            case "scrolltoelement":
                WebElement elementToScroll = wait.until(ExpectedConditions.presenceOfElementLocated(testExecutor.getDynamicLocator(xpath)));
                scrollActions.scrollToElement(elementToScroll);
                break;

            case "scrollby":
                String[] coords = value.split(",");
                if (coords.length >= 2) {
                    int x = Integer.parseInt(coords[0].trim());
                    int y = Integer.parseInt(coords[1].trim());
                    scrollActions.scrollBy(x, y);
                }
                break;

            case "click":
                if (driver instanceof io.appium.java_client.AppiumDriver) {
                    mobileActions.tap(xpath);
                } else {
                    if (xpath != null && !xpath.isEmpty()) {
                        clickActions.clickElementWithRetry(xpath, null);
                    } else {
                        clickActions.clickElementWithRetry(xpath, value);
                    }
                }
                break;

            case "click_if_present":
                try {
                    WebDriverWait optionalClickWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                    WebElement element = optionalClickWait.until(ExpectedConditions.elementToBeClickable(testExecutor.getDynamicLocator(xpath)));
                    element.click();
                    Thread.sleep(2000);
                } catch (Exception ignored) {}
                break;

            case "hover":
            case "movetoelement":
                try {
                    if (!(driver instanceof io.appium.java_client.AppiumDriver)) {
                        if (xpath != null && !xpath.isEmpty()) {
                            WebElement targetElement = driver.findElement(By.xpath(xpath));
                            new Actions(driver).moveToElement(targetElement).perform();
                        }
                    }
                } catch (Exception e) {
                    throw e;
                }
                break;

            case "select":
                if (driver instanceof io.appium.java_client.AppiumDriver) {
                    mobileActions.tap(xpath);
                    By mobileItemSelector = AppiumBy.androidUIAutomator(
                            "new UiSelector().text(\"" + value + "\").description(\"" + value + "\")"
                    );
                    WebElement item = wait.until(ExpectedConditions.elementToBeClickable(mobileItemSelector));
                    item.click();
                } else {
                    clickActions.selectElementWithRetry(xpath, value);
                }
                break;

            case "type":
            case "enter":
                String resolvedValue = value;
                if (value != null && config != null && config.containsKey(value.trim())) {
                    resolvedValue = config.getProperty(value.trim());
                }

                if (resolvedValue != null && resolvedValue.contains("{") && resolvedValue.contains("}")) {
                    resolvedValue = StepParser.processPlaceholders(resolvedValue);
                }

                if (driver instanceof io.appium.java_client.AppiumDriver) {
                    WebElement mobileElement = null;
                    int attempts = 0;

                    while (attempts < 2) {
                        try {
                            By activeMobileLocator = testExecutor.getDynamicLocator(xpath);
                            mobileElement = wait.until(ExpectedConditions.presenceOfElementLocated(activeMobileLocator));

                            String lowerXpath = xpath != null ? xpath.toLowerCase() : "";
                            boolean isOtpFieldXpath = lowerXpath.contains("verify") || lowerXpath.contains("otp") || lowerXpath.contains("instance") || lowerXpath.contains("automator");
                            boolean isInputTarget = lowerXpath.contains("edittext") || lowerXpath.contains("descendant") || lowerXpath.contains("widget.view");
                            boolean isNumericOtpValue = resolvedValue != null && resolvedValue.matches("\\d+") && (resolvedValue.length() == 4 || resolvedValue.length() == 6);

                            if ((isOtpFieldXpath || isInputTarget) && isNumericOtpValue) {
                                AndroidDriver androidDriver = (AndroidDriver) driver;

                                Point location = mobileElement.getLocation();
                                Dimension size = mobileElement.getSize();

                                int targetX = location.getX() + (int)(size.getWidth() * 0.12);
                                int targetY = location.getY() + (size.getHeight() / 2);

                                PointerInput initialFinger = new PointerInput(PointerInput.Kind.TOUCH, "initialFinger");
                                Sequence baseTap = new Sequence(initialFinger, 1);

                                baseTap.addAction(initialFinger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), targetX, targetY));
                                baseTap.addAction(initialFinger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                                baseTap.addAction(initialFinger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                                androidDriver.perform(Collections.singletonList(baseTap));

                                try { Thread.sleep(1200); } catch (InterruptedException ignored) {}

                                for (char ch : resolvedValue.toCharArray()) {
                                    io.appium.java_client.android.nativekey.AndroidKey targetKey;

                                    switch (ch) {
                                        case '0': targetKey = io.appium.java_client.android.nativekey.AndroidKey.DIGIT_0; break;
                                        case '1': targetKey = io.appium.java_client.android.nativekey.AndroidKey.DIGIT_1; break;
                                        case '2': targetKey = io.appium.java_client.android.nativekey.AndroidKey.DIGIT_2; break;
                                        case '3': targetKey = io.appium.java_client.android.nativekey.AndroidKey.DIGIT_3; break;
                                        case '4': targetKey = io.appium.java_client.android.nativekey.AndroidKey.DIGIT_4; break;
                                        case '5': targetKey = io.appium.java_client.android.nativekey.AndroidKey.DIGIT_5; break;
                                        case '6': targetKey = io.appium.java_client.android.nativekey.AndroidKey.DIGIT_6; break;
                                        case '7': targetKey = io.appium.java_client.android.nativekey.AndroidKey.DIGIT_7; break;
                                        case '8': targetKey = io.appium.java_client.android.nativekey.AndroidKey.DIGIT_8; break;
                                        case '9': targetKey = io.appium.java_client.android.nativekey.AndroidKey.DIGIT_9; break;
                                        default: continue;
                                    }

                                    androidDriver.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(targetKey));
                                    try { Thread.sleep(350); } catch (InterruptedException ignored) {}
                                }

                                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

                            } else {
                                mobileElement.click();
                                try { Thread.sleep(200); } catch (InterruptedException ignored) {}

                                try {
                                    mobileElement.clear();
                                } catch (Exception ignored) {}
                                mobileElement.sendKeys(resolvedValue);
                            }

                            break;

                        } catch (StaleElementReferenceException e) {
                            attempts++;
                            if (attempts == 2) throw e;
                        }
                    }

                    try { mobileActions.hideKeyboard(); } catch (Exception ignored) {}
                } else {
                    inputActions.typeText(xpath, resolvedValue);
                }
                break;

            case "clear":
                if (driver instanceof io.appium.java_client.AppiumDriver) {
                    By activeMobileLocator = testExecutor.getDynamicLocator(xpath);
                    wait.until(ExpectedConditions.presenceOfElementLocated(activeMobileLocator)).clear();
                } else {
                    inputActions.clearField(xpath);
                }
                break;

            case "arrow_down":
                driver.findElement(testExecutor.getDynamicLocator(xpath)).sendKeys(Keys.ARROW_DOWN);
                break;

            case "arrow_up":
                driver.findElement(testExecutor.getDynamicLocator(xpath)).sendKeys(Keys.ARROW_UP);
                break;

            case "press_enter":
                driver.findElement(testExecutor.getDynamicLocator(xpath)).sendKeys(Keys.ENTER);
                break;

            case "tab":
                WebElement currentElement = driver.findElement(testExecutor.getDynamicLocator(xpath));

                int repeat = 1;
                try {
                    if (value != null && !value.isEmpty()) {
                        repeat = Integer.parseInt(value.trim());
                    }
                } catch (NumberFormatException ignored) {
                    repeat = 1;
                }

                for (int i = 0; i < repeat; i++) {
                    currentElement.sendKeys(Keys.TAB);
                    currentElement = driver.switchTo().activeElement();
                    try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                }
                break;

            case "uploadfile":
            case "selectfile":
            case "attachfile":
                if (driver instanceof io.appium.java_client.AppiumDriver) {
                    File localFile = new File(value);
                    String fileName = localFile.getName();
                    String remotePath = "/sdcard/Download/" + fileName;

                    mobileActions.pushFileToDevice(value, remotePath);

                    By activeUploadLocator = testExecutor.getDynamicLocator(xpath);
                    WebElement uploadBtn = wait.until(ExpectedConditions.elementToBeClickable(activeUploadLocator));
                    uploadBtn.click();
                    Thread.sleep(2000);

                    String galleryButtonXpath = "//*[@content-desc='GALLERY'] | //*[contains(@text,'GALLERY')]";
                    WebElement galleryBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(galleryButtonXpath)));
                    galleryBtn.click();
                    Thread.sleep(3000);

                    try {
                        String permissionBtn = "//android.widget.Button[@resource-id='com.android.permissioncontroller:id/permission_allow_button']";
                        driver.findElement(By.xpath(permissionBtn)).click();
                    } catch (Exception ignored) {}

                    String firstPhotoInGrid = "//android.widget.ImageView[1] | //android.view.ViewGroup[contains(@content-desc,'Photo taken')][1]";
                    WebElement nativePhoto = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(firstPhotoInGrid)));
                    nativePhoto.click();

                } else {
                    fileActions.uploadFile(value, xpath);
                }
                break;

            case "robotupload":
                if (driver instanceof io.appium.java_client.AppiumDriver) {
                    throw new RuntimeException("RobotUpload is a Windows-only feature and cannot be used on Mobile.");
                }

                if (xpath != null && !xpath.isEmpty()) {
                    driver.findElement(testExecutor.getDynamicLocator(xpath)).click();
                    waitActions.waitFor(1000);
                }
                fileActions.uploadFileWithRobot(value);
                break;

            case "waitforupload":
                if (driver instanceof io.appium.java_client.AppiumDriver) {
                    By targetUploadLocator = testExecutor.getDynamicLocator(xpath);
                    wait.until(ExpectedConditions.visibilityOfElementLocated(targetUploadLocator));
                } else {
                    fileActions.waitForUploadComplete(xpath);
                }
                break;

            case "element_present":
                try {
                    WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                    shortWait.until(ExpectedConditions.presenceOfElementLocated(testExecutor.getDynamicLocator(xpath)));
                } catch (Exception e) {
                    throw new RuntimeException("Validation Failed: Element was expected but NOT found: " + xpath);
                }
                break;

            case "element_absent":
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
                try {
                    List<WebElement> elements = driver.findElements(testExecutor.getDynamicLocator(xpath));
                    if (!elements.isEmpty()) {
                        throw new RuntimeException("Validation Failed: Element was found but should be ABSENT: " + xpath);
                    }
                } finally {
                    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
                }
                break;

            case "verifytoast":
            case "verifytoastmessage":
            case "verifysuccesstoast":
            case "verifyerrortoast":
                if (xpath != null && !xpath.isEmpty()) {
                    toastActions.verifyToastMessage(value, xpath);
                } else {
                    toastActions.verifyToastMessageByText(value);
                }
                break;

            case "verifyalert":
            case "verifyalertmessage":
                toastActions.verifyToastMessage(value, xpath);
                break;

            case "waitfortoast":
                toastActions.waitForToastToAppearAndDisappear(xpath);
                break;

            case "verify":
            case "verifyvisible":
            case "verifydisplayed":
                verificationActions.verifyElementVisible(xpath);
                break;

            case "verifytext":
                verificationActions.verifyElementValueOrText(xpath, value);
                break;

            case "verifyvalue":
                verificationActions.verifyElementValue(xpath, value);
                break;

            case "drawpolygon":
                drawPolygon(xpath, value);
                break;

            case "verifydate":
                if (value != null && !value.isEmpty()) {
                    verificationActions.verifyElementDate(xpath, value);
                } else {
                    verificationActions.verifyDateFieldHasValue(xpath);
                }
                break;

            case "verifycurrentdate":
            case "verifytodaydate":
                verificationActions.verifyDateFieldIsToday(xpath);
                break;

            case "verifyenabled":
                verificationActions.verifyElementEnabled(xpath);
                break;

            case "verifydisabled":
                verificationActions.verifyElementDisabled(xpath);
                break;

            case "verifyselected":
            case "verifychecked":
                verificationActions.verifyElementSelected(xpath);
                break;

            case "verifyexists":
            case "verifypresent":
                verificationActions.verifyElementExists(xpath);
                break;

            case "verifyhidden":
            case "verifynotvisible":
                verificationActions.verifyElementNotVisible(xpath);
                break;

            case "verifycontains":
                verificationActions.verifyElementContainsText(xpath, value);
                break;

            case "verifycount":
                int count = Integer.parseInt(value);
                verificationActions.verifyElementCount(xpath, count);
                break;

            case "verifyattribute":
                String[] parts = value.split("=", 2);
                verificationActions.verifyElementAttribute(xpath, parts[0], parts[1]);
                break;

            case "verifypagetitle":
            case "verifytitle":
                verificationActions.verifyPageTitle(value);
                if (xpath != null && !xpath.isEmpty()) {
                    verificationActions.verifyElementVisible(xpath);
                }
                break;

            case "verifypagetitlecontains":
            case "verifytitlecontains":
                verificationActions.verifyPageTitleContains(value);
                break;

            case "verifyurl":
            case "verifycurrenturl":
                verificationActions.verifyCurrentUrl(value);
                break;

            case "verifyurlcontains":
                verificationActions.verifyUrlContains(value);
                break;

            case "verifymapshape":
            case "verifypolygon":
            case "verifymapelement":
                verificationActions.verifyMapShapePresent(xpath);
                break;

            case "verifygridvalue":
                String[] gridParts = value.split("=", 2);
                if (gridParts.length < 2) {
                    throw new RuntimeException("Invalid format for verifygridvalue. Expected 'ColumnName=ExpectedValue', got: " + value);
                }
                verificationActions.verifyGridCellValue(xpath, gridParts[0].trim(), gridParts[1].trim());
                break;

            case "autoit":
            case "executeautoit":
            case "runautoit":
                String scriptArgs = "";
                if (context != null && !context.isEmpty()) {
                    scriptArgs = context;
                } else if (xpath != null && !xpath.isEmpty()) {
                    scriptArgs = xpath;
                }
                autoItActions.executeScript(value, scriptArgs);
                break;

            case "wait":
                if (value != null && value.matches("\\d+")) {
                    waitActions.waitFor(Long.parseLong(value));
                } else if (xpath != null && !xpath.isEmpty()) {
                    waitActions.waitForElementVisible(xpath);
                } else {
                    waitActions.waitFor(1000);
                }
                break;

            case "waitforvisible":
            case "wait for visible":
                waitActions.waitForElementVisible(xpath);
                break;

            case "waitforclickable":
            case "wait for clickable":
                waitActions.waitForElementClickable(xpath);
                break;

            case "wait_until_visible":
            case "wait_visible":
                try {
                    WebDriverWait structuralCheck = new WebDriverWait(driver, Duration.ofSeconds(90));
                    By dynamicWaitLocator = testExecutor.getDynamicLocator(xpath);
                    structuralCheck.until(ExpectedConditions.visibilityOfElementLocated(dynamicWaitLocator));
                } catch (Exception e) {
                    throw new RuntimeException("Page load timeout on locator target: " + xpath, e);
                }
                break;

            case "wait_if_present":
                try {
                    WebDriverWait optionalCheck = new WebDriverWait(driver, Duration.ofSeconds(5));
                    By dynamicWaitLocator = testExecutor.getDynamicLocator(xpath);
                    optionalCheck.until(ExpectedConditions.visibilityOfElementLocated(dynamicWaitLocator));
                } catch (Exception ignored) {}
                break;

            case "sql_cleanup":
                try {
                    if (this.isCleanupMode) {
                        showCleanupOverlay();
                    }
                    DatabaseUtils.executeCleanup(value, this.config);
                } catch (Exception e) {
                    throw e;
                }
                break;

            case "fetch_db_value":
                try {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

                    String sqlQuery = xpath;
                    String variableName = value;

                    if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
                        throw new IllegalArgumentException("❌ Excel column 'Locator' cannot be blank when using fetch_db_value command.");
                    }
                    if (variableName == null || variableName.trim().isEmpty()) {
                        throw new IllegalArgumentException("❌ Excel column 'Value' cannot be blank when using fetch_db_value command.");
                    }

                    String dbResult = DatabaseUtils.executeQueryAndFetchValue(sqlQuery, this.config);

                    if (dbResult == null) {
                        throw new RuntimeException("❌ Database fetch failed! Zero data rows returned for query: " + sqlQuery);
                    }

                    StepParser.saveRuntimeValue(variableName.trim(), dbResult);

                } catch (Exception e) {
                    throw e;
                }
                break;

            case "swipe":
            case "scroll_mobile":
                String swipeDirection = "up";
                if (value != null && !value.isEmpty()) {
                    swipeDirection = value.trim().toLowerCase();
                } else if (xpath != null && !xpath.isEmpty() &&
                        (xpath.trim().equalsIgnoreCase("up") || xpath.trim().equalsIgnoreCase("down") ||
                                xpath.trim().equalsIgnoreCase("left") || xpath.trim().equalsIgnoreCase("right"))) {
                    swipeDirection = xpath.trim().toLowerCase();
                }

                boolean hasSpecificTarget = (xpath != null && !xpath.isEmpty() &&
                        !xpath.trim().equalsIgnoreCase("up") &&
                        !xpath.trim().equalsIgnoreCase("down") &&
                        !xpath.trim().equalsIgnoreCase("left") &&
                        !xpath.trim().equalsIgnoreCase("right"));

                if (hasSpecificTarget) {
                    try {
                        By dynamicSwipeLocator = testExecutor.getDynamicLocator(xpath);
                        WebElement targetElement = wait.until(ExpectedConditions.visibilityOfElementLocated(dynamicSwipeLocator));
                        mobileActions.swipeOnElement(targetElement, swipeDirection);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to execute bounded swipe on locator: " + xpath, e);
                    }
                } else {
                    try {
                        mobileActions.swipe(swipeDirection);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to execute full-screen swipe", e);
                    }
                }
                break;

            case "hide_keyboard":
                mobileActions.hideKeyboard();
                break;

            case "tap":
                try {
                    By dynamicTapLocator = testExecutor.getDynamicLocator(xpath);
                    WebElement element = wait.until(ExpectedConditions.elementToBeClickable(dynamicTapLocator));
                    element.click();
                    Thread.sleep(2000);
                } catch (StaleElementReferenceException ignored) {
                } catch (Exception e) {
                    throw new RuntimeException("Failed to execute tap on locator: " + xpath, e);
                }
                break;

            case "tap_if_present":
                try {
                    WebDriverWait optionalTapWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                    By dynamicTapLocator = testExecutor.getDynamicLocator(xpath);
                    WebElement element = optionalTapWait.until(ExpectedConditions.elementToBeClickable(dynamicTapLocator));
                    element.click();
                    Thread.sleep(2000);
                } catch (Exception ignored) {}
                break;

            case "tap_coordinate":
                if (value == null || !value.contains(":")) {
                    throw new IllegalArgumentException("Invalid coordinate format: " + value);
                }

                String[] coordinates = value.split(":");
                int absoluteX = Integer.parseInt(coordinates[0].trim());
                int absoluteY = Integer.parseInt(coordinates[1].trim());

                try {
                    new Actions(driver).moveToLocation(absoluteX, absoluteY).click().perform();
                } catch (Exception e) {
                    throw e;
                }
                break;

            case "reload_app":
                if (testExecutor.getDriverPool() != null && testExecutor.getDriverPool().containsKey(value)) {
                    WebDriver mobileDriverInstance = testExecutor.getDriverPool().get(value);

                    if (mobileDriverInstance instanceof AndroidDriver) {
                        AndroidDriver androidDriver = (AndroidDriver) mobileDriverInstance;
                        String appPackage = config.getProperty(value + ".app.package");

                        if (appPackage != null && !appPackage.isEmpty()) {
                            try {
                                androidDriver.terminateApp(appPackage);
                                androidDriver.executeScript("mobile: clearApp", java.util.Map.of("appId", appPackage));
                                androidDriver.activateApp(appPackage);
                                testExecutor.switchSession(value);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
                break;

            case "set_date":
                WebElement dateInput = driver.findElement(By.xpath(xpath));
                String excelDateValue = value;
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].value='" + excelDateValue + "';" +
                                "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                                "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                        dateInput
                );
                break;

            case "store_text":
                String targetLocator = xpath;
                String variableKey = value;
                By finalSelector;

                if (targetLocator.startsWith("accessibility=")) {
                    finalSelector = AppiumBy.accessibilityId(targetLocator.replace("accessibility=", ""));
                } else if (targetLocator.startsWith("id=")) {
                    finalSelector = By.id(targetLocator.replace("id=", ""));
                } else if (targetLocator.startsWith("automator=")) {
                    finalSelector = AppiumBy.androidUIAutomator(targetLocator.replace("automator=", ""));
                } else {
                    finalSelector = By.xpath(targetLocator);
                }

                WebElement targetElement = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.visibilityOfElementLocated(finalSelector));

                String rawExtractedText = targetElement.getText();
                if (rawExtractedText == null || rawExtractedText.trim().isEmpty()) {
                    try {
                        rawExtractedText = targetElement.getAttribute("value");
                    } catch (Exception ignored) {
                        rawExtractedText = null;
                    }
                }

                if (rawExtractedText == null || rawExtractedText.trim().isEmpty()) {
                    rawExtractedText = targetElement.getAttribute("content-desc");
                    if (rawExtractedText == null || rawExtractedText.trim().isEmpty()) {
                        rawExtractedText = targetElement.getAttribute("text");
                    }
                }

                if (rawExtractedText == null) {
                    rawExtractedText = "";
                } else {
                    rawExtractedText = rawExtractedText.trim();
                }

                StepParser.saveRuntimeValue(variableKey, rawExtractedText);
                break;

            case "verify_variables_match":
                String firstVariableKey = (xpath != null) ? xpath.trim() : "";
                String secondVariableKey = (value != null) ? value.trim() : "";

                String firstValue = StepParser.getRuntimeValue(firstVariableKey);
                String secondValue = StepParser.getRuntimeValue(secondVariableKey);

                if (firstValue == null) {
                    throw new RuntimeException("Framework Variable context key {" + firstVariableKey + "} is completely uninitialized!");
                }
                if (secondValue == null) {
                    throw new RuntimeException("Framework Variable context key {" + secondVariableKey + "} is completely uninitialized!");
                }

                if (!firstValue.equals(secondValue)) {
                    throw new AssertionError(String.format(
                            "Value mismatch! Variable {%s} containing '%s' does not match Variable {%s} containing '%s'.",
                            firstVariableKey, firstValue, secondVariableKey, secondValue
                    ));
                }
                break;

            case "set_location":
                try {
                    String[] geoPoints = step.getValue().split(";");
                    double latitude = Double.parseDouble(geoPoints[0].trim());
                    double longitude = Double.parseDouble(geoPoints[1].trim());

                    if (this.driver instanceof AndroidDriver) {
                        java.util.Map<String, Object> coordinatesMap = new java.util.HashMap<>();
                        coordinatesMap.put("latitude", latitude);
                        coordinatesMap.put("longitude", longitude);
                        coordinatesMap.put("altitude", 0.0);

                        ((AndroidDriver) this.driver).executeScript("mobile: setGeolocation", coordinatesMap);
                        Thread.sleep(2000);
                    }
                } catch (Exception ignored) {}
                break;

            default:
                throw new RuntimeException("Unknown action: " + action);
        }
    }

    private String generateXPathFromValue(String value, String context) {
        if (driver instanceof io.appium.java_client.AppiumDriver) {
            return value;
        }

        if (context != null && !context.isEmpty()) {
            return String.format(
                    "//tr[contains(., '%2$s')]//*[contains(text(), '%1$s') or @title='%1$s' or @alt='%1$s' or @aria-label='%1$s' or contains(@class, '%1$s')]",
                    value, context);
        }

        return String.format(
                "//*[normalize-space()='%1$s' or @placeholder='%1$s' or @value='%1$s' or @title='%1$s' or @name='%1$s' or @id='%1$s' or @aria-label='%1$s' or @data-testid='%1$s' or contains(text(), '%1$s')]",
                value);
    }

    private void drawPolygon(String xpath, String value) {
        if (driver instanceof io.appium.java_client.AppiumDriver) {
            return;
        }

        By canvasLocator = testExecutor.getDynamicLocator(xpath);
        WebElement map = wait.until(ExpectedConditions.presenceOfElementLocated(canvasLocator));

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", map);

        Point mapLocation = map.getLocation();
        int width = map.getSize().getWidth();
        int height = map.getSize().getHeight();

        int centerX = mapLocation.getX() + (width / 2);
        int centerY = mapLocation.getY() + (height / 2);

        int maxX = (width / 2) - 30;
        int maxY = (height / 2) - 30;

        String[] points = value.split("\\s*:\\s*");
        if (points.length < 3) {
            throw new RuntimeException("A polygon requires a minimum of 3 coordinate sets to close a shape area.");
        }

        int[][] absolutePoints = new int[points.length][2];
        for (int i = 0; i < points.length; i++) {
            int[] relativeXY = parsePoint(points[i], maxX, maxY);
            absolutePoints[i][0] = centerX + relativeXY[0];
            absolutePoints[i][1] = centerY + relativeXY[1];
        }

        PointerInput mouse = new PointerInput(PointerInput.Kind.MOUSE, "defaultMouse");
        Sequence drawSequence = new Sequence(mouse, 1);

        drawSequence.addAction(mouse.createPointerMove(Duration.ofMillis(500),
                PointerInput.Origin.viewport(), absolutePoints[0][0], absolutePoints[0][1]));

        drawSequence.addAction(mouse.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        drawSequence.addAction(new Pause(mouse, Duration.ofMillis(150)));
        drawSequence.addAction(mouse.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        drawSequence.addAction(new Pause(mouse, Duration.ofMillis(600)));

        for (int i = 1; i < absolutePoints.length; i++) {
            drawSequence.addAction(mouse.createPointerMove(Duration.ofMillis(600),
                    PointerInput.Origin.viewport(), absolutePoints[i][0], absolutePoints[i][1]));

            if (i == absolutePoints.length - 1) {
                drawSequence.addAction(mouse.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                drawSequence.addAction(mouse.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                drawSequence.addAction(new Pause(mouse, Duration.ofMillis(100)));
                drawSequence.addAction(mouse.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                drawSequence.addAction(mouse.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                drawSequence.addAction(new Pause(mouse, Duration.ofMillis(600)));
            } else {
                drawSequence.addAction(mouse.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                drawSequence.addAction(new Pause(mouse, Duration.ofMillis(150)));
                drawSequence.addAction(mouse.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                drawSequence.addAction(new Pause(mouse, Duration.ofMillis(600)));
            }
        }

        ((org.openqa.selenium.remote.RemoteWebDriver) driver).perform(Collections.singletonList(drawSequence));

        try {
            Thread.sleep(1000);
        } catch (Exception ignored) {}
    }

    private int[] parsePoint(String point, int maxX, int maxY) {
        String[] xy = point.split(";");
        if (xy.length < 2) {
            throw new RuntimeException("Invalid point format: '" + point + "'. Expected 'X;Y'");
        }
        int x = Integer.parseInt(xy[0].trim());
        int y = Integer.parseInt(xy[1].trim());
        x = Math.max(-maxX, Math.min(maxX, x));
        y = Math.max(-maxY, Math.min(maxY, y));
        return new int[] { x, y };
    }

    private void showCleanupOverlay() {
        try {
            if (driver == null) return;
            JavascriptExecutor js = (JavascriptExecutor) driver;

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
}