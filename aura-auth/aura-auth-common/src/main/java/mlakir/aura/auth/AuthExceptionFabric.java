package mlakir.aura.auth;

import mlakir.aura.exception.*;
import org.springframework.http.*;

public class AuthExceptionFabric {

    public static AuraException createInvalidToken() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Токен просрочен или неверного формата");

        problemDetail.setTitle("Invalid token");

        return new AuraException(HttpStatus.UNAUTHORIZED, problemDetail);
    }

    public static AuraException createInvalidCredentials() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Неверный логин или пароль");

        problemDetail.setTitle("Invalid credentials");

        return new AuraException(HttpStatus.UNAUTHORIZED, problemDetail);
    }

    public static AuraException createTokenNotFound() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Пользователь не аутентифицирован");

        problemDetail.setTitle("Missing token");

        return new AuraException(HttpStatus.UNAUTHORIZED, problemDetail);
    }

    public static AuraException createRegistrationDuplicate() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Пользователь с таким именем уже зарегистрирован");

        problemDetail.setTitle("Registration duplicate");

        return new AuraException(HttpStatus.CONFLICT, problemDetail);
    }

    public static AuraException createDefaultRoleNotFound() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Роль пользователя по умолчанию не найдена");

        problemDetail.setTitle("Default role not found");

        return new AuraException(HttpStatus.INTERNAL_SERVER_ERROR, problemDetail);
    }

    public static AuraException createAccessDenied() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Недостаточно прав доступа");

        problemDetail.setTitle("Access denied");

        return new AuraException(HttpStatus.FORBIDDEN, problemDetail);
    }

}
