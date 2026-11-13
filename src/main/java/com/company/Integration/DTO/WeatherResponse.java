package com.company.Integration.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "Current weather data for a city")
public class WeatherResponse implements Serializable {

    @Schema(description = "Main weather readings")
    private Main main;

    @Data
    @Schema(description = "Temperature and humidity readings")
    public static class Main implements Serializable {

        @Schema(description = "Temperature in Kelvin (OpenWeatherMap's default unit)", example = "293.15")
        private double temp;

        @Schema(description = "Relative humidity in percent", example = "64")
        private int humidity;
    }
}