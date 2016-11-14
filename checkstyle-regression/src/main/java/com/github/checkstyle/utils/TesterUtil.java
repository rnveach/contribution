
package com.github.checkstyle.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

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

    private static final MavenCli MAVEN = new MavenCli();

    public static void init() throws IOException {
        final File downloadDirectory = new File(Utils.getTesterDownloadsDirectory());
        final File sourceDirectory = new File(Utils.getTesterSrcDirectory());
        final File saveRefDirectory = new File(Utils.getSaveRefDirectory());

        Utils.createFolder(downloadDirectory);
        Utils.createFolder(sourceDirectory);
        Utils.createFolder(saveRefDirectory);

        Utils.deleteFolderContents(saveRefDirectory);

        for (RunType type : RunType.values()) {
            final File saveLocation = new File(Utils.getSaveDirectory(type));

            Utils.createFolder(saveLocation);
            Utils.deleteFolderContents(saveLocation);
        }
    }

    public static void run(List<Project> projectsToTest, RunType saveLocation) throws Exception {
        for (Project project : projectsToTest) {
            setupSourceFolder(project);

            runCheckstyle(project.getExcludes());

            processResult(project.getRepositoryName(), saveLocation);
        }
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
                projRepository =
                        new Git(new FileRepositoryBuilder().findGitDir(gitDirectory).build());
            }

            // reset and copy

            projRepository.fetch().setRemote("origin").call();

            projRepository.reset().setMode(ResetType.HARD)
                    .setRef(project.getCommitId() == null ? "origin/master" : project.getCommitId())
                    .call();

            projRepository.clean().setCleanDirectories(true).setForce(true).call();

            Utils.copyFolderContents(gitDirectory.toPath(), sourceDirectory.toPath());
        }
        else {
            throw new IllegalStateException(
                    "Unknown repository type: " + project.getRepositoryType());
        }
    }

    private static void runCheckstyle(String excludes) throws IOException {
        final int result = MAVEN.doMain(new String[] {
            "--batch-mode", //
            "clean", //
            "site", //
            "-Dcheckstyle.excludes=" + excludes, //
            "-Dcheckstyle.config.location=my_check.xml", //
            // "-DMAVEN_OPTS=-Xmx3024m"
        }, Utils.getTesterDirectory(), System.out, System.err);

        System.out.println("Checkstyle finished with: " + result);

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
        final String contents = new String(Files
                .readAllBytes(new File(Utils.getTesterSiteDirectory() + "/index.html").toPath()));

        // TODO: minimize
    }

    private static void save(String repositoryName, RunType saveLocation) throws IOException {
        // TODO: change xml paths to save directory

        final File sourceDirectory = new File(Utils.getTesterSrcDirectory());
        final File targetDirectory = new File(Utils.getTesterTargetDirectory());
        final File saveDirectory =
                new File(Utils.getSaveDirectory(saveLocation) + "/" + repositoryName);
        final File saveRefDirectory = new File(Utils.getSaveRefDirectory() + "/" + repositoryName);

        Utils.createFolder(saveDirectory);
        Utils.moveFolderContents(targetDirectory.toPath(), saveDirectory.toPath());

        if (!saveRefDirectory.exists()) {
            Utils.createFolder(saveRefDirectory);
            Utils.moveFolderContents(sourceDirectory.toPath(), saveRefDirectory.toPath());
        }
    }
}
