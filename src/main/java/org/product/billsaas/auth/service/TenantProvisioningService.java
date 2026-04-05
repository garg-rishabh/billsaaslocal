package org.product.billsaas.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final JdbcTemplate jdbcTemplate;

    public void provisionSchema(String schemaName) {

        // Validate input (basic safety)
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("Schema name cannot be empty");
        }

        // Call PostgreSQL function
        jdbcTemplate.execute(
                "SELECT provision_tenant_schema('" + schemaName + "')"
        );
    }
}