package mlakir.aura.auth.handler.strategy;

import mlakir.aura.auth.*;
import mlakir.aura.exception.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;

public class UnauthenticatedStrategy implements AuthorizationDeniedStrategy {

    @Override
    public boolean supports(Authentication auth) {
        return auth == null
               || auth instanceof AnonymousAuthenticationToken
               || !auth.isAuthenticated();
    }

    @Override
    public AuraException buildException() {
        return AuthExceptionFabric.createTokenNotFound();
    }

}
