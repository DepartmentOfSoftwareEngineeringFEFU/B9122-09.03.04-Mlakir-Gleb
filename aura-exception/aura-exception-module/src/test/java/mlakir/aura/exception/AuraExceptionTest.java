package mlakir.aura.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

class AuraExceptionTest {

    @Test
    void shouldKeepStatusAndProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "detail");
        AuraException exception = new AuraException(HttpStatus.BAD_REQUEST, problemDetail);

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertSame(problemDetail, exception.getBody());
        assertEquals("detail", exception.getBody().getDetail());
    }

}
