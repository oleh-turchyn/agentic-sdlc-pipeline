package com.agenticsdlc.agents;

import com.agenticsdlc.model.AgentReport;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class BaseAgent {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper = new ObjectMapper();

    private final AnthropicClient anthropicClient;

    protected BaseAgent() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY environment variable is not set");
        }
        this.anthropicClient = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .build();
    }

    /**
     * Each agent defines its own system prompt and analysis logic.
     */
    public abstract AgentReport.AgentResult analyze(String javaCode, String openApiSpec);

    protected abstract String getAgentName();

    protected abstract String buildPrompt(String javaCode, String openApiSpec);

    /**
     * Calls Anthropic API with Claude Haiku and returns raw text response.
     */
    protected String callBedrock(String prompt) {
        try {
            String systemPrompt = """
                You are an expert Java code reviewer working in an agentic SDLC pipeline.
                You MUST respond ONLY with valid JSON. No markdown, no explanation outside JSON.
                JSON structure:
                {
                  "summary": "brief summary of findings",
                  "issues": [
                    {
                      "severity": "CRITICAL|WARNING|INFO",
                      "category": "category name",
                      "description": "what the issue is",
                      "suggestion": "how to fix it"
                    }
                  ]
                }
                """;

            MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_HAIKU_4_5)
                .maxTokens(4096L)
                .system(systemPrompt)
                .addUserMessage(prompt)
                .build();

            Message message = anthropicClient.messages().create(params);
            String text = message.content().get(0).text().get().text();
            return text;

        } catch (Exception e) {
            log.error("Anthropic API call failed for agent {}: {}", getAgentName(), e.getMessage(), e);
            throw new RuntimeException("Anthropic API invocation failed", e);
        }
    }

    /**
     * Parses the JSON response from Claude into AgentResult.
     */
    protected AgentReport.AgentResult parseResponse(String rawJson) {
        try {
            String cleanJson = rawJson
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
            Map<?, ?> parsed = objectMapper.readValue(cleanJson, Map.class);
            String summary = (String) parsed.get("summary");
            List<?> issuesRaw = (List<?>) parsed.get("issues");

            List<AgentReport.Issue> issues = issuesRaw.stream()
                .map(i -> {
                    Map<?, ?> issue = (Map<?, ?>) i;
                    return new AgentReport.Issue(
                        AgentReport.Severity.valueOf((String) issue.get("severity")),
                        (String) issue.get("category"),
                        (String) issue.get("description"),
                        (String) issue.get("suggestion")
                    );
                })
                .toList();

            return new AgentReport.AgentResult(getAgentName(), summary, issues);

        } catch (Exception e) {
            log.error("Failed to parse response for agent {}: {}", getAgentName(), e.getMessage());
            log.error("Raw response was: {}", rawJson);
            throw new RuntimeException("Response parsing failed", e);
        }
    }
}
