package org.product.billsaas.auth.service;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.product.billsaas.auth.dto.AuthResponse;
import org.product.billsaas.auth.dto.LoginRequest;
import org.product.billsaas.auth.dto.RefreshRequest;
import org.product.billsaas.auth.dto.RegisterRequest;
import org.product.billsaas.auth.entity.Tenant;
import org.product.billsaas.auth.entity.User;
import org.product.billsaas.auth.entity.UserTenant;
import org.product.billsaas.auth.repository.TenantRepository;
import org.product.billsaas.auth.repository.UserRepository;
import org.product.billsaas.auth.repository.UserTenantRepository;
import org.product.billsaas.common.security.JwtTokenProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final TenantProvisioningService provisioningService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserTenantRepository userTenantRepository;
    private final StringRedisTemplate redisTemplate;


    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String phone = normalizePhone(request.getPhone());

        // 🔥 USER: find or create
        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> createUser(request));

        // 🔥 TENANT
        String slug = generateSlug(request.getBusinessName());
        String schema = generateSchemaName(slug);

        Tenant tenant = tenantRepository.save(
                Tenant.builder()
                        .name(request.getBusinessName())
                        .slug(slug)
                        .gstin(request.getGstin())
                        .dbSchemaName(schema)
                        .isActive(true)
                        .createdAt(Instant.now())
                        .build()
        );

        try {
            provisioningService.provisionSchema(schema);
        } catch (Exception e) {
            tenantRepository.deleteById(tenant.getId());
            throw new RuntimeException("Schema provisioning failed", e);
        }

        // 🔥 MAPPING
        userTenantRepository.save(
                UserTenant.builder()
                        .userId(user.getId())
                        .tenantId(tenant.getId())
                        .role("OWNER")
                        .createdAt(Instant.now())
                        .build()
        );

        // 🔥 JWT
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId().toString(),
                tenant.getDbSchemaName(),
                user.getEmail(),
                "OWNER"
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId().toString()
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                tenant.getId().toString(),
                user.getId().toString()
        );
    }

    // =========================
    // HELPERS
    // =========================

    private User createUser(RegisterRequest request) {
        return userRepository.save(
                User.builder()
                        .phone(normalizePhone(request.getPhone()))
                        .email(normalizeEmail(request.getEmail()))
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .isActive(true)
                        .createdAt(Instant.now())
                        .build()
        );
    }

    private String normalizePhone(String phone) {
        return phone.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.toLowerCase().trim();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String generateSchemaName(String slug) {
        String random = UUID.randomUUID().toString().substring(0, 6);
        return "tenant_" + slug + "_" + random;
    }

    // =========================
    // LOGIN
    // =========================
    public AuthResponse login(LoginRequest request) {

        String phone = request.getPhone().trim();

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        // 🔥 Get user's tenants
        List<UserTenant> mappings = userTenantRepository.findAll()
                .stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .toList();

        if (mappings.isEmpty()) {
            throw new RuntimeException("No tenant associated");
        }

        // 👉 For now pick first tenant (later: selection API)
        UUID tenantId = mappings.get(0).getTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow();

        // 🔥 Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId().toString(),
                tenant.getDbSchemaName(),
                user.getEmail(),
                mappings.get(0).getRole()
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId().toString()
        );

        // 🔥 Store refresh token in Redis
        String key = "refresh:" + user.getId();

        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                Duration.ofDays(30)
        );

        // 🔥 update last_login
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                tenant.getId().toString(),
                user.getId().toString()
        );
    }

    // =========================
    // REFRESH
    // =========================
    public String refreshToken(RefreshRequest request) {

        String token = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String userId = jwtTokenProvider.getUserId(token);

        String key = "refresh:" + userId;

        String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken == null || !storedToken.equals(token)) {
            throw new RuntimeException("Refresh token revoked");
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow();

        // get tenant again
        UserTenant mapping = userTenantRepository.findAll()
                .stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        Tenant tenant = tenantRepository.findById(mapping.getTenantId())
                .orElseThrow();

        return jwtTokenProvider.generateAccessToken(
                user.getId().toString(),
                tenant.getDbSchemaName(),
                user.getEmail(),
                mapping.getRole()
        );
    }

    // =========================
    // LOGOUT
    // =========================
    public void logout(String refreshToken) {

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return;
        }

        String userId = jwtTokenProvider.getUserId(refreshToken);

        String key = "refresh:" + userId;

        redisTemplate.delete(key);
    }

}
