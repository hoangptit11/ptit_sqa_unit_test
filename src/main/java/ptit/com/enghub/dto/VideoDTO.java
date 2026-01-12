package ptit.com.enghub.dto;

import lombok.Data;

@Data
public class VideoDTO {
    private Long id;
    private String url;
    private String description;
    private Integer duration;
}