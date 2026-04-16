package org.product.billsaas.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    private String gstin;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(name = "plan_type")
    private String planType;

    @Column(name = "db_schema_name", unique = true, nullable = false)
    private String dbSchemaName;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;
    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.isActive = true;
    }
}