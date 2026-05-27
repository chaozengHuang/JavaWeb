package com.hcz.nexusbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserStatusDTO {
    @NotBlank(message = "状态不能为空")
    private String status;
}
