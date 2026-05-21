package com.ctgu.reviewbot.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 构建 LangChain4j ChatLanguageModel Bean。
 * <p>
 * 使用 OpenAiChatModel 接入所有兼容 OpenAI 格式的 AI 提供商。
 * </p>
 */
@Slf4j
@Configuration
public class AiModelConfig
{
    /**
     * 创建 ChatLanguageModel Bean，使用 OpenAiChatModel 连接 AI 服务
     */
    @Bean
    public ChatLanguageModel chatLanguageModel(AiConfig config)
    {
        log.info("Initializing AI model: provider={}, baseUrl={}, model={}", config.getProvider(), config.getBaseUrl(),
            config.getModel());
        return OpenAiChatModel.builder().baseUrl(config.getBaseUrl()).apiKey(config.getApiKey())
            .modelName(config.getModel()).maxTokens(config.getMaxTokens()).temperature(config.getTemperature())
            .topP(config.getTopP()).timeout(config.getTimeout()).build();
    }
}
