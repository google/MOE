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
   */
  public static Set<String> filterByRegEx(Set<String> c, List<String> excludeRes) {
    return ImmutableSet.copyOf(Sets.filter(c, nonMatchingPredicateFromRes(excludeRes)));
  }

  /** @return a Predicate that's true iff a CharSequence doesn't match any of the given regexes */
  public static Predicate<CharSequence> nonMatchingPredicateFromRes(List<String> excludeRes) {
    ImmutableList.Builder<Predicate<CharSequence>> rePredicateBuilder = ImmutableList.builder();
    for (String excludeRe : excludeRes) {
      rePredicateBuilder.add(Predicates.not(Predicates.containsPattern(excludeRe)));
    }
    return Predicates.and(rePredicateBuilder.build());
  }

  public static void checkKeys(Map<String, String> options, Set<String> allowedOptions) {
    if (!allowedOptions.containsAll(options.keySet())) {
      throw new MoeProblem(
          "Options contains invalid keys:%nOptions: %s%nAllowed keys: %s", options, allowedOptions);
    }
  }

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

  /** Applies the given Function to all files under baseDir. */
  public static void doToFiles(File baseDir, Function<File, Void> doFunction) {
    for (File file : Injector.INSTANCE.fileSystem().findFiles(baseDir)) {
      doFunction.apply(file);
    }
  }

  /** Delete files under baseDir whose paths relative to baseDir don't match the given Predicate. */
  public static void filterFiles(File baseDir, final Predicate<CharSequence> positiveFilter) {
    final URI baseUri = baseDir.toURI();
    Utils.doToFiles(
        baseDir,
        new Function<File, Void>() {
          @Override
          public Void apply(File file) {
            if (!positiveFilter.apply(baseUri.relativize(file.toURI()).getPath())) {
              try {
                Injector.INSTANCE.fileSystem().deleteRecursively(file);
              } catch (IOException e) {
                throw new MoeProblem("Error deleting file: " + file);
              }
            }
            return null;
          }
        });
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
    File expandedDir = Injector.INSTANCE.fileSystem().getTemporaryDirectory("expanded_tar_");
    Injector.INSTANCE.fileSystem().makeDirs(expandedDir);
    try {
      Injector.INSTANCE
          .cmd()
          .runCommand(
              "tar", ImmutableList.of("-xf", tar.getAbsolutePath()), expandedDir.getAbsolutePath());
    } catch (CommandException e) {
      Injector.INSTANCE.fileSystem().deleteRecursively(expandedDir);
      throw e;
    }
    return expandedDir;
  }

  public static void copyDirectory(File src, File dest) throws IOException, CommandException {
    if (src == null) {
      return;
    }
    Injector.INSTANCE.fileSystem().makeDirsForFile(dest);
    if (Injector.INSTANCE.fileSystem().isFile(src)) {
      Injector.INSTANCE.fileSystem().copyFile(src, dest);
      return;
    }
    File[] files = Injector.INSTANCE.fileSystem().listFiles(src);
    if (files != null) {
      for (File subFile : files) {
        File newFile = new File(dest, Injector.INSTANCE.fileSystem().getName(subFile));
        if (Injector.INSTANCE.fileSystem().isDirectory(subFile)) {
          copyDirectory(subFile, newFile);
        } else {
          Injector.INSTANCE.fileSystem().makeDirsForFile(newFile);
          Injector.INSTANCE.fileSystem().copyFile(subFile, newFile);
        }
      }
    }
    return;
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
      Injector.INSTANCE.fileSystem().write("#!/bin/sh -e\n" + content, script);
      Injector.INSTANCE.fileSystem().setExecutable(script);
    } catch (IOException e) {
      throw new MoeProblem("Could not generate shell script: " + e);
    }
  }

  /** A Gson parser used specifically for cloning Gson-ready objects */
  private static final Gson CLONER = new Gson();

  /**
   * Does a simple clone of a Gson-ready object, by marshalling into a Json intermediary and
   * processing into a new object.  This is not in any way efficient, but it guarantees the correct
   * cloning semantics.  It should not be used in tight loops where performance is a concern.
   */
  @SuppressWarnings("unchecked")
  public static <T> T cloneGsonObject(T t) {
    return (T) CLONER.fromJson(CLONER.toJsonTree(t), t.getClass());
  }
}
