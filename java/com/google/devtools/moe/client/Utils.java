/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Random utilities and shared code.
 */
public class Utils {

  /**
   * Returns a Set that excludes strings matching any of excludeRes.
   * 
   * @param originalSet original set of strings to be filtered.
   * @param excludeRes regular expressions to be used as filter.
   * 
   * @return new Set of strings filtered.
   */
  public static Set<String> filterByRegEx(Set<String> originalSet, List<String> excludeRes) {
    return ImmutableSet.copyOf(Sets.filter(originalSet, nonMatchingPredicateFromRes(excludeRes)));
  }

  /** 
   * Creates a Predicate with the nonmatching regular expressions.
   * 
   * @param excludeRes regular expressions to match.
   * @return a Predicate that's true if a CharSequence doesn't match any of the 
   * given regular expressions.
   */
  public static Predicate<CharSequence> nonMatchingPredicateFromRes(List<String> excludeRes) {
    ImmutableList.Builder<Predicate<CharSequence>> rePredicateBuilder = ImmutableList.builder();
    for (String excludeRe : excludeRes) {
      rePredicateBuilder.add(Predicates.not(Predicates.containsPattern(excludeRe)));
    }
    return Predicates.and(rePredicateBuilder.build());
  }

  /**
   * Checks the keys passed into the options to MOE. If an invalid option is
   * found, a MoeProblem is thrown.
   * 
   * @param options options received to be checked.
   * @param allowedOptions allowed options.
   */
  public static void checkKeys(Map<String, String> options, Set<String> allowedOptions) {
    if (!allowedOptions.containsAll(options.keySet())) {
      throw new MoeProblem(
          "Options contains invalid keys:%nOptions: %s%nAllowed keys: %s", options, allowedOptions);
    }
  }
  
  /**
   * Makes the files under a path become relative.
   * 
   * @param files files to have the path converted.
   * @param basePath base path for the files.
   * @return set of strings containing the relative paths created.
   */
  public static Set<String> makeFilenamesRelative(Set<File> files, File basePath) {
    Set<String> result = Sets.newLinkedHashSet();
    for (File f : files) {
      if (!f.getAbsolutePath().startsWith(basePath.getAbsolutePath())) {
        throw new MoeProblem("File %s is under %s but does not begin with it", f, basePath);
      }
      result.add(f.getAbsolutePath().substring(basePath.getAbsolutePath().length() + 1));
    }
    return ImmutableSet.copyOf(result);
  }

  /**
   * Applies the given Function to all files under a base directory.
   * 
   * @param baseDirectory the base directory to have the files applied to the function.
   * @param doFunction function to be applied to the files.
   */
  public static void doToFiles(File baseDirectory, Function<File, Void> doFunction) {
    for (File file : Injector.INSTANCE.getFileSystem().findFiles(baseDirectory)) {
      doFunction.apply(file);
    }
  }

  /** 
   * Delete files under a base directory whose paths relative to base directory 
   * don't match the given Predicate.
   * 
   * @param baseDirectory the base directory containing the files.
   * @param positiveFilter predicate to be used as filter.
   */
  public static void filterFiles(File baseDirectory, final Predicate<CharSequence> positiveFilter) {
    final URI baseUri = baseDirectory.toURI();
    Utils.doToFiles(
        baseDirectory,
        new Function<File, Void>() {
          @Override
          public Void apply(File file) {
            if (!positiveFilter.apply(baseUri.relativize(file.toURI()).getPath())) {
              try {
                Injector.INSTANCE.getFileSystem().deleteRecursively(file);
              } catch (IOException e) {
                throw new MoeProblem("Error deleting file: " + file);
              }
            }
            return null;
          }
        });
  }

  /**
   * Expands the specified File to a new temporary directory.
   * 
   * @param inputFile The File to be extracted.
   * @return File pointing to a directory, or null if the file type is unsupported.
   * 
   * @throws CommandException if some error occurs while performing the command.
   * @throws IOException is some I/O error occurs.
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

  /**
   * Expands a tar file.
   * 
   * @param tar tar file to be expanded.
   * @return the expanded directory for the tar file.
   * @throws IOException if some error occurs while expanding the tar.
   * @throws CommandException if some error occurs while running the command.
   */
  public static File expandTar(File tar) throws IOException, CommandException {
    File expandedDir = Injector.INSTANCE.getFileSystem().getTemporaryDirectory("expanded_tar_");
    Injector.INSTANCE.getFileSystem().makeDirs(expandedDir);
    try {
      Injector.INSTANCE
          .getCommand()
          .runCommand(
              "tar", ImmutableList.of("-xf", tar.getAbsolutePath()), expandedDir.getAbsolutePath());
    } catch (CommandException e) {
      Injector.INSTANCE.getFileSystem().deleteRecursively(expandedDir);
      throw e;
    }
    return expandedDir;
  }

  /**
   * Copies a directory to another.
   * 
   * @param source source directory to be copied.
   * @param destination destination directory of the copy.
   * 
   * @throws IOException if some I/O error occurs while copying the directory.
   * @throws CommandException if some error occurs while performing the command.
   */
  public static void copyDirectory(File source, File destination) throws IOException, CommandException {
    if (source == null) {
      return;
    }
    Injector.INSTANCE.getFileSystem().makeDirsForFile(destination);
    if (Injector.INSTANCE.getFileSystem().isFile(source)) {
      Injector.INSTANCE.getFileSystem().copyFile(source, destination);
      return;
    }
    File[] files = Injector.INSTANCE.getFileSystem().listFiles(source);
    if (files != null) {
      for (File subFile : files) {
        File newFile = new File(destination, Injector.INSTANCE.getFileSystem().getName(subFile));
        if (Injector.INSTANCE.getFileSystem().isDirectory(subFile)) {
          copyDirectory(subFile, newFile);
        } else {
          Injector.INSTANCE.getFileSystem().makeDirsForFile(newFile);
          Injector.INSTANCE.getFileSystem().copyFile(subFile, newFile);
        }
      }
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
      Injector.INSTANCE.getFileSystem().write("#!/bin/sh -e\n" + content, script);
      Injector.INSTANCE.getFileSystem().setExecutable(script);
    } catch (IOException e) {
      throw new MoeProblem("Could not generate shell script: " + e);
    }
  }

  /**
   * A Gson parser used specifically for cloning Gson-ready objects
   */
  private static final Gson CLONER = new Gson();

  /**
   * Does a simple clone of a Gson-ready object, by marshalling into a Json intermediary and
   * processing into a new object.  This is not in any way efficient, but it guarantees the correct
   * cloning semantics. It should not be used in tight loops where performance is a concern.
   * 
   * @param <T> type of the object being cloned.
   * @param t object to be cloned.
   * @return object cloned.
   */
  @SuppressWarnings("unchecked")
  public static <T> T cloneGsonObject(T t) {
    return (T) CLONER.fromJson(CLONER.toJsonTree(t), t.getClass());
  }
}
