package com.bank.service.impl;

import com.bank.client.NotificationClient;
import com.bank.dtos.CreateNomineeRequest;
import com.bank.dtos.NomineeResponse;
import com.bank.dtos.NotificationRequest;
import com.bank.exception.NomineeNotFoundException;
import com.bank.model.Nominee;
import com.bank.repository.NomineeRepository;
import com.bank.service.NomineeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NomineeServiceImpl implements NomineeService {

    private final NomineeRepository nomineeRepository;
    private final NotificationClient notificationClient;

    @Override
    public NomineeResponse createNominee(CreateNomineeRequest request) {
        Nominee nominee =
                Nominee.builder()
                        .id(UUID.randomUUID().toString())
                        .customerId(request.getCustomerId())
                        .accountNumber(request.getAccountNumber())
                        .nomineeName(request.getNomineeName())
                        .relationship(request.getRelationship())
                        .dateOfBirth(request.getDateOfBirth())
                        .mobileNumber(request.getMobileNumber())
                        .email(request.getEmail())
                        .address(request.getAddress())
                        .percentageShare(request.getPercentageShare())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
        nomineeRepository.save(nominee);
        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(nominee.getCustomerId())
                        .title("Nominee Added")
                        .message(nominee.getNomineeName()+ " added as nominee")
                        .build()
        );
        return map(nominee);
    }

    @Override
    public List<NomineeResponse> getCustomerNominees(String customerId) {
        return nomineeRepository
                .findByCustomerId(customerId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public void deleteNominee(String nomineeId) {
        nomineeRepository.deleteById(nomineeId);
    }

    @Override
    public NomineeResponse updateNominee(String nomineeId,CreateNomineeRequest request) {
        Nominee nominee =
                nomineeRepository.findById(nomineeId)
                        .orElseThrow(() ->
                                new NomineeNotFoundException("Nominee not found"));

        validateSharePercentage(request.getAccountNumber(),request.getPercentageShare(),nomineeId);
        nominee.setNomineeName(request.getNomineeName());
        nominee.setRelationship(request.getRelationship());
        nominee.setDateOfBirth(request.getDateOfBirth());
        nominee.setMobileNumber(request.getMobileNumber());
        nominee.setEmail(request.getEmail());
        nominee.setAddress(request.getAddress());
        nominee.setPercentageShare(request.getPercentageShare());
        nominee.setUpdatedAt(LocalDateTime.now());
        nomineeRepository.save(nominee);

        notificationClient.createNotification(
                NotificationRequest
                        .builder()
                        .userId(nominee.getCustomerId())
                        .title("Nominee Updated")
                        .message(nominee.getNomineeName()+ " updated successfully")
                        .build()
        );
        return map(nominee);
    }

    @Override
    public List<NomineeResponse> getAccountNominees(String accountNumber) {
        return nomineeRepository
                .findByAccountNumber(accountNumber)
                .stream()
                .map(this::map)
                .toList();
    }


    private void validateSharePercentage(String accountNumber,Integer share,String nomineeId) {
        List<Nominee> nominees =
                nomineeRepository.findByAccountNumber(accountNumber);
        int totalShare = nominees.stream()
                        .filter(n -> nomineeId == null || !n.getId().equals(nomineeId))
                        .mapToInt(Nominee::getPercentageShare)
                        .sum();

        if (totalShare + share > 100) {
            throw new RuntimeException("Total nominee share cannot exceed 100%");
        }
    }

    private NomineeResponse map(Nominee nominee) {
        return NomineeResponse.builder()
                .id(nominee.getId())
                .customerId(nominee.getCustomerId())
                .accountNumber(nominee.getAccountNumber())
                .nomineeName(nominee.getNomineeName())
                .relationship(nominee.getRelationship())
                .dateOfBirth(nominee.getDateOfBirth())
                .mobileNumber(nominee.getMobileNumber())
                .email(nominee.getEmail())
                .address(nominee.getAddress())
                .percentageShare(nominee.getPercentageShare())
                .build();
    }
}
