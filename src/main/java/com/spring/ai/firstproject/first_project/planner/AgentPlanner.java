package com.spring.ai.firstproject.first_project.planner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AgentPlanner {

    private final ChatClient chatClient;

    public AgentPlanner(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public MigrationPlan createPlan(String userRequest) {

        String systemPrompt = """
                You are a GitHub migration test dataset planner.

                Your job is to convert the user's natural language request
                into a structured MigrationPlan.

                Do NOT execute any GitHub operations.

                Do NOT create repositories.
                Do NOT create files.
                Do NOT create branches.
                Do NOT create commits.
                Do NOT create pull requests.
                Do NOT create tags.

                Only create the execution plan.

                Planning rules:

                1. Identify every repository requested by the user.
                2. For every repository determine:
                   - repository name
                   - number of branches
                   - number of files
                   - number of commits
                   - branch names
                   - realistic file types
                   - number of pull requests
                   - release tag

                3. If the user specifies "at least" a number,
                   choose a concrete value that satisfies the requirement.

                4. For branches, prefer realistic names such as:
                   main
                   develop
                   feature/auth-v2
                   feature/redis-cache
                   bugfix/null-pointer-fix
                   release/v2.1.0

                5. For files, use realistic software-project file types
                   such as:
                   .java
                   .json
                   .yml
                   .properties
                   .xml
                   Dockerfile

                6. The plan must be internally consistent.

                7. Never invent repository names when the user has
                   explicitly provided repository names.

                Return only a structured MigrationPlan.
                """;

        return chatClient
                .prompt()
                .system(systemPrompt)
                .user(userRequest)
                .call()
                .entity(MigrationPlan.class);
    }
}