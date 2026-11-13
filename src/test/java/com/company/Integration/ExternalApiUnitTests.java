package com.company.Integration;

import com.company.Integration.Client.CurrencyApiClient;
import com.company.Integration.Client.WeatherApiClient;
import com.company.Integration.Config.OpenApiConfig;
import com.company.Integration.Config.WeatherConfig;
import com.company.Integration.Controller.CurrencyController;
import com.company.Integration.Controller.WeatherController;
import com.company.Integration.DTO.CurrencyResponse;
import com.company.Integration.DTO.ErrorResponse;
import com.company.Integration.DTO.WeatherResponse;
import com.company.Integration.Exception.ExternalApiException;
import com.company.Integration.Exception.GlobalExceptionHandler;
import com.company.Integration.Kafka.AuditEvent;
import com.company.Integration.Kafka.KafkaAuditEventProducer;
import com.company.Integration.Service.CurrencyService;
import com.company.Integration.Service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalApiUnitTests {

    @Mock
    private CurrencyApiClient currencyApiClient;

    @InjectMocks
    private CurrencyService currencyService;

    @Mock
    private WeatherApiClient weatherApiClient;

    @Mock
    private KafkaAuditEventProducer kafkaAuditEventProducer;

    @InjectMocks
    private WeatherService weatherService;

    private CurrencyController currencyController;
    private WeatherController weatherController;

    @BeforeEach
    void setUp() {
        currencyController = new CurrencyController();
        ReflectionTestUtils.setField(currencyController, "currencyService", currencyService);

        weatherController = new WeatherController();
        ReflectionTestUtils.setField(weatherController, "weatherService", weatherService);
    }

    @Test
    @DisplayName("1. Currency API (Service & Controller) returns exchange rates for base and symbols")
    void testCurrencyServiceAndController() {
        CurrencyResponse mockResponse = new CurrencyResponse();
        mockResponse.setSuccess(true);
        mockResponse.setSource("USD");
        mockResponse.setRates(Map.of("USDEUR", 0.92, "USDNPR", 133.50));

        when(currencyApiClient.fetchRates("USD", "EUR,NPR")).thenReturn(mockResponse);

        CurrencyResponse result = currencyController.getCurrencyRates("USD", "EUR,NPR");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("USD", result.getSource());
        assertEquals(0.92, result.getRates().get("USDEUR"));
        assertEquals(133.50, result.getRates().get("USDNPR"));
        verify(currencyApiClient, times(1)).fetchRates("USD", "EUR,NPR");
    }

    @Test
    @DisplayName("2. CurrencyApiClient parses JSON quotes into rates map via WebClient")
    void testCurrencyApiClientWebClientParsing() {
        String jsonBody = """
                {
                  "success": true,
                  "source": "USD",
                  "quotes": {
                    "USDEUR": 0.9185,
                    "USDGBP": 0.7892
                  }
                }
                """;

        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(jsonBody)
                        .build()
        );

        WebClient mockWebClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        CurrencyApiClient client = new CurrencyApiClient();
        ReflectionTestUtils.setField(client, "webClient", mockWebClient);
        ReflectionTestUtils.setField(client, "apiKey", "test-currency-key");

        CurrencyResponse response = client.fetchRates("USD", "EUR,GBP");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("USD", response.getSource());
        assertEquals(0.9185, response.getRates().get("USDEUR"));
        assertEquals(0.7892, response.getRates().get("USDGBP"));
    }

    @Test
    @DisplayName("3. Weather API (Service & Controller) returns temp/humidity and triggers async Kafka audit")
    void testWeatherServiceAndController() {
        WeatherResponse.Main main = new WeatherResponse.Main();
        main.setTemp(295.15);
        main.setHumidity(65);
        WeatherResponse mockWeather = new WeatherResponse();
        mockWeather.setMain(main);

        when(weatherApiClient.getWeather("Kathmandu")).thenReturn(mockWeather);

        WeatherResponse result = weatherController.getWeather("Kathmandu");

        assertNotNull(result);
        assertNotNull(result.getMain());
        assertEquals(295.15, result.getMain().getTemp());
        assertEquals(65, result.getMain().getHumidity());
        verify(weatherApiClient, times(1)).getWeather("Kathmandu");
        verify(kafkaAuditEventProducer, times(1)).sendAuditEvent(any(AuditEvent.class));
    }

    @Test
    @DisplayName("4. WeatherService isolates Kafka failure and still returns weather response")
    void testWeatherServiceIsolatesKafkaFailure() {
        WeatherResponse.Main main = new WeatherResponse.Main();
        main.setTemp(290.0);
        main.setHumidity(50);
        WeatherResponse mockWeather = new WeatherResponse();
        mockWeather.setMain(main);

        when(weatherApiClient.getWeather("Pokhara")).thenReturn(mockWeather);
        doThrow(new RuntimeException("Kafka broker down")).when(kafkaAuditEventProducer).sendAuditEvent(any());

        WeatherResponse result = assertDoesNotThrow(() -> weatherService.getWeather("Pokhara"));

        assertNotNull(result);
        assertEquals(290.0, result.getMain().getTemp());
    }

    @Test
    @DisplayName("5. WeatherApiClient parses OpenWeatherMap JSON response via WebClient")
    void testWeatherApiClientWebClientParsing() {
        String jsonBody = """
                {
                  "main": {
                    "temp": 296.4,
                    "humidity": 58
                  }
                }
                """;

        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(jsonBody)
                        .build()
        );

        WebClient mockWebClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        WeatherConfig config = new WeatherConfig();
        config.setBaseUrl("https://api.openweathermap.org/data/2.5");

        WeatherApiClient client = new WeatherApiClient();
        ReflectionTestUtils.setField(client, "webClient", mockWebClient);
        ReflectionTestUtils.setField(client, "weatherConfig", config);
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");

        WeatherResponse response = client.getWeather("Kathmandu");

        assertNotNull(response);
        assertNotNull(response.getMain());
        assertEquals(296.4, response.getMain().getTemp());
        assertEquals(58, response.getMain().getHumidity());
    }

    @Test
    @DisplayName("6. GlobalExceptionHandler returns 502 ErrorResponse and OpenApiConfig builds OpenAPI metadata")
    void testGlobalExceptionHandlerAndOpenApiConfig() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ErrorResponse error = handler.handleExternalApi(new ExternalApiException("Weather API failed"));

        assertNotNull(error);
        assertEquals(502, error.status());
        assertEquals("Bad Gateway", error.error());
        assertEquals("Weather API failed", error.message());
        assertNotNull(error.timestamp());

        OpenApiConfig openApiConfig = new OpenApiConfig();
        var openAPI = openApiConfig.integrationOpenAPI();
        assertNotNull(openAPI.getInfo());
        assertEquals("External API Integration Service", openAPI.getInfo().getTitle());
        assertEquals("v1.0.0", openAPI.getInfo().getVersion());
    }
}

