package mlakir.aura.core.security;

import mlakir.aura.auth.Principal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "manual-user";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Principal currentUser && currentUser.getId() != null) {
            return String.valueOf(currentUser.getId());
        }

        String name = authentication.getName();
        return (name == null || name.isBlank()) ? "manual-user" : name;
    }
}
