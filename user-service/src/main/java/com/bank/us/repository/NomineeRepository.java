package com.bank.us.repository;

import com.bank.us.model.Nominee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NomineeRepository extends JpaRepository<Nominee,String> {
}