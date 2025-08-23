package com.killiann.briefsaas.controller;

import com.killiann.briefsaas.dto.*;
import com.killiann.briefsaas.entity.Brief;
import com.killiann.briefsaas.entity.BriefStatus;
import com.killiann.briefsaas.entity.User;
import com.killiann.briefsaas.exception.BadRequestException;
import com.killiann.briefsaas.exception.ForbiddenException;
import com.killiann.briefsaas.service.BriefService;
import com.killiann.briefsaas.service.PdfService;
import com.killiann.briefsaas.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/briefs")
@RequiredArgsConstructor
@CrossOrigin
public class BriefController {

    private final BriefService briefService;
    private final PdfService pdfService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<BriefResponse>> getMyBriefs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size
    ) {
        User currentUser = userService.getCurrentUser();
        Page<BriefResponse> briefs = briefService.getUserBriefs(currentUser, status, page, size);
        return ResponseEntity.ok(briefs);
    }

    @PostMapping
    public ResponseEntity<BriefResponse> createBrief(@RequestBody BriefRequest request) throws ForbiddenException {
        User currentUser = userService.getCurrentUser();
        BriefResponse brief = briefService.createBrief(request, currentUser);
        return ResponseEntity.ok(brief);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<BriefResponse> submitBriefToClient(@PathVariable Long id) throws BadRequestException, ForbiddenException {
        User currentUser = userService.getCurrentUser();
        BriefResponse response = briefService.submitToClient(id, currentUser);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<BriefResponse> updateBrief(@PathVariable Long id, @RequestBody BriefRequest request) throws ForbiddenException {
        User currentUser = userService.getCurrentUser();
        BriefResponse brief = briefService.updateBrief(id, request, currentUser);
        return ResponseEntity.ok(brief);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BriefResponse> updateBriefStatus(
            @PathVariable Long id,
            @RequestBody BriefUpdateRequest request
    ) throws ForbiddenException {
        User currentUser = userService.getCurrentUser();
        BriefResponse updated = briefService.updateBriefStatus(id, request.getStatus(), currentUser);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadBriefPdf(@PathVariable Long id, @RequestHeader(name = "Accept-Language", required = false) Locale locale) throws ForbiddenException {
        User currentUser = userService.getCurrentUser();
        Brief brief = briefService.getBriefByIdForCurrentUser(id, currentUser);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generateBriefPdf(brief, locale != null ? locale : Locale.FRENCH);
        } catch (IOException e) {
            throw new RuntimeException("Erreur génération PDF", e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("brief.pdf").build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
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