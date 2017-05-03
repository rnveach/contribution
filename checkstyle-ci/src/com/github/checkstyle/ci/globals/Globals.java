package com.github.checkstyle.ci.globals;

import java.io.File;

import com.github.checkstyle.ci.utils.EnvironmentUtil;

public final class Globals {
    private Globals() {
    }

    public static final String HOST = EnvironmentUtil.getEnvironmentVariable("HOST", "0.0.0.0");

    public static final int PORT = EnvironmentUtil.getEnvironmentVariable("PORT", 10000);

    public static final String PROJECT = EnvironmentUtil.getEnvironmentVariable("PROJECT",
            "checkstyle");

    public static final String REPOSITORY = EnvironmentUtil.getEnvironmentVariable("REPOSITORY",
            "https://github.com/checkstyle/checkstyle.git");

    public static final File WORKING_DIRECTORY = new File(EnvironmentUtil.getEnvironmentVariable(
            "WORKING_DIRECTORY", System.getProperty("user.dir")));

    public static final File CS_DIRECTORY = new File(EnvironmentUtil.getEnvironmentVariable(
            "CS_DIRECTORY", WORKING_DIRECTORY.getAbsolutePath() + File.separator + "checkstyle"));

    public static final File WORKER_DIRECTORY = new File(EnvironmentUtil.getEnvironmentVariable(
            "WORKER_DIRECTORY", WORKING_DIRECTORY.getAbsolutePath()));

    public static final File QUEUE_SAVE = new File(WORKING_DIRECTORY, "queue-save.dat");

    public static final int WORKER_COUNT = EnvironmentUtil
            .getEnvironmentVariable("WORKER_COUNT", 4);

    public static final String YAML_FILE_NAME = EnvironmentUtil.getEnvironmentVariable(
            "YAML_FILE_NAME", "wercker.yml");
}
