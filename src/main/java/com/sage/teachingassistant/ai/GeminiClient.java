package com.sage.teachingassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Client for Google Gemini API with support for Google Search grounding
 * and automatic model fallback.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String FALLBACK_MODEL = "gemini-3.5-flash-lite";

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(@Value("${gemini.api-key:}") String apiKey,
                        @Value("${gemini.model:gemini-3.5-flash}") String model,
                        @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}") String baseUrl,
                        ObjectMapper objectMapper) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null ? model.trim() : "gemini-3.5-flash";
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "https://generativelanguage.googleapis.com/v1beta/models";
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    /**
     * Calls Gemini with optional search grounding and automatic fallback.
     */
    public String generate(String systemInstruction, String prompt, boolean enableSearch) {
        if (!isConfigured()) {
            log.warn("Gemini API key is not configured. Falling back to mock generator.");
            return generateMock(prompt);
        }

        // Attempt 1: With requested settings on primary model
        try {
            String result = callApi(this.model, systemInstruction, prompt, enableSearch);
            if (result != null && !result.isBlank()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Primary model {} attempt with search={} failed: {}. Retrying without search...",
                    this.model, enableSearch, e.getMessage());
        }

        // Attempt 2: Primary model without search grounding (if search had failed or quota exceeded)
        if (enableSearch) {
            try {
                String result = callApi(this.model, systemInstruction, prompt, false);
                if (result != null && !result.isBlank()) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("Primary model {} attempt without search failed: {}", this.model, e.getMessage());
            }
        }

        // Attempt 3: Fast fallback model (gemini-3.5-flash-lite)
        if (!this.model.equals(FALLBACK_MODEL)) {
            try {
                log.info("Falling back to secondary model {}", FALLBACK_MODEL);
                String result = callApi(FALLBACK_MODEL, systemInstruction, prompt, false);
                if (result != null && !result.isBlank()) {
                    return result;
                }
            } catch (Exception e) {
                log.error("Fallback model {} also failed: {}", FALLBACK_MODEL, e.getMessage());
            }
        }

        return generateMock(prompt);
    }

    private String callApi(String modelName, String systemInstruction, String prompt, boolean enableSearch) {
        String url = baseUrl + "/" + modelName + ":generateContent?key=" + apiKey;

        Map<String, Object> requestPayload;
        if (enableSearch) {
            requestPayload = Map.of(
                    "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "tools", List.of(Map.of("google_search", Map.of()))
            );
        } else {
            requestPayload = Map.of(
                    "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );
        }

        String responseBody = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(String.class);

        if (responseBody == null) {
            throw new IllegalStateException("Empty response from Gemini API");
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode part : parts) {
                        if (part.has("text")) {
                            sb.append(part.get("text").asText());
                        }
                    }
                    return sb.toString().trim();
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse Gemini JSON response", e);
        }

        throw new IllegalStateException("No content generated in response: " + responseBody);
    }

    private String generateMock(String prompt) {
        return """
                **Topic Overview & Learning Objectives:**
                - Core principles and definitions for the requested grade level.
                - Conceptual framework aligned with curriculum standards (CBSE/ICSE/NCERT).
                - Real-world applications and key problem-solving formulas.
                
                **Key Sections to Cover:**
                1. Fundamental Concepts & Key Terms
                2. Step-by-Step Explanations & Examples
                3. High-Yield Practice Questions / Exercises
                4. Self-Assessment & Summary Notes
                
                [Generated via Sage Teaching Assistant]""";
    }
}
