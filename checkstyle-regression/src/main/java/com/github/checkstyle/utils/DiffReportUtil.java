
package com.github.checkstyle.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.checkstyle.Main;
import com.github.checkstyle.structs.Project;
import com.github.checkstyle.utils.TesterUtil.RunType;

public final class DiffReportUtil {
    private DiffReportUtil() {
    }

    public static void init() throws IOException {
        final File resultsDirectory = new File(Utils.getResultsDirectory());

        Utils.createFolder(resultsDirectory);
        Utils.deleteFolderContents(resultsDirectory);
    }

    public static void run(List<Project> projectsToTest) throws Exception {
        final File resultsDirectory = new File(Utils.getResultsDirectory());

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(resultsDirectory, "index.html")))) {
            writer.write("<html><body>");

            for (final Project project : projectsToTest) {
                final String projectName = project.getRepositoryName();
                final String projectDirectory = "/" + projectName;
                final String resultFile = projectDirectory + "/checkstyle-result.xml";

                Main.main(new String[] {
                        "--baseReport", //
                        Utils.getSaveDirectory(RunType.MASTER) + resultFile, //
                        "--patchReport", //
                        Utils.getSaveDirectory(RunType.PULL) + resultFile, //
                        "--output", //
                        Utils.getResultsDirectory() + projectDirectory, //
                        "--baseConfig", //
                        Utils.getTesterDirectory() + "/my_check.xml", //
                        "--patchConfig", //
                        Utils.getTesterDirectory() + "/my_check.xml", //
                        "--refFiles", //
                        Utils.getSaveRefDirectory() //
                });

                writer.write("<a href='" + projectName + "/index.html'>" + projectName + "</a>");

                final long differences = getDifferenceCount(
                        new File(Utils.getResultsDirectory() + projectDirectory + "/index.html"));

                if (differences > 0) {
                    writer.write(" (" + differences + ")");
                }

                writer.write("<br />");
            }

            writer.write("</body></html>");
        }
    }

    private static final Pattern COUNT_LINE = Pattern.compile("totalDiff\">[0-9]+");

    private static long getDifferenceCount(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = br.readLine()) != null) {
                final Matcher matcher = COUNT_LINE.matcher(line);

                if (matcher.find()) {
                    final String text = matcher.group();

                    return Long.parseLong(text.substring(text.indexOf(">") + 1));
                }
            }
        }
        catch (IOException | NumberFormatException ex) {
            // ignore
        }

        return 0;
    }

}
