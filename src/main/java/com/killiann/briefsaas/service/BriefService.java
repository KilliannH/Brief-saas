package com.killiann.briefsaas.service;

import com.killiann.briefsaas.dto.BriefRequest;
import com.killiann.briefsaas.dto.BriefResponse;
import com.killiann.briefsaas.entity.Brief;
import com.killiann.briefsaas.entity.BriefStatus;
import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.repository.BriefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BriefService {

    private final BriefRepository briefRepository;

    public BriefResponse createBrief(BriefRequest request, User user) {
        Brief brief = Brief.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(BriefStatus.DRAFT)
                .user(user)
                .build();

        briefRepository.save(brief);

        return mapToResponse(brief);
    }

    public List<BriefResponse> getUserBriefs(User user) {
        return briefRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public BriefResponse getBriefById(Long id, User user) {
        Brief brief = briefRepository.findById(id)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Brief not found"));
        return mapToResponse(brief);
    }

    public BriefResponse getPublicBrief(String uuid) {
        Brief brief = briefRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Public brief not found"));
        return mapToResponse(brief);
    }

    public BriefResponse updateBrief(Long id, BriefRequest request, User user) {
        Brief brief = briefRepository.findById(id)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Brief not found"));

        brief.setTitle(request.getTitle());
        brief.setDescription(request.getDescription());

        briefRepository.save(brief);

        return mapToResponse(brief);
    }

    public void deleteBrief(Long id, User user) {
        Brief brief = briefRepository.findById(id)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Brief not found"));

        briefRepository.delete(brief);
    }

    public BriefResponse validateBrief(Long id, User user) {
        Brief brief = briefRepository.findById(id)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Brief not found"));

        brief.setStatus(BriefStatus.VALIDATED);
        brief.setValidatedAt(LocalDateTime.now());
        briefRepository.save(brief);

        return mapToResponse(brief);
    }

    public BriefResponse publicValidate(String uuid, String clientName) {
        Brief brief = briefRepository.findByPublicUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Public brief not found"));

        brief.setStatus(BriefStatus.VALIDATED);
        brief.setValidatedAt(LocalDateTime.now());
        brief.setClientName(clientName);
        briefRepository.save(brief);

        return mapToResponse(brief);
    }

    private BriefResponse mapToResponse(Brief brief) {
        return BriefResponse.builder()
                .id(brief.getId())
                .title(brief.getTitle())
                .description(brief.getDescription())
                .status(brief.getStatus().name())
                .clientName(brief.getClientName())
                .validatedAt(brief.getValidatedAt())
                .publicUuid(brief.getPublicUuid())
                .build();
    }
}