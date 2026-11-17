package com.example.banking.repository;

import com.example.banking.model.BankUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BankUserRepository extends JpaRepository<BankUser, UUID> {
    Optional<BankUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
