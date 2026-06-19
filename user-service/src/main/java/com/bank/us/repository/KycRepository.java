package com.bank.us.repository;

import com.bank.us.model.KycDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycRepository extends JpaRepository<KycDetail,String> {
}