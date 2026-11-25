package com.hbys.survey.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hbys.survey.application.ai.AiSummaryGenerator;
import com.hbys.survey.application.ai.AiSummaryInput;
import com.hbys.survey.application.ai.AiSummaryResult;
import com.hbys.survey.application.exception.AIServiceException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "hbys.ai", name = "provider", havingValue = "openai")
public class OpenAiSummaryGenerator implements AiSummaryGenerator {

    private final RestClient restClient;
    private final AiProperties.OpenAi openAiProperties;
    private final AiSummaryPromptBuilder promptBuilder;

    public OpenAiSummaryGenerator(AiProperties aiProperties) {
        this.openAiProperties = aiProperties.getOpenai();
        this.promptBuilder = new AiSummaryPromptBuilder(
                openAiProperties.getMaxComments(),
                openAiProperties.getMaxCommentLength()
        );
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = (int) Duration.ofSeconds(Math.max(1, openAiProperties.getTimeoutSeconds())).toMillis();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(openAiProperties.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AiSummaryResult generate(AiSummaryInput input) {
        if (!StringUtils.hasText(openAiProperties.getApiKey())) {
            throw new AIServiceException("OpenAI API key is not configured.");
        }

        Map<String, Object> requestBody = Map.of(
                "model", openAiProperties.getModel(),
                "temperature", 0.2,
                "max_tokens", openAiProperties.getMaxTokens(),
                "messages", List.of(
                        Map.of("role", "system", "content", promptBuilder.systemPrompt()),
                        Map.of("role", "user", "content", promptBuilder.userPrompt(input))
                )
        );

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                    .body(requestBody)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().get(0).message() == null) {
                throw new AIServiceException("OpenAI response did not contain a summary choice.");
            }

            String sanitized = AiOutputSanitizer.sanitize(response.choices().get(0).message().content());
            String modelName = StringUtils.hasText(response.model())
                    ? response.model()
                    : openAiProperties.getModel();
            return new AiSummaryResult(sanitized, modelName);
        } catch (AIServiceException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new AIServiceException(
                    "OpenAI request failed with status " + exception.getStatusCode().value() + ".",
                    exception
            );
        } catch (RestClientException exception) {
            throw new AIServiceException("OpenAI request failed.", exception);
        } catch (IllegalStateException exception) {
            throw new AIServiceException(exception.getMessage(), exception);
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(
            String model,
            List<Choice> choices
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Message(String content) {
    }
}
