package com.spring.ai.firstproject.first_project.service;

import org.springframework.stereotype.Service;

import com.spring.ai.firstproject.first_project.executor.MigrationExecutor;
import com.spring.ai.firstproject.first_project.planner.AgentPlanner;
import com.spring.ai.firstproject.first_project.planner.MigrationPlan;

@Service
public class AgentService {

    private final AgentPlanner planner;
    private final MigrationExecutor executor;

    public AgentService(
            AgentPlanner planner,
            MigrationExecutor executor) {
        this.planner = planner;
        this.executor = executor;
    }

    public String execute(String request) {

        MigrationPlan plan =
                planner.createPlan(request);


        return executor.execute(plan);
    }
}
