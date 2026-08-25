package com.eit.automation.utils;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CSVTestCaseReader {

    public static class TestCaseData {
        private String testCaseName;
        private String stepBlock;

        public TestCaseData(String testCaseName, String stepBlock) {
            this.testCaseName = testCaseName;
            this.stepBlock = stepBlock;
        }

        public String getTestCaseName() {
            return testCaseName;
        }

        public String getStepBlock() {
            return stepBlock;
        }
    }

    /**
     * Read test cases from a CSV file
     * 
     * @param filePath Path to the CSV file
     * @return List of TestCaseData objects
     * @throws IOException If file cannot be read
     */
    public static List<TestCaseData> readTestCases(String filePath) throws IOException {
        Map<String, StringBuilder> testCaseMap = new LinkedHashMap<>();

        try (Reader reader = new FileReader(filePath);
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setTrim(true)
                        .build())) {

            for (CSVRecord record : csvParser) {
                // Skip empty rows
                if (record.size() == 0) {
                    continue;
                }

                // Get test case ID from column 1 (0-indexed)
                String testCaseId = getColumnValue(record, 1);
                if (testCaseId == null || testCaseId.isEmpty()) {
                    continue;
                }

                // Get step details from columns
                String description = getColumnValue(record, 2);
                String action = getColumnValue(record, 3);
                String value = getColumnValue(record, 4);
                String xpath = getColumnValue(record, 5);

                // Build step in the format expected by StepParser
                // Format: Action | Value | XPath | Description
                StringBuilder stepBuilder = new StringBuilder();
                stepBuilder.append(action != null ? action : "");
                stepBuilder.append(" | ");
                stepBuilder.append(value != null ? value : "");
                stepBuilder.append(" | ");
                stepBuilder.append(xpath != null ? xpath : "");
                stepBuilder.append(" | ");
                stepBuilder.append(description != null ? description : "");

                // Add step to the test case's step block
                testCaseMap.putIfAbsent(testCaseId, new StringBuilder());
                if (testCaseMap.get(testCaseId).length() > 0) {
                    testCaseMap.get(testCaseId).append("\n");
                }
                testCaseMap.get(testCaseId).append(stepBuilder.toString());
            }
        }

        // Convert map to list of TestCaseData
        List<TestCaseData> testCases = new ArrayList<>();
        for (Map.Entry<String, StringBuilder> entry : testCaseMap.entrySet()) {
            testCases.add(new TestCaseData(entry.getKey(), entry.getValue().toString()));
        }

        return testCases;
    }

    private static String getColumnValue(CSVRecord record, int index) {
        if (index < record.size()) {
            return record.get(index);
        }
        return "";
    }
}
