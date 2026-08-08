package in.ankush.cloudshareapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private Set<String> roles;
    private Instant createdAt;
}
