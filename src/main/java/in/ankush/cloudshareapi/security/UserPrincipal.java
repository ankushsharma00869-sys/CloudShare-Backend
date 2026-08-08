package in.ankush.cloudshareapi.security;

import in.ankush.cloudshareapi.document.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final String id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        Set<String> roles = user.getRoles();
        this.authorities = (roles == null ? Set.<String>of("ROLE_USER") : roles).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    /** Constructor used by the JWT filter, where we already trust the token's claims. */
    public UserPrincipal(String id, String email, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = null;
        this.authorities = authorities;
    }

    /**
     * IMPORTANT: the "username" here is the Mongo user id, not the email.
     * Every service in this app (FileMetaDataService, UserCreditsService, PaymentService...)
     * reads the current user's identifier via SecurityContextHolder...getAuthentication().getName().
     * Returning the id keeps that contract identical to how it worked with Clerk's clerkId.
     */
    @Override
    public String getUsername() {
        return id;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
