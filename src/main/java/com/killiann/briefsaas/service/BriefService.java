package com.killiann.briefsaas.service;

import com.killiann.briefsaas.dto.BriefRequest;
import com.killiann.briefsaas.dto.BriefResponse;
import com.killiann.briefsaas.dto.ClientDto;
import com.killiann.briefsaas.dto.PublicBriefResponse;
import com.killiann.briefsaas.entity.Brief;
import com.killiann.briefsaas.entity.BriefStatus;
import com.killiann.briefsaas.entity.Client;
import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.exception.BadRequestException;
import com.killiann.briefsaas.exception.ForbiddenException;
import com.killiann.briefsaas.exception.NotFoundException;
import com.killiann.briefsaas.repository.BriefRepository;
import com.killiann.briefsaas.repository.ClientRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final ClientRepository clientRepository;
    private final MailService mailService;
    private static final Logger log = LoggerFactory.getLogger(BriefService.class);

    public BriefResponse createBrief(BriefRequest request, User user) throws ForbiddenException {
        checkBriefCreationAllowed(user);
        Client client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findById(request.getClientId())
                    .filter(c -> c.getOwner().getId().equals(user.getId()))
                    .orElseThrow(() -> new ForbiddenException("Client not found or unauthorized"));
        }
        Brief brief = Brief.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .objectives(request.getObjectives())
                .targetAudience(request.getTargetAudience())
                .budget(request.getBudget())
                .deadline(request.getDeadline())
                .deliverables(request.getDeliverables())
                .constraints(request.getConstraints())
                .client(client)
                .owner(user)
                .publicUuid(UUID.randomUUID())
                .status(BriefStatus.DRAFT)
                .build();

        brief.setValidationCode(generateCode());
        brief.setClientValidated(false);

        Brief saved = briefRepository.save(brief);

        return mapToResponse(saved);
    }

    @Transactional
    public BriefResponse submitToClient(Long briefId, User currentUser) throws BadRequestException, ForbiddenException {
        Brief brief = briefRepository.findById(briefId)
                .orElseThrow(() -> new NotFoundException("Brief not found with id " + briefId));

        if (brief.getStatus() == BriefStatus.SUBMITTED) {
            throw new BadRequestException("Brief already submitted to the client.");
        }

        if (!brief.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Access denied.");
        }

        mailService.sendValidationEmail(
                brief.getClient().getEmail(),
                brief.getPublicUuid(),
                brief.getValidationCode(),
                currentUser.getLanguage()
        );

        brief.setStatus(BriefStatus.SUBMITTED);
        Brief saved = briefRepository.save(brief);

        log.info("Brief {} submitted to client {}", briefId, brief.getClient().getEmail());

        return mapToResponse(saved);
    }

    @Transactional
    public BriefResponse updateBriefStatus(Long id, BriefStatus status, User currentUser) throws ForbiddenException {
        Brief brief = briefRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Brief not found with id " + id));

        if (!brief.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not allowed to update this brief.");
        }

        brief.setStatus(status);

        Brief updated = briefRepository.save(brief);
        return mapToResponse(updated);
    }

    public Page<BriefResponse> getUserBriefs(User user, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Brief> briefs;
        if ("ALL".equalsIgnoreCase(status) || status == null) {
            briefs = briefRepository.findByOwner(user, pageable);
        } else {
            briefs = briefRepository.findByOwnerAndStatus(user, BriefStatus.valueOf(status), pageable);
        }
        return briefs.map(this::mapToResponse);
    }

    public BriefResponse getBriefById(Long id, User user) {
        Brief brief = briefRepository.findById(id)
                .filter(b -> b.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Brief not found"));
        return mapToResponse(brief);
    }

    public PublicBriefResponse getPublicBrief(UUID uuid) {
        Brief brief = briefRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new NotFoundException("Public brief not found"));
        return mapToPublicResponse(brief);
    }

    public BriefResponse updateBrief(Long id, BriefRequest request, User user) throws ForbiddenException {
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

        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .filter(c -> c.getOwner().getId().equals(user.getId()))
                    .orElseThrow(() -> new ForbiddenException("Unauthorized client"));
            brief.setClient(client);
        }

        brief.setTitle(request.getTitle());
        brief.setDescription(request.getDescription());
        brief.setObjectives(request.getObjectives());
        brief.setTargetAudience(request.getTargetAudience());
        brief.setBudget(request.getBudget());
        brief.setDeadline(request.getDeadline());
        brief.setStatus(BriefStatus.DRAFT);
        brief.setDeliverables(request.getDeliverables());
        brief.setConstraints(request.getConstraints());

        Brief updated = briefRepository.save(brief);
        return mapToResponse(updated);
    }

    public void deleteBrief(Long id, User user) {
        Brief brief = briefRepository.findById(id)
                .filter(b -> b.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Brief not found"));

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
                .orElseThrow(() -> new NotFoundException("Brief not found"));

        brief.setStatus(BriefStatus.VALIDATED);
        brief.setValidatedAt(LocalDateTime.now());
        briefRepository.save(brief);

        return mapToResponse(brief);
    }

    public BriefResponse publicValidate(UUID uuid, String code) {
        Brief brief = briefRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new NotFoundException("Public brief not found"));

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

    private ClientDto mapClient(Client client) {
        if (client == null) return null;
        return ClientDto.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .build();
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
                .client(mapClient(brief.getClient()))
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
                .clientName(brief.getClient().getName())
                .clientValidated(brief.getClientValidated())
                .validatedAt(brief.getValidatedAt())
                .status(brief.getStatus())
                .createdAt(brief.getCreatedAt())
                .updatedAt(brief.getUpdatedAt())
                .build();
    }

    private void checkBriefCreationAllowed(User currentUser) throws ForbiddenException {
        boolean isFree = !currentUser.isSubscriptionActive(); // ou getSubscription() == null

        if (isFree) {
            long briefsCount = briefRepository.countByOwner(currentUser);
            if (briefsCount >= 1) {
                throw new ForbiddenException("Limite atteinte pour un compte gratuit.");
            }
        }
    }

    public Brief getBriefByIdForCurrentUser(Long briefId, User currentUser) throws ForbiddenException {
        Brief brief = briefRepository.findById(briefId)
                .orElseThrow(() -> new NotFoundException("Brief not found"));

        Long currentUserId = currentUser.getId();
        if (!brief.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("You are not allowed to access this brief.");
        }

        return brief;
    }
}