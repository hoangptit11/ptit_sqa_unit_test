package ptit.com.enghub.dto.request.aiRequest;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Data
@Setter
@RequiredArgsConstructor
public class ChatbotRequest {
    private String message;
    private String mode;
}
