package com.spring.ai.firstproject.first_project.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PullTools {

    private final RestClient restClient;

    public PullTools(@Value("${github.token}") String githubToken) {

        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(
                        "Accept",
                        "application/vnd.github+json")
                .defaultHeader(
                        "Authorization",
                        "Bearer " + githubToken)
                .defaultHeader(
                        "X-GitHub-Api-Version",
                        "2026-03-10")
                .build();
    }

    @Tool(description = """
            Creates a pull request in a GitHub repository.

            repositoryName: name of the repository
            sourceBranch: branch containing the changes
            targetBranch: branch that should receive the changes
            title: pull request title
            description: pull request description
            """)
    public String createPullRequest(
            String repositoryName,
            String sourceBranch,
            String targetBranch,
            String title,
            String description) {

        try {

            CreatePullRequestRequest request =
                    new CreatePullRequestRequest(
                            title,
                            description,
                            sourceBranch,
                            targetBranch);

            PullRequestResponse response = restClient
                    .post()
                    .uri("/repos/migrationPOCAction/"
                            + repositoryName
                            + "/pulls")
                    .body(request)
                    .retrieve()
                    .body(PullRequestResponse.class);

            if (response == null) {
                return "Failed to create pull request.";
            }

            return "Pull request created successfully. "
                    + "PR #" + response.number()
                    + " | URL: " + response.htmlUrl();

        } catch (Exception e) {

            return "Failed to create pull request: "
                    + e.getMessage();
        }
    }

    @Tool(description = """
            Merges an existing GitHub pull request using a merge commit.

            repositoryName: name of the repository
            pullRequestNumber: pull request number to merge

            The pull request is merged using GitHub's standard merge
            strategy so that a merge commit is created when possible.
            """)
    public String mergePullRequest(
            String repositoryName,
            int pullRequestNumber) {

        try {

            MergePullRequestRequest request =
                    new MergePullRequestRequest(
                            "merge");

            MergeResponse response = restClient
                    .put()
                    .uri("/repos/migrationPOCAction/"
                            + repositoryName
                            + "/pulls/"
                            + pullRequestNumber
                            + "/merge")
                    .body(request)
                    .retrieve()
                    .body(MergeResponse.class);

            if (response == null) {
                return "Failed to merge pull request.";
            }

            if (response.merged()) {

                return "Pull request #"
                        + pullRequestNumber
                        + " merged successfully. "
                        + "Merge commit: "
                        + response.sha();
            }

            return "Pull request was not merged: "
                    + response.message();

        } catch (Exception e) {

            return "Failed to merge pull request #"
                    + pullRequestNumber
                    + ": "
                    + e.getMessage();
        }
    }

    private record CreatePullRequestRequest(
            String title,
            String body,
            String head,
            String base) {
    }

    private record MergePullRequestRequest(
            String merge_method) {
    }

    private record PullRequestResponse(
            int number,
            String htmlUrl) {
    }

    private record MergeResponse(
            boolean merged,
            String message,
            String sha) {
    }
}