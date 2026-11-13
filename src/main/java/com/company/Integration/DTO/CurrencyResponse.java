package com.company.Integration.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@Schema(description = "Exchange rates for a base currency")
public class CurrencyResponse implements Serializable {

    @Schema(description = "Whether the upstream API call succeeded", example = "true")
    private boolean success;

    @Schema(description = "Base currency code", example = "USD")
    private String source;

    @Schema(description = "Date of the rates", example = "2026-11-12")
    private String date;

    @JsonProperty("quotes")
    @Schema(description = "Map of currency pair to rate", example = "{\"USDEUR\": 0.92, \"USDGBP\": 0.79}")
    private Map<String, Double> rates;
}