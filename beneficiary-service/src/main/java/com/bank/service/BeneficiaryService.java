package com.bank.service;

import com.bank.dtos.BeneficiaryResponse;
import com.bank.dtos.CreateBeneficiaryRequest;

import java.util.List;

public interface BeneficiaryService {
    BeneficiaryResponse createBeneficiary(CreateBeneficiaryRequest request);
    List<BeneficiaryResponse> getCustomerBeneficiaries(String customerId);
    BeneficiaryResponse approveBeneficiary(String beneficiaryId);
    BeneficiaryResponse rejectBeneficiary(String beneficiaryId);
    void deleteBeneficiary(String beneficiaryId);
    Long getBeneficiaryCount(String customerId);
    List<BeneficiaryResponse> getPendingBeneficiaries();

    Long count();
}
