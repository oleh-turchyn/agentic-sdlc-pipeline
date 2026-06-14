package com.agenticsdlc.agents;

import com.agenticsdlc.model.AgentReport;

public class TestGapAnalyzerAgent extends BaseAgent {

    @Override
    public AgentReport.AgentResult analyze(String javaCode, String openApiSpec) {
        log.info("[TestGapAnalyzerAgent] Analyzing test coverage gaps...");
        String prompt = buildPrompt(javaCode, openApiSpec);
        String rawResponse = callBedrock(prompt);
        AgentReport.AgentResult result = parseResponse(rawResponse);
        log.info("[TestGapAnalyzerAgent] Found {} test gaps", result.issues().size());
        return result;
    }

    @Override
    protected String getAgentName() {
        return "Test Gap Analyzer Agent";
    }

    @Override
    protected String buildPrompt(String javaCode, String openApiSpec) {
        return """
            Analyze the Java code and identify missing test scenarios.
            
            Look for untested cases:
            - Null input handling (null userId, null productIds, empty list)
            - Empty collections edge cases
            - Business logic branches (discount calculation threshold)
            - Error paths (product not found, payment failure)
            - Authorization edge cases (user accessing other user's orders)
            - Boundary conditions (very large orders, zero-price products)
            - Concurrent access scenarios
            - Missing integration test scenarios for each API endpoint
            - Each HTTP error code defined in OpenAPI (400, 404, 409, 422) needs a test
            
            For each gap, provide a JUnit 5 test method signature as the suggestion, like:
            @Test void shouldReturnBadRequestWhenUserIdIsNull()
            
            Java code:
            ```java
            %s
            ```
            
            OpenAPI spec (for endpoint-level test gaps):
            ```yaml
            %s
            ```
            """.formatted(javaCode, openApiSpec);
    }
}
