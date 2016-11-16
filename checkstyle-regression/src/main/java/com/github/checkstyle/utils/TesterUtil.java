
package com.github.checkstyle.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.cli.MavenCli;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.github.checkstyle.structs.Project;

public final class TesterUtil {
    private TesterUtil() {
    }

    public static enum RunType {
        MASTER, PULL;

        public String getFolderName() {
            switch (this) {
            case MASTER:
                return "savemaster";
            case PULL:
                return "savepull";
            default:
                return null;
            }
        }
    }

    private static final MavenCli MAVEN = new MavenCli(Utils.classWorld);

    public static void init() throws IOException {
        final File downloadDirectory = new File(Utils.getTesterDownloadsDirectory());
        final File sourceDirectory = new File(Utils.getTesterSrcDirectory());
        final File saveRefDirectory = new File(Utils.getSaveRefDirectory());

        Utils.createFolder(downloadDirectory);
        Utils.createFolder(sourceDirectory);
        Utils.createFolder(saveRefDirectory);

        Utils.deleteFolderContents(saveRefDirectory);

        for (final RunType type : RunType.values()) {
            final File saveLocation = new File(Utils.getSaveDirectory(type));

            Utils.createFolder(saveLocation);
            Utils.deleteFolderContents(saveLocation);
        }
    }

    public static void run(List<Project> projectsToTest, RunType saveLocation) throws Exception {
        System.out.println("Running Tester with " + saveLocation.name());

        for (final Project project : projectsToTest) {
            System.out.println("Running Tester on " + project.toString());

            setupSourceFolder(project);

            runTester(project.getExcludes());

            processResult(project.getRepositoryName(), saveLocation);
        }

        // clear out source from last run

        Utils.deleteFolderContents(new File(Utils.getTesterSrcDirectory()));
    }

    private static void setupSourceFolder(Project project) throws Exception {
        final File downloadDirectory = new File(Utils.getTesterDownloadsDirectory());
        final File sourceDirectory = new File(Utils.getTesterSrcDirectory());

        // clear out source

        Utils.deleteFolderContents(sourceDirectory);

        // download/unpack files

        if ("github".equals(project.getRepositoryType())) {
            final String download = "https://api.github.com/repos/" + project.getRepositoryUrl()
                    + "/tarball/" + project.getCommitId();
            final File tar = new File(downloadDirectory,
                    download.replace("/", "-").replace(":", "_") + ".tar.gz");

            if (!tar.exists()) {
                System.out.println("Downloading: " + download);

                FileUtils.copyURLToFile(new URL(download), tar);
            }

            // unzip and copy

            final TarGZipUnArchiver ua = new TarGZipUnArchiver();

            ua.enableLogging(new ConsoleLogger(Logger.LEVEL_DISABLED, "console"));
            ua.setSourceFile(tar);
            ua.setDestDirectory(sourceDirectory);
            ua.extract();
        }
        else if ("git".equals(project.getRepositoryType())) {
            final File gitDirectory = new File(downloadDirectory,
                    project.getRepositoryUrl().replace("/", "-").replace(":", "_"));
            final Git projRepository;

            if (!gitDirectory.exists()) {
                System.out.println("Cloning: " + project.getRepositoryUrl());

                projRepository = Git.cloneRepository().setURI(project.getRepositoryUrl())
                        .setDirectory(gitDirectory)
                        .setBranchesToClone(Arrays.asList("refs/heads/master"))
                        .setBranch("refs/heads/master").call();
            }
            else {
                projRepository = new Git(
                        new FileRepositoryBuilder().findGitDir(gitDirectory).build());
            }

            // reset and copy

            projRepository.fetch().setRemote("origin").call();

            projRepository.reset().setMode(ResetType.HARD)
                    .setRef(project.getCommitId() == null ? "origin/master" : project.getCommitId())
                    .call();

            projRepository.clean().setCleanDirectories(true).setForce(true).call();

            Utils.copyFolderJavaContents(gitDirectory.toPath(), sourceDirectory.toPath());
        }
        else {
            throw new IllegalStateException(
                    "Unknown repository type: " + project.getRepositoryType());
        }
    }

    private static void runTester(String excludes) throws IOException {
        final int result = MAVEN.doMain(new String[] {
                "--batch-mode", //
                "clean", //
                "site", //
                // the next line is needed because maven embeder uses the same
                // arguments as the previous run (Checkstyle install), which set
                // this value to 'true', but the value passes over to here and
                // tester requires it to be 'false'
                "-Dcheckstyle.skip=false", //
                "-Dcheckstyle.excludes=" + excludes, //
                "-Dcheckstyle.config.location=my_check.xml", //
                // "-DMAVEN_OPTS=-Xmx3024m"
        }, Utils.getTesterDirectory(), System.out, System.err);

        System.out.println("Tester finished with: " + result);

        if (result != 0) {
            System.exit(result);
        }
    }

    private static void processResult(String repositoryName, RunType saveLocation)
            throws IOException {
        final File siteDirectory = new File(Utils.getTesterSiteDirectory());

        Utils.moveRenameFile(siteDirectory, "index.html", siteDirectory, "_index.html");
        Utils.moveRenameFile(siteDirectory, "checkstyle.html", siteDirectory, "index.html");

        minimize();

        save(repositoryName, saveLocation);
    }

    private static void minimize() throws IOException {
        final List<String> allLinks = retrieveAllLinks(
                Utils.getTesterSiteDirectory() + "/index.html");

        final File xrefDirectory = new File(Utils.getTesterSiteDirectory() + "/xref");

        Utils.deleteFolderContents(xrefDirectory.toPath(), allLinks);

        // TODO: Utils.deletEmptyeFolderContents(xrefDirectory);
    }

    private static final Pattern LINK = Pattern.compile("<a href=\"./xref/([^#\"]+)#\\d+\">");

    private static List<String> retrieveAllLinks(String file) throws IOException {
        final List<String> result = new ArrayList<String>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = br.readLine()) != null) {
                final Matcher matcher = LINK.matcher(line);

                while (matcher.find()) {
                    final String text = matcher.group(1);
                    final int position = Collections.binarySearch(result, text);

                    if (position < 0) {
                        result.add(-1 - position, text);
                    }
                }
            }
        }

        // standard files that must stay

        result.add("allclasses-frame.html");
        result.add("index.html");
        result.add("overview-frame.html");
        result.add("overview-summary.html");

        return result;
    }

    private static void save(String repositoryName, RunType saveLocation) throws IOException {
        final File sourceDirectory = new File(Utils.getTesterSrcDirectory());
        final File targetDirectory = new File(Utils.getTesterTargetDirectory());
        final File saveDirectory = new File(
                Utils.getSaveDirectory(saveLocation) + "/" + repositoryName);
        final File saveRefDirectory = new File(Utils.getSaveRefDirectory() + "/" + repositoryName);

        Utils.replaceLinesInFile(targetDirectory, "checkstyle-result.xml",
                sourceDirectory.getCanonicalPath(), saveRefDirectory.getCanonicalPath());

        Utils.createFolder(saveDirectory);
        Utils.moveFolderContents(targetDirectory.toPath(), saveDirectory.toPath());

        if (!saveRefDirectory.exists()) {
            Utils.createFolder(saveRefDirectory);
            Utils.moveFolderContents(sourceDirectory.toPath(), saveRefDirectory.toPath());
        }
    }
}
