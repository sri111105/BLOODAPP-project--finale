package com.hospital.bloodapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
public class LlmService {

    @Value("${llm.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public LlmService() {
        this.webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public String generateChatResponse(String systemPrompt, String userMessage) {
        String fullPrompt = systemPrompt + "\n\nUser: " + userMessage;
        String response = callGemini(fullPrompt);
        if ("RATE_LIMIT_ERROR".equals(response) || "ERROR".equals(response)) {
            boolean isTamil = systemPrompt.toLowerCase().contains("tamil");
            if (isTamil) {
                return "{\"reply\": \"மன்னிக்கவும், கணினி தற்போது மிகவும் பிஸியாக உள்ளது. தயவுசெய்து சிறிது நேரம் கழித்து மீண்டும் முயற்சிக்கவும்.\", \"language\": \"ta\"}";
            } else {
                return "{\"reply\": \"The chatbot is currently busy due to rate limits. Please ask again in a moment.\", \"language\": \"en\"}";
            }
        }
        return response;
    }

    public String extractHospitalRequest(String systemPrompt, String rawText) {
        String fullPrompt = systemPrompt + "\n\nRaw Request: " + rawText;
        return callGemini(fullPrompt);
    }

    private String callGemini(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                    Map.of("parts", new Object[]{
                        Map.of("text", prompt)
                    })
                },
                "generationConfig", Map.of(
                    "responseMimeType", "application/json"
                )
            );

            String response = webClient.post()
                .uri("/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests e) {
            System.err.println("[LLM] Gemini API 429 Too Many Requests (Rate limit reached). Using local fallback.");
            return "RATE_LIMIT_ERROR";
        } catch (Exception e) {
            System.err.println("[LLM] Error calling Gemini: " + e.getMessage());
            return "ERROR";
        }
    }

    public String generateAnalyticsSummary(String inventoryData) {
        String prompt = "You are a smart blood bank analytics assistant.\n" +
                "Given the current blood inventory units, generate a short, actionable summary (1-2 sentences) " +
                "for the admin dashboard. Recommend blood drives, highlight critical stock shortages, etc.\n" +
                "Always return your response as JSON: {\"summary\": \"...\"}\n\n" +
                "Current Blood Inventory Data:\n" + inventoryData;
        try {
            String response = callGemini(prompt);
            if ("RATE_LIMIT_ERROR".equals(response) || "ERROR".equals(response)) {
                return generateRuleBasedSummary(inventoryData);
            }
            return response;
        } catch (Exception e) {
            return generateRuleBasedSummary(inventoryData);
        }
    }

    private String generateRuleBasedSummary(String inventoryData) {
        java.util.List<String> lowGroups = new java.util.ArrayList<>();
        try {
            String[] lines = inventoryData.split("\n");
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] parts = line.split(":");
                    String bg = parts[0].trim();
                    String unitsStr = parts[1].replaceAll("[^0-9]", "").trim();
                    if (!unitsStr.isEmpty()) {
                        int units = Integer.parseInt(unitsStr);
                        if (units < 5) {
                            lowGroups.add(bg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }

        String summaryText;
        if (lowGroups.isEmpty()) {
            summaryText = "Inventory stock levels are healthy across all blood groups. Continue regular operations.";
        } else {
            String joined = String.join(", ", lowGroups);
            summaryText = "Smart Alert: Stock is low (<5 units) for " + joined + ". Consider organizing a targeted donation camp or contacting eligible donors.";
        }
        return "{\"summary\": \"" + summaryText + "\"}";
    }
}
