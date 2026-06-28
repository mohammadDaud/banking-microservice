package com.bank.service;

import com.bank.dtos.BeneficiaryDashboardResponse;
import com.bank.dtos.BeneficiaryEligibilityResponse;
import com.bank.dtos.BeneficiaryResponse;
import com.bank.dtos.CreateBeneficiaryRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface BeneficiaryService {
    BeneficiaryResponse createBeneficiary(CreateBeneficiaryRequest request, String makerId,HttpServletRequest  httpServletRequest);

    List<BeneficiaryResponse> getCustomerBeneficiaries(String customerId);

    BeneficiaryResponse approveBeneficiary(String beneficiaryId, String checkerId, String remarks,HttpServletRequest  httpServletRequest);

    BeneficiaryResponse rejectBeneficiary(String beneficiaryId, String checkerId, String remarks,HttpServletRequest  httpServletRequest);

    void deleteBeneficiary(String beneficiaryId, String customerId, HttpServletRequest httpServletRequest);

    Long getBeneficiaryCount(String customerId);

    List<BeneficiaryResponse> getPendingBeneficiaries();

    Long count();

    BeneficiaryEligibilityResponse checkEligibility(String beneficiaryId, String customerId);

    BeneficiaryDashboardResponse getDashboardStats();
}
