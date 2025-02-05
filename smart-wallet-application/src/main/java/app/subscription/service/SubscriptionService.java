package app.subscription.service;

import app.exception.DomainException;
import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionPeriod;
import app.subscription.model.SubscriptionStatus;
import app.subscription.model.SubscriptionType;
import app.subscription.property.SubscriptionProperties;
import app.subscription.repository.SubscriptionRepository;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.user.model.User;
import app.wallet.service.WalletService;
import app.web.dto.UpgradeOption;
import app.web.dto.UpgradeRequest;
import app.web.dto.UpgradeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.metamodel.mapping.internal.SimpleAttributeMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static app.subscription.model.SubscriptionType.*;
import static app.subscription.model.SubscriptionType.ULTIMATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    private final SubscriptionProperties properties;

    private final WalletService walletService;


    public void createDefaultDescription(User user) {

        Subscription subscription = initializeSubscription(user);
        subscriptionRepository.save(subscription);

        log.info("Successfully created new subscription with id [%s] and type [%s]".formatted(subscription.getId(), subscription.getType()));

    }

    private Subscription initializeSubscription(User user) {

        LocalDateTime now = LocalDateTime.now();
        return Subscription.builder()
                .owner(user)
                .status(SubscriptionStatus.ACTIVE)
                .period(SubscriptionPeriod.MONTHLY)
                .type(SubscriptionType.DEFAULT)
                .price(new BigDecimal("0.00"))
                .renewalAllowed(true)
                .createdOn(now)
                .completedOn(now.plusMonths(1))
                .build();
    }

    public List<Subscription> getHistory(UUID id) {
        return subscriptionRepository.findByOwnerIdOrderByCreatedOnDesc(id);
    }

    public Map<String, UpgradeOption> getUpgradeOptions(UUID id) {

        Subscription currentUserSubscription = getCurrentActiveSubscription(id);

        UpgradeOption defaultSubscriptionOption = getOptionByType(DEFAULT);
        UpgradeOption premiumSubscriptionOption = getOptionByType(PREMIUM);
        UpgradeOption ultimateSubscriptionOption = getOptionByType(ULTIMATE);

        switch (currentUserSubscription.getType()) {
            case DEFAULT -> defaultSubscriptionOption.setChoosable(false);
            case PREMIUM -> premiumSubscriptionOption.setChoosable(false);
            case ULTIMATE -> ultimateSubscriptionOption.setChoosable(false);
        }

        return Map.of(
                DEFAULT.name(), defaultSubscriptionOption,
                PREMIUM.name(), premiumSubscriptionOption,
                ULTIMATE.name(), ultimateSubscriptionOption
        );
    }
    private Subscription getCurrentActiveSubscription(UUID id) {

        return subscriptionRepository.findByStatusAndOwnerId(SubscriptionStatus.ACTIVE, id)
                .orElseThrow(() -> new DomainException("User with id [%s] doesn't have any active subscription.".formatted(id), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private UpgradeOption getOptionByType(SubscriptionType type) {

        return UpgradeOption.builder()
                .type(type)
                .benefits(properties.getUpgradeOptions().get(type).getBenefits())
                .monthlyPrice(properties.getUpgradeOptions().get(type).getMonthlyPrice())
                .yearlyPrice(properties.getUpgradeOptions().get(type).getYearlyPrice())
                .isChoosable(true)
                .build();
    }

    public UpgradeResult upgrade(User user, UpgradeRequest request) {

        Subscription currentUserSubscription = getCurrentActiveSubscription(user.getId());

        SubscriptionType desiredSubscriptionType = SubscriptionType.valueOf(request.getSubscriptionType().toUpperCase());
        SubscriptionPeriod desiredSubscriptionPeriod = SubscriptionPeriod.valueOf(request.getSubscriptionPeriod().toUpperCase());

        BigDecimal price;
        if (desiredSubscriptionPeriod == SubscriptionPeriod.MONTHLY) {
            price = properties.getUpgradeOptions().get(desiredSubscriptionType).getMonthlyPrice();
        } else {
            price = properties.getUpgradeOptions().get(desiredSubscriptionType).getYearlyPrice();
        }

        String chargeDescription = "Purchased %s subscription".formatted(desiredSubscriptionType);
        Transaction chargeResult = walletService.charge(user, UUID.fromString(request.getWalletId()), price, chargeDescription);

        if (chargeResult.getStatus() == TransactionStatus.FAILED) {

            log.warn("Upgrade attempt failed because of [%s].".formatted(chargeResult.getFailureReason()));
            return UpgradeResult.builder()
                    .transaction(chargeResult)
                    .oldSubscription(currentUserSubscription)
                    .newSubscription(currentUserSubscription)
                    .build();
        }

        Subscription newUserSubscription = buildNewUserSubscription(user, desiredSubscriptionType, desiredSubscriptionPeriod, price);
        currentUserSubscription.setCompletedOn(LocalDateTime.now());
        currentUserSubscription.setStatus(SubscriptionStatus.COMPLETED);

        subscriptionRepository.save(currentUserSubscription);
        subscriptionRepository.save(newUserSubscription);

        return UpgradeResult.builder()
                .transaction(chargeResult)
                .oldSubscription(currentUserSubscription)
                .newSubscription(newUserSubscription)
                .build();
    }

    private Subscription buildNewUserSubscription(User user, SubscriptionType type, SubscriptionPeriod period, BigDecimal price) {

        LocalDateTime createdOn = LocalDateTime.now();
        LocalDateTime completedOn = LocalDateTime.now();
        if (period == SubscriptionPeriod.YEARLY) {
            completedOn = completedOn.plusMonths(12);
        } else {
            completedOn = completedOn.plusMonths(1);
        }

        return Subscription.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .status(SubscriptionStatus.ACTIVE)
                .period(period)
                .type(type)
                .price(price)
                .renewalAllowed(period == SubscriptionPeriod.MONTHLY)
                .createdOn(createdOn)
                .completedOn(completedOn)
                .build();
    }
}
