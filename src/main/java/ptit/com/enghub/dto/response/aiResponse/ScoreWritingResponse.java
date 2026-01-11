package ptit.com.enghub.dto.response.aiResponse;

import lombok.Data;

import java.util.List;

@Data
public class ScoreWritingResponse {
    private int grammarScore;
    private String grammarFeedback;
    private int vocabularyScore;
    private String vocabularyFeedback;
    private int coherenceScore;
    private String coherenceFeedback;
    private int contentScore;
    private String contentFeedback;
    private int overallScore;
    private List<String> improvements;
}
