# Agentic SDLC Pipeline

A multi-agent code analysis pipeline built with Java 21 and AWS Bedrock (Claude Haiku).
Demonstrates agentic AI applied to Software Delivery — each agent owns a specific SDLC domain.

## Architecture

```
Java file + openapi.yaml
         │
         ▼
┌─────────────────────┐
│  PipelineOrchestrator│  ← coordinates all agents
└──────────┬──────────┘
           │
     ┌─────┴──────┐
     ▼            ▼            ▼
┌─────────┐ ┌──────────┐ ┌──────────┐
│ Agent 1 │ │ Agent 2  │ │ Agent 3  │
│  Code   │ │ OpenAPI  │ │  Test    │
│ Review  │ │Enforcer  │ │  Gap     │
└────┬────┘ └────┬─────┘ └────┬─────┘
     │           │             │
     └───────────┴─────────────┘
                 │
                 ▼
         PipelineReport
         (JSON output)
```

### Agents

| Agent | Responsibility | Severity levels |
|---|---|---|
| **CodeReviewAgent** | SOLID violations, null checks, god methods, unsafe casting | CRITICAL / WARNING / INFO |
| **OpenApiEnforcerAgent** | Missing endpoints, contract mismatches, wrong response types | CRITICAL / WARNING / INFO |
| **TestGapAnalyzerAgent** | Untested edge cases, missing error path tests, boundary conditions | WARNING / INFO |

Each agent calls **AWS Bedrock (Claude Haiku)** independently and returns structured JSON.

## Prerequisites

- Java 21+
- AWS account with Bedrock enabled in `us-east-1`
- Claude Haiku model access requested in Bedrock Model Access
- AWS credentials configured locally (`aws configure`)

## Setup

### 1. Enable Bedrock + Claude Haiku

1. AWS Console → Amazon Bedrock → Model access
2. Request access to **Claude Haiku** (`anthropic.claude-haiku-20240307-v1:0`)
3. Wait for approval (usually instant)

### 2. Configure AWS credentials

```bash
aws configure
# AWS Access Key ID: <your key>
# AWS Secret Access Key: <your secret>
# Default region: us-east-1
# Default output format: json
```

### 3. Build

```bash
./gradlew build
```

### 4. Run

```bash
# With sample input (default)
./gradlew run

# With custom files
./gradlew run --args="path/to/MyController.java path/to/openapi.yaml"
```

## Output

The pipeline produces a `pipeline-report.json` with all findings:

```json
{
  "file": "sample-input/OrderController.java",
  "totalIssues": 12,
  "criticalCount": 4,
  "warningCount": 6,
  "infoCount": 2,
  "agentResults": [
    {
      "agentName": "Code Review Agent",
      "summary": "...",
      "issues": [
        {
          "severity": "CRITICAL",
          "category": "SOLID - DIP Violation",
          "description": "OrderService is directly instantiated instead of injected",
          "suggestion": "Use constructor injection with an interface: private final OrderService orderService;"
        }
      ]
    }
  ]
}
```

## Cost estimate

Using Claude Haiku on Bedrock (~$0.00025 per 1K input tokens):
- One full pipeline run ≈ ~3,000–5,000 tokens total
- Cost per run: **< $0.002** (less than 0.2 cents)
- 100 runs during development: **~$0.20**

## Extending the pipeline

To add a new agent:
1. Create a class extending `BaseAgent`
2. Implement `getAgentName()`, `buildPrompt()`, `analyze()`
3. Add it to `PipelineOrchestrator.main()`

## Tech stack

- Java 21
- AWS SDK v2 (`bedrockruntime`)
- AWS Bedrock — Claude Haiku (`anthropic.claude-haiku-20240307-v1:0`)
- Jackson (JSON serialization)
- Gradle 8
- SLF4J + Logback
