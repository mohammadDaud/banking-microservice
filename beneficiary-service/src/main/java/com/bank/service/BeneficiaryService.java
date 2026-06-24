package com.bank.service;

import com.bank.dtos.BeneficiaryEligibilityResponse;
import com.bank.dtos.BeneficiaryResponse;
import com.bank.dtos.CreateBeneficiaryRequest;

import java.util.List;

public interface BeneficiaryService {
    BeneficiaryResponse createBeneficiary(CreateBeneficiaryRequest request, String makerId);

    List<BeneficiaryResponse> getCustomerBeneficiaries(String customerId);

    BeneficiaryResponse approveBeneficiary(String beneficiaryId, String checkerId, String remarks);

    BeneficiaryResponse rejectBeneficiary(String beneficiaryId, String checkerId, String remarks);

    void deleteBeneficiary(String beneficiaryId, String customerId);

    Long getBeneficiaryCount(String customerId);

    List<BeneficiaryResponse> getPendingBeneficiaries();

    Long count();

    BeneficiaryEligibilityResponse checkEligibility(String beneficiaryId, String customerId);
}
