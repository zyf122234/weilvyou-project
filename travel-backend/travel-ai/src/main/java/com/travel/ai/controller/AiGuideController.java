package com.travel.ai.controller;

import com.travel.ai.dto.AiGuideRequest;
import com.travel.ai.service.AiGuideService;
import com.travel.ai.vo.AiConversationVO;
import com.travel.common.result.R;
import com.travel.common.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai/customer-service")
@RequiredArgsConstructor
public class AiGuideController {

    private final AiGuideService aiGuideService;

    @GetMapping("/session/current")
    public R<AiConversationVO> currentConversation(@AuthenticationPrincipal LoginUser loginUser) {
        return R.ok(aiGuideService.currentConversation(loginUser));
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody @Valid AiGuideRequest request,
                             @AuthenticationPrincipal LoginUser loginUser,
                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        return aiGuideService.chat(request, loginUser, authorization);
    }

    @PostMapping("/session/invalidate-current")
    public R<Void> invalidateCurrentConversation(@AuthenticationPrincipal LoginUser loginUser) {
        aiGuideService.invalidateCurrentConversation(loginUser);
        return R.ok();
    }
}
