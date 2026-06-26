package mlakir.aura.core.services.tabiturient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsoupTabiturientAjaxClientTest {

    @Mock
    private JsoupConnectionFactory connectionFactory;
    @Mock
    private Connection connection;
    @Mock
    private Connection.Response response;

    private TabiturientScraperProperties properties;
    private JsoupTabiturientAjaxClient client;

    @BeforeEach
    void setUp() throws Exception {
        properties = new TabiturientScraperProperties();
        properties.setAjaxUrl("https://tabiturient.ru/ajax/ajsliv.php");
        properties.setUserAgent("AuraReviewBot/1.0");
        properties.setTimeoutMs(10000);
        client = new JsoupTabiturientAjaxClient(properties, connectionFactory);

        when(connectionFactory.connect("https://tabiturient.ru/ajax/ajsliv.php")).thenReturn(connection);
        when(connection.userAgent("AuraReviewBot/1.0")).thenReturn(connection);
        when(connection.timeout(10000)).thenReturn(connection);
        when(connection.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")).thenReturn(connection);
        when(connection.method(Connection.Method.POST)).thenReturn(connection);
        when(connection.data("vuzid", "dvfu")).thenReturn(connection);
        when(connection.data("limit", "50")).thenReturn(connection);
        when(connection.data("sortby", "3")).thenReturn(connection);
        when(connection.data("sortby2", "")).thenReturn(connection);
        when(connection.execute()).thenReturn(response);
        when(response.body()).thenReturn("<div class=\"mobpadd20-2\"></div>");
    }

    @Test
    void shouldSendExpectedAjaxFormParams() {
        Document result = client.fetchReviews("dvfu", 50);

        assertEquals("https://tabiturient.ru", result.location());
        verify(connection).data("vuzid", "dvfu");
        verify(connection).data("limit", "50");
        verify(connection).data("sortby", "3");
        verify(connection).data("sortby2", "");
        verify(connection).method(Connection.Method.POST);
    }
}
