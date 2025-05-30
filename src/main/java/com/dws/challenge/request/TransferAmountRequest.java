package com.dws.challenge.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferAmountRequest {

    @NotNull
    private final String accountFrom;

    @NotNull
    private final String accountTo;

    @NotNull
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

}
