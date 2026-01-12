package ptit.com.enghub.dto.response.aiResponse;

import lombok.Data;
import ptit.com.enghub.dto.request.aiRequest.MessageAnalysis;

import java.util.List;

@Data
public class MessageResponse {
    private String response;
    private MessageAnalysis analysis;
    private List<String> alternatives;
    private String translation;
}