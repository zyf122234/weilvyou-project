package com.travel.ai.service;

import com.travel.ai.dto.AiGuideRequest;
import com.travel.ai.vo.AiConversationVO;
import com.travel.common.security.LoginUser;
import reactor.core.publisher.Flux;

public interface AiGuideService {

    AiConversationVO currentConversation(LoginUser loginUser);

    Flux<String> chat(AiGuideRequest request, LoginUser loginUser, String authorization);

    void invalidateCurrentConversation(LoginUser loginUser);
}
