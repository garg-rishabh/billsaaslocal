package org.product.billsaas.auth.service;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.product.billsaas.auth.dto.AuthResponse;
import org.product.billsaas.auth.dto.RegisterRequest;
import org.product.billsaas.auth.entity.Tenant;
import org.product.billsaas.auth.entity.User;
import org.product.billsaas.auth.repository.TenantRepository;
import org.product.billsaas.auth.repository.UserRepository;
import org.product.billsaas.common.security.JwtTokenProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final TenantProvisioningService tenantProvisioningService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        String slug = generateSlug(registerRequest.getBusinessName());
        String schemaName = generateSchemaName(slug);
        Tenant tenant = Tenant.builder().name(registerRequest.getBusinessName())
                .email(registerRequest.getEmail())
                .slug(slug)
                .dbSchemaName(schemaName)
                .phone(registerRequest.getPhone())
                .build();
        tenant = tenantRepository.save(tenant);

        try {
            tenantProvisioningService.provisionSchema(schemaName);
        }
        catch (Exception e){
            tenantRepository.deleteById(tenant.getId());
                throw new RuntimeException("Schema provisioning failed", e);
        }

        User user = User.builder()
                .email(registerRequest.getEmail())
                .role("Owner")
                .tenantId(tenant.getId())
                .fullName(registerRequest.getFullName())
                .passwordHash(bCryptPasswordEncoder.encode(registerRequest.getPassword()))
                .build();
        user = userRepository.save(user);

        String accessToken = jwtTokenProvider.
                generateAccessToken(user.getId().toString(),schemaName, user.getEmail(),user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        return new AuthResponse(accessToken,refreshToken,tenant.getId().toString(),user.getId().toString());
    }

    private String generateSlug(@NotBlank String businessName) {
        return businessName.toLowerCase().replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");
    }

    private String generateSchemaName(String slug) {
        String random = UUID.randomUUID().toString().substring(0, 6);
        return "tenant_" + slug + "_" + random;
    }

}
