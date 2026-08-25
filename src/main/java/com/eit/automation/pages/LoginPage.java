package com.eit.automation.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage extends BasePage {

    // URL of the login page
    public static final String PAGE_URL = "https://admin.we1.app/admin/login";

    // Define elements using @FindBy

    @FindBy(xpath = "//input[@id='email']")
    private WebElement emailField;

    @FindBy(xpath = "//input[@id='password']")
    private WebElement passwordField;

    @FindBy(xpath = "//label[contains(text(),'Super')]")
    private WebElement superAdminLabel;

    @FindBy(xpath = "//button[@type='submit']")
    private WebElement submitButton;

    @FindBy(xpath = "//h5[normalize-space()='Dashboard']")
    private WebElement dashboardHeader;

    public LoginPage(WebDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    /**
     * Generic login action
     */
    public void login(String email, String password) {
        emailField.clear();
        emailField.sendKeys(email);

        passwordField.clear();
        passwordField.sendKeys(password);

        superAdminLabel.click();
        submitButton.click();
    }

    // Getters
    public WebElement getEmailField() {
        return emailField;
    }

    public WebElement getPasswordField() {
        return passwordField;
    }

    public WebElement getSuperAdminLabel() {
        return superAdminLabel;
    }

    public WebElement getSubmitButton() {
        return submitButton;
    }

    public WebElement getDashboardHeader() {
        return dashboardHeader;
    }
}
