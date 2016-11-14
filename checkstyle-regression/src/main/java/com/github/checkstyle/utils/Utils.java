package com.github.checkstyle.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class Utils {
    private Utils() {
    }

    public static String getWorkingDirectory() throws IOException {
        final File directory = new File(new File(".").getCanonicalPath());

        return directory.getCanonicalPath();
    }

    public static String getTesterDirectory() throws IOException {
        return new File("../checkstyle-tester").getCanonicalPath();
    }

    public static String getTesterSrcDirectory() throws IOException {
        return new File("../checkstyle-tester/src/main/java").getCanonicalPath();
    }

    public static String getTesterDownloadsDirectory() throws IOException {
        return new File("../checkstyle-tester/downloads").getCanonicalPath();
    }

    public static void createFolder(File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            System.err.println("Failed to create folder: " + folder.getAbsolutePath());
            System.exit(1);
        }
    }

    public static void moveRenameFile(File sourceDirectory, String sourceFile,
            File destinationDirectory, String destinationFile) {
        new File(sourceDirectory, sourceFile)
                .renameTo(new File(destinationDirectory, destinationFile));
    }

    public static void deleteFolderContents(File folder) {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolderContents(f);

                    f.delete();
                }
                else {
                    f.delete();
                }
            }
        }

    }

    public static void copyFolderContents(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir,
                    final BasicFileAttributes attrs) throws IOException {
                if (!dir.getFileName().toString().equals(".git")) {
                    Files.createDirectories(destination.resolve(source.relativize(dir)));

                    return FileVisitResult.CONTINUE;
                }

                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                if (file.getFileName().toString().endsWith(".java")) {
                    Files.copy(file, destination.resolve(source.relativize(file)));
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }
}
