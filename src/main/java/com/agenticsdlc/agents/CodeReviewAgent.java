package com.agenticsdlc.agents;

import com.agenticsdlc.model.AgentReport;

public class CodeReviewAgent extends BaseAgent {

    @Override
    public AgentReport.AgentResult analyze(String javaCode, String openApiSpec) {
        log.info("[CodeReviewAgent] Analyzing code quality...");
        String prompt = buildPrompt(javaCode, openApiSpec);
        String rawResponse = callBedrock(prompt);
        AgentReport.AgentResult result = parseResponse(rawResponse);
        log.info("[CodeReviewAgent] Found {} issues", result.issues().size());
        return result;
    }

    @Override
    protected String getAgentName() {
        return "Code Review Agent";
    }

    @Override
    protected String buildPrompt(String javaCode, String openApiSpec) {
        return """
            Analyze the following Java code for quality issues.
            Focus on:
            - SOLID principle violations (especially SRP, DIP)
            - Missing null checks and input validation
            - God methods (methods doing too many things)
            - Unsafe type casting
            - Missing error handling
            - Hardcoded business logic in wrong layers
            - Missing transaction management
            - Use of raw types instead of typed DTOs
            - Security concerns (authorization, data exposure)
            
            Java code to analyze:
            ```java
            %s
            ```
            
            Identify all issues with severity CRITICAL, WARNING, or INFO.
            """.formatted(javaCode);
    }
}
