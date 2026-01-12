package ptit.com.enghub.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ptit.com.enghub.dto.request.AiWordRequest;
import ptit.com.enghub.dto.response.AiWordResponse;
import ptit.com.enghub.service.AIService;
import ptit.com.enghub.service.RedisService;

import java.security.Principal;
import java.util.Set;

@Controller
@AllArgsConstructor
@Slf4j
public class WritingSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisService redisService;
    private final AIService service;

    @MessageMapping("/writing/suggest")
    public void handleSuggestWord(AiWordRequest request,
                                  Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthenticated WebSocket user");
        }

        String normalizedWord = request.getWord().trim().toLowerCase();

        if ( isBlacklisted(normalizedWord) ){
            return;
        }

        String redisKey = "suggest:" + normalizedWord;

        Object cached = redisService.get(redisKey);
        if (cached instanceof AiWordResponse cachedResponse) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/topic/suggest-words",
                    cachedResponse
            );
            return;
        }

        AiWordResponse response = service.suggestWord(request.getWord());
        redisService.set( redisKey, response );
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/suggest-words",
                response
        );
    }


    public static final Set<String> COMMON_WORDS = Set.of(
            "a","an","the",
            "i","me","my","mine","you","your","yours",
            "he","him","his","she","her","hers","it","it's",
            "we","us","our","ours","they","them","their","theirs",
            "be","am","is","are","was","were","been","being",
            "do","does","did","have","has","had",
            "will","would","can","could","may","might","must","should",
            "in","on","at","to","from","for","with","by","about",
            "and","or","but","so","because","if","while",
            "very","too","just","only","also"
    );

    public static boolean isBlacklisted(String word) {
        return COMMON_WORDS.contains(word.toLowerCase().trim());
    }

}
