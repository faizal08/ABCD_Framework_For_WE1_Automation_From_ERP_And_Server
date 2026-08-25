package com.eit.automation.erp;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONObject;

public class TestERPApiClient {

    public static void main(String[] args) {
        Properties config = new Properties();
        String configPath = "EPIC_0001_Platform_Configuration.properties";

        // 1. Load config properties
        try (InputStream input = new FileInputStream(configPath)) {
            config.load(input);
            System.out.println("✅ Config loaded successfully from: " + configPath);
        } catch (Exception e) {
            System.out.println("⚠️ Could not load " + configPath + ". Proceeding with fallback defaults.");
        }

        // 2. Read values dynamically from config file
        String targetType = config.getProperty("erp.target.type", "Epic");
        String targetName = config.getProperty("erp.target.name", "EPIC-0001");

        // 3. Initialize API Client
        ERPApiClient erpClient = new ERPApiClient(config);

        try {
            System.out.println("\n--- Testing API Request [" + targetType + ": " + targetName + "] ---");
            String jsonResponse;

            // 4. Trigger specific API fetch based on target type
            if ("Backlog".equalsIgnoreCase(targetType)) {
                jsonResponse = erpClient.fetchBacklogData(targetName);
            } else {
                jsonResponse = erpClient.fetchEpicData(targetName);
            }

            System.out.println("\n✅ SUCCESS! Received Response from ERP:");
            System.out.println("==================================================");

            // 5. Parse and print structured logs
            logTestCaseSummary(jsonResponse);

            System.out.println("==================================================");

        } catch (Exception e) {
            System.err.println("\n❌ API Request Failed!");
            e.printStackTrace();
        }
    }

    /**
     * Parses the raw ERP JSON response and logs it in a clean, readable format.
     */
    private static void logTestCaseSummary(String jsonResponse) {
        try {
            JSONObject responseObj = new JSONObject(jsonResponse);

            if (!responseObj.has("message")) {
                System.out.println("⚠️ No 'message' key found in ERP response.");
                return;
            }

            JSONArray messageArray = responseObj.getJSONArray("message");

            for (int i = 0; i < messageArray.length(); i++) {
                JSONObject epicData = messageArray.getJSONObject(i);

                // --- EPIC LEVEL ---
                if (epicData.has("epic")) {
                    JSONObject epic = epicData.getJSONObject("epic");
                    String epicId = epic.optString("name", "N/A");
                    String epicName = epic.optString("epic_name", "N/A");
                    System.out.println("\n📌 EPIC: " + epicId + " (" + epicName + ")");
                }

                // --- BACKLOG LEVEL ---
                if (epicData.has("backlogs")) {
                    JSONArray backlogs = epicData.getJSONArray("backlogs");

                    for (int j = 0; j < backlogs.length(); j++) {
                        JSONObject backlogData = backlogs.getJSONObject(j);

                        if (backlogData.has("backlog")) {
                            JSONObject backlog = backlogData.getJSONObject("backlog");
                            String backlogId = backlog.optString("name", "N/A");
                            String backlogSubject = backlog.optString("subject", "N/A");
                            System.out.println("\n  📂 Backlog: " + backlogId + " - " + backlogSubject);
                        }

                        // --- TEST CASES LEVEL ---
                        if (backlogData.has("test_cases")) {
                            JSONArray testCases = backlogData.getJSONArray("test_cases");

                            for (int k = 0; k < testCases.length(); k++) {
                                JSONObject tc = testCases.getJSONObject(k);
                                String tcId = tc.optString("test_case_id", "N/A");
                                String tcDesc = tc.optString("test_case_description", "N/A");

                                System.out.println("\n    🧪 Test Case: " + tcId + " (" + tcDesc + ")");
                                System.out.println("    --------------------------------------------------");

                                // --- REGRESSION TEST STEPS ---
                                if (tc.has("regression_test_steps")) {
                                    JSONArray steps = tc.getJSONArray("regression_test_steps");
                                    for (int s = 0; s < steps.length(); s++) {
                                        System.out.println("      🔹 Step " + (s + 1) + ": " + steps.getString(s));
                                    }
                                } else {
                                    System.out.println("      ⚠️ No regression_test_steps found.");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error parsing ERP JSON response: " + e.getMessage());
            System.out.println("Raw Response Body:\n" + jsonResponse);
        }
    }
}