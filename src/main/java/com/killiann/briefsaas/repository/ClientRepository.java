package com.killiann.briefsaas.repository;

import com.killiann.briefsaas.entity.Client;
import com.killiann.briefsaas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByOwner(User owner);
    long countByOwner(User owner);
    void deleteByOwner(User owner);
}