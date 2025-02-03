package app.wallet.service;

import app.exception.DomainException;
import app.subscription.model.Subscription;
import app.subscription.model.SubscriptionType;
import app.transaction.model.Transaction;
import app.transaction.model.TransactionStatus;
import app.transaction.model.TransactionType;
import app.transaction.service.TransactionService;
import app.user.model.User;
import app.wallet.model.Wallet;
import app.wallet.model.WalletStatus;
import app.wallet.repository.WalletRepository;
import app.web.dto.TransferRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class WalletService {
    private static final String SMART_WALLET_LTD = "Smart Wallet Ltd";

    private final WalletRepository walletRepository;
    private final TransactionService transactionService;

    public WalletService(WalletRepository walletRepository, TransactionService transactionService) {
        this.walletRepository = walletRepository;

        this.transactionService = transactionService;
    }

    public void creatNewWallet(User user) {

        Wallet wallet = initialiseWallet(user);
        walletRepository.save(wallet);

        log.info("Successfully created new wallet with id [%s] and balance [%.2f].".formatted(wallet.getId(), wallet.getBalance()));
    }

    @Transactional
    public Transaction topUp(UUID walletId, BigDecimal amount) {
        Wallet wallet = getWalletById(walletId);
        String transactionDescription = "Top up %.2f".formatted(amount.doubleValue());

        if (wallet.getStatus() == WalletStatus.INACTIVE) {
            return transactionService.createNewTransaction(wallet.getOwner(),
                    SMART_WALLET_LTD,
                    walletId.toString(),
                    amount,
                    wallet.getBalance(),
                    wallet.getCurrency(),
                    TransactionType.DEPOSIT,
                    TransactionStatus.FAILED,
                    transactionDescription,
                    "Inactive wallet");
        }

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedOn(LocalDateTime.now());

        walletRepository.save(wallet);

        return transactionService.createNewTransaction(wallet.getOwner(),
                SMART_WALLET_LTD,
                walletId.toString(),
                amount,
                wallet.getBalance(),
                wallet.getCurrency(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCEEDED,
                transactionDescription,
                null);

    }

    @Transactional
    public Transaction charge(User user, UUID walletId, BigDecimal amount, String chargeDescription) {

        Wallet wallet = getWalletById(walletId);

        if (wallet.getStatus() == WalletStatus.INACTIVE) {
            String failureReason = "Inactive wallet";
            return transactionService.createNewTransaction(user, walletId.toString(), SMART_WALLET_LTD, amount,
                    wallet.getBalance(), wallet.getCurrency(),  TransactionType.WITHDRAWAL,TransactionStatus.FAILED,
                    chargeDescription, failureReason);
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            String failureReason = "Insufficient funds, top up your account";
            return transactionService.createNewTransaction(user, walletId.toString(), SMART_WALLET_LTD, amount,
                    wallet.getBalance(), wallet.getCurrency(),TransactionType.WITHDRAWAL, TransactionStatus.FAILED,
                    chargeDescription, failureReason);
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedOn(LocalDateTime.now());

        walletRepository.save(wallet);

        return transactionService.createNewTransaction(user, walletId.toString(), SMART_WALLET_LTD, amount, newBalance,
                wallet.getCurrency(), TransactionType.WITHDRAWAL,  TransactionStatus.SUCCEEDED,chargeDescription, null);
    }

    private Wallet getWalletById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new DomainException("Wallet with id [%s] did not exist.".formatted(walletId)));

    }

    private Wallet initialiseWallet(User user) {
        return Wallet.builder()
                .owner(user)
                .status(WalletStatus.ACTIVE)
                .balance(new BigDecimal("20.00"))
                .currency(Currency.getInstance("EUR"))
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    @Transactional
    public Transaction transferFunds(User sender, TransferRequest transferRequest) {

        Wallet senderWallet = getWalletById(transferRequest.getFromWalletId());
        Optional<Wallet> receiverWalletOptional = walletRepository.findAllByOwnerUsername(
                        transferRequest.getToUsername())
                .stream()
                .filter(w -> w.getStatus() == WalletStatus.ACTIVE)
                .findFirst();
        String transferDescription = "%.2f %s from %s".formatted(transferRequest.getAmount(),
                senderWallet.getCurrency(), sender.getUsername());

        if (receiverWalletOptional.isEmpty()) {

            return transactionService.createNewTransaction(sender,
                    senderWallet.getId().toString(),
                    transferRequest.getToUsername(),
                    transferRequest.getAmount(),
                    senderWallet.getBalance(),
                    senderWallet.getCurrency(),
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.FAILED,
                    transferDescription,
                    "Unable to perform transfer due to criteria not met");
        }

        Wallet receiverWallet = receiverWalletOptional.get();

        BigDecimal transferTax = calculateTransferTax(sender, transferRequest.getAmount());
        boolean isEligibleForCountryTax = sender.getCountry() != receiverWallet.getOwner().getCountry()
                && sender.getSubscriptions().get(0).getType() != SubscriptionType.ULTIMATE;
        BigDecimal countryTax = isEligibleForCountryTax
                ? BigDecimal.valueOf(0.20)
                : BigDecimal.ZERO;

        Transaction withdrawal = charge(sender, transferRequest.getFromWalletId(),
                transferRequest.getAmount().add(transferTax).add(countryTax), transferDescription);
        if (withdrawal.getStatus() == TransactionStatus.FAILED) {
            return withdrawal;
        }

        BigDecimal newReceiverBalance = receiverWallet.getBalance().add(transferRequest.getAmount());
        receiverWallet.setBalance(newReceiverBalance);
        receiverWallet.setUpdatedOn(LocalDateTime.now());

        walletRepository.save(receiverWallet);
        transactionService.createNewTransaction(receiverWallet.getOwner(),
                senderWallet.getId().toString(),
                receiverWallet.getId().toString(),
                transferRequest.getAmount(),
                newReceiverBalance,
                receiverWallet.getCurrency(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCEEDED,
                transferDescription,
                null);

        return withdrawal;
    }

    private BigDecimal calculateTransferTax(User sender, BigDecimal transferAmount) {

        double percentage = 0;

        Subscription currentActiveSubscription = sender.getSubscriptions().get(0);
        switch (currentActiveSubscription.getType()) {
            case DEFAULT -> percentage = 0.15;
            case PREMIUM -> percentage = 0.08;
            case ULTIMATE -> percentage = 0.02;
        }

        return transferAmount.multiply(BigDecimal.valueOf(percentage));
    }
}
