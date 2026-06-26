package mlakir.aura.core.services.vuzopedia;

public class VuzopediaScrapingException extends RuntimeException {

    public VuzopediaScrapingException(String message) {
        super(message);
    }

    public VuzopediaScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}
