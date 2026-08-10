package com.spring.ai.firstproject.first_project.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TagTools {

    private final RestClient restClient;

    public TagTools(@Value("${github.token}") String githubToken) {

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
            Creates a Git tag in a GitHub repository.

            repositoryName: name of the repository
            tagName: tag name such as v1.0.0
            branchName: branch or commit SHA that the tag should point to

            Use semantic versioning for release tags, for example:
            v1.0.0, v1.1.0, v2.0.0.
            """)
    public String createTag(
            String repositoryName,
            String tagName,
            String branchName) {

        System.out.println(
                "Creating tag " + tagName
                        + " on " + branchName
                        + " in " + repositoryName);

        try {

            // First get the commit SHA of the branch
            BranchResponse branch = restClient
                    .get()
                    .uri("/repos/migrationPOCAction/"
                            + repositoryName
                            + "/git/ref/heads/"
                            + branchName)
                    .retrieve()
                    .body(BranchResponse.class);

            if (branch == null
                    || branch.object() == null
                    || branch.object().sha() == null) {

                return "Failed to find branch: " + branchName;
            }

            String sha = branch.object().sha();

            // Create the Git reference
            CreateReferenceRequest request =
                    new CreateReferenceRequest(
                            "refs/tags/" + tagName,
                            sha);

            ReferenceResponse response = restClient
                    .post()
                    .uri("/repos/migrationPOCAction/"
                            + repositoryName
                            + "/git/refs")
                    .body(request)
                    .retrieve()
                    .body(ReferenceResponse.class);

            if (response == null) {
                return "Failed to create tag " + tagName;
            }

            return "Tag " + tagName
                    + " created successfully on "
                    + branchName
                    + " at commit "
                    + sha;

        } catch (Exception e) {

            return "Failed to create tag "
                    + tagName
                    + ": "
                    + e.getMessage();
        }
    }

    private record BranchResponse(
            GitObject object) {
    }

    private record GitObject(
            String sha) {
    }

    private record CreateReferenceRequest(
            String ref,
            String sha) {
    }

    private record ReferenceResponse(
            String ref,
            String url) {
    }
}