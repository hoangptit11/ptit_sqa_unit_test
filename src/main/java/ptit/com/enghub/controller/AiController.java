package ptit.com.enghub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ptit.com.enghub.dto.request.AddFlashcardRequest;
import ptit.com.enghub.dto.request.AiWordRequest;
import ptit.com.enghub.dto.request.BulkFlashcardRequest;
import ptit.com.enghub.dto.request.ExternalDictionaryRequest;
import ptit.com.enghub.dto.request.aiRequest.*;
import ptit.com.enghub.dto.response.AiWordResponse;
import ptit.com.enghub.dto.response.BulkFlashcardResponse;
import ptit.com.enghub.dto.response.FlashcardResponse;
import ptit.com.enghub.dto.response.aiResponse.*;
import ptit.com.enghub.service.AIService;
import ptit.com.enghub.service.FlashcardService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/")
public class AiController {
    private final AIService aiService;
    private final FlashcardService flashcardService;

    @PostMapping("/suggest-words")
    public AiWordResponse suggestWord(@RequestBody AiWordRequest request) {
        return aiService.suggestWord(request.getWord());
    }

    @PostMapping("/generate-flashcards")
    public List<FlashcardResponse> generateFlashcards(
            @RequestBody BulkFlashcardRequest request) {
        return flashcardService.createFlashcardsFromListWords(request);
    }

    @PostMapping("/add-flashcards")
    public List<FlashcardResponse> addFlashcards(
            @RequestBody AddFlashcardRequest request) {
        return flashcardService.addFlashcardsFromListWords(request);
    }

    @PostMapping("/chatbot/chat")
    public ChatbotResponse chat(@RequestBody ChatbotRequest request) {
        return aiService.sendMessage(
                request.getMessage(),
                request.getMode()
        );
    }

    @GetMapping("/variants")
    public Variant[] getVariants() {
        return aiService.getVariants();
    }

    @GetMapping("/variants/scenarios")
    public Scenario[] getVariantScenarios() {
        return aiService.getVariantScenarios();
    }

    @PostMapping("/conversation/session/create")
    public CreateSessionResponse createSession(
            @RequestBody CreateSessionRequest request) {

        return aiService.createSession(request);
    }

    @PostMapping("/conversation/message")
    public MessageResponse sendConversationMessage(
            @RequestBody MessageRequest request) {

        return aiService.sendConversationMessage(request);
    }

    @PostMapping("/score-writing")
    public ScoreWritingResponse scoreWriting(
            @RequestBody ScoreWritingRequest request) {

        return aiService.scoreWriting(request);
    }

    @PostMapping("/translate-word")
    public TranslateWordResponse translateWord(
            @RequestBody TranslateWordRequest request) {

        return aiService.translateWord(request.getWord());
    }
}
