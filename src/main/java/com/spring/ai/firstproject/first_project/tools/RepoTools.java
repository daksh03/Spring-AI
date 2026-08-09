package com.spring.ai.firstproject.first_project.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class RepoTools {

	private final RestClient restClient;

	public RepoTools(@Value("${github.token}") String githubToken) {
		this.restClient = RestClient.builder().baseUrl("https://api.github.com")
				.defaultHeader("Accept", "application/vnd.github+json")
				.defaultHeader("Authorization", "Bearer " + githubToken)
				.defaultHeader("X-GitHub-Api-Version", "2026-03-10").build();
	}

	@Tool(description = """
	        Creates a GitHub repository in the configured organization.

	        You must provide:
	        - repositoryName: the name of the repository
	        - description: the repository description
	        - privateRepo: whether the repository should be private

	        Always ask the user for the description and visibility if they
	        have not provided them.
	        """)
	public String createRepository(String repositoryName) {
		System.out.println("Creating repo with name" + repositoryName);

		try {
			GithubRepositoryResponse response = restClient.post().uri("/orgs/migrationPOCAction/repos")
					.body(new CreateRepositoryRequest(repositoryName, "Repository created by Agent", true)).retrieve()
					.body(GithubRepositoryResponse.class);

			if (null == response) {
				return "Failed to create";
			}

			return "Repo creation was successful: " + response.htmlUrl();

		} catch (Exception e) {
			return "Failed to create: " + e.getMessage();
		}
	}

	private record CreateRepositoryRequest(String name, String description,
			@JsonProperty("private") boolean privateRepo) {
	}

	private record GithubRepositoryResponse(String name, String htmlUrl) {

	}

}
