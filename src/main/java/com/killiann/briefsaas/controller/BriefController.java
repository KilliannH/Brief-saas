package com.killiann.briefsaas.controller;

import com.killiann.briefsaas.dto.BriefRequest;
import com.killiann.briefsaas.dto.BriefResponse;
import com.killiann.briefsaas.dto.ClientValidationRequest;
import com.killiann.briefsaas.dto.PublicBriefResponse;
import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.service.BriefService;
import com.killiann.briefsaas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/briefs")
@RequiredArgsConstructor
@CrossOrigin
public class BriefController {

    private final BriefService briefService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<BriefResponse>> getMyBriefs() {
        User currentUser = userService.getCurrentUser();
        List<BriefResponse> briefs = briefService.getUserBriefs(currentUser);
        return ResponseEntity.ok(briefs);
    }

    @PostMapping
    public ResponseEntity<BriefResponse> createBrief(@RequestBody BriefRequest request) {
        User currentUser = userService.getCurrentUser();
        BriefResponse brief = briefService.createBrief(request, currentUser);
        return ResponseEntity.ok(brief);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BriefResponse> getBriefById(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        BriefResponse brief = briefService.getBriefById(id, currentUser);
        return ResponseEntity.ok(brief);
    }

    @GetMapping("/public/{uuid}")
    public ResponseEntity<PublicBriefResponse> getPublicBrief(@PathVariable UUID uuid) {
        PublicBriefResponse brief = briefService.getPublicBrief(uuid);
        return ResponseEntity.ok(brief);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BriefResponse> updateBrief(@PathVariable Long id, @RequestBody BriefRequest request) {
        User currentUser = userService.getCurrentUser();
        BriefResponse brief = briefService.updateBrief(id, request, currentUser);
        return ResponseEntity.ok(brief);
    }

    @PutMapping("/{id}/validate")
    public ResponseEntity<BriefResponse> validateBrief(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        BriefResponse brief = briefService.validateBrief(id, currentUser);
        return ResponseEntity.ok(brief);
    }

    @PutMapping("/public/{uuid}/validate")
    public ResponseEntity<BriefResponse> publicValidate(
            @PathVariable UUID uuid,
            @RequestBody ClientValidationRequest request
    ) {
        BriefResponse brief = briefService.publicValidate(uuid, request.getCode());
        return ResponseEntity.ok(brief);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrief(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        briefService.deleteBrief(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}