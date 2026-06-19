package com.bank.service;

import com.bank.dtos.CreateNomineeRequest;
import com.bank.dtos.NomineeResponse;

import java.util.List;

public interface NomineeService {
    NomineeResponse createNominee(CreateNomineeRequest request);
    List<NomineeResponse> getCustomerNominees(String customerId);
    void deleteNominee(String nomineeId);
    NomineeResponse updateNominee(String nomineeId,CreateNomineeRequest request);
    List<NomineeResponse> getAccountNominees(String accountNumber);
}