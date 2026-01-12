package ptit.com.enghub.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ExternalDictionaryRequest {
    private List<String> word;
}
