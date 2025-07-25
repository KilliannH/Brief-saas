package com.killiann.briefsaas.repository;

import com.killiann.briefsaas.entity.Brief;
import com.killiann.briefsaas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BriefRepository extends JpaRepository<Brief, Long> {
    List<Brief> findByOwner(User owner);
    Optional<Brief> findByPublicUuid(UUID uuid);
}