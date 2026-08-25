package com.eit.automation.erp;

import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileOutputStream;
import java.io.IOException;

public class TestERPDataReader {

    public static void main(String[] args) {
        try {
            // 1. Fetch JSON using your ERPApiClient
            ERPApiClient apiClient = new ERPApiClient();
            String jsonResponse = apiClient.fetchEpicData("EPIC-0001"); // Replace with valid Epic/Backlog name

            // 2. Generate the in-memory workbook using ERPTestDataReader
            Workbook workbook = ERPTestDataReader.createWorkbookFromJson(jsonResponse, "TC_RG_");

            // 3. Write in-memory workbook to a physical local file
            String outputPath = "target/Generated_ERP_TestCases.xlsx";
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
            workbook.close();

            System.out.println("✅ SUCCESS! Excel file written successfully to: " + outputPath);

        } catch (Exception e) {
            System.err.println("❌ Verification Failed!");
            e.printStackTrace();
        }
    }
}
