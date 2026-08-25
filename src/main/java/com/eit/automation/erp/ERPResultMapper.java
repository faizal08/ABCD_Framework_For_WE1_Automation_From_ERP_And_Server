package com.eit.automation.erp;

import com.eit.automation.utils.ReportGenerator;
import com.eit.automation.utils.ReportGenerator.StepResult;
import com.eit.automation.utils.ReportGenerator.TestCaseResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to map internal test results from ReportGenerator to the Frappe ERP JSON response structure.
 */
public class ERPResultMapper {

    public static JSONObject buildResultPayload(String targetEpic, ReportGenerator reportGenerator) {
        JSONObject payload = new JSONObject();

        // Preserve insertion order so "epic" stays at the top of the JSON payload
        try {
            Field mapField = JSONObject.class.getDeclaredField("map");
            mapField.setAccessible(true);
            mapField.set(payload, new LinkedHashMap<String, Object>());
        } catch (Exception e) {
            // Fallback in case reflection is blocked by module constraints
        }

        // SimpleDateFormat matching your Report HTML UI output ("10 Aug 2026 09:28:51 am")
        SimpleDateFormat summaryDateFormat = new SimpleDateFormat("dd MMM yyyy hh:mm:ss a");

        // 1. Top Level: Add "epic" object FIRST with nested "execution_summary"
        JSONObject epicObj = new JSONObject();
        epicObj.put("name", targetEpic != null && !targetEpic.trim().isEmpty() ? targetEpic : "EPIC-0001");

        // Construct Execution Summary
        JSONObject summaryObj = new JSONObject();
        summaryObj.put("start_time", reportGenerator.getStartTime() != null
                ? summaryDateFormat.format(reportGenerator.getStartTime()) : "");
        summaryObj.put("end_time", reportGenerator.getEndTime() != null
                ? summaryDateFormat.format(reportGenerator.getEndTime()) : "");
        summaryObj.put("duration", reportGenerator.getFormattedDuration());
        summaryObj.put("total_test_cases", reportGenerator.getTotalTests());
        summaryObj.put("passed", reportGenerator.getPassedTests());
        summaryObj.put("failed", reportGenerator.getFailedTests());
        summaryObj.put("success_rate", reportGenerator.getFormattedSuccessRate());

        // Attach execution summary under epic
        epicObj.put("execution_summary", summaryObj);

        payload.put("epic", epicObj);

        List<TestCaseResult> testResults = reportGenerator.getTestResults();

        // 2. Group test cases by Backlog ID (e.g. US-2026-00855)
        Map<String, List<TestCaseResult>> groupedByBacklog = new LinkedHashMap<>();

        if (testResults != null) {
            for (TestCaseResult tcr : testResults) {
                String backlogName = tcr.getUserStory();
                if (backlogName == null || backlogName.trim().isEmpty()) {
                    backlogName = "US-UNASSIGNED";
                }
                groupedByBacklog.computeIfAbsent(backlogName, k -> new ArrayList<>()).add(tcr);
            }
        }

        // 3. Construct "backlogs" JSONArray
        JSONArray backlogsArray = new JSONArray();

        for (Map.Entry<String, List<TestCaseResult>> entry : groupedByBacklog.entrySet()) {
            JSONObject backlogItem = new JSONObject();

            JSONObject backlogDetails = new JSONObject();
            String backlogName = entry.getKey();
            backlogDetails.put("name", backlogName);

            // Preserve Subject correctly from TestCaseResult metadata or default cleanly
            String subject = backlogName;
            if (!entry.getValue().isEmpty()) {
                TestCaseResult firstTc = entry.getValue().get(0);
                if (firstTc.getBacklogSubject() != null && !firstTc.getBacklogSubject().trim().isEmpty()) {
                    subject = firstTc.getBacklogSubject().trim();
                } else {
                    subject = backlogName.replace("-", "_");
                }
            }
            backlogDetails.put("subject", subject);

            backlogItem.put("backlog", backlogDetails);

            // Nested "test_cases" array
            JSONArray testCasesArray = new JSONArray();

            for (TestCaseResult tcr : entry.getValue()) {
                JSONObject tcObj = new JSONObject();
                tcObj.put("test_case_id", tcr.getName());
                tcObj.put("status", tcr.getStatus());

                // Using getVideoPath() directly as defined in TestCaseResult
                tcObj.put("execution_video_url", tcr.getVideoPath() != null ? tcr.getVideoPath() : "");

                // Nested "test_steps" array
                JSONArray stepsArray = new JSONArray();
                if (tcr.getSteps() != null) {
                    for (StepResult step : tcr.getSteps()) {
                        JSONObject stepObj = new JSONObject();
                        stepObj.put("step_no", step.getStepNumber());
                        stepObj.put("action", step.getAction() != null ? step.getAction() : "");
                        stepObj.put("description", step.getDescription() != null ? step.getDescription() : "");
                        stepObj.put("status", step.getStatus());
                        stepObj.put("screenshot_url", step.getScreenshotPath() != null ? step.getScreenshotPath() : "");
                        stepObj.put("error_message", step.getMessage() != null ? step.getMessage() : "");

                        stepsArray.put(stepObj);
                    }
                }

                tcObj.put("test_steps", stepsArray);
                testCasesArray.put(tcObj);
            }

            backlogItem.put("test_cases", testCasesArray);
            backlogsArray.put(backlogItem);
        }

        // 4. Attach backlogs array to top-level payload AFTER epic
        payload.put("backlogs", backlogsArray);

        return payload;
    }
}