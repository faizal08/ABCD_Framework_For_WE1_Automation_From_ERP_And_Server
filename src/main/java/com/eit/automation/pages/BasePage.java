package com.eit.automation.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all Page Objects.
 * Handles initialization and dynamic element lookup.
 */
public abstract class BasePage {
    protected WebDriver driver;
    protected WebDriverWait wait;

    // Cache for name -> WebElement mapping
    private Map<String, Field> elementMap = new HashMap<>();

    public BasePage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        PageFactory.initElements(driver, this);
        cacheElements();
    }

    /**
     * Cache all fields annotated with @FindBy for quick lookup by name.
     */
    private void cacheElements() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType().equals(WebElement.class)) {
                field.setAccessible(true);
                elementMap.put(field.getName().toLowerCase(), field);
            }
        }
    }

    /**
     * Get a WebElement by its field name.
     */
    public WebElement getElement(String name) {
        Field field = elementMap.get(name.toLowerCase().replaceAll("\\s+", ""));
        if (field != null) {
            try {
                return (WebElement) field.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Error accessing field: " + name, e);
            }
        }
        return null;
    }
}
