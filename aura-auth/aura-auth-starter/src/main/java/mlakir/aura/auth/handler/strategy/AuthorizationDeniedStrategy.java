package mlakir.aura.auth.handler.strategy;

import mlakir.aura.exception.*;
import org.springframework.security.core.*;

public interface AuthorizationDeniedStrategy {

    boolean supports(Authentication auth);

    AuraException buildException();

}
