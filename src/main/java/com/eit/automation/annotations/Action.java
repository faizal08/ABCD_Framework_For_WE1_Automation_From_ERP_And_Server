package com.eit.automation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an executable action in the automation framework.
 * mapped via the action keyword (e.g., "click", "verify").
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
    /**
     * The action keyword(s) that map to this method.
     * Example: @Action(keys = {"click", "select"})
     */
    String[] keys();
}
