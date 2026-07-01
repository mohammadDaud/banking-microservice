package com.bank.us.repository;

import com.bank.us.model.Nominee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NomineeRepository extends JpaRepository<Nominee, String> {

    List<Nominee> findAllByUserId(String userId);
}