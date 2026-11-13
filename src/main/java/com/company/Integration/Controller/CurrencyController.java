package com.company.Integration.Controller;

import com.company.Integration.DTO.CurrencyResponse;
import com.company.Integration.DTO.ErrorResponse;
import com.company.Integration.Service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/currency")
@Slf4j
@Tag(name = "Currency", description = "Live currency exchange rates")
public class CurrencyController {

    @Autowired
    private CurrencyService currencyService;

    @Operation(
            summary = "Get latest exchange rates",
            description = "Returns the latest exchange rates for a base currency, "
                    + "optionally filtered to specific target currencies."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rates fetched successfully",
                    content = @Content(schema = @Schema(implementation = CurrencyResponse.class))),
            @ApiResponse(responseCode = "502", description = "External Currency API failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/latest")
    public CurrencyResponse getCurrencyRates(
            @Parameter(description = "Base currency code", example = "USD")
            @RequestParam(defaultValue = "USD") String base,
            @Parameter(description = "Comma-separated target currency codes (optional)", example = "EUR,GBP")
            @RequestParam(required = false) String symbols) {
        log.info("Currencies are {}", base);
        return currencyService.getRates(base, symbols);
    }
}