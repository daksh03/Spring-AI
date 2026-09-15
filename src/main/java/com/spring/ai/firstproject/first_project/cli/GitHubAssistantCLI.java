package com.spring.ai.firstproject.first_project.cli;

import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.spring.ai.firstproject.first_project.advisor.ToolCallLoggingAdvisor;
import com.spring.ai.firstproject.first_project.tools.BranchTools;
import com.spring.ai.firstproject.first_project.tools.FileTools;
import com.spring.ai.firstproject.first_project.tools.PullTools;
import com.spring.ai.firstproject.first_project.tools.RepoTools;
import com.spring.ai.firstproject.first_project.tools.TagTools;

@Component
public class GitHubAssistantCLI implements CommandLineRunner {

	private final ChatClient chatClient;

	private final RepoTools repoTools;
	private final FileTools fileTools;
	private final BranchTools branchTools;
	private final PullTools pullTools;
	private final TagTools tagTools;

	public GitHubAssistantCLI(ChatClient.Builder builder, RepoTools repoTools, FileTools fileTools,
			BranchTools branchTools, PullTools pullTools, TagTools tagTools) {

		ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(20).build();

		this.chatClient = builder
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
						new ToolCallLoggingAdvisor())
				.build();

		this.repoTools = repoTools;
		this.fileTools = fileTools;
		this.branchTools = branchTools;
		this.pullTools = pullTools;
		this.tagTools = tagTools;
	}

	@Override
	public void run(String... args) throws Exception {

		String conversationId = "cli-session";

		try (Scanner scanner = new Scanner(System.in)) {

			printWelcome();

			while (true) {

				System.out.print("\n> ");

				String prompt = scanner.hasNextLine() ? scanner.nextLine().trim() : "";

				if (prompt.equalsIgnoreCase("exit") || prompt.equalsIgnoreCase("quit")) {

					System.out.println("Goodbye!");
					break;
				}

				if (prompt.equalsIgnoreCase("help")) {
					printHelp();
					continue;
				}

				if (prompt.isEmpty()) {
					continue;
				}

				try {

					System.out.println("\n⏳ Thinking...");

					ChatResponse chatResponse = chatClient.prompt().user(prompt)
							.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
							.tools(repoTools, fileTools, branchTools, pullTools, tagTools).call().chatResponse();

					String result = chatResponse.getResult().getOutput().getText();

					System.out.println("\n----- AI response -----");
					System.out.println(result);
					System.out.println("-----------------------");

					Usage usage = chatResponse.getMetadata().getUsage();

					if (usage != null) {

						System.out.println("\n----- Token usage -----");
						System.out.println("Input tokens: " + usage.getPromptTokens());
						System.out.println("Output tokens: " + usage.getCompletionTokens());
						System.out.println("Total tokens: " + usage.getTotalTokens());
						System.out.println("-----------------------");
					}

				} catch (Exception e) {

					System.out.println("Failed to get response from chat client: " + e.getMessage());

					e.printStackTrace(System.out);
				}
			}
		}
	}

	private void printWelcome() {

		System.out.println("=========================================");
		System.out.println(" GitHub Assistant CLI");
		System.out.println(" Type a natural-language GitHub request and press Enter.");
		System.out.println(" Type 'help' for commands, 'exit' or 'quit' to exit.");
		System.out.println(
				" Examples: 'List all repositories' or " + "'Create a branch called feature/test from main in repo1'");
		System.out.println("=========================================");
	}

	private void printHelp() {

		System.out.println();

		System.out.println("Commands:");
		System.out.println("  help   - Show this help");
		System.out.println("  exit   - Exit the assistant");
		System.out.println("  quit   - Exit the assistant");

		System.out.println();

		System.out.println("Tips:");
		System.out.println(" - Ask the assistant to perform repo, branch, file, pull, or tag operations.");
		System.out.println(" - You can ask follow-up questions about previous requests.");
	}
}