// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.CommandRunner.CommandException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilties (all pure functions!) to make writing MOE easier.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Utils {

  /**
   * Returns a Set that excludes strings that match excludeRe.
   */
  public static Set<String> filterByRegEx(Collection<String> c, String excludeRe) {
    Pattern p = Pattern.compile(excludeRe);
    Set<String> result = Sets.newLinkedHashSet();
    for (String s : c) {
      if (p.matcher(s).matches()) {
        continue;
      }
      result.add(s);
    }
    return ImmutableSet.copyOf(result);
  }

  public static void checkKeys(Map<String, String> options, Set<String> allowedOptions) {
    if (!allowedOptions.containsAll(options.keySet())) {
      throw new MoeProblem(
          String.format(
              "Options contains invalid keys:\nOptions: %s\nAllowed keys: %s",
              options, allowedOptions));
    }
  }

  public static Set<String> makeFilenamesRelative(Set<File> files, File basePath) {
    Set<String> result = Sets.newLinkedHashSet();
    for (File f : files) {
      if (!f.getAbsolutePath().startsWith(basePath.getAbsolutePath())) {
        throw new MoeProblem(
            String.format("File %s is under %s but does not begin with it", f, basePath));
      }
      result.add(f.getAbsolutePath().substring(basePath.getAbsolutePath().length() + 1));
    }
    return ImmutableSet.copyOf(result);
  }

  /**
   * Expands the specified File to a new temporary directory, or returns null if the file
   * type is unsupported.
   * @param inputFile The File to be extracted.
   * @return File pointing to a directory, or null.
   * @throws CommandException
   * @throws IOException
   */
  public static File expandToDirectory(File inputFile) throws IOException, CommandException {
    // If the specified path already is a directory, return it without modification.
    if (inputFile.isDirectory()) {
      return inputFile;
    }

    // Determine the file type by looking at the file extension.
    String lowerName = inputFile.getName().toLowerCase();
    if (lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tar")) {
      return Utils.expandTar(inputFile);
    }

    // If this file extension is unknown, return null.
    return null;
  }

  public static File expandTar(File tar) throws IOException, CommandException {
    File expandedDir = AppContext.RUN.fileSystem.getTemporaryDirectory("expanded_tar_");
    AppContext.RUN.fileSystem.makeDirs(expandedDir);
    try {
      AppContext.RUN.cmd.runCommand(
          "tar",
          ImmutableList.of("-xf", tar.getAbsolutePath()),
          "", expandedDir.getAbsolutePath());
    } catch (CommandRunner.CommandException e) {
      AppContext.RUN.fileSystem.deleteRecursively(expandedDir);
      throw e;
    }
    return expandedDir;
  }

  public static void copyDirectory(File src, File dest)
      throws IOException, CommandException {
    if (src == null) {
      return;
    }
    AppContext.RUN.fileSystem.makeDirsForFile(dest);
    if (AppContext.RUN.fileSystem.isFile(src)) {
      AppContext.RUN.fileSystem.copyFile(src, dest);
      return;
    }
    File[] files = AppContext.RUN.fileSystem.listFiles(src);
    if (files != null) {
      for (File subFile : files) {
        File newFile = new File(dest,
                                AppContext.RUN.fileSystem.getName(subFile));
        if (AppContext.RUN.fileSystem.isDirectory(subFile)) {
          copyDirectory(subFile, newFile);
        } else {
          AppContext.RUN.fileSystem.makeDirsForFile(newFile);
          AppContext.RUN.fileSystem.copyFile(subFile, newFile);
        }
      }
    }
    return;
  }

  public static void checkOptionsEmpty(Map<String, String> options) {
    if (!options.isEmpty()) {
      throw new MoeProblem(
          "Options given where configuration must be done in config file");
    }
  }

  /**
   * Generates a shell script with contents content
   *
   * @param content contents of the script
   * @param name  path for the script
   */
  public static void makeShellScript(String content, String name) {
    try {
      File script = new File(name);
      AppContext.RUN.fileSystem.write("#!/bin/sh\n" + content, script);
      AppContext.RUN.fileSystem.setExecutable(script);
    } catch (IOException e) {
      throw new MoeProblem("Could not generate shell script: " + e);
    }
  }
}
