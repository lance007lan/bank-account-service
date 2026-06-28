package com.bank.accountservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @Size(min = 5, max = 30, message = "Customer nickname must be between 5 and 30 characters")
    private String customerNickname;
}
