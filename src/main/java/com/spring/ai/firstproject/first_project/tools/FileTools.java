package com.spring.ai.firstproject.first_project.tools;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FileTools {

	private final RestClient restClient;
	private final String organization;
	private final String committerName;
	private final String committerEmail;

	public FileTools(@Value("${github.token}") String githubToken, @Value("${github.organization}") String organization,
			@Value("${github.committer-name}") String committerName,
			@Value("${github.committer-email}") String committerEmail) {

		this.organization = organization;
		this.committerName = committerName;
		this.committerEmail = committerEmail;

		this.restClient = RestClient.builder().baseUrl("https://api.github.com")
				.defaultHeader("Accept", "application/vnd.github+json")
				.defaultHeader("Authorization", "Bearer " + githubToken)
				.defaultHeader("X-GitHub-Api-Version", "2026-03-10").build();
	}

	@Tool(description = """
			Creates a new file or updates an existing file in a GitHub repository.
			The file can be any text-based file such as txt, md, java, json, xml,
			yml, yaml, properties, csv, or similar.
			The content must be provided as plain text.
			""")
	public String createOrUpdateFile(String repositoryName, String branchName, String filePath, String content,
			String commitMessage) {

		try {

			String existingSha = getExistingFileSha(repositoryName, branchName, filePath);

			String encodedContent = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));

			GithubFileResponse response;

			if (existingSha == null) {

				response = restClient.put()
						.uri("/repos/{owner}/{repo}/contents/{path}", organization, repositoryName, filePath)
						.body(new FileRequest(commitMessage, encodedContent, branchName,
								new Committer(committerName, committerEmail), null))
						.retrieve().body(GithubFileResponse.class);

				return "File created successfully: " + repositoryName + "/" + filePath + " on branch " + branchName;

			} else {

				response = restClient.put()
						.uri("/repos/{owner}/{repo}/contents/{path}", organization, repositoryName, filePath)
						.body(new FileRequest(commitMessage, encodedContent, branchName,
								new Committer(committerName, committerEmail), existingSha))
						.retrieve().body(GithubFileResponse.class);

				return "File updated successfully: " + repositoryName + "/" + filePath + " on branch " + branchName;
			}

		} catch (Exception e) {

			return "Failed to create/update file: " + e.getMessage();
		}
	}

	private String getExistingFileSha(String repositoryName, String branchName, String filePath) {

		try {

			GithubFileResponse response = restClient.get()
					.uri(uriBuilder -> uriBuilder.path("/repos/{owner}/{repo}/contents/{path}")
							.queryParam("ref", branchName).build(organization, repositoryName, filePath))
					.retrieve().body(GithubFileResponse.class);

			return response != null ? response.sha() : null;

		} catch (Exception e) {

			// 404 means the file does not exist.
			return null;
		}
	}

	private record FileRequest(String message, String content, String branch, Committer committer, String sha) {
	}

	private record Committer(String name, String email) {
	}

	private record GithubFileResponse(String sha, String path) {
	}
}
