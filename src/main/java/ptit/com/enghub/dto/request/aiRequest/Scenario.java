package ptit.com.enghub.dto.request.aiRequest;


import lombok.Data;

import java.util.List;

@Data
public class Scenario {
    private String id;
    private String name;
    private String description;
    private List<Context> contexts;
}
