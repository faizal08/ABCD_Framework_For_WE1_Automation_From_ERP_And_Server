package com.eit.automation.core;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelReader {
	private final String filePath;

	public ExcelReader(String filePath) {
		this.filePath = filePath;
	}

	public List<TestCase> getTestCasesFromSheet(String sheetName) {
		List<TestCase> testCases = new ArrayList<>();

		try (FileInputStream fis = new FileInputStream(filePath); Workbook workbook = new XSSFWorkbook(fis)) {

			Sheet sheet = workbook.getSheet(sheetName);
			if (sheet == null) {
				System.out.println("❌ Sheet not found: " + sheetName);
				return testCases;
			}

			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null)
					continue;

				Cell nameCell = row.getCell(2); // Test Case ID(s)
				Cell descCell = row.getCell(4); // Test Step Description

				if (nameCell == null || descCell == null)
					continue;

				String name = nameCell.toString().trim();
				String desc = descCell.toString().trim();

				if (!name.isEmpty() && !desc.isEmpty()) {
					testCases.add(new TestCase(name, desc));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return testCases;
	}
}
