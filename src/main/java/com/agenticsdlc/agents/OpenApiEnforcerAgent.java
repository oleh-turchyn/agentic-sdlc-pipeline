package com.agenticsdlc.agents;

import com.agenticsdlc.model.AgentReport;

public class OpenApiEnforcerAgent extends BaseAgent {

    @Override
    public AgentReport.AgentResult analyze(String javaCode, String openApiSpec) {
        log.info("[OpenApiEnforcerAgent] Checking API contract compliance...");
        String prompt = buildPrompt(javaCode, openApiSpec);
        String rawResponse = callBedrock(prompt);
        AgentReport.AgentResult result = parseResponse(rawResponse);
        log.info("[OpenApiEnforcerAgent] Found {} compliance issues", result.issues().size());
        return result;
    }

    @Override
    protected String getAgentName() {
        return "OpenAPI Enforcer Agent";
    }

    @Override
    protected String buildPrompt(String javaCode, String openApiSpec) {
        return """
            Compare the Java controller implementation against the OpenAPI specification.
            
            Find all mismatches and missing implementations:
            - Endpoints defined in OpenAPI but missing in the Java code
            - Methods in Java that don't match the OpenAPI contract
            - Missing pagination support (page/size params defined in spec but not in code)
            - Missing response codes handling (404, 400, 409, 422 defined but not handled)
            - Wrong return types (raw Map vs typed DTO defined in spec)
            - Missing request validation for required fields
            - Missing path parameters that are defined in spec
            
            OpenAPI Specification:
            ```yaml
            %s
            ```
            
            Java Implementation:
            ```java
            %s
            ```
            
            Report every mismatch between spec and implementation.
            """.formatted(openApiSpec, javaCode);
    }
}
