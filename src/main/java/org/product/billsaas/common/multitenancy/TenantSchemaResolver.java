package org.product.billsaas.common.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantSchemaResolver implements CurrentTenantIdentifierResolver {
    private static final String DEFAULT_TENANT = "public";

    @Override
    public Object resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getTenantId();
        return (tenant!=null && !tenant.isBlank()) ? tenant : DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
