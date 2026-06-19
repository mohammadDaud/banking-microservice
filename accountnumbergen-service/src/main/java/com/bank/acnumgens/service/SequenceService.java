package com.bank.acnumgens.service;

import com.bank.acnumgens.exception.ResourceNotFoundException;
import com.bank.acnumgens.model.SequenceMaster;
import com.bank.acnumgens.repository.SequenceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SequenceService {

    private final SequenceRepository repository;

    @Transactional
    public String generateAccountNumber() {

        SequenceMaster sequence =
                getAndIncrement("ACCOUNT");

        return String.format("%010d", sequence.getCurrentValue());
    }

    @Transactional
    public String generateCustomerId() {
        SequenceMaster sequence =
                getAndIncrement("CUSTOMER");

        return "CUST"
                + String.format(
                "%010d",
                sequence.getCurrentValue());
    }

    @Transactional
    public String generateTransactionReference() {

        SequenceMaster sequence =
                getAndIncrement("TRANSACTION");

        return "TXN"
                + String.format(
                "%010d",
                sequence.getCurrentValue());
    }

    private SequenceMaster getAndIncrement( String type) {

    SequenceMaster sequence = repository
                    .findBySequenceTypeForUpdate(type)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(type+ " sequence not found"));

        sequence.setCurrentValue(
                sequence.getCurrentValue() + 1);
        return repository.save(sequence);
    }
}