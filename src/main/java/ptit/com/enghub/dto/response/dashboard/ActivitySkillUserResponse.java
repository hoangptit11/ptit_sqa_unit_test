package ptit.com.enghub.dto.response.dashboard;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivitySkillUserResponse {
    private String name;
    private int times;
}
