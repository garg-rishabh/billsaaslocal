package org.product.billsaas.common.config;

import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.product.billsaas.common.multitenancy.TenantConnectionProvider;
import org.product.billsaas.common.multitenancy.TenantSchemaResolver;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class JpaConfig {

    private final DataSource dataSource;
    private final TenantConnectionProvider connectionProvider;
    private final TenantSchemaResolver tenantResolver;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder
    ) {

        Map<String, Object> properties = new HashMap<>();

        properties.put("hibernate.multiTenancy", "SCHEMA");
        properties.put("hibernate.multi_tenant_connection_provider", connectionProvider);
        properties.put("hibernate.tenant_identifier_resolver", tenantResolver);

        return builder
                .dataSource(dataSource)
                .packages("org.product.billsaas")
                .properties(properties)
                .build();
    }
}