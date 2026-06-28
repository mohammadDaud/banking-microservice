package com.bank.service;

import com.bank.dtos.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

public interface KycService {


    KycResponse createKyc(
            String userId,
            String panNumber,
            String aadhaarNumber,
            MultipartFile panDocument,
            MultipartFile aadhaarDocument,
            HttpServletRequest httpServletRequest
    );

    KycResponse resubmitKyc(
            String userId,
            String panNumber,
            String aadhaarNumber,
            MultipartFile panDocument,
            MultipartFile aadhaarDocument,
            HttpServletRequest httpServletRequest
    );

    KycResponse getKyc(String userId);

    KycResponse reviewKyc(
            String userId,
            String checkerId,
            String remark,
            HttpServletRequest httpServletRequest
    );

    KycResponse approveKyc(
            String userId,
            String checkerId,
            String remark,
            HttpServletRequest httpServletRequest
    );

    KycResponse rejectKyc(
            String userId,
            String checkerId,
            String remark,
            HttpServletRequest httpServletRequest
    );

    List<KycResponse> getPendingKyc();

    Long countByStatus(String status);

    List<Object[]> getStats();

    KycEligibilityResponse checkEligibility(String userId);

    List<KycResponse> getKycByStatus(String status);

    List<KycResponse> getCheckerQueue();

    KycDashboardResponse getDashboardStats();
}