package com.spring.ai.firstproject.first_project.planner;

import java.util.List;

public record MigrationPlan(
        List<RepositoryPlan> repositories) {

    public record RepositoryPlan(
            String name,
            int branchCount,
            int fileCount,
            int commitCount,
            List<String> branches,
            List<String> fileTypes,
            int pullRequests,
            String releaseTag) {
    }
}