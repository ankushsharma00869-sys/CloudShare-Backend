package in.ankush.cloudshareapi.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Replaces the old ProfileDocument (which was synced from Clerk via webhooks).
 * This is now the single source of truth for both authentication (email + password + roles)
 * and basic profile info. The Mongo-generated "id" field is the stable user identifier used
 * everywhere else in the app (files, credits, payments) - it plays the exact role that
 * "clerkId" used to play.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    /** BCrypt-hashed password. Never returned in any API response. */
    private String password;

    private String firstName;
    private String lastName;
    private String photoUrl;

    @Builder.Default
    private Set<String> roles = new HashSet<>();

    @CreatedDate
    private Instant createdAt;
}
