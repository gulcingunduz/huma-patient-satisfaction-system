package com.hbys.survey.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hbys.ai")
public class AiProperties {

    /**
     * Active provider: mock | openai
     */
    private String provider = "mock";

    private final OpenAi openai = new OpenAi();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public static class OpenAi {

        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
        private int timeoutSeconds = 30;
        private int maxTokens = 500;
        private int maxComments = 50;
        private int maxCommentLength = 500;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getMaxComments() {
            return maxComments;
        }

        public void setMaxComments(int maxComments) {
            this.maxComments = maxComments;
        }

        public int getMaxCommentLength() {
            return maxCommentLength;
        }

        public void setMaxCommentLength(int maxCommentLength) {
            this.maxCommentLength = maxCommentLength;
        }
    }
}
