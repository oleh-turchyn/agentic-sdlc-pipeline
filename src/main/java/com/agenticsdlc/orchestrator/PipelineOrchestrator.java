package com.agenticsdlc.orchestrator;

import com.agenticsdlc.agents.CodeReviewAgent;
import com.agenticsdlc.agents.OpenApiEnforcerAgent;
import com.agenticsdlc.agents.TestGapAnalyzerAgent;
import com.agenticsdlc.model.AgentReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestrator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) throws Exception {
        String javaFilePath = args.length > 0 ? args[0] : "sample-input/OrderController.java";
        String openApiFilePath = args.length > 1 ? args[1] : "sample-input/openapi.yaml";

        log.info("Starting Agentic SDLC Pipeline");
        log.info("Java file:    {}", javaFilePath);
        log.info("OpenAPI spec: {}", openApiFilePath);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        String javaCode = Files.readString(Path.of(javaFilePath));
        String openApiSpec = Files.readString(Path.of(openApiFilePath));

        // Run agents in parallel using CompletableFuture
        ExecutorService executor = Executors.newFixedThreadPool(3);

        CompletableFuture<AgentReport.AgentResult> codeReviewFuture =
            CompletableFuture.supplyAsync(() -> new CodeReviewAgent().analyze(javaCode, openApiSpec), executor);

        CompletableFuture<AgentReport.AgentResult> apiComplianceFuture =
            CompletableFuture.supplyAsync(() -> new OpenApiEnforcerAgent().analyze(javaCode, openApiSpec), executor);

        CompletableFuture<AgentReport.AgentResult> testGapsFuture =
            CompletableFuture.supplyAsync(() -> new TestGapAnalyzerAgent().analyze(javaCode, openApiSpec), executor);

        List<AgentReport.AgentResult> results = List.of(
            codeReviewFuture.join(),
            apiComplianceFuture.join(),
            testGapsFuture.join()
        );
        executor.shutdown();

        // Aggregate into final report
        AgentReport.PipelineReport report = AgentReport.PipelineReport.from(
            javaFilePath,
            results
        );

        // Print final report
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("Pipeline complete!");
        log.info("Total issues: {} (Critical: {}, Warning: {}, Info: {})",
            report.totalIssues(), report.criticalCount(), report.warningCount(), report.infoCount());

        String reportJson = objectMapper.writeValueAsString(report);

        // Save to file
        Path outputPath = Paths.get("pipeline-report.json");
        Files.writeString(outputPath, reportJson);
        log.info("Full report saved to: {}", outputPath.toAbsolutePath());

        // Also print to console
        System.out.println("\n" + reportJson);
    }
}
