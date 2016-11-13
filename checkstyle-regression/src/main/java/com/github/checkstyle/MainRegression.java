package com.github.checkstyle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.maven.cli.MavenCli;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

public class MainRegression {
    private static final MavenCli MAVEN = new MavenCli();

    private static final List<String[]> projectsToTest = new ArrayList<String[]>();

    public static Repository gitRepository;
    public static Git git;

    private static boolean createdPrRemote;
    private static String prRemoteName;

    public static void main(String[] args) throws Exception {
        if ((args == null) || (args.length < 2)) {
            System.exit(1);
        }

        final String workingDirectory = Utils.getWorkingDirectory();

        System.out.println("Working directory: " + workingDirectory);

        loadProperties();

        final String repositoryLocation = args[0];
        final String userName = args[1];
        final String branchName = args[2];
        final File repository = new File(repositoryLocation);

        if (!repository.exists() || !repository.isDirectory()) {
            System.err.println("Couldn't find the repository at: " + repositoryLocation);
            System.exit(1);
        }

        gitRepository = new FileRepositoryBuilder().findGitDir(repository).build();
        git = new Git(gitRepository);

        // resetMaster();
        // install(repositoryLocation);
        regressCheckstyle("savemaster");

        resetPull(userName, branchName);
        install(repositoryLocation);
        regressCheckstyle("savepull");

        // createReport();
    }

    private static void loadProperties() throws Exception {
        try (BufferedReader br = new BufferedReader(
                new FileReader(Utils.getTesterDirectory() + "/projects-to-test-on.properties"))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.length() == 0)
                    continue;
                if (line.startsWith("#"))
                    continue;

                final String[] information = line.split("\\|");

                projectsToTest.add(Arrays.copyOf(information, 5));
            }
        }

        System.out.println("Loaded " + projectsToTest.size() + " projects to test on");
    }

    private static void resetMaster() throws Exception {
        System.out.println("Checking out Master");

        git.fetch().setRemote("origin").call();

        // reset current working branch
        final Ref newHead = git.reset().setMode(ResetType.HARD).setRef("HEAD").call();

        System.out.println("head reset to: " + newHead.toString());

        // checkout remote branch: master
        git.checkout().setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setName("origin/master").setStartPoint("origin/master").call();

        final Set<String> removed = git.clean().setCleanDirectories(true).setForce(true).call();
        for (String item : removed) {
            System.out.println("Removed: " + item);
        }
    }

    private static void resetPull(String userName, String branchName) throws Exception {
        setRemote(userName);

        System.out.println("Identified remote: " + prRemoteName);
        System.out.println("Checking out PR " + branchName + " for " + userName);

        git.fetch().setRemote(prRemoteName).call();

        final String remoteBranch = prRemoteName + "/" + branchName;
        final boolean branchExists = gitRepository.findRef(remoteBranch) != null;

        if (!branchExists) {
            System.err.println("Can't find " + branchName + " for " + prRemoteName + " remote");
            System.exit(1);
        }

        // checkout remote branch
        git.checkout().setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setName(remoteBranch).setStartPoint(remoteBranch).call();

        final Set<String> removed = git.clean().setCleanDirectories(true).setForce(true).call();
        for (String item : removed) {
            System.out.println("Removed: " + item);
        }
    }

    private static void setRemote(String userName) throws Exception {
        final List<RemoteConfig> remotes = git.remoteList().call();

        for (RemoteConfig remote : remotes) {
            System.out.println(remote.getName());

            for (URIish uris : remote.getURIs()) {
                if (uris.getPath().startsWith("/" + userName + "/checkstyle.git")) {
                    createdPrRemote = false;
                    prRemoteName = remote.getName();
                    return;
                }
                System.out.println(uris.getPath());
            }
        }

        // TODO: create temp remote
        throw new IllegalStateException("create temp remote");
    }

    private static void install(String checkstyleLocation) {
        System.out.println("Installing Checkstyle");

        int result = MAVEN.doMain(new String[] {
                "--batch-mode", //
                "clean", //
                "install", //
                "-Dmaven.test.skip=true", //
                "-Dcheckstyle.ant.skip=true", //
                "-Dcheckstyle.skip=true", //
                "-Dpmd.skip=true", //
                "-Dfindbugs.skip=true", //
                "-Dcobertura.skip=true", //
                "-Dforbiddenapis.skip=true", //
                "-Dxml.skip=true"
        }, checkstyleLocation, System.out, System.err);

        System.out.println("Install finished with: " + result);

        if (result != 0) {
            System.exit(result);
        }
    }

    private static void regressCheckstyle(String saveLocation) throws Exception {
        final File downloadDirectory = new File(Utils.getTesterDownloadsDirectory());
        final File sourceDirectory = new File(Utils.getTesterSrcDirectory());

        if (!downloadDirectory.exists() && !downloadDirectory.mkdirs()) {
            System.err.println("Failed to make download directory");
            System.exit(1);
        }

        if (!sourceDirectory.exists() && !sourceDirectory.mkdirs()) {
            System.err.println("Failed to make source directory");
            System.exit(1);
        }

        for (String[] project : projectsToTest) {
            final String projRepositoryName = project[0];
            final String projRepositoryType = project[1];
            final String projRepositoryUrl = project[2];
            final String projCommitId = project[3];
            final String projExcludes = project[4];

            // clear out source

            Utils.deleteFolderContents(sourceDirectory);

            // download/unpack files

            if ("github".equals(projRepositoryType)) {
                final String download = "https://api.github.com/repos/" + projRepositoryUrl
                        + "/tarball/" + projCommitId;
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
            else if ("git".equals(projRepositoryType)) {
                final File gitDirectory = new File(downloadDirectory,
                        projRepositoryUrl.replace("/", "-").replace(":", "_"));
                final Git projRepository;

                if (!gitDirectory.exists()) {
                    System.out.println("Cloning: " + projRepositoryUrl);

                    projRepository = Git.cloneRepository().setURI(projRepositoryUrl)
                            .setDirectory(gitDirectory)
                            .setBranchesToClone(Arrays.asList("refs/heads/master"))
                            .setBranch("refs/heads/master").call();
                }
                else {
                    projRepository = new Git(
                            new FileRepositoryBuilder().findGitDir(gitDirectory).build());
                }

                projRepository.fetch().setRemote("origin").call();

                projRepository.reset().setMode(ResetType.HARD)
                        .setRef(projCommitId == null ? "origin/master" : projCommitId).call();

                projRepository.clean().setCleanDirectories(true).setForce(true).call();

                // copy files

                // FileUtils.copyDirectory(gitDirectory, sourceDirectory,
                // "**/*.java", null);

                Utils.copyFolderContents(gitDirectory.toPath(), sourceDirectory.toPath());
            }
            else {
                throw new IllegalStateException("Unknown repository type: " + projRepositoryType);
            }

            // run checkstyle

            int result = MAVEN.doMain(new String[] {
                    "--batch-mode", //
                    "clean", //
                    "site", //
                    "-Dcheckstyle.excludes=" + projExcludes, //
                    "-Dcheckstyle.config.location=my_check.xml", //
                    "-DMAVEN_OPTS=-Xmx3024m"
            }, Utils.getTesterDirectory(), System.out, System.err);

            System.out.println("Checkstyle finished with: " + result);

            if (result != 0) {
                System.exit(result);
            }

            // TODO: save report
        }
    }
}
