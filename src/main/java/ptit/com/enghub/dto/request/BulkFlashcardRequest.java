package ptit.com.enghub.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BulkFlashcardRequest {
    private Long deckId;
    private List<String> word;
}