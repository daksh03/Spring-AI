
<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/4a40027e-4201-4984-a73c-dc3102ac6c35" />

# GitHub Assistant — Spring AI Agent

A Spring Boot application that lets you manage GitHub repositories using **plain English**, instead of Git commands or REST calls.

You type something like:

> "Create a branch called `feature/login` from `main` in `payments-service`"

...and the assistant figures out which GitHub operation to run, calls the GitHub API, and reports back what happened.

It's built with [Spring AI](https://spring.io/projects/spring-ai) and demonstrates a clean, practical **tool-calling agent**: an LLM that doesn't just chat, but takes real actions on a real system.

<img width="939" height="403" alt="image" src="https://github.com/user-attachments/assets/c7f8eb31-cc41-46e9-a220-1dc328d34274" />

---

## What it does

The assistant understands natural-language requests and can:

- List, create, and delete **repositories**
- Create, list, and delete **branches**
- Create or update **files** in a repository
- Open and **merge pull requests**
- Create **tags** (e.g. release tags)

It only performs actions it actually has a tool for. If you ask for something it can't do (like deleting a single file), it tells you plainly instead of guessing or running the wrong operation.

You can talk to it two ways:
- An **interactive CLI** (type requests in the terminal)
- A **REST API** (send requests from any HTTP client)

---

## Architecture

At a high level, every request flows through the same pipeline, whether it comes from the CLI or the REST API:

```mermaid
flowchart TD
    A["User<br/>(CLI or HTTP request)"] --> B["Spring AI ChatClient"]
    B --> C["LLM<br/>(via OpenRouter)"]
    C --> D{"Does the request<br/>need a GitHub action?"}
    D -- "No" --> E["Plain text answer"]
    D -- "Yes" --> F["Tool Selection<br/>(LLM picks the right tool + arguments)"]
    F --> G["GitHub Tools<br/>(Repo / Branch / File / Pull / Tag)"]
    G --> H["GitHub REST API"]
    H --> G
    G --> C
    C --> I["Final natural-language response"]
    E --> I
    I --> A
```

**In plain terms:** the LLM never talks to GitHub directly. It decides *what* needs to happen and *which tool* should do it. The tool is a regular Java method that calls the GitHub API and returns a plain-text result. The LLM then turns that result into a friendly response.

---

## How you interact with it

### 1. CLI (Command Line)

Run the app and a REPL-style prompt starts in your terminal. You type requests, the assistant responds, and it remembers the conversation so you can ask follow-up questions.

```
=========================================
 GitHub Assistant CLI
 Type a natural-language GitHub request and press Enter.
 Type 'help' for commands, 'exit' or 'quit' to exit.
 Examples: 'List all repositories' or 'Create a branch called feature/test from main in repo1'
=========================================

> List all repositories

⏳ Thinking...

----- AI response -----
Repository: payments-service
URL: https://github.com/your-org/payments-service
Default branch: main
-----------------------

----- Token usage -----
Input tokens: 812
Output tokens: 46
Total tokens: 858
-----------------------
```

Type `help` to see available commands, or `exit` / `quit` to leave.

<img width="545" height="385" alt="image" src="https://github.com/user-attachments/assets/568db59e-d2c4-410d-a151-33765fdf79ca" />


### 2. REST API

The same assistant is exposed as an HTTP endpoint, so it can be called from Postman, curl, another service, or a UI.

**Endpoint:** `POST /chat`
**Content-Type:** `text/plain`
**Body:** your request, as plain text

```bash
curl -X POST http://localhost:3142/chat \
  -H "Content-Type: text/plain" \
  -d "Create a repository called demo-service that is public"
```

