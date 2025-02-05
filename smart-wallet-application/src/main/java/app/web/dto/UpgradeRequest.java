package app.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpgradeRequest {

    private String subscriptionType;

    private String subscriptionPeriod;

    private String walletId;
}
