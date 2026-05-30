package com.travel.ai.vo;

import java.time.LocalDateTime;

public record AiMessageVO(String role, String content, LocalDateTime createdTime) {
}
