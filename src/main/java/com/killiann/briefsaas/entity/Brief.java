package com.killiann.briefsaas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "briefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brief {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String description;

    @ElementCollection
    private List<String> objectives;

    private String targetAudience;

    private String budget;

    private LocalDate deadline;

    @ElementCollection
    private List<String> deliverables;

    private String constraints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    private Boolean clientValidated = false;

    private String validationCode;

    private LocalDateTime validatedAt;

    @Column(unique = true, updatable = false)
    private UUID publicUuid;

    @Enumerated(EnumType.STRING)
    private BriefStatus status = BriefStatus.DRAFT;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;
}