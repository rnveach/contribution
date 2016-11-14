package com.github.checkstyle.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import com.github.checkstyle.Main;
import com.github.checkstyle.structs.Project;

public final class DiffReportUtil {
    private DiffReportUtil() {
    }

    public static void run(List<Project> projectsToTest) throws Exception {
        final File workingDirectory = new File(Utils.getWorkingDirectory());
        final File resultsDirectory = new File(workingDirectory, "results");

        Utils.createFolder(resultsDirectory);
        Utils.deleteFolderContents(resultsDirectory);

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(resultsDirectory, "index.html")))) {
            writer.write("<html><body>");

            for (Project project : projectsToTest) {
                final String projectName = project.getRepositoryName();

                // TODO

                writer.write("<a href='" + projectName + "/index.html'>" + projectName + "</a>");

                // TODO: add counts

                writer.write("<br />");
            }

            writer.write("</body></html>");
        }

        // TODO Auto-generated method stub

        Main.main(null);
    }

}
