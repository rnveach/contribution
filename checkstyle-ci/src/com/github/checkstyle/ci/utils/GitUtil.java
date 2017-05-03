package com.github.checkstyle.ci.utils;

import java.io.File;

public final class GitUtil {
    private GitUtil() {
    }

    public static void clone(String repository, File folder) throws Exception {
        final Process p = Runtime.getRuntime().exec(
                "git clone " + repository + " " + folder.getCanonicalPath());
        final int result = p.waitFor();

        if (result != 0) {
            throw new IllegalStateException("Cloning remote failed with: " + result);
        }
    }

    public static boolean hasRemote(String remote, File workingDirectory) throws Exception {
        final Process p = Runtime.getRuntime().exec(
                "git remote show " + remote + " 2>&1 1>/dev/null", null, workingDirectory);

        return p.waitFor() == 0;
    }

    public static void addRemote(String remote, String remoteUrl, File workingDirectory)
            throws Exception {
        final Process p = Runtime.getRuntime().exec("git remote add " + remote + " " + remoteUrl,
                null, workingDirectory);

        final int result = p.waitFor();

        if (result != 0) {
            throw new IllegalStateException("Failed to create the remote for '" + remote + ": "
                    + result);
        }
    }

    public static void fetch(String remote, File workingDirectory) throws Exception {
        final Process p = Runtime.getRuntime().exec("git fetch " + remote, null, workingDirectory);

        final int result = p.waitFor();

        if (result != 0) {
            throw new IllegalStateException("Failed to fetch the remote for '" + remote + ": "
                    + result);
        }
    }

    public static void resetAndClean(String remote, String branch, File workingDirectory)
            throws Exception {
        final Process p = Runtime.getRuntime().exec(
                "git reset --hard " + remote + "/" + branch + " && git clean -f -d", null,
                workingDirectory);

        final int result = p.waitFor();

        if (result != 0) {
            throw new IllegalStateException("Failed to reset and clean the remote branch '"
                    + branch + "' for '" + remote + ": " + result);
        }
    }
}
