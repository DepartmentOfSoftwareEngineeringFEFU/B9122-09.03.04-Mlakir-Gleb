package mlakir.aura.exception;

import java.net.*;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.boot.context.properties.*;
import org.springframework.validation.annotation.*;

@Getter
@Setter
@Validated
@ConfigurationProperties("mlakir.aura.exception")
public class AuraExceptionProperties {

    private String defaultTitle = "Internal Server Error";

    private URI defaultType = URI.create("about:blank");

    private String defaultDetail = "Произошла неожиданная ошибка.";

    @Min(400)
    @Max(599)
    private int defaultStatus = 500;

    private boolean includeExceptionMessage = false;

    private String validationErrorDetail = "Ошибка в одном или нескольких полях.";

    private URI validationErrorType = URI.create("about:blank");

    private String validationErrorTitle = "Validation error";

    private String validationErrorDefaultFieldMessage = "Введите корректные данные";

    private String constraintViolationDetail = "Ошибка в одном или нескольких параметрах.";

    private String malformedRequestTitle = "Malformed request";

    private String malformedRequestDetail = "Тело запроса имеет неверный формат.";

    private String typeMismatchTitle = "Invalid parameter";

    private String typeMismatchDetail = "Параметр запроса имеет неверный формат или тип.";

    private String illegalArgumentTitle = "Bad Request";

    private String illegalArgumentDetail = "Запрос некорректен, поскольку выбранные параметры "
        + "указаны неверно или произошла функциональная ошибка.";

    private String missingTokenTitle = "Missing token";

    private String missingTokenDetail = "Пользователь не аутентифицирован.";

    private String missingRequestHeaderTitle = "Missing request header";

}
