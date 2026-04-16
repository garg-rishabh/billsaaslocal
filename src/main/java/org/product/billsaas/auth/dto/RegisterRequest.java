package org.product.billsaas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String businessName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;

    @Pattern(regexp = "^[+]?[0-9]{10,13}$", message = "Phone number must be valid")
    @NotBlank
    private String phone;
    @NotBlank
    private String fullName;
}
