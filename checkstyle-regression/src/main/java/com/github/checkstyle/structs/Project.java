package com.github.checkstyle.structs;

import java.util.Arrays;

public class Project {
    protected final String repositoryName;
    protected final String repositoryType;
    protected final String repositoryUrl;
    protected final String commitId;
    protected final String excludes;

    public Project(String[] line) {
        if (line.length < 3) {
            throw new IllegalArgumentException("Line too short: " + Arrays.toString(line));
        }

        repositoryName = line[0];
        repositoryType = line[1];
        repositoryUrl = line[2];

        if (line.length < 4 || line[3].trim().isEmpty()) {
            commitId = null;
        }
        else {
            commitId = line[3];
        }

        if (line.length < 5 || line[4].trim().isEmpty()) {
            excludes = null;
        }
        else {
            excludes = line[4];
        }
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getRepositoryType() {
        return repositoryType;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getExcludes() {
        return excludes;
    }
}