The response is a plain-text natural-language answer describing what happened (e.g. the new repo's URL).

> Note: the REST endpoint currently handles each request independently — conversation memory (see below) is wired into the CLI, not yet into this endpoint.

---

## How the LLM and tools work together

This is the core idea of the project: the LLM never calls GitHub itself. It only chooses *which* tool to call and *with what arguments*. Spring AI executes the tool and feeds the result back to the model so it can produce a final answer.

```mermaid
sequenceDiagram
    participant U as User
    participant C as ChatClient
    participant L as LLM (OpenRouter)
    participant T as GitHub Tool (Java method)
    participant G as GitHub API

    U->>C: "Create a branch feature/x from main in repo1"
    C->>L: Prompt + system instructions + available tools
    L-->>C: "Call createBranch(repo1, feature/x, main)"
    C->>T: createBranch(...)
    T->>G: HTTP request to GitHub REST API
    G-->>T: Branch created (JSON)
    T-->>C: "Branch 'feature/x' created successfully from 'main'."
    C->>L: Tool result
    L-->>C: Final natural-language answer
    C-->>U: "✅ Branch created successfully."
```

Every tool method returns a simple, human-readable string — success or failure — so the model always knows exactly what really happened and never has to guess.



<img width="695" height="296" alt="image" src="https://github.com/user-attachments/assets/cbce91c2-d988-4c39-981d-1246681764a9" />


---

## Memory

The CLI keeps track of the ongoing conversation using Spring AI's **`MessageWindowChatMemory`**, capped at the last 20 messages. This means you can ask a follow-up question like *"now delete it"* and the assistant will know what "it" refers to, without you repeating the repository or branch name.

Each CLI session uses a fixed conversation ID, so memory persists for the lifetime of that CLI run.

<img width="519" height="157" alt="image" src="https://github.com/user-attachments/assets/a9c8d53c-2407-427a-966c-ad617384cad0" />

<img width="913" height="320" alt="image" src="https://github.com/user-attachments/assets/eb06dfec-148b-464a-9b82-9d8007056bba" />

<img width="844" height="117" alt="image" src="https://github.com/user-attachments/assets/171cf9c5-774e-4030-b0df-ec8795944226" />




---

## Advisors

Spring AI **advisors** are hooks that wrap around every call to the model. This project uses two:

| Advisor | Purpose |
|---|---|
| `MessageChatMemoryAdvisor` | Injects the conversation history into each request so the model has context (CLI only). |
| `ToolCallLoggingAdvisor` (custom) | Logs each request/response cycle to the console — which tool was called and with what arguments — useful for debugging and understanding the model's decisions. |

The custom advisor doesn't change the model's behavior; it's purely observational, printing the model's reasoning trail as the request happens.

---

## System prompt

The assistant is guided by a single, focused system prompt that keeps it honest and predictable:

- Only use a tool when the user actually asks for a GitHub operation.
- Never claim an action succeeded unless the tool confirms it.
- If there's no matching tool for a request, say so — don't silently substitute a different action (e.g. never delete a branch when asked to delete a file).

This keeps the assistant's behavior safe and explainable rather than "creative."

<img width="925" height="218" alt="image" src="https://github.com/user-attachments/assets/c1ceec1f-c7f7-447a-865e-e5ccb6b60d0d" />

<img width="920" height="338" alt="image" src="https://github.com/user-attachments/assets/42393130-7d82-42ce-acfb-f5af4a0a181d" />

---

## GitHub operations supported

| Area | Operation | Description |
|---|---|---|
| **Repositories** | Create | Creates a new repository in the configured organization |
| | List | Lists all repositories with their URL and default branch |
| | Delete | Deletes a repository (only when explicitly requested) |
| **Branches** | Create | Creates a new branch from a given source branch |
| | List | Lists all branches in a repository |
| | Delete | Deletes a branch |
| **Files** | Create / Update | Creates a new file, or updates it if it already exists, on a given branch |
| **Pull Requests** | Create | Opens a pull request from a source branch into a target branch |
| | Merge | Merges an existing pull request |
| **Tags** | Create | Creates a tag (e.g. a release tag) pointing at a branch/commit |

Operations that are **not** currently supported (like deleting a single file) are explicitly called out by the assistant instead of being faked.

---

## Part of a larger multi-agent system

This project is intentionally self-contained, which makes it a good building block for a bigger system. Because it's exposed both as tools (in-process) and as a REST endpoint, it can act as a **specialized "GitHub agent"** that a larger orchestrator delegates to:

```mermaid
flowchart LR
    subgraph Orchestrator["Orchestrator / Router Agent"]
        R["Decides which specialist<br/>should handle a task"]
    end

    R -->|"GitHub-related task"| GH["GitHub Assistant<br/>(this project)"]
    R -->|"e.g. Jira task"| J["Ticketing Agent"]
    R -->|"e.g. Slack task"| S["Messaging Agent"]

    GH --> GHAPI["GitHub API"]
    J --> JAPI["Ticketing System"]
    S --> SAPI["Chat Platform"]
```

In practice, this means the GitHub tool classes (`RepoTools`, `BranchTools`, `FileTools`, `PullTools`, `TagTools`) can be reused directly inside another Spring AI `ChatClient`, or this whole service can be called over HTTP as one agent among several in a bigger workflow — e.g. "review this PR, then update the changelog, then notify the team" spanning multiple specialized agents.


---
