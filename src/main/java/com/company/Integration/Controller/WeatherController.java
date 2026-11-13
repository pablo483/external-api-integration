package com.company.Integration.Controller;

import com.company.Integration.DTO.ErrorResponse;
import com.company.Integration.DTO.WeatherResponse;
import com.company.Integration.Service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
@Tag(name = "Weather", description = "Current weather from OpenWeatherMap (Redis-cached)")
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @Operation(
            summary = "Get current weather for a city",
            description = "Fetches current weather from OpenWeatherMap. Results are cached in Redis "
                    + "for 10 minutes per city, and each uncached call publishes an audit event to Kafka."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Weather fetched successfully",
                    content = @Content(schema = @Schema(implementation = WeatherResponse.class))),
            @ApiResponse(responseCode = "400", description = "Required parameter 'city' is missing",
                    content = @Content),
            @ApiResponse(responseCode = "502", description = "External Weather API failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public WeatherResponse getWeather(
            @Parameter(description = "City name", example = "Kathmandu", required = true)
            @RequestParam String city) {
        return weatherService.getWeather(city);
    }
}