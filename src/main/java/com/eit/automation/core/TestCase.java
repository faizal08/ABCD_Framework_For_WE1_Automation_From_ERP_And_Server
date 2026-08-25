package com.eit.automation.core;
public class TestCase {
    private final String name;
    private final String description;

    public TestCase(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}
