package com.hospital.bloodapp.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private List<String> guidelines = new ArrayList<>();

    public RagService() {
        loadGuidelines();
    }

    private void loadGuidelines() {
        try {
            ClassPathResource resource = new ClassPathResource("guidelines.txt");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        guidelines.add(line.trim());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading guidelines: " + e.getMessage());
            // Fallback rules in case the file reading fails
            guidelines.add("AGE REQUIREMENT: Must be 18 to 65 years old.");
            guidelines.add("WEIGHT REQUIREMENT: Must weigh at least 50 kg.");
            guidelines.add("DONATION FREQUENCY: Minimum 90 days interval between donations.");
            guidelines.add("TATTOO AND PIERCING: 6 months (180 days) wait after tattoo/piercing.");
            guidelines.add("MEDICATION: Wait 7 days after completing antibiotics.");
            guidelines.add("PREGNANCY: Pregnant or breastfeeding women are ineligible.");
        }
    }

    public String retrieveGroundingContext(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "";
        }

        String lowerQuery = query.toLowerCase();
        List<String> matchedRules = new ArrayList<>();

        // Simple keywords extraction
        boolean matchAll = false;
        if (lowerQuery.contains("age") || lowerQuery.contains("old") || lowerQuery.contains("வயது") || lowerQuery.contains("வருடம்")) {
            matchedRules.add(findRule("AGE"));
        }
        if (lowerQuery.contains("weight") || lowerQuery.contains("kg") || lowerQuery.contains("எடை") || lowerQuery.contains("கிலோ")) {
            matchedRules.add(findRule("WEIGHT"));
        }
        if (lowerQuery.contains("frequency") || lowerQuery.contains("often") || lowerQuery.contains("how many times") || lowerQuery.contains("days") || lowerQuery.contains("gap") || lowerQuery.contains("நாட்கள்") || lowerQuery.contains("இடைவெளி")) {
            matchedRules.add(findRule("FREQUENCY"));
        }
        if (lowerQuery.contains("tattoo") || lowerQuery.contains("piercing") || lowerQuery.contains("ink") || lowerQuery.contains("பச்சை குத்துதல்") || lowerQuery.contains("காது குத்துதல்")) {
            matchedRules.add(findRule("TATTOO"));
        }
        if (lowerQuery.contains("travel") || lowerQuery.contains("malaria") || lowerQuery.contains("பயணம்")) {
            matchedRules.add(findRule("TRAVEL"));
        }
        if (lowerQuery.contains("medicine") || lowerQuery.contains("antibiotic") || lowerQuery.contains("pill") || lowerQuery.contains("fever") || lowerQuery.contains("flu") || lowerQuery.contains("sick") || lowerQuery.contains("மருந்து" ) || lowerQuery.contains("காய்ச்சல்")) {
            matchedRules.add(findRule("MEDICAL"));
            matchedRules.add(findRule("MEDICATION"));
        }
        if (lowerQuery.contains("pregnant") || lowerQuery.contains("breastfeed") || lowerQuery.contains("mother") || lowerQuery.contains("baby") || lowerQuery.contains("கர்ப்பிணி")) {
            matchedRules.add(findRule("PREGNANCY"));
        }
        if (lowerQuery.contains("care") || lowerQuery.contains("after") || lowerQuery.contains("before") || lowerQuery.contains("eat") || lowerQuery.contains("sleep") || lowerQuery.contains("உணவு")) {
            matchedRules.add(findRule("PRE-DONATION"));
            matchedRules.add(findRule("POST-DONATION"));
        }

        // If no keyword matched, return top 3 general guidelines as default context
        if (matchedRules.isEmpty()) {
            return guidelines.stream().limit(4).collect(Collectors.joining("\n"));
        }

        return matchedRules.stream()
                .filter(r -> r != null && !r.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    private String findRule(String keyword) {
        for (String rule : guidelines) {
            if (rule.toUpperCase().contains(keyword.toUpperCase())) {
                return rule;
            }
        }
        return "";
    }
}
