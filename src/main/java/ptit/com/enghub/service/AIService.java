package ptit.com.enghub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ptit.com.enghub.dto.request.AiWordRequest;
import ptit.com.enghub.dto.request.ExternalDictionaryRequest;
import ptit.com.enghub.dto.request.aiRequest.*;
import ptit.com.enghub.dto.response.AiWordResponse;
import ptit.com.enghub.dto.response.BulkFlashcardResponse;
import ptit.com.enghub.dto.response.aiResponse.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AIService {
    private final RestTemplate restTemplate;

    @Value("${ai.service.url}")
    private String aiServiceUrl;


    public AiWordResponse suggestWord(String word) {

        AiWordRequest request = new AiWordRequest(word);
        return restTemplate.postForObject(
                aiServiceUrl + "/api/suggest-words",
                request,
                AiWordResponse.class
        );
    }

    public List<BulkFlashcardResponse> fetchFlashcards(List<String> words) {

        ExternalDictionaryRequest request = new ExternalDictionaryRequest();
        request.setWord(words);

        ResponseEntity<List<BulkFlashcardResponse>> response =
                restTemplate.exchange(
                        aiServiceUrl + "/api/generate-flashcards",
                        HttpMethod.POST,
                        new HttpEntity<>(request),
                        new ParameterizedTypeReference<>() {}
                );
        return response.getBody();
    }

    public ChatbotResponse sendMessage(String message, String mode) {

        ChatbotRequest request = new ChatbotRequest();
        request.setMessage(message);
        request.setMode(mode);

        return restTemplate.postForObject(
                aiServiceUrl + "/chatbot/chat",
                request,
                ChatbotResponse.class
        );
    }

    public Variant[] getVariants() {
        return restTemplate.getForObject(
                aiServiceUrl + "/variants",
                Variant[].class
        );
    }

    public Scenario[] getVariantScenarios() {
        return restTemplate.getForObject(
                aiServiceUrl + "/variants/scenarios",
                Scenario[].class
        );
    }

    public CreateSessionResponse createSession(CreateSessionRequest request) {

        return restTemplate.postForObject(
                aiServiceUrl + "/conversation/session/create",
                request,
                CreateSessionResponse.class
        );
    }

    public MessageResponse sendConversationMessage(MessageRequest request) {

        return restTemplate.postForObject(
                aiServiceUrl + "/conversation/message",
                request,
                MessageResponse.class
        );
    }

    public ScoreWritingResponse scoreWriting(ScoreWritingRequest request) {

        return restTemplate.postForObject(
                aiServiceUrl + "/api/score-writing",
                request,
                ScoreWritingResponse.class
        );
    }

    public TranslateWordResponse translateWord(String word) {

        TranslateWordRequest request = new TranslateWordRequest();
        request.setWord(word);

        return restTemplate.postForObject(
                aiServiceUrl + "/api/translate-word",
                request,
                TranslateWordResponse.class
        );
    }

}
