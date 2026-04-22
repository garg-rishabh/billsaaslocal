package org.product.billsaas.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "user_tenants",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tenant_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTenant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    private String role;

    private Instant createdAt;
}