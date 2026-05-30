package com.travel.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.ai.dto.AiGuideRequest;
import com.travel.ai.entity.AiConversation;
import com.travel.ai.entity.AiMessage;
import com.travel.ai.mapper.AiConversationMapper;
import com.travel.ai.mapper.AiMessageMapper;
import com.travel.ai.service.AiGuideService;
import com.travel.ai.tool.CustomerServiceTools;
import com.travel.ai.vo.AiConversationVO;
import com.travel.ai.vo.AiMessageVO;
import com.travel.common.exception.BusinessException;
import com.travel.common.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AiGuideServiceImpl implements AiGuideService {

    private static final String SYSTEM_PROMPT = """
            你是旅游平台的智能客服小微。
            当用户咨询酒店、价格、城市、品牌、星级、订单、余额或账户信息时，优先调用工具查询真实数据。
            如果工具没有查询到数据，要明确告诉用户没有找到匹配结果，不要编造酒店、订单、余额、价格或状态。
            回答要简洁、自然。必要时展示酒店名称、城市、品牌、星级、价格、订单编号、订单状态、订单金额等关键信息。
            """;

    private static final String AI_ERROR_MESSAGE = "AI 模型服务调用失败，请检查 DeepSeek API Key、Base URL、模型名称和模型权限";

    private final ChatClient chatClient;
    private final CustomerServiceTools customerServiceTools;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;


    public AiGuideServiceImpl(ChatClient chatClient,
                              CustomerServiceTools customerServiceTools,
                              AiConversationMapper conversationMapper,
                              AiMessageMapper messageMapper) {
        this.chatClient = chatClient;
        this.customerServiceTools = customerServiceTools;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }


    // 当前会话
    @Override
    @Transactional
    public AiConversationVO currentConversation(LoginUser loginUser) {
        LoginContext context = requireLogin(loginUser);
        AiConversation conversation = findActiveConversation(context);
        if (conversation == null) {
            conversation = new AiConversation();
            conversation.setConversationId(newConversationId());
            conversation.setUserId(context.userId());
            conversation.setLoginSessionId(context.loginSessionId());
            conversation.setStatus(AiConversation.STATUS_ACTIVE);
            conversationMapper.insert(conversation);
        }
        return toConversationVO(conversation.getConversationId());
    }


    // 聊天
    @Override
    public Flux<String> chat(AiGuideRequest request, LoginUser loginUser, String authorization) {
        LoginContext context = requireLogin(loginUser);
        AiConversation conversation = requireActiveConversation(context, request.getConversationId());
        saveMessage(conversation.getConversationId(), context.userId(), AiMessage.ROLE_USER, request.getMessage());

        try {
            StringBuilder answer = new StringBuilder();
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(request.getMessage())
                    .tools(customerServiceTools.withAuthorization(authorization))
                    .stream()
                    .content()
                    .doOnNext(answer::append)
                    .doOnComplete(() -> saveAssistantMessage(conversation, context, answer.toString()))
                    .doOnError(e -> log.error("智能客服模型调用失败", e))
                    .onErrorResume(e -> {
                        saveAssistantMessage(conversation, context, AI_ERROR_MESSAGE);
                        return Flux.just(AI_ERROR_MESSAGE);
                    });
        } catch (Exception e) {
            log.error("智能客服模型调用失败", e);
            throw new BusinessException(AI_ERROR_MESSAGE);
        }
    }

    // 结束会话
    /**
     * 结束当前会话：将当前用户的有效会话状态置为无效，并记录失效时间。
     *
     * @param loginUser 当前登录用户信息
     */
    @Override
    @Transactional
    public void invalidateCurrentConversation(LoginUser loginUser) {
        // 1. 校验用户登录状态，获取用户ID和会话ID
        LoginContext context = requireLogin(loginUser);

        // 2. 构建更新条件：
        // - 匹配当前用户ID
        // - 匹配当前登录会话ID
        // - 仅处理状态为“活跃”的会话
        LambdaUpdateWrapper<AiConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AiConversation::getUserId, context.userId())
                .eq(AiConversation::getLoginSessionId, context.loginSessionId())
                .eq(AiConversation::getStatus, AiConversation.STATUS_ACTIVE)
                // 3. 设置更新字段：
                // - 将会话状态更新为“无效”
                // - 记录当前时间为失效时间
                .set(AiConversation::getStatus, AiConversation.STATUS_INVALID)
                .set(AiConversation::getInvalidTime, LocalDateTime.now());

        // 4. 执行数据库更新操作
        conversationMapper.update(null, updateWrapper);
    }


    /**
     * 查找当前用户的有效会话。
     * 根据用户ID、登录会话ID和活跃状态查询数据库，返回第一个匹配的会话记录。
     *
     * @param context 包含用户ID和登录会话ID的上下文对象
     * @return 活跃的会话实体，如果不存在则返回 null
     */
    private AiConversation findActiveConversation(LoginContext context) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, context.userId())
                .eq(AiConversation::getLoginSessionId, context.loginSessionId())
                .eq(AiConversation::getStatus, AiConversation.STATUS_ACTIVE)
                .last("LIMIT 1"));
    }

    /**
     * 获取并校验活跃的会话。
     * 确保传入的 conversationId 有效，且该会话属于当前用户、当前登录会话，并且状态为活跃。
     *
     * @param context       当前登录用户的上下文信息
     * @param conversationId 会话ID
     * @return 活跃的会话实体
     * @throws BusinessException 如果 conversationId 为空或会话不存在/无效
     */
    private AiConversation requireActiveConversation(LoginContext context, String conversationId) {
        // 1. 校验 conversationId 是否为空或空白字符串
        if (!StringUtils.hasText(conversationId)) {
            throw new BusinessException(400, "conversationId cannot be empty");
        }

        // 2. 查询数据库中的会话记录
        // 条件：
        // - 会话ID匹配
        // - 用户ID匹配（防止越权访问其他用户的会话）
        // - 登录会话ID匹配（确保是同一登录会话）
        // - 状态为活跃（STATUS_ACTIVE）
        AiConversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getConversationId, conversationId)
                .eq(AiConversation::getUserId, context.userId())
                .eq(AiConversation::getLoginSessionId, context.loginSessionId())
                .eq(AiConversation::getStatus, AiConversation.STATUS_ACTIVE)
                .last("LIMIT 1"));

        // 3. 如果未找到匹配的活跃会话，抛出异常
        if (conversation == null) {
            throw new BusinessException(403, "Current conversation is invalid, please reopen the customer service page");
        }

        // 4. 返回找到的会话对象
        return conversation;
    }

    private AiConversationVO toConversationVO(String conversationId) {
        List<AiMessageVO> messages = messageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreatedTime)
                        .orderByAsc(AiMessage::getId))
                .stream()
                .map(message -> new AiMessageVO(message.getRole(), message.getContent(), message.getCreatedTime()))
                .toList();
        return new AiConversationVO(conversationId, messages);
    }

    private void saveAssistantMessage(AiConversation conversation, LoginContext context, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        saveMessage(conversation.getConversationId(), context.userId(), AiMessage.ROLE_ASSISTANT, content);
        conversationMapper.update(null, new LambdaUpdateWrapper<AiConversation>()
                .eq(AiConversation::getId, conversation.getId())
                .set(AiConversation::getUpdatedTime, LocalDateTime.now()));
    }

    private void saveMessage(String conversationId, Long userId, String role, String content) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        messageMapper.insert(message);
    }

    // 登录状态校验
    private LoginContext requireLogin(LoginUser loginUser) {
        if (loginUser == null || loginUser.userId() == null || !StringUtils.hasText(loginUser.loginSessionId())) {
            throw new BusinessException(401, "Not logged in or login session is invalid");
        }
        return new LoginContext(loginUser.userId(), loginUser.loginSessionId());
    }

    private String newConversationId() {
        return "conv-" + UUID.randomUUID().toString().replace("-", "");
    }

    private record LoginContext(Long userId, String loginSessionId) {
    }
}
