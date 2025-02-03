package app.web.mapper;

import app.transaction.model.Transaction;
import app.user.model.User;
import app.web.dto.TransactionResult;
import app.web.dto.UserEditRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {

    public static UserEditRequest mapToUserEditRequest(User user) {

        return UserEditRequest.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .profilePicture(user.getProfilePicture())
                .build();
    }

    public static TransactionResult toTransactionResult(Transaction transaction) {

        return TransactionResult.builder()
                .id(transaction.getId())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .amount(transaction.getAmount())
                .balanceLeft(transaction.getBalanceLeft())
                .currency(transaction.getCurrency())
                .type(transaction.getType())
                .failureReason(transaction.getFailureReason())
                .createdOn(transaction.getCreatedOn())
                .build();
    }

}
