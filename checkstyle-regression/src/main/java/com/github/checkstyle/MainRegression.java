
package com.github.checkstyle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.impl.SimpleLogger;

import com.github.checkstyle.structs.Project;
import com.github.checkstyle.utils.CheckstyleUtil;
import com.github.checkstyle.utils.DiffReportUtil;
import com.github.checkstyle.utils.TesterUtil;
import com.github.checkstyle.utils.TesterUtil.RunType;
import com.github.checkstyle.utils.Utils;

public class MainRegression {
    public static void main(String[] args) throws Exception {
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "INFO");
        System.setProperty(SimpleLogger.SHOW_LOG_NAME_KEY, "false");

        if (args == null || args.length < 3) {
            System.err.println("Expecting 3 parameters: checkstyleLocation userName branchName");
            System.exit(1);
        }

        final String workingDirectory = Utils.getWorkingDirectory();

        System.out.println("Working directory: " + workingDirectory);

        final List<Project> projectsToTest = loadProperties();

        final String repositoryLocation = args[0];
        final String userName = args[1];
        final String branchName = args[2];
        final File repository = new File(repositoryLocation);

        if (!repository.exists() || !repository.isDirectory()) {
            System.err.println("Couldn't find the repository at: " + repositoryLocation);
            System.exit(1);
        }

        CheckstyleUtil.init(repository);
        TesterUtil.init();
        DiffReportUtil.init();

        // TODO
        // CheckstyleUtil.resetMaster();
        // CheckstyleUtil.install(repositoryLocation);

        TesterUtil.run(projectsToTest, RunType.MASTER);

        CheckstyleUtil.resetPull(userName, branchName);
        CheckstyleUtil.install(repositoryLocation);
        TesterUtil.run(projectsToTest, RunType.PULL);

        DiffReportUtil.run(projectsToTest);
    }

    private static List<Project> loadProperties() throws Exception {
        final List<Project> projectsToTest = new ArrayList<Project>();

        try (BufferedReader br = new BufferedReader(
                new FileReader(Utils.getTesterDirectory() + "/projects-to-test-on.properties"))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) {
                    continue;
                }

                final Project information = new Project(line.split("\\|"));

                projectsToTest.add(information);
            }
        }

        System.out.println("Loaded " + projectsToTest.size() + " projects to test on");

        return projectsToTest;
    }

}
