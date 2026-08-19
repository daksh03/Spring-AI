package com.spring.ai.firstproject.first_project.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class RepoTools {

	private final RestClient restClient;
	private final String organization;

	public RepoTools(@Value("${github.token}") String githubToken,
			@Value("${github.organization}") String organization) {

		this.organization = organization;

		this.restClient = RestClient.builder().baseUrl("https://api.github.com")
				.defaultHeader("Accept", "application/vnd.github+json")
				.defaultHeader("Authorization", "Bearer " + githubToken)
				.defaultHeader("X-GitHub-Api-Version", "2026-03-10").build();
	}

	@Tool(description = """
			Creates a GitHub repository in the configured organization.

			You must provide:
			- repositoryName: unique repository name
			- description: repository description
			- privateRepo: true for private repository, false for public repository

			Do not create a repository with a name that already exists.
			""")
	public String createRepository(String repositoryName, String description, boolean privateRepo) {

		System.out.println("Creating repo with name: " + repositoryName);

		try {
			GithubRepositoryResponse response = restClient.post().uri("/orgs/{organization}/repos", organization)
					.body(new CreateRepositoryRequest(repositoryName, description, privateRepo)).retrieve()
					.body(GithubRepositoryResponse.class);

			if (response == null) {
				return "Failed to create repository.";
			}

			return "Repo creation was successful: " + response.htmlUrl();

		} catch (Exception e) {
			return "Failed to create repository: " + e.getMessage();
		}
	}

	@Tool(description = """
			Lists repositories in the configured GitHub organization.

			For each repository, return the actual repository name,
			URL, and default branch reported by GitHub.

			Do not guess or infer the default branch.
			""")
	public String listRepositories() {

		try {
			List<GithubRepositoryResponse> repositories = restClient.get()
					.uri("/orgs/{organization}/repos", organization).retrieve()
					.body(new ParameterizedTypeReference<List<GithubRepositoryResponse>>() {
					});

			if (repositories == null || repositories.isEmpty()) {
				return "No repositories found.";
			}

			StringBuilder result = new StringBuilder();

			for (GithubRepositoryResponse repo : repositories) {
				result.append("Repository: ").append(repo.name()).append("\n");

				result.append("URL: ").append(repo.htmlUrl()).append("\n");

				result.append("Default branch: ").append(repo.defaultBranch()).append("\n\n");
			}

			return result.toString();

		} catch (Exception e) {
			return "Failed to list repositories: " + e.getMessage();
		}
	}

	@Tool(description = """
			Deletes a GitHub repository from the configured organization.

			IMPORTANT:
			Use this tool ONLY when the user explicitly asks to delete
			a specific repository.

			Never delete a repository merely because it already exists,
			because repository creation failed, or because another
			repository with the same name was requested.

			The repositoryName must be explicitly provided by the user.
			""")
	public String deleteRepository(String repositoryName) {

		System.out.println("Deleting repository: " + repositoryName);

		try {
			restClient.delete().uri("/repos/{organization}/{repositoryName}", organization, repositoryName).retrieve()
					.toBodilessEntity();

			return "Repository deleted successfully: " + organization + "/" + repositoryName;

		} catch (Exception e) {
			return "Failed to delete repository: " + e.getMessage();
		}
	}

	private record CreateRepositoryRequest(String name, String description,
			@JsonProperty("private") boolean privateRepo) {
	}

	private record GithubRepositoryResponse(String name, String htmlUrl,
			@JsonProperty("default_branch") String defaultBranch) {
	}
}