package com.killiann.briefsaas.repository;

import com.killiann.briefsaas.entity.Brief;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BriefRepository extends JpaRepository<Brief, Long> {
    List<Brief> findByUserId(Long userId);
    Optional<Brief> findByPublicUuid(String uuid);
}