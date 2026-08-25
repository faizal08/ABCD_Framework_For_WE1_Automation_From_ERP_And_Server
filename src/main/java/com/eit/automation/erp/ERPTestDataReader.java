package com.eit.automation.erp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;

public class ERPTestDataReader {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Workbook createWorkbookFromJson(String jsonResponse, String filterName) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        CellStyle wrapTextStyle = workbook.createCellStyle();
        wrapTextStyle.setWrapText(true);

        // Check if backlogs array is direct at root OR inside "message" array
        JsonNode backlogs = rootNode.path("backlogs");
        if (!backlogs.isArray()) {
            JsonNode messageArray = rootNode.path("message");
            if (messageArray.isArray() && messageArray.size() > 0) {
                backlogs = messageArray.get(0).path("backlogs");
            }
        }

        if (!backlogs.isArray()) {
            return workbook;
        }

        for (JsonNode backlogWrapper : backlogs) {
            JsonNode backlog = backlogWrapper.path("backlog");
            JsonNode testCases = backlogWrapper.path("test_cases");

            String backlogName = backlog.path("name").asText("").trim();
            String backlogSubject = backlog.path("subject").asText("Default_Sheet").trim();

            String sheetName = backlogSubject;

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.createSheet(sheetName);
                createHeaderRow(sheet);
            }

            if (!testCases.isArray()) continue;

            for (JsonNode tc : testCases) {
                String testCaseId = tc.path("test_case_id").asText("").trim();

                if (filterName != null && !filterName.trim().isEmpty()) {
                    if (!testCaseId.toLowerCase().contains(filterName.toLowerCase().trim())) {
                        continue;
                    }
                }

                // Support both "test_steps" and "regression_test_steps"
                JsonNode stepsNode = tc.has("test_steps") ? tc.path("test_steps") : tc.path("regression_test_steps");
                StringBuilder stepsBuilder = new StringBuilder();

                if (stepsNode.isArray()) {
                    for (int i = 0; i < stepsNode.size(); i++) {
                        JsonNode stepItem = stepsNode.get(i);
                        String stepText;

                        // Check if step item is an object (contains "description") or plain text
                        if (stepItem.isObject()) {
                            stepText = stepItem.path("description").asText("").trim();
                        } else {
                            stepText = stepItem.asText().trim();
                        }

                        if (!stepText.isEmpty()) {
                            stepsBuilder.append(stepText);
                            if (i < stepsNode.size() - 1) {
                                stepsBuilder.append("\n");
                            }
                        }
                    }
                } else if (stepsNode.isTextual()) {
                    String rawSteps = stepsNode.asText().trim();
                    String formattedSteps = rawSteps.replaceAll("(?<=\\D)(?=\\d+\\.)", "\n");
                    stepsBuilder.append(formattedSteps);
                }

                int lastRowNum = sheet.getLastRowNum();
                Row row = sheet.createRow(lastRowNum + 1);

                // Cell Index 3 (Column D): Test Case ID [Regression]
                Cell testCaseCell = row.createCell(3);
                testCaseCell.setCellValue(testCaseId);

                // Cell Index 6 (Column G): Regression Test Steps
                Cell stepBlockCell = row.createCell(6);
                stepBlockCell.setCellValue(stepsBuilder.toString());
                stepBlockCell.setCellStyle(wrapTextStyle);

                // Cell Index 7 (Column H): Backlog Name (US-XXXX)
                Cell backlogNameCell = row.createCell(7);
                backlogNameCell.setCellValue(backlogName);

                // Cell Index 8 (Column I): Backlog Subject
                Cell backlogSubjectCell = row.createCell(8);
                backlogSubjectCell.setCellValue(backlogSubject);
            }
        }

        return workbook;
    }

    private static void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Requirement ID");
        headerRow.createCell(1).setCellValue("Requirement Title");
        headerRow.createCell(2).setCellValue("Test Case ID [Manual]");
        headerRow.createCell(3).setCellValue("Test Case ID [Regression]");
        headerRow.createCell(4).setCellValue("Test Case Description");
        headerRow.createCell(5).setCellValue("Manual Test Steps");
        headerRow.createCell(6).setCellValue("Regression Test Steps");
        headerRow.createCell(7).setCellValue("Backlog Name");
        headerRow.createCell(8).setCellValue("Backlog Subject");
    }
}