
package com.github.checkstyle.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.classworlds.ClassWorld;

import com.github.checkstyle.utils.TesterUtil.RunType;

public final class Utils {
    private Utils() {
    }

    public static final ClassWorld classWorld;

    static {
        classWorld = new ClassWorld("plexus.core", Utils.class.getClassLoader());
    }

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

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

    public static String getTesterTargetDirectory() throws IOException {
        return new File("../checkstyle-tester/target").getCanonicalPath();
    }

    public static String getTesterSiteDirectory() throws IOException {
        return new File("../checkstyle-tester/target/site").getCanonicalPath();
    }

    public static String getTesterDownloadsDirectory() throws IOException {
        return new File("../checkstyle-tester/downloads").getCanonicalPath();
    }

    public static String getSaveDirectory(RunType type) throws IOException {
        return new File("../checkstyle-tester/" + type.getFolderName()).getCanonicalPath();
    }

    public static String getSaveRefDirectory() throws IOException {
        return new File("../checkstyle-tester/saverefs").getCanonicalPath();
    }

    public static String getResultsDirectory() throws IOException {
        return new File("results").getCanonicalPath();
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
        final File[] files = folder.listFiles();

        if (files != null) {
            for (final File f : files) {
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

    public static void deleteFolderContents(Path source, final List<String> filesToKeep)
            throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (Collections.binarySearch(filesToKeep, source.relativize(file).toString()) < 0) {
                    Files.delete(file);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void copyFolderJavaContents(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                if (!dir.getFileName().toString().equals(".git")) {
                    Files.createDirectories(destination.resolve(source.relativize(dir)));

                    return FileVisitResult.CONTINUE;
                }

                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (file.getFileName().toString().endsWith(".java")) {
                    Files.copy(file, destination.resolve(source.relativize(file)));
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void moveFolderContents(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                if (!dir.getFileName().toString().equals(".git")) {
                    Files.createDirectories(destination.resolve(source.relativize(dir)));

                    return FileVisitResult.CONTINUE;
                }

                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.move(file, destination.resolve(source.relativize(file)));

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void replaceLinesInFile(File directory, String fileName, String find,
            String replace) throws IOException {
        final File temp = File.createTempFile("cs-regression", ".tmp");
        final File input = new File(directory, fileName);

        try (final BufferedReader reader = new BufferedReader(new FileReader(input));
                final BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
            String line;

            while ((line = reader.readLine()) != null) {
                writer.write(line.replace(find, replace));
                writer.write(LINE_SEPARATOR);
            }
        }

        if (!input.delete()) {
            System.err.println("Failed to delete file: " + input.getAbsolutePath());
            System.exit(1);
        }

        temp.renameTo(input);
    }
}
