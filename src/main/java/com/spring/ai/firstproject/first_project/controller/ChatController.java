package com.spring.ai.firstproject.first_project.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spring.ai.firstproject.first_project.config.AssistantPrompt;
import com.spring.ai.firstproject.first_project.tools.BranchTools;
import com.spring.ai.firstproject.first_project.tools.FileTools;
import com.spring.ai.firstproject.first_project.tools.PullTools;
import com.spring.ai.firstproject.first_project.tools.RepoTools;
import com.spring.ai.firstproject.first_project.tools.TagTools;

@RestController
@RequestMapping("/")
public class ChatController {
	
	private ChatClient chatClient;
	
	private final RepoTools repoTools;
	private final FileTools fileTools;
	private final BranchTools branchTools;
	private final PullTools pullTools;
	private final TagTools tagTools;
	
	private ChatController(ChatClient.Builder builder,PullTools pullTools,RepoTools repoTools,FileTools fileTools, BranchTools branchTools, TagTools tagTools) {
		this.chatClient=builder.build();
		this.repoTools=repoTools;
		this.fileTools=fileTools;
		this.branchTools=branchTools;
		this.pullTools=pullTools;
		this.tagTools=tagTools;
	}
	

	@PostMapping(value = "/chat", consumes = "text/plain")
	public ResponseEntity<String> chat(@RequestBody String q) {

	    var resultResponse = chatClient
	            .prompt(q)
	            .system(AssistantPrompt.SYSTEM_PROMPT)
	            .tools(repoTools, fileTools, branchTools, pullTools, tagTools)
	            .call()
	            .content();

	    return ResponseEntity.ok(resultResponse);
	}

}
