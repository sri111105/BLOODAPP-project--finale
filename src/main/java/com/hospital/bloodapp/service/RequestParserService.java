package com.hospital.bloodapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.bloodapp.model.BloodRequest;
import org.springframework.stereotype.Service;

@Service
public class RequestParserService {

    private final LlmService llmService;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequestParserService(LlmService llmService, PromptBuilder promptBuilder) {
        this.llmService = llmService;
        this.promptBuilder = promptBuilder;
    }

    public BloodRequest parseRequest(String rawText) {
        String jsonResponse = llmService.extractHospitalRequest(promptBuilder.getRequestParsingPrompt(), rawText);
        if ("RATE_LIMIT_ERROR".equals(jsonResponse) || "ERROR".equals(jsonResponse)) {
            return parseFallback(rawText);
        }
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            BloodRequest request = new BloodRequest();
            request.setRawText(rawText);
            request.setBloodGroup(root.path("bloodGroup").asText(null));
            request.setUnits(root.path("units").asInt(1));
            request.setUrgency(root.path("urgency").asText("medium"));
            request.setLocation(root.path("location").asText(null));
            request.setPatientName(root.path("patientName").asText("Unknown"));
            return request;
        } catch (Exception e) {
            System.err.println("[Parser] JSON parsing failed. Using offline fallback parser.");
            return parseFallback(rawText);
        }
    }

    private BloodRequest parseFallback(String rawText) {
        BloodRequest request = new BloodRequest();
        request.setRawText(rawText);
        request.setPatientName("Unknown");
        request.setUnits(1);
        request.setUrgency("high");
        
        // Match blood group (A/B/O/AB followed by +/- or text)
        java.util.regex.Pattern bgPattern = java.util.regex.Pattern.compile("\\b(A|B|O|AB)[+-]\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher bgMatcher = bgPattern.matcher(rawText);
        if (bgMatcher.find()) {
            request.setBloodGroup(bgMatcher.group(0).toUpperCase().replace(" ", ""));
        } else {
            String lower = rawText.toLowerCase();
            if (lower.contains("o positive") || lower.contains("o+")) request.setBloodGroup("O+");
            else if (lower.contains("o negative") || lower.contains("o-")) request.setBloodGroup("O-");
            else if (lower.contains("a positive") || lower.contains("a+")) request.setBloodGroup("A+");
            else if (lower.contains("a negative") || lower.contains("a-")) request.setBloodGroup("A-");
            else if (lower.contains("b positive") || lower.contains("b+")) request.setBloodGroup("B+");
            else if (lower.contains("b negative") || lower.contains("b-")) request.setBloodGroup("B-");
            else if (lower.contains("ab positive") || lower.contains("ab+")) request.setBloodGroup("AB+");
            else if (lower.contains("ab negative") || lower.contains("ab-")) request.setBloodGroup("AB-");
        }
        
        // Match units
        java.util.regex.Pattern unitsPattern = java.util.regex.Pattern.compile("\\b(\\d+)\\s*(unit|pack|bag|bottle)s?\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher unitsMatcher = unitsPattern.matcher(rawText);
        if (unitsMatcher.find()) {
            request.setUnits(Integer.parseInt(unitsMatcher.group(1)));
        } else {
            java.util.regex.Pattern digitPattern = java.util.regex.Pattern.compile("\\b([1-9])\\b");
            java.util.regex.Matcher digitMatcher = digitPattern.matcher(rawText);
            if (digitMatcher.find()) {
                request.setUnits(Integer.parseInt(digitMatcher.group(1)));
            }
        }
        
        // Match location/hospital
        String lower = rawText.toLowerCase();
        if (lower.contains("at ")) {
            int idx = lower.indexOf("at ");
            String sub = rawText.substring(idx + 3).trim();
            String[] words = sub.split(" ");
            if (words.length > 0) {
                request.setLocation(words[0].replaceAll("[^a-zA-Z]", ""));
            }
        }
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            request.setLocation("City Hospital");
        }
        
        // Match patient name
        java.util.regex.Pattern patientPattern = java.util.regex.Pattern.compile("(for patient|patient)\\s+([a-zA-Z]+(\\s+[a-zA-Z]+)?)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher patientMatcher = patientPattern.matcher(rawText);
        if (patientMatcher.find()) {
            request.setPatientName(patientMatcher.group(2).trim());
        }
        
        if (request.getBloodGroup() == null) {
            request.setBloodGroup("O+");
        }
        
        return request;
    }
}
