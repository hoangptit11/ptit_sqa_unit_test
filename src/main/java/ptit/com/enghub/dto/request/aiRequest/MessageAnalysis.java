package ptit.com.enghub.dto.request.aiRequest;

import lombok.Data;

@Data
public class MessageAnalysis {
    private boolean hasError;
    private String topic;
    private String corrected;
    private String explanation;
}

