package com.spring.ai.firstproject.first_project.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BranchTools {

    private final RestClient restClient;
    private final String organization;

    public BranchTools(
            @Value("${github.token}") String githubToken,
            @Value("${github.organization}") String organization) {

        this.organization = organization;

        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("X-GitHub-Api-Version", "2026-03-10")
                .build();
    }

    @Tool(description = """
            Creates a new branch in a GitHub repository.
            The new branch is created from the specified source branch.
            """)
    public String createBranch(
            String repositoryName,
            String newBranchName,
            String sourceBranchName) {

        try {

            // 1. Get the SHA of the source branch
            BranchResponse sourceBranch = restClient
                    .get()
                    .uri(
                            "/repos/{owner}/{repo}/branches/{branch}",
                            organization,
                            repositoryName,
                            sourceBranchName)
                    .retrieve()
                    .body(BranchResponse.class);

            if (sourceBranch == null || sourceBranch.commit() == null) {
                return "Unable to find source branch: "
                        + sourceBranchName;
            }

            String sourceSha = sourceBranch.commit().sha();

            // 2. Create the new branch using that SHA
            restClient
                    .post()
                    .uri(
                            "/repos/{owner}/{repo}/git/refs",
                            organization,
                            repositoryName)
                    .body(new CreateReferenceRequest(
                            "refs/heads/" + newBranchName,
                            sourceSha))
                    .retrieve()
                    .toBodilessEntity();

            return "Branch '" + newBranchName
                    + "' created successfully from '"
                    + sourceBranchName + "'.";

        } catch (Exception e) {

            return "Failed to create branch: "
                    + e.getMessage();
        }
    }

    @Tool(description = "Lists all branches in a GitHub repository")
    public String listBranches(String repositoryName) {

        try {

            BranchResponse[] branches = restClient
                    .get()
                    .uri(
                            "/repos/{owner}/{repo}/branches",
                            organization,
                            repositoryName)
                    .retrieve()
                    .body(BranchResponse[].class);

            if (branches == null || branches.length == 0) {
                return "No branches found.";
            }

            StringBuilder result = new StringBuilder();

            for (BranchResponse branch : branches) {
                result.append(branch.name())
                        .append("\n");
            }

            return result.toString();

        } catch (Exception e) {

            return "Failed to list branches: "
                    + e.getMessage();
        }
    }

    @Tool(description = "Deletes a branch from a GitHub repository")
    public String deleteBranch(
            String repositoryName,
            String branchName) {

        try {

            restClient
                    .delete()
                    .uri(
                            "/repos/{owner}/{repo}/git/refs/heads/{branch}",
                            organization,
                            repositoryName,
                            branchName)
                    .retrieve()
                    .toBodilessEntity();

            return "Branch '" + branchName
                    + "' deleted successfully.";

        } catch (Exception e) {

            return "Failed to delete branch: "
                    + e.getMessage();
        }
    }

    private record CreateReferenceRequest(
            String ref,
            String sha) {
    }

    private record BranchResponse(
            String name,
            CommitResponse commit) {
    }

    private record CommitResponse(
            String sha) {
    }
}