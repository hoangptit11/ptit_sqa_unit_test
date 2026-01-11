package ptit.com.enghub.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AddFlashcardRequest {
    private List<String> words;
}
