package ptit.com.enghub.dto.response;

import lombok.Data;

@Data
public class BulkFlashcardResponse {
    private String term;
    private String phonetic;
    private String definition;
    private String partOfSpeech;
    private String exampleSentence;
}