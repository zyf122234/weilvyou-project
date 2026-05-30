package com.travel.user.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SentinelFlowRuleConfig {

    //限流方法名称
    private static final String USER_LOGIN_RESOURCE = "userLogin";

    @Value("${travel.sentinel.login-qps}")
    private double loginQps;

    // 创建限流切面
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    // 初始化流控规则  构建之前加载
    @PostConstruct
    public void initFlowRules() {
        FlowRule loginRule = new FlowRule();
        loginRule.setResource(USER_LOGIN_RESOURCE);
        loginRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        loginRule.setCount(loginQps);
//        FlowRuleManager.loadRules(List.of(loginRule));
    }
}
