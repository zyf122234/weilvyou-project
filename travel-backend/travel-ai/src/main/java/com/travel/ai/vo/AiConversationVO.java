package com.travel.ai.vo;

import java.util.List;

public record AiConversationVO(String conversationId, List<AiMessageVO> messages) {
}
