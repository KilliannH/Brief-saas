package com.killiann.briefsaas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "briefs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Brief {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BriefStatus status;

    @Column(unique = true, updatable = false)
    private String publicUuid; // lien public

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private String clientName;

    @Column
    private LocalDateTime validatedAt;


    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        publicUuid = UUID.randomUUID().toString();
    }
}