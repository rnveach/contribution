
package com.github.checkstyle.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.cli.MavenCli;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

public final class CheckstyleUtil {
    private CheckstyleUtil() {
    }

    private static final MavenCli MAVEN = new MavenCli(Utils.classWorld);

    public static Repository gitRepository;
    public static Git git;

    private static String prRemoteName;

    public static void init(File repository) throws IOException {
        gitRepository = new FileRepositoryBuilder().findGitDir(repository).build();
        git = new Git(gitRepository);
    }

    public static void resetMaster() throws Exception {
        System.out.println("Checking out Master");

        git.fetch().setRemote("origin").call();

        // reset current working branch

        final Ref newHead = git.reset().setMode(ResetType.HARD).setRef("HEAD").call();

        System.out.println("head reset to: " + newHead.toString());

        // checkout remote branch: master

        while (true) {
            try {
                git.checkout().setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setName("origin/master").setStartPoint("origin/master").call();
                break;
            }
            catch (final CheckoutConflictException ex) {
                // TODO
                // ex.getConflictingPaths();
                throw ex;
            }
        }

        displayHead();

        final Set<String> removed = git.clean().setCleanDirectories(true).setForce(true).call();
        for (final String item : removed) {
            System.out.println("Removed: " + item);
        }
    }

    public static void resetPull(String userName, String branchName) throws Exception {
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

        displayHead();

        final Set<String> removed = git.clean().setCleanDirectories(true).setForce(true).call();
        for (final String item : removed) {
            System.out.println("Removed: " + item);
        }
    }

    private static void displayHead() throws Exception {
        try (RevWalk walk = new RevWalk(gitRepository)) {
            final RevCommit commit = walk.parseCommit(gitRepository.findRef("HEAD").getObjectId());

            System.out
                    .println("Head at: " + commit.getId().name() + " " + commit.getShortMessage());
        }
    }

    private static void setRemote(String userName) throws Exception {
        final List<RemoteConfig> remotes = git.remoteList().call();

        for (final RemoteConfig remote : remotes) {
            for (final URIish uris : remote.getURIs()) {
                if (uris.getPath().startsWith("/" + userName + "/checkstyle.git")) {
                    // TODO: createdPrRemote = false;
                    prRemoteName = remote.getName();
                    return;
                }
            }
        }

        // TODO: create temp remote
        throw new IllegalStateException("create temp remote");
    }

    public static void install(String checkstyleLocation) {
        System.out.println("Installing Checkstyle");

        final int result = MAVEN.doMain(new String[] {
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
}
