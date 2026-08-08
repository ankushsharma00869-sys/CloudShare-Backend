package in.ankush.cloudshareapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 🤖 AI Feature: thin wrapper around the Gemini API REST endpoints.
 * - embed(text)    -> used for Smart File Search (semantic search)
 * - summarize(text)-> used for Auto File Summarization
 *
 * Uses plain REST calls (no heavy SDK) via Spring WebClient, so it stays
 * lightweight and easy to swap providers later if needed.
 */
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.embedding.model}")
    private String embeddingModel;

    @Value("${gemini.generation.model}")
    private String generationModel;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a dense vector embedding for the given text.
     * taskType differs for documents (being indexed) vs queries (being searched).
     */
    public List<Double> embed(String text, String taskType) {
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("GEMINI_API_KEY is not set — skipping embedding generation.");
            return null;
        }
        if (text == null || text.isBlank()) return null;

        try {
            Map<String, Object> body = Map.of(
                    "content", Map.of("parts", List.of(Map.of("text", text))),
                    "taskType", taskType // "RETRIEVAL_DOCUMENT" or "RETRIEVAL_QUERY"
            );

            String response = webClient.post()
                    .uri("/v1beta/models/{model}:embedContent?key={key}", embeddingModel, apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode values = root.path("embedding").path("values");

            List<Double> vector = new ArrayList<>();
            for (JsonNode v : values) {
                vector.add(v.asDouble());
            }
            return vector.isEmpty() ? null : vector;

        } catch (Exception e) {
            System.err.println("Gemini embedding call failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates a short 2-3 sentence summary of the given text using Gemini Flash.
     */
    public String summarize(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("GEMINI_API_KEY is not set — skipping summarization.");
            return null;
        }
        if (text == null || text.isBlank()) return null;

        try {
            String prompt = "Summarize the following document in 2-3 short sentences. "
                    + "Be concise and factual, do not add opinions:\n\n" + text;

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );

            String response = webClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", generationModel, apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText(null);

        } catch (Exception e) {
            System.err.println("Gemini summarization call failed: " + e.getMessage());
            return null;
        }
    }
}
