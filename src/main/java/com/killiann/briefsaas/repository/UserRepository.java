package com.killiann.briefsaas.repository;

import com.killiann.briefsaas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByStripeCustomerId(String customerId);
}