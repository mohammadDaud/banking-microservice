package com.bank.service;

import com.bank.dtos.BeneficiaryApprovalRequest;
import com.bank.dtos.BeneficiaryEligibilityResponse;
import com.bank.dtos.BeneficiaryResponse;
import com.bank.dtos.CreateBeneficiaryRequest;

import java.util.List;

public interface BeneficiaryService {
    BeneficiaryResponse createBeneficiary(CreateBeneficiaryRequest request);
    List<BeneficiaryResponse> getCustomerBeneficiaries(String customerId);
    BeneficiaryResponse approveBeneficiary(String beneficiaryId,BeneficiaryApprovalRequest request );
    BeneficiaryResponse rejectBeneficiary(String beneficiaryId,BeneficiaryApprovalRequest request);
    void deleteBeneficiary(String beneficiaryId);
    Long getBeneficiaryCount(String customerId);
    List<BeneficiaryResponse> getPendingBeneficiaries();

    Long count();

    BeneficiaryEligibilityResponse checkEligibility(String beneficiaryId, String customerId);

}
