package com.eit.automation.core;

import com.eit.automation.erp.ERPTestDataReader;
import com.eit.automation.parser.StepParser;
import com.eit.automation.parser.TestStep;
import com.eit.automation.utils.CSVTestCaseReader;
import com.eit.automation.utils.CloudinaryUploader;
import com.eit.automation.utils.ReportGenerator;
import com.eit.automation.utils.VideoRecorder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Main {
    public static Properties config;
    public static TestExecutor executor;
    private static VideoRecorder videoRecorder;

    // Track sheets already completed to avoid infinite loops and double runs
    private static Set<String> executedSheets = new HashSet<>();

    // Tracks which sessions from the DriverPool have been initialized
    public static Set<String> activeSessions = new HashSet<>();

    static {
        try {
            videoRecorder = new VideoRecorder();
        } catch (Exception e) {
            System.err.println("❌ Critical: Failed to initialize Video Recorder: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ReportGenerator reportGenerator = new ReportGenerator();
        Workbook workbook = null;

        try {
            System.out.println("=== 🚀 EIT Universal Test Automation Started (Web + Mobile) ===\n");

            // -----------------------------------------------------------------
            // 1. DYNAMIC & HIERARCHICAL PROPERTIES LOADING
            // -----------------------------------------------------------------
            config = new Properties();

            // Step 1A: Load base secrets file if present (Git-Ignored / Local / Server)
            File secretFile = new File("secret.properties");
            if (!secretFile.exists()) {
                secretFile = new File("secrets.properties"); // Fallback check
            }

            if (secretFile.exists()) {
                System.out.println("🔒 Loading Secret Properties from: " + secretFile.getAbsolutePath());
                try (FileInputStream secretFis = new FileInputStream(secretFile)) {
                    config.load(secretFis);
                    System.out.println("✅ Secret properties successfully loaded.");
                } catch (Exception e) {
                    System.err.println("⚠️ Warning: Failed to load secret properties file: " + e.getMessage());
                }
            } else {
                System.out.println("ℹ️ No local secret.properties file found. Proceeding with configuration file and system environment variables.");
            }

            // Step 1B: Load specific Epic / Environment configuration file
            String env = System.getProperty("env");

            String configFileName = (env != null && !env.trim().isEmpty())
                    ? env.trim() + ".properties"
                    : "config.properties";

            File configFile = new File(configFileName);

            if (!configFile.exists()) {
                System.out.println("⚠️ Config '" + configFileName + "' not found in root directory. Falling back to 'config.properties'.");
                configFileName = "config.properties";
                configFile = new File(configFileName);
            }

            System.out.println("🔧 Loading Configuration from Root: " + configFile.getAbsolutePath());
            if (configFile.exists()) {
                try (FileInputStream configFis = new FileInputStream(configFile)) {
                    config.load(configFis); // Merges with or overrides secrets
                }
            } else {
                System.err.println("⚠️ Warning: Config file '" + configFileName + "' was not found.");
            }

            // Step 1C: Environment Variable Overrides for CI/CD Server execution
            for (String key : config.stringPropertyNames()) {
                String envVarKey = key.toUpperCase().replace('.', '_');
                String envValue = System.getenv(envVarKey);
                if (envValue != null && !envValue.trim().isEmpty()) {
                    config.setProperty(key, envValue.trim());
                }
            }

            String cliSource = System.getProperty("source");
            String dataSource = (cliSource != null && !cliSource.trim().isEmpty())
                    ? cliSource.trim().toUpperCase()
                    : config.getProperty("data.source", "EXCEL").trim().toUpperCase();

            String filterName = config.getProperty("filter.name", "TC_RG_");

            executor = new TestExecutor(reportGenerator, config);

            // -----------------------------------------------------------------
            // 2. DYNAMIC WORKBOOK INITIALIZATION (ERP VS LOCAL EXCEL / CSV)
            // -----------------------------------------------------------------
            if ("ERP".equals(dataSource)) {
                System.out.println("\n🌐 [EXECUTION MODE: FRAPPE ERP API]");

                String erpBaseUrl = config.getProperty("erp.base.url");
                String epicName = config.getProperty("erp.target.name");
                if (epicName == null || epicName.isEmpty()) {
                    epicName = config.getProperty("erp.epic.name");
                }

                if (erpBaseUrl == null || epicName == null) {
                    throw new RuntimeException("❌ ERP Configuration error: 'erp.base.url' or 'erp.target.name' is missing in " + configFileName);
                }

                System.out.println("🔗 Connecting to ERP API at: " + erpBaseUrl);
                System.out.println("🎯 Fetching Test Data for Epic/Backlog: " + epicName);

                com.eit.automation.erp.ERPApiClient apiClient = new com.eit.automation.erp.ERPApiClient(config);
                String jsonResponse = apiClient.fetchEpicData(epicName);

                workbook = ERPTestDataReader.createWorkbookFromJson(jsonResponse, filterName);
                reportGenerator.setExcelFileName("ERP_" + epicName);
                reportGenerator.startTestExecution();

                executeWorkbookTestCases(workbook, reportGenerator);

            } else {
                System.out.println("\n📊 [EXECUTION MODE: LOCAL FILE (EXCEL/CSV)]");

                String testFilePath = config.getProperty("excel.name");
                if (testFilePath == null || testFilePath.trim().isEmpty()) {
                    throw new RuntimeException("❌ Error: 'excel.name' is missing or empty in " + configFileName);
                }

                reportGenerator.setExcelFileName(testFilePath);
                reportGenerator.startTestExecution();

                System.out.println("📂 Reading test cases from local file: " + testFilePath);
                File testFile = new File(testFilePath);
                if (!testFile.exists()) {
                    throw new RuntimeException("❌ Local test file not found at: " + testFile.getAbsolutePath());
                }

                if (testFilePath.toLowerCase().endsWith(".csv")) {
                    System.out.println("📄 Detected CSV file format");
                    readCSVTestCases(testFilePath, executor, reportGenerator);
                } else if (testFilePath.toLowerCase().endsWith(".xlsx") || testFilePath.toLowerCase().endsWith(".xls")) {
                    System.out.println("📊 Detected Excel file format");
                    try (FileInputStream fis = new FileInputStream(testFile)) {
                        workbook = new XSSFWorkbook(fis);
                        executeWorkbookTestCases(workbook, reportGenerator);
                    }
                } else {
                    throw new RuntimeException("Unsupported file format. Please use .csv, .xlsx, or .xls files.");
                }
            }

            reportGenerator.endTestExecution();

            // ==========================================
            // 3. POST-SUITE ARTIFACT PROCESSING
            // ==========================================
            Map<String, String> localArtifacts = ScreenshotHelper.getLocalArtifactsMap();

            // Check system property directly: -Dsource=erp
            boolean isErpRun = "erp".equalsIgnoreCase(System.getProperty("source"));

            if (isErpRun) {
                if (!localArtifacts.isEmpty()) {
                    System.out.println("\n☁️ [ERP RUN] Starting High-Speed Post-Suite Parallel Batch Upload to Cloudinary...");
                    int concurrencyLimit = 30;
                    Map<String, String> uploadedUrls = CloudinaryUploader.uploadBatchInParallel(localArtifacts, concurrencyLimit);

                    System.out.println("🔄 Syncing Cloudinary media links with HTML report...");
                    reportGenerator.updateMediaPaths(uploadedUrls);
                    System.out.println("✅ HTML report updated with hosted Cloudinary links!");
                }
            } else {
                System.out.println("\n📁 [LOCAL EXCEL RUN] Retaining local relative artifact paths for HTML report.");
            }

            // Clear registry and shutdown executors
            ScreenshotHelper.clearRegistry();
            CloudinaryUploader.shutdownExecutors();

            // -----------------------------------------------------------------
            // 4. POST RESULTS BACK TO FRAPPE ERP API (ONLY IF DATA SOURCE IS ERP)
            // -----------------------------------------------------------------
            if ("ERP".equals(dataSource)) {
                System.out.println("\n🌐 [ERP SYNC: Posting Execution Results to ERP Server]");

                String epicName = config.getProperty("erp.target.name");
                if (epicName == null || epicName.isEmpty()) {
                    epicName = config.getProperty("erp.epic.name");
                }

                if (epicName == null || epicName.trim().isEmpty()) {
                    System.err.println("❌ ERP Sync Failed: Neither 'erp.target.name' nor 'erp.epic.name' is configured in properties.");
                } else {

                    if (reportGenerator.getTestResults() != null) {
                        for (ReportGenerator.TestCaseResult tcr : reportGenerator.getTestResults()) {
                            if (tcr.getUserStory() == null || tcr.getUserStory().trim().isEmpty()) {
                                tcr.setUserStory(epicName);
                            }
                        }
                    }

                    org.json.JSONObject resultPayload = com.eit.automation.erp.ERPResultMapper.buildResultPayload(epicName, reportGenerator);

                    System.out.println("\n================ GENERATED ERP JSON PAYLOAD ================");
                    System.out.println(resultPayload.toString(4));
                    System.out.println("============================================================\n");

                    com.eit.automation.erp.ERPApiClient apiClient = new com.eit.automation.erp.ERPApiClient(config);
                    String erpResponse = apiClient.sendTestResults(resultPayload);
                    System.out.println("📩 ERP Response: " + erpResponse);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Execution failed: " + e.getMessage());
            e.printStackTrace();
            reportGenerator.endTestExecution();
        } finally {
            ScreenshotHelper.clearRegistry();

            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception ignored) {}
            }
            if (executor != null) {
                System.out.println("🛑 Terminating all active automation sessions...");
                executor.close();
            }
        }
    }

    private static void executeWorkbookTestCases(Workbook workbook, ReportGenerator reportGenerator) throws Exception {
        String sheetNameConfig = config.getProperty("sheets.name");
        String[] rawSheetTokens = (sheetNameConfig != null) ? sheetNameConfig.split(",") : new String[0];

        if (executor == null) {
            executor = new TestExecutor(reportGenerator, config);
        }

        for (String token : rawSheetTokens) {
            String cleanToken = token.trim();
            if (cleanToken.isEmpty()) continue;

            String sheetName = cleanToken;
            int repeatCount = 1;

            if (cleanToken.contains("[") && cleanToken.endsWith("]")) {
                try {
                    sheetName = cleanToken.substring(0, cleanToken.indexOf("[")).trim();
                    String countStr = cleanToken.substring(cleanToken.indexOf("[") + 1, cleanToken.length() - 1).trim();
                    repeatCount = Integer.parseInt(countStr);
                } catch (Exception e) {
                    System.err.println("⚠️ Warning: Failed to parse iteration count for token '" + cleanToken + "'. Falling back to 1 loop.");
                    sheetName = cleanToken.replace("[", "").replace("]", "").trim();
                    repeatCount = 1;
                }
            }

            System.out.println("\n🎯 Target Configuration Set: Sheet [" + sheetName + "] will iterate " + repeatCount + " time(s).");
            for (int currentRun = 1; currentRun <= repeatCount; currentRun++) {
                System.out.println("\n========================================================");
                System.out.println("🔄 HYBRID STRESS LOOP ATTEMPT #" + currentRun + " OF " + repeatCount + " FOR SHEET: [" + sheetName + "]");
                System.out.println("========================================================");

                executedSheets.remove(sheetName);

                runSheetWithPrecondition(sheetName, workbook, reportGenerator);
            }
        }
    }

    private static void runSheetWithPrecondition(String sheetName, Workbook workbook, ReportGenerator reportGenerator) throws Exception {
        if (executedSheets.contains(sheetName)) return;

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            System.err.println("⚠️ Warning: Sheet '" + sheetName + "' not found in generated workbook!");
            return;
        }

        Row firstRow = sheet.getRow(1);
        if (firstRow != null) {
            Cell preconditionCell = firstRow.getCell(7);
            if (preconditionCell != null) {
                String fullText = preconditionCell.getStringCellValue();

                if (fullText.contains("RunSheet:")) {
                    String[] parts = fullText.split("RunSheet:");
                    for (int j = 1; j < parts.length; j++) {
                        String rawNames = parts[j].split("\\n|\\r")[0].trim();
                        String[] dependencies = rawNames.split(",");

                        for (String dep : dependencies) {
                            String dependencySheet = dep.trim().split("\\s+")[0].replace(".", "");
                            if (!dependencySheet.isEmpty() && !executedSheets.contains(dependencySheet)) {
                                System.out.println("🔗 Multi-Dependency Found: [" + dependencySheet + "]");
                                runSheetWithPrecondition(dependencySheet, workbook, reportGenerator);
                            }
                        }
                    }
                }
            }
        }

        processSheetData(sheet, sheetName, reportGenerator);
        executedSheets.add(sheetName);
    }

    private static void processSheetData(Sheet sheet, String sheetName, ReportGenerator reportGenerator) {
        System.out.println("\n📖 Processing Sheet: [" + sheetName + "]");

        String cliSource = System.getProperty("source");
        String dataSource = (cliSource != null && !cliSource.trim().isEmpty())
                ? cliSource.trim().toUpperCase()
                : config.getProperty("data.source", "EXCEL").trim().toUpperCase();

        boolean isErpRun = "ERP".equals(dataSource);

        if (executor != null) {
            executor.setCleanupMode(sheetName.equalsIgnoreCase("DataCleanUpSheet"));
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell testCaseCell = row.getCell(3);
            Cell stepBlockCell = row.getCell(6);
            if (testCaseCell == null || stepBlockCell == null) continue;

            String testCaseName = testCaseCell.getStringCellValue().trim();
            String stepBlock = stepBlockCell.getStringCellValue().trim();

            String filterName = config.getProperty("filter.name");
            if (filterName != null && !filterName.trim().isEmpty()
                    && !testCaseName.toLowerCase().contains(filterName.toLowerCase().trim())) {
                continue;
            }

            String backlogName = "";
            String backlogSubject = "";

            if (isErpRun) {
                Cell backlogNameCell = row.getCell(7);
                Cell backlogSubjectCell = row.getCell(8);

                backlogName = (backlogNameCell != null) ? backlogNameCell.getStringCellValue().trim() : "";
                backlogSubject = (backlogSubjectCell != null) ? backlogSubjectCell.getStringCellValue().trim() : "";
            }

            String videoFileName = testCaseName.replaceAll("[^a-zA-Z0-9]", "_") + ".mp4";

            try {
                if (videoRecorder != null) {
                    System.out.println("🎥 Starting Video Recording: " + videoFileName);
                    videoRecorder.startRecording(reportGenerator.getReportDir(), videoFileName);
                }

                executeTestCase(sheetName, testCaseName, stepBlock, executor, reportGenerator);

                if (isErpRun) {
                    List<ReportGenerator.TestCaseResult> results = reportGenerator.getTestResults();
                    if (results != null && !results.isEmpty()) {
                        ReportGenerator.TestCaseResult latestResult = results.get(results.size() - 1);
                        if (latestResult != null && latestResult.getName().equals(testCaseName)) {
                            latestResult.setUserStory(backlogName);
                            latestResult.setBacklogSubject(backlogSubject);
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("❌ Error in " + testCaseName + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    if (videoRecorder != null && videoRecorder.isRecording()) {
                        String savedVideoPath = videoRecorder.stopRecording(videoFileName);
                        if (savedVideoPath != null && !savedVideoPath.isEmpty()) {
                            reportGenerator.addVideoToTestCase(videoFileName);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to stop video recorder: " + e.getMessage());
                }
            }
        }
    }

    private static void readCSVTestCases(String csvPath, TestExecutor executor, ReportGenerator reportGenerator)
            throws Exception {
        List<CSVTestCaseReader.TestCaseData> testCases = CSVTestCaseReader.readTestCases(csvPath);

        System.out.println("✓ Found " + testCases.size() + " test case(s) in CSV file");

        for (CSVTestCaseReader.TestCaseData testCase : testCases) {
            String testCaseName = testCase.getTestCaseName();
            String stepBlock = testCase.getStepBlock();

            String filterName = config.getProperty("filter.name");
            boolean match = (filterName == null || filterName.isEmpty());

            if (!match) {
                String[] filters = filterName.split(",");
                for (String f : filters) {
                    if (testCaseName.toLowerCase().contains(f.trim().toLowerCase())) {
                        match = true;
                        break;
                    }
                }
            }

            if (!match) continue;

            String videoFileName = testCaseName.replaceAll("[^a-zA-Z0-9]", "_") + "_CSV.mp4";

            try {
                System.out.println("🎥 [CSV] Starting Video Recording: " + videoFileName);
                videoRecorder.startRecording(reportGenerator.getReportDir(), videoFileName);

                executeTestCase("CSV_Data", testCaseName, stepBlock, executor, reportGenerator);

            } catch (Exception e) {
                System.err.println("❌ CSV Execution Error in " + testCaseName + ": " + e.getMessage());
            } finally {
                try {
                    if (videoRecorder != null && videoRecorder.isRecording()) {
                        String savedVideoPath = videoRecorder.stopRecording(videoFileName);
                        if (savedVideoPath != null && !savedVideoPath.isEmpty()) {
                            reportGenerator.addVideoToTestCase(videoFileName);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to stop CSV video recorder: " + e.getMessage());
                }
            }
        }
    }

    private static void executeTestCase(String sheetName, String testCaseName, String stepBlock, TestExecutor executor, ReportGenerator reportGenerator) {
        List<TestStep> steps = StepParser.parseSteps(stepBlock);
        if (steps.isEmpty()) return;

        for (TestStep step : steps) {
            String action = step.getAction().toLowerCase().trim();
            String role = (step.getValue() != null) ? step.getValue().toLowerCase().trim() : "";

            if ((action.equals("switch_to") || action.equals("switchsession")) && !role.isEmpty()) {
                if (!executor.getDriverPool().containsKey(role)) {
                    System.out.println("🚀 Universal Switch: Initializing [" + role.toUpperCase() + "]");
                    if (role.equals("web")) {
                        executor.setupWebDriver();
                    } else {
                        executor.setupMobileDriver(role);
                    }
                }
            }
        }

        String firstAction = steps.get(0).getAction().toLowerCase().trim();
        String firstValue = (steps.get(0).getValue() != null) ? steps.get(0).getValue().toLowerCase().trim() : "";

        if ((firstAction.equals("switch_to") || firstAction.equals("switchsession")) && !firstValue.isEmpty()) {
            System.out.println("🎯 Setting Initial Test Focus context directly to: [" + firstValue.toUpperCase() + "]");
            executor.switchSession(firstValue);
        } else {
            System.out.println("🌐 No initial switch step found. Setting default session context focus to: [WEB]");
            executor.switchSession("web");
        }

        if (executor.getDriverPool().containsKey("web")) {
            String baseUrl = config.getProperty("base.url");
            String currentUrl = executor.getDriverPool().get("web").getCurrentUrl();
            if (baseUrl != null && !baseUrl.isEmpty() && (currentUrl == null || currentUrl.equals("about:blank") || currentUrl.startsWith("data:"))) {
                System.out.println("🌐 Navigating Web Driver to Base URL: " + baseUrl);
                executor.getDriverPool().get("web").get(baseUrl);
            }
        }

        System.out.println("🚀 Handing over synchronized session execution block to TestExecutor engine...");
        executor.run(sheetName, steps, testCaseName);
    }
}