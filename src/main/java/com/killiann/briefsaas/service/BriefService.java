package com.killiann.briefsaas.service;

import com.killiann.briefsaas.dto.BriefRequest;
import com.killiann.briefsaas.dto.BriefResponse;
import com.killiann.briefsaas.dto.PublicBriefResponse;
import com.killiann.briefsaas.entity.Brief;
import com.killiann.briefsaas.entity.BriefStatus;
import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.exception.NotFoundException;
import com.killiann.briefsaas.repository.BriefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BriefService {

    private final BriefRepository briefRepository;
    private final MailService mailService;

    public BriefResponse createBrief(BriefRequest request, User user) {
        Brief brief = Brief.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .objectives(request.getObjectives())
                .targetAudience(request.getTargetAudience())
                .budget(request.getBudget())
                .deadline(request.getDeadline())
                .deliverables(request.getDeliverables())
                .constraints(request.getConstraints())
                .clientName(request.getClientName())
                .clientEmail(request.getClientEmail())
                .owner(user)
                .publicUuid(UUID.randomUUID())
                .status(BriefStatus.DRAFT)
                .build();

        brief.setValidationCode(generateCode());
        brief.setClientValidated(false);

        Brief saved = briefRepository.save(brief);

        mailService.sendValidationEmail(
                brief.getClientEmail(),
                brief.getPublicUuid(),
                brief.getValidationCode()
        );

        return mapToResponse(saved);
    }

    public List<BriefResponse> getUserBriefs(User user) {
        return briefRepository.findByOwner(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public BriefResponse getBriefById(Long id, User user) {
        Brief brief = briefRepository.findById(id)
                .filter(b -> b.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Brief not found"));
        return mapToResponse(brief);
    }

    public PublicBriefResponse getPublicBrief(UUID uuid) {
        Brief brief = briefRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Public brief not found"));
        return mapToPublicResponse(brief);
    }

    public BriefResponse updateBrief(Long id, BriefRequest request, User user) {
        Brief brief = briefRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brief not found"));

        if (Boolean.TRUE.equals(brief.getClientValidated())) {
            throw new IllegalStateException("Le brief a déjà été validé et ne peut plus être modifié.");
        }

        if (!brief.getOwner().getId().equals(user.getId())) {
            try {
                throw new AccessDeniedException("You are not the owner of this brief.");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        brief.setTitle(request.getTitle());
        brief.setDescription(request.getDescription());
        brief.setObjectives(request.getObjectives());
        brief.setTargetAudience(request.getTargetAudience());
        brief.setBudget(request.getBudget());
        brief.setDeadline(request.getDeadline());
        brief.setDeliverables(request.getDeliverables());
        brief.setConstraints(request.getConstraints());
        brief.setClientName(request.getClientName());
        brief.setClientEmail(request.getClientEmail());

        Brief updated = briefRepository.save(brief);
        return mapToResponse(updated);
    }

    public void deleteBrief(Long id, User user) {
        Brief brief = briefRepository.findById(id)
                .filter(b -> b.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Brief not found"));

        if (!brief.getOwner().getId().equals(user.getId())) {
            try {
                throw new AccessDeniedException("Unauthorized");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        briefRepository.delete(brief);
    }

    public BriefResponse validateBrief(Long id, User user) {
        Brief brief = briefRepository.findById(id)
                .filter(b -> b.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Brief not found"));

        brief.setStatus(BriefStatus.VALIDATED);
        brief.setValidatedAt(LocalDateTime.now());
        briefRepository.save(brief);

        return mapToResponse(brief);
    }

    public BriefResponse publicValidate(UUID uuid, String code) {
        Brief brief = briefRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Public brief not found"));

        if (brief.getValidationCode() == null || !brief.getValidationCode().equals(code)) {
            throw new RuntimeException("Invalid validation code");
        }

        brief.setStatus(BriefStatus.VALIDATED);
        brief.setValidatedAt(LocalDateTime.now());
        brief.setClientValidated(true);
        briefRepository.save(brief);

        return mapToResponse(brief);
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private BriefResponse mapToResponse(Brief brief) {
        return BriefResponse.builder()
                .id(brief.getId())
                .publicUuid(brief.getPublicUuid())
                .title(brief.getTitle())
                .description(brief.getDescription())
                .objectives(brief.getObjectives())
                .targetAudience(brief.getTargetAudience())
                .budget(brief.getBudget())
                .deadline(brief.getDeadline())
                .deliverables(brief.getDeliverables())
                .constraints(brief.getConstraints())
                .clientName(brief.getClientName())
                .clientEmail(brief.getClientEmail())
                .clientValidated(brief.getClientValidated())
                .validatedAt(brief.getValidatedAt())
                .status(brief.getStatus())
                .createdAt(brief.getCreatedAt())
                .updatedAt(brief.getUpdatedAt())
                .build();
    }

    private PublicBriefResponse mapToPublicResponse(Brief brief) {
        return PublicBriefResponse.builder()
                .id(brief.getId())
                .publicUuid(brief.getPublicUuid())
                .title(brief.getTitle())
                .description(brief.getDescription())
                .objectives(brief.getObjectives())
                .targetAudience(brief.getTargetAudience())
                .budget(brief.getBudget())
                .deadline(brief.getDeadline())
                .deliverables(brief.getDeliverables())
                .constraints(brief.getConstraints())
                .clientName(brief.getClientName())
                .clientValidated(brief.getClientValidated())
                .validatedAt(brief.getValidatedAt())
                .status(brief.getStatus())
                .createdAt(brief.getCreatedAt())
                .updatedAt(brief.getUpdatedAt())
                .build();
    }
}