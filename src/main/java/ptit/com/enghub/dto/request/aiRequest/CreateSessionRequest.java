package ptit.com.enghub.dto.request.aiRequest;

import lombok.Data;

@Data
public class CreateSessionRequest {
    private Long userId;
    private String variantId;
    private String scenarioId;
    private String contextId;
}

