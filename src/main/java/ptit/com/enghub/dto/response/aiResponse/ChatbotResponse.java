package ptit.com.enghub.dto.response.aiResponse;

import lombok.Data;

@Data
public class ChatbotResponse {
    private String response;
    private String mode;
    private String timestamp;
}