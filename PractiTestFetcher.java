package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PractiTestFetcher {

    private static final String API_TOKEN = "Yout_Token";
    private static final String DEVELOPER_EMAIL = "Your_mail";
    private static final int API_CALL_LIMIT = 100; // Maximum number of API calls before waiting
    private static final int WAIT_TIME_IN_MINUTES = 1; // Wait time in minutes


    public static void main(String[] args) {
        String projectId = "Ptoject - ID"; // Replace with your project ID
        String customFieldId = "---f-000000"; // Replace with your custom field ID
        String lookupValue = "text"; // Replace with the value you are looking for

        try {
            fetchTests(projectId, customFieldId, lookupValue);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    public static void fetchTests(String projectId, String customFieldId, String lookupValue) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        // Ensure the customFieldId retains the leading "---f-"
        if (!customFieldId.startsWith("---f-")) {
            customFieldId = "---f-" + customFieldId;
        }

        int currentPage = 1;
        int totalPages = 1;
        int apiCallCount = 0;

        while (currentPage <= totalPages) {
            String url = "https://api.practitest.com/api/v2/projects/" + projectId + "/tests.json"
                    + "?api_token=" + API_TOKEN
                    + "&developer_email=" + DEVELOPER_EMAIL
                    + "&page[number]=" + currentPage;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + java.util.Base64.getEncoder()
                            .encodeToString((DEVELOPER_EMAIL + ":" + API_TOKEN).getBytes()))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            apiCallCount++;

            if (apiCallCount == API_CALL_LIMIT) {
                System.out.println("API call limit reached. Waiting for " + WAIT_TIME_IN_MINUTES + " minute(s)...");
                TimeUnit.MINUTES.sleep(WAIT_TIME_IN_MINUTES);
                apiCallCount = 0; // Reset the counter after waiting
            }

            if (response.statusCode() != 200) {
                System.err.println("Error fetching tests: " + response.body());
                break;
            }

            JsonNode rootNode = mapper.readTree(response.body());
            totalPages = rootNode.path("meta").path("total-pages").asInt();

            for (JsonNode testNode : rootNode.path("data")) {
                JsonNode customFieldsNode = testNode.path("attributes").path("custom-fields");

                // Compare using the full customFieldId with the leading "---f-"
                if (customFieldsNode.has(customFieldId)
                        && customFieldsNode.get(customFieldId).asText().equals(lookupValue)) {
                    System.out.println("Match found! Full JSON response for the matched test:");
                    System.out.println(testNode.toPrettyString());

                    return; // Exit the function after finding the first match
                }
            }

            currentPage++;
        }

        System.out.println("No matching test found.");
    }
}