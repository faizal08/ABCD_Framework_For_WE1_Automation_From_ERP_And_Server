package com.eit.automation.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;

public class ExcelReader {
    public static void main(String[] args) {
        try {
            String excelPath = "testingwe12.xlsx";
            String sheetName = "Sheet4";
            String searchTerm = "SA_TS_3";

            System.out.println("Reading: " + excelPath + " - Sheet: " + sheetName);
            System.out.println("=" + "=".repeat(80));

            File excelFile = new File(excelPath);
            FileInputStream fis = new FileInputStream(excelFile);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheet(sheetName);

            if (sheet == null) {
                System.out.println("Sheet '" + sheetName + "' not found!");
                workbook.close();
                return;
            }

            // Print headers
            Row headerRow = sheet.getRow(0);
            System.out.println("\nColumn Headers:");
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    System.out.println("  Column " + (char) ('A' + i) + " (" + i + "): " + cell.getStringCellValue());
                }
            }

            System.out.println("\n" + "=".repeat(80));
            System.out.println("Searching for Requirement ID: " + searchTerm);
            System.out.println("=".repeat(80) + "\n");

            // Search for SA_TS_3
            boolean found = false;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                boolean rowContainsSearch = false;
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        String cellValue = getCellValueAsString(cell);
                        if (cellValue.contains(searchTerm)) {
                            rowContainsSearch = true;
                            break;
                        }
                    }
                }

                if (rowContainsSearch) {
                    found = true;
                    System.out.println("Row " + (i + 1) + ":");
                    for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                        Cell headerCell = headerRow.getCell(j);
                        Cell dataCell = row.getCell(j);

                        if (headerCell != null && dataCell != null) {
                            String header = headerCell.getStringCellValue();
                            String value = getCellValueAsString(dataCell);
                            if (!value.trim().isEmpty()) {
                                System.out.println("  " + header + ": " + value);
                            }
                        }
                    }
                    System.out.println();
                }
            }

            if (!found) {
                System.out.println("No rows found with '" + searchTerm + "'");
                System.out.println("\nShowing first 3 rows to help identify the data:");
                for (int i = 1; i <= Math.min(3, sheet.getLastRowNum()); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null)
                        continue;

                    System.out.println("\nRow " + (i + 1) + ":");
                    for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                        Cell headerCell = headerRow.getCell(j);
                        Cell dataCell = row.getCell(j);

                        if (headerCell != null && dataCell != null) {
                            String header = headerCell.getStringCellValue();
                            String value = getCellValueAsString(dataCell);
                            if (!value.trim().isEmpty()) {
                                System.out.println("  " + header + ": " + value);
                            }
                        }
                    }
                }
            }

            workbook.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
