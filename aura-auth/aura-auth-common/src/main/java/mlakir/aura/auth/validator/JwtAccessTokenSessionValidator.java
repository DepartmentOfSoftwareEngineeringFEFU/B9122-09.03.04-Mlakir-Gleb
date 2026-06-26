package mlakir.aura.auth.validator;

import java.util.*;

public interface JwtAccessTokenSessionValidator {

    boolean isActive(UUID accessJti);

}
