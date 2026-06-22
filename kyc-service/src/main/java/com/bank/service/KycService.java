package com.bank.service;

import com.bank.dtos.CreateKycRequest;
import com.bank.dtos.KycApprovalRequest;
import com.bank.dtos.KycEligibilityResponse;
import com.bank.dtos.KycResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

public interface KycService {

    KycResponse createKyc(String userId,String panNumber,String aadhaarNumber,MultipartFile panDocument,MultipartFile aadhaarDocument);

    KycResponse resubmitKyc(String userId,String panNumber,String aadhaarNumber,MultipartFile panDocument,MultipartFile aadhaarDocument);

    KycResponse getKyc(String userId);

    KycResponse reviewKyc(String userId, KycApprovalRequest request);

    KycResponse approveKyc(String userId, KycApprovalRequest request);

    KycResponse rejectKyc(String userId, KycApprovalRequest request);

    public List<KycResponse> getPendingKyc();

    Long countByStatus(String pending);

    List<Object[]> getStats();

    KycEligibilityResponse checkEligibility(String userId);

    List<KycResponse> getKycByStatus(String status);
}