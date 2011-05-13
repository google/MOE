// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.CommandRunner;

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
    for (File f: files) {
      if (!f.getAbsolutePath().startsWith(basePath.getAbsolutePath())) {
        throw new MoeProblem(
            String.format("File %s is under %s but does not begin with it", f, basePath));
      }
      result.add(f.getAbsolutePath().substring(basePath.getAbsolutePath().length()+1));
    }
    return ImmutableSet.copyOf(result);
  }

  public static File expandTar(File tar) throws IOException, CommandRunner.CommandException {
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
}
