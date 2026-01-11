package ptit.com.enghub.dto.request.aiRequest;

import lombok.Data;

@Data
public class ScoreWritingRequest {
    private String title;
    private String description;
    private String content;
}
