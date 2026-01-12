package ptit.com.enghub.dto.request.aiRequest;


import lombok.Data;

@Data
public class Context {
    private String id;
    private String name;
    private String description;
    private String initialAiMessage;
}