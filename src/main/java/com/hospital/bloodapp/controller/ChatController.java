package com.hospital.bloodapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.bloodapp.model.ChatLog;
import com.hospital.bloodapp.repository.ChatLogRepository;
import com.hospital.bloodapp.service.LlmService;
import com.hospital.bloodapp.service.PromptBuilder;
import com.hospital.bloodapp.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/chat")
public class ChatController {

    private final LlmService llmService;
    private final PromptBuilder promptBuilder;
    private final ChatLogRepository chatLogRepository;
    private final RagService ragService;
    private final ObjectMapper objectMapper = new ObjectMapper();
 
    public ChatController(LlmService llmService, PromptBuilder promptBuilder, ChatLogRepository chatLogRepository, RagService ragService) {
        this.llmService = llmService;
        this.promptBuilder = promptBuilder;
        this.chatLogRepository = chatLogRepository;
        this.ragService = ragService;
    }
 
    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.get("sessionId");
        String languagePref = request.getOrDefault("language", "en");
 
        String context = ragService.retrieveGroundingContext(message);
        String groundedSystemPrompt = promptBuilder.getSystemPrompt() 
                + "\n\nGrounding Context from Official Guidelines:\n" + context
                + "\n\nUser Preferred Language: " + ("ta".equalsIgnoreCase(languagePref) ? "Tamil (ta)" : "English (en)")
                + "\nYou MUST generate the response in the user preferred language and set the 'language' field in the JSON reply accordingly.";
 
        String jsonResponse = llmService.generateChatResponse(groundedSystemPrompt, message);

        String reply = "Sorry, I couldn't understand.";
        String language = languagePref;

        try {
            // Because LLM might wrap response in markdown blocks like ```json ... ``` we strip it
            String cleanJson = jsonResponse.replaceAll("```json|```", "").trim();
            JsonNode root = objectMapper.readTree(cleanJson);
            reply = root.path("reply").asText();
            language = root.path("language").asText();
        } catch (Exception e) {
            e.printStackTrace();
            reply = jsonResponse; // Fallback
        }

        ChatLog log = new ChatLog();
        log.setSessionId(sessionId);
        log.setMessage(message);
        log.setReply(reply);
        log.setLanguage(language);
        chatLogRepository.save(log);

        return ResponseEntity.ok(Map.of("reply", reply, "language", language));
    }
}
