package com.tftgogo.domain.deck.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "client_version_patch_mapping",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"client_version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientVersionPatchMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_version", nullable = false, length = 20)
    private String clientVersion;

    @Column(name = "patch_version", nullable = false, length = 20)
    private String patchVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ClientVersionPatchMapping(String clientVersion, String patchVersion) {
        this.clientVersion = clientVersion;
        this.patchVersion = patchVersion;
    }

    public void update(String clientVersion, String patchVersion) {
        this.clientVersion = clientVersion;
        this.patchVersion = patchVersion;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
