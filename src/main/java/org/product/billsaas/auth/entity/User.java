package org.product.billsaas.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String phone;

    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "created_at")
    private Instant createdAt;
    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.isActive = true;
    }
}