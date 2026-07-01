package com.bank.us.validation;

import com.bank.us.client.AccountClient;
import com.bank.us.client.TransactionClient;
import com.bank.us.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerValidationService {

    private final AccountClient accountClient;
    private final TransactionClient transactionClient;
  /*  private final BeneficiaryClient beneficiaryClient;
    private final KycClient kycClient;*/

    public void validateCustomerDeletion(String userId) {

        validate(accountClient.validateCustomerDeletion(userId));

        validate(transactionClient.validateCustomerDeletion(userId));

        /*validate(beneficiaryClient.validateCustomerDeletion(userId));

        validate(kycClient.validateCustomerDeletion(userId));*/

    }

    private void validate(CustomerDeletionValidationResponse response) {

        if (response == null) {
            return;
        }

        if (response.isAllowed()) {
            return;
        }

        StringBuilder builder = new StringBuilder();

        for (ValidationError error : response.getErrors()) {

            builder.append(error.getMessage());

            builder.append("\n");

        }

        throw new BadRequestException(builder.toString());

    }

}