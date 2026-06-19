package com.bank.acnumgens.config;

import com.bank.acnumgens.model.SequenceMaster;
import com.bank.acnumgens.repository.SequenceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Transactional
public class DataInitializer implements CommandLineRunner {

    private final SequenceRepository repository;

    @Override
    public void run(String... args) {
        createIfMissing("ACCOUNT");
        createIfMissing("CUSTOMER");
        createIfMissing("TRANSACTION");
    }

    private void createIfMissing(String type) {
        repository.findBySequenceTypeForUpdate(type)
                .orElseGet(() ->
                        repository.save(
                                SequenceMaster
                                        .builder()
                                        .sequenceType(type)
                                        .currentValue(1L)
                                        .build()
                        ));
    }
}