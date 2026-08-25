package com.eit.automation.erp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles communication with Frappe ERP Master Test Cases API.
 */
public class ERPApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;

    public ERPApiClient(Properties config) {
        this.baseUrl = config.getProperty("erp.base.url", "https://erp.qa.ethicalintelligent.com");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public ERPApiClient() {
        this.baseUrl = "https://erp.qa.ethicalintelligent.com";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public String fetchEpicData(String epicName) throws IOException, InterruptedException {
        return fetchMasterTestCases("Epic", epicName);
    }

    public String fetchBacklogData(String backlogName) throws IOException, InterruptedException {
        return fetchMasterTestCases("Backlog", backlogName);
    }

    /**
     * Executes request sending a JSON body with type and names array matching Postman.
     */
    private String fetchMasterTestCases(String type, String targetName) throws IOException, InterruptedException {
        String endpointUrl = baseUrl + "/api/method/eit_scrum.api.testcases_api.master_testcases";

        // Construct JSON body matching Postman payload:
        // { "type": "Epic", "names": ["EPIC_0001_Platform_Configuration"] }
        String jsonBody = String.format("{\n  \"type\": \"%s\",\n  \"names\": [\"%s\"]\n}", type, targetName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .method("GET", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        System.out.println("📥 Requesting Master Testcases from ERP [" + type + ": " + targetName + "]...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Sort backlogs sequentially (US-2026-00851 before US-2026-00852) before returning
            return sortBacklogsAscending(response.body());
        } else {
            throw new RuntimeException("❌ ERP API Request Failed! Status Code: "
                    + response.statusCode() + " | Response: " + response.body());
        }
    }

    /**
     * Posts execution results back to Frappe ERP receiver endpoint.
     */
    public String sendTestResults(JSONObject resultPayload) throws IOException, InterruptedException {
        String endpointUrl = baseUrl + "/api/method/eit_scrum.api.receivetestcase.receive_data";

        System.out.println("📤 Sending Execution Results to Frappe ERP at: " + endpointUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(resultPayload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            System.out.println("✅ Execution results successfully posted to ERP API!");
            return response.body();
        } else {
            System.err.println("❌ Failed to post execution results to ERP! Status: "
                    + response.statusCode() + " | Response: " + response.body());
            throw new RuntimeException("ERP Result Sync Failed with status " + response.statusCode());
        }
    }

    /**
     * Sorts the backlogs inside the response JSON by backlog name/ID in ascending order.
     */
    private String sortBacklogsAscending(String rawJsonResponse) {
        try {
            JSONObject root = new JSONObject(rawJsonResponse);
            if (!root.has("message")) {
                return rawJsonResponse;
            }

            JSONArray messageArray = root.getJSONArray("message");

            for (int i = 0; i < messageArray.length(); i++) {
                JSONObject epicObj = messageArray.getJSONObject(i);

                if (epicObj.has("backlogs")) {
                    JSONArray backlogsArray = epicObj.getJSONArray("backlogs");

                    // Convert JSONArray to java.util.List for sorting
                    List<JSONObject> backlogList = new ArrayList<>();
                    for (int j = 0; j < backlogsArray.length(); j++) {
                        backlogList.add(backlogsArray.getJSONObject(j));
                    }

                    // Sort backlogs by Backlog Name (e.g. US-2026-00851 comes before US-2026-00852)
                    backlogList.sort((b1, b2) -> {
                        String name1 = b1.optJSONObject("backlog") != null
                                ? b1.getJSONObject("backlog").optString("name", "") : "";
                        String name2 = b2.optJSONObject("backlog") != null
                                ? b2.getJSONObject("backlog").optString("name", "") : "";

                        return name1.compareTo(name2);
                    });

                    // Rebuild the sorted JSONArray and update the parent object
                    JSONArray sortedBacklogsArray = new JSONArray();
                    for (JSONObject b : backlogList) {
                        sortedBacklogsArray.put(b);
                    }
                    epicObj.put("backlogs", sortedBacklogsArray);
                }
            }

            return root.toString();
        } catch (Exception e) {
            // If JSON sorting fails for any reason, return raw response as fallback
            return rawJsonResponse;
        }
    }
}