package com.travel.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiGuideRequest {

    @NotBlank(message = "消息不能为空")
    private String message;

    private String conversationId;
}
