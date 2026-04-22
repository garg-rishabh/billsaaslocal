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
    private String fullName;

    @NotBlank
    private String phone;

    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;

    private String gstin;
}
