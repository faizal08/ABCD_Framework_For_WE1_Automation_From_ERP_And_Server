package com.eit.automation.core;

import com.eit.automation.annotations.Action;
import com.eit.automation.parser.TestStep;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ActionRegistry {

    private final Map<String, Method> actionMap = new HashMap<>();
    private final Map<String, Object> instanceMap = new HashMap<>();

    /**
     * Register an object handling actions.
     * Scans the object's class for @Action annotated methods.
     */
    public void registerHandler(Object handler) {
        Class<?> clazz = handler.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Action.class)) {
                Action action = method.getAnnotation(Action.class);
                for (String key : action.keys()) {
                    actionMap.put(key.toLowerCase(), method);
                    instanceMap.put(key.toLowerCase(), handler);
                }
            }
        }
    }

    /**
     * Execute an action based on the step.
     */
    public void execute(TestStep step, WebDriver driver) throws Exception {
        String actionKey = step.getAction().toLowerCase();
        Method method = actionMap.get(actionKey);
        Object handler = instanceMap.get(actionKey);

        if (method != null && handler != null) {
            // Flexible argument matching could be implemented here.
            // For now, we assume methods take specific standard arguments or we adapt.
            // This is a simplified version; in a full implementation, we might inspect
            // parameter types.

            try {
                Object cachedElement = step.getCachedElement();

                // 1. Try (WebElement, String) - for InputActions
                if (cachedElement instanceof WebElement && method.getParameterCount() == 2
                        && method.getParameterTypes()[0] == WebElement.class
                        && method.getParameterTypes()[1] == String.class) {
                    method.invoke(handler, (WebElement) cachedElement, step.getValue());
                    return;
                }

                // 2. Try (WebElement) - for ClickActions
                if (cachedElement instanceof WebElement && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == WebElement.class) {
                    method.invoke(handler, (WebElement) cachedElement);
                    return;
                }

                // 3. Try (TestStep)
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == TestStep.class) {
                    method.invoke(handler, step);
                    return;
                }

                // 4. Try (String xpath, String value)
                if (method.getParameterCount() == 2
                        && method.getParameterTypes()[0] == String.class
                        && method.getParameterTypes()[1] == String.class) {
                    method.invoke(handler, step.getXpath(), step.getValue());
                    return;
                }

                // 5. Try (String xpath)
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                    method.invoke(handler, step.getXpath());
                    return;
                }

                if (method.getParameterCount() == 0) {
                    method.invoke(handler);
                    return;
                }

                throw new RuntimeException("No matching signature found for action: " + actionKey);

            } catch (Exception e) {
                // Unwrap reflection exception
                if (e instanceof java.lang.reflect.InvocationTargetException) {
                    throw (Exception) e.getCause();
                }
                throw e;
            }
        } else {
            // Fallback or Exception?
            // specific logic can remain in TestExecutor or be migrated slowly.
            throw new RuntimeException("Unknown action in Registry: " + actionKey);
        }
    }

    public boolean hasAction(String key) {
        return actionMap.containsKey(key.toLowerCase());
    }
}
