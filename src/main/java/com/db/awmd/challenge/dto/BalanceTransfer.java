package com.db.awmd.challenge.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class BalanceTransfer {

    @NotNull
    @NotEmpty
    private String accountFrom;

    @NotNull
    @NotEmpty
    private String accountTo;

    @NotNull
    @Min(value = 1, message = "Initial balance must be positive.")
    private BigDecimal amount;

}
