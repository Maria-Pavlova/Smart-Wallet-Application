package app.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Data
public class TransferRequest {

    private UUID fromWalletId;
    @NotNull
    private String toUsername;
    @NotNull
    @Positive
    private BigDecimal amount;
}
