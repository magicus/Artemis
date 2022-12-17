/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils;

import com.wynntils.core.WynntilsMod;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystemException;

public final class FileUtils {
    /**
     * Wraps File#mkdirs with a log output, in case of failure
     */
    public static void mkdir(File dir) {
        if (dir.isDirectory()) return;

        if (!dir.mkdirs()) {
            WynntilsMod.error("Directory " + dir + " could not be created");
        }
    }

    /**
     * Wraps File#mkdirs with a log output, in case of failure
     */
    public static void createNewFile(File file) {
        if (file.isFile()) {
            assert false;
            return;
        }

        try {
            if (!file.createNewFile()) {
                WynntilsMod.error("File " + file + " could not be created");
            }
        } catch (IOException e) {
            WynntilsMod.error("IOException while created File " + file);
        }
    }

    public static void deleteFile(File file) {
        if (!file.exists()) return;

        if (!file.delete()) {
            WynntilsMod.error("File " + file + " could not be deleted");
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (sourceFile == null || destFile == null) {
            throw new IllegalArgumentException("Argument files should not be null.");
        }

        try {
            org.apache.commons.io.FileUtils.copyFile(sourceFile, destFile);
        } catch (FileSystemException exception) {
            // Jar is locked on Windows, use streams
            copyFileWindows(sourceFile, destFile);
        }
    }

    private static void copyFileWindows(File sourceFile, File destFile) {
        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
                FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getMd5(File file) {
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("Argument file should not be null or a directory.");
        }

        MD5Verification verification = new MD5Verification(file);
        return verification.getMd5();
    }
}
