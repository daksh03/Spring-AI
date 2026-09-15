package com.spring.ai.firstproject.first_project.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.core.Ordered;

public class ToolCallLoggingAdvisor implements CallAdvisor {

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

		System.out.println("\n========================================");
		System.out.println("🔄 Spring AI - Advisor iteration");
		System.out.println("========================================");

		ChatClientResponse response = chain.nextCall(request);

		System.out.println("✅ LLM response received");

		if (response.chatResponse() != null && response.chatResponse().getResults() != null) {

			response.chatResponse().getResults().forEach(result -> {

				if (result.getOutput() != null) {

					System.out.println("\n📤 Assistant message:");
					System.out.println(result.getOutput().getText());

					if (result.getOutput().getToolCalls() != null && !result.getOutput().getToolCalls().isEmpty()) {

						System.out.println("\n🔧 Tool calls requested:");

						result.getOutput().getToolCalls().forEach(toolCall -> {

							System.out.println("   Tool: " + toolCall.name());

							System.out.println("   Arguments: " + toolCall.arguments());
						});
					}
				}
			});
		}

		System.out.println("========================================");

		return response;
	}

	@Override
	public String getName() {
		return "ToolCallLoggingAdvisor";
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 400;
	}
}