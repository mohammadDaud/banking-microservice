package com.bank.controller;

import com.bank.dtos.CreateNomineeRequest;
import com.bank.dtos.NomineeResponse;
import com.bank.service.NomineeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nominees")
@RequiredArgsConstructor
public class NomineeController {

    private final NomineeService service;

    @PostMapping
    public NomineeResponse createNominee(@RequestBody CreateNomineeRequest request) {
        return service.createNominee(request);
    }

    @GetMapping("/customer/{customerId}")
    public List<NomineeResponse> getCustomerNominees(@PathVariable String customerId) {
        return service.getCustomerNominees(customerId);
    }

    @DeleteMapping("/{nomineeId}")
    public void deleteNominee(@PathVariable String nomineeId) {
        service.deleteNominee(nomineeId);
    }

    @GetMapping("/account/{accountNumber}")
    public List<NomineeResponse> getAccountNominees(@PathVariable String accountNumber) {
        return service.getAccountNominees(accountNumber);
    }

    @PutMapping("/{nomineeId}")
    public NomineeResponse updateNominee(@PathVariable String nomineeId,@RequestBody CreateNomineeRequest request) {
        return service.updateNominee(nomineeId,request);
    }
}