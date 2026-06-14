package com.agenticsdlc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AgentReport {

    public enum Severity { CRITICAL, WARNING, INFO }

    public record Issue(
        @JsonProperty("severity") Severity severity,
        @JsonProperty("category") String category,
        @JsonProperty("description") String description,
        @JsonProperty("suggestion") String suggestion
    ) {}

    public record AgentResult(
        @JsonProperty("agentName") String agentName,
        @JsonProperty("summary") String summary,
        @JsonProperty("issues") List<Issue> issues
    ) {}

    public record PipelineReport(
        @JsonProperty("file") String file,
        @JsonProperty("totalIssues") int totalIssues,
        @JsonProperty("criticalCount") long criticalCount,
        @JsonProperty("warningCount") long warningCount,
        @JsonProperty("infoCount") long infoCount,
        @JsonProperty("agentResults") List<AgentResult> agentResults
    ) {
        public static PipelineReport from(String file, List<AgentResult> results) {
            List<Issue> allIssues = results.stream()
                .flatMap(r -> r.issues().stream())
                .toList();
            return new PipelineReport(
                file,
                allIssues.size(),
                allIssues.stream().filter(i -> i.severity() == Severity.CRITICAL).count(),
                allIssues.stream().filter(i -> i.severity() == Severity.WARNING).count(),
                allIssues.stream().filter(i -> i.severity() == Severity.INFO).count(),
                results
            );
        }
    }
}
