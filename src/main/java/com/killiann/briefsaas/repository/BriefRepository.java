package com.killiann.briefsaas.repository;

import com.killiann.briefsaas.entity.Brief;
import com.killiann.briefsaas.entity.BriefStatus;
import com.killiann.briefsaas.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BriefRepository extends JpaRepository<Brief, Long> {
    Page<Brief> findByOwner(User owner, Pageable pageable);
    Page<Brief> findByOwnerAndStatus(User owner, BriefStatus status, Pageable pageable);
    Optional<Brief> findByPublicUuid(UUID uuid);
    long countByOwner(User user);
}