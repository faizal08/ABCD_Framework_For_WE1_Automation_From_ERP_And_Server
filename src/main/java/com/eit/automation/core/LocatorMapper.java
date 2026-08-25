package com.eit.automation.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class LocatorMapper {
    private static final Properties props = new Properties();

    static {
        // 🚀 Dynamic Root Folder Scan: Looks for a 'locators' folder at the root directory
        String rootPath = System.getProperty("user.dir");
        File locatorsFolder = new File(rootPath + File.separator + "locators");

        System.out.println("📂 Looking for Object Repositories in folder: " + locatorsFolder.getAbsolutePath());

        if (locatorsFolder.exists() && locatorsFolder.isDirectory()) {
            File[] files = locatorsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".properties"));

            if (files != null && files.length > 0) {
                for (File file : files) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        Properties tempProps = new Properties();
                        tempProps.load(fis);
                        props.putAll(tempProps); // Merges individual module properties into the master map
                        System.out.println("   ↳ Loaded: " + file.getName() + " (" + tempProps.size() + " mappings)");
                    } catch (IOException e) {
                        System.out.println("❌ Error reading locator file " + file.getName() + ": " + e.getMessage());
                    }
                }
                System.out.println("✅ Centralized Object Repository Initialized. Total merged mappings: " + props.size());
            } else {
                System.out.println("⚠️ Warning: No .properties files found inside the 'locators' folder.");
            }
        } else {
            System.out.println("⚠️ Warning: 'locators' folder not found at project root. Direct fallback mode active.");
        }
    }

    /**
     * Resolves an Excel/CSV locator element.
     * @param label The logical key (e.g., 'driver.agree_btn') or a raw XPath/ID.
     * @return The real target locator string.
     */
    public static String getXPath(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "";
        }

        // Strip out any accidental wrapping quotes added by Excel/CSV sheet formatting
        String cleanLabel = label.replace("\"", "").trim();

        // Fallback: Return the property value if mapped, otherwise return the original sheet text
        return props.getProperty(cleanLabel, label);
    }
}