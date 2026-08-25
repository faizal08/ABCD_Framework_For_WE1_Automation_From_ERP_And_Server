package com.eit.automation.core;

import com.eit.automation.pages.BasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages Page Object instances and provides element lookup across pages.
 */
public class PageObjectManager {
    private WebDriver driver;
    private WebDriverWait wait;
    private Map<Class<? extends BasePage>, BasePage> pages = new HashMap<>();

    public PageObjectManager(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        initializePages();
    }

    private void initializePages() {
        // Auto-discover all BasePage subclasses in the 'pages' package
        Reflections reflections = new Reflections("com.eit.automation.pages");
        Set<Class<? extends BasePage>> pageClasses = reflections.getSubTypesOf(BasePage.class);

        for (Class<? extends BasePage> pageClass : pageClasses) {
            try {
                if (!pages.containsKey(pageClass)) {
                    BasePage pageInstance = pageClass.getDeclaredConstructor(WebDriver.class, WebDriverWait.class)
                            .newInstance(driver, wait);
                    pages.put(pageClass, pageInstance);
                }
            } catch (Exception e) {
                System.err.println("Failed to initialize page: " + pageClass.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * Try to find an element by name across all registered pages.
     */
    public WebElement findElementByName(String name) {
        for (BasePage page : pages.values()) {
            WebElement element = page.getElement(name);
            if (element != null) {
                return element;
            }
        }
        return null; // Not found in any page object
    }

    public <T extends BasePage> T getPage(Class<T> pageClass) {
        return pageClass.cast(pages.get(pageClass));
    }
}
