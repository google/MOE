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

package com.google.devtools.moe.client.translation.editors;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.config.EditorConfig;
import com.google.devtools.moe.client.InvalidProject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The renaming editor reorganizes the project's hierarchy. */
@AutoFactory(implementing = Editor.Factory.class)
public class RenamingEditor implements Editor, InverseEditor {
  private static final CharMatcher FILE_SEP_CHAR_MATCHER = CharMatcher.is(File.separatorChar);
  private static final Joiner FILE_SEP_JOINER = Joiner.on(File.separator);
  private static final Splitter FILE_SEP_SPLITTER = Splitter.on(File.separator);
  private static final java.lang.reflect.Type MAP_TYPE =
      new TypeToken<Map<String, String>>() {}.getType();

  private final FileSystem filesystem;
  private final String editorName;
  private final Map<Pattern, String> regexMappings;
  private final boolean useRegex;

  RenamingEditor(
      @Provided FileSystem filesystem, @Provided Gson gson, String name, EditorConfig config) {
    this.filesystem = filesystem;
    this.editorName = name;
    if (config.mappings() == null) {
      throw new MoeProblem("No mappings object found in the config for editor %s", editorName);
    }
    regexMappings = mappingsFromConfig(gson, config);
    this.useRegex = config.useRegex();
  }

  /** Preprocesses the mappings from the given {@link EditorConfig}. */
  private static Map<Pattern, String> mappingsFromConfig(Gson gson, EditorConfig config) {
    Map<String, String> mappings = gson.fromJson(config.mappings(), MAP_TYPE);
    ImmutableMap.Builder<Pattern, String> regexMappingsBuilder = ImmutableMap.builder();
    for (String mapping : mappings.keySet()) {
      regexMappingsBuilder.put(
          Pattern.compile(config.useRegex() ? mapping : Pattern.quote(mapping)),
          mappings.get(mapping));
    }
    return regexMappingsBuilder.build();
  }

  /**
   * Returns a description of what this editor will do.
   */
  @Override
  public String getDescription() {
    return "rename step " + editorName;
  }

  @Override
  public InverseEditor validateInversion() throws InvalidProject {
    if (useRegex) {
      // TODO(cgruber): Work out how to soft-land this change.
      // throw new InvalidProject(
      //     "Editor type %s is not reversable if use_regex=true", EditorType.renamer);
    }
    return this;
  }

  /**
   * Recursively copies files from src to dest, changing the filenames as specified
   * in mappings.
   *
   * @param srcFile  the absolute path of a file to rename and copy or a dir to crawl
   * @param srcFolder  the absolute root of the from folder being crawled
   * @param destFolder  the absolute root of the to folder receiving renamed files
   */
  @VisibleForTesting
  void copyDirectoryAndRename(File srcFile, File srcFolder, File destFolder) throws IOException {
    if (filesystem.isDirectory(srcFile)) {
      File[] files = filesystem.listFiles(srcFile);
      for (File subFile : files) {
        copyDirectoryAndRename(subFile, srcFolder, destFolder);
      }
    } else {
      // "/srcFolder/path/to/file" -> "path/to/file"
      String relativePath = srcFolder.toURI().relativize(srcFile.toURI()).getPath();
      File renamedFile = new File(destFolder, renameFile(relativePath));
      filesystem.makeDirsForFile(renamedFile);
      filesystem.copyFile(srcFile, renamedFile);
    }
  }

  /**
   * Returns the filename according to the rules in mappings.
   *
   * @param inputFilename  the filename to be renamed, relative to the root of the codebase
   *
   * @return the new relative filename
   * @throws MoeProblem  if a mapping for inputFilename could not be found
   */
  String renameFile(String inputFilename) {
    for (Pattern searchExp : regexMappings.keySet()) {
      Matcher matcher = searchExp.matcher(inputFilename);
      if (matcher.find()) {
        String renamed = matcher.replaceFirst(regexMappings.get(searchExp));
        // Erase leading path separators, e.g. when the rule "dir" -> "" maps
        // "dir/filename.txt" to "/filename.txt".
        return FILE_SEP_CHAR_MATCHER.trimLeadingFrom(renamed);
      }
    }
    throw new MoeProblem(
        "Cannot find a rename mapping that covers file %s. "
            + "Every file needs an applicable renaming rule.",
        inputFilename);
  }

  /**
   * Copies the input Codebase's contents, renaming the files according to this.mappings and returns
   * a new Codebase with the results.
   *
   * @param input the Codebase to edit
   * @param options a map containing any command line options such as a specific revision
   */
  @Override
  public Codebase edit(Codebase input, Map<String, String> options) {

    File tempDir = filesystem.getTemporaryDirectory("rename_run_");
    try {
      copyDirectoryAndRename(
          input.root().getAbsoluteFile(),
          input.root().getAbsoluteFile(),
          tempDir.getAbsoluteFile());
    } catch (IOException e) {
      throw new MoeProblem(e, "Failed to copy %s to %s", input.root(), tempDir);
    }
    return Codebase.create(tempDir, input.projectSpace(), input.expression());
  }

  @Override
  public Codebase inverseEdit(
      Codebase input, Codebase referenceFrom, Codebase referenceTo, Map<String, String> options) {
    File tempDir = filesystem.getTemporaryDirectory("inverse_rename_run_");
    inverseRenameAndCopy(input, tempDir, referenceTo);
    return Codebase.create(tempDir, referenceTo.projectSpace(), referenceTo.expression());
  }

  private void inverseRenameAndCopy(Codebase input, File destination, Codebase reference) {
    Set<String> renamedFilenames =
        Utils.makeFilenamesRelative(filesystem.findFiles(input.root()), input.root());
    Map<String, String> renamedToReferenceMap =
        makeRenamedToReferenceMap(
            Utils.makeFilenamesRelative(filesystem.findFiles(reference.root()), reference.root()));

    for (String renamedFilename : renamedFilenames) {
      String inverseRenamedFilename = inverseRename(renamedFilename, renamedToReferenceMap);
      copyFile(renamedFilename, inverseRenamedFilename, input.root(), destination);
    }
  }

  private void copyFile(String inputFilename, String destFilename, File inputRoot, File destRoot) {
    File inputFile = new File(inputRoot, inputFilename);
    File destFile = new File(destRoot, destFilename);
    try {
      filesystem.makeDirsForFile(destFile);
      filesystem.copyFile(inputFile, destFile);
    } catch (IOException e) {
      throw new MoeProblem(e, "%s", e.getMessage());
    }
  }

  /**
   * Walks backwards through the dir prefixes of renamedFilename looking for a match in
   * renamedToReferenceMap.
   */
  private String inverseRename(String renamedFilename, Map<String, String> renamedToReferenceMap) {
    List<String> renamedAllParts = FILE_SEP_SPLITTER.splitToList(renamedFilename);
    for (int i = renamedAllParts.size(); i > 0; i--) {
      String renamedParts = FILE_SEP_JOINER.join(renamedAllParts.subList(0, i));
      String partsToSubstitute = renamedToReferenceMap.get(renamedParts);
      if (partsToSubstitute != null) {
        return renamedFilename.replace(renamedParts, partsToSubstitute);
      }
    }
    // No inverse renaming found.
    return renamedFilename;
  }

  /**
   * Returns mappings (renamed path, original/reference path) for all paths in the renamed/input
   * Codebase.
   */
  private Map<String, String> makeRenamedToReferenceMap(Set<String> referenceFilenames) {
    // Use a HashMap instead of ImmutableMap.Builder because we may put the same key (e.g. a
    // high-level dir) multiple times. We may want to complain if trying to put a new value for a
    // dir (i.e. if two different reference paths are renamed to the same path), but we don't now.
    HashMap<String, String> tmpPathMap = Maps.newHashMap();
    for (String refFilename : referenceFilenames) {

      String renamed = this.renameFile(refFilename);
      LinkedList<String> renamedPathParts = Lists.newLinkedList(FILE_SEP_SPLITTER.split(renamed));
      LinkedList<String> refPathParts = Lists.newLinkedList(FILE_SEP_SPLITTER.split(refFilename));

      // Put a mapping for each directory prefix of the renaming, stopping at the root of either
      // path. For example, a renaming a/b/c/file -> x/y/file creates mappings for each dir prefix:
      // - x/y/file -> a/b/c/file
      // - x/y -> a/b/c
      // - x -> a/b
      while (!renamedPathParts.isEmpty() && !refPathParts.isEmpty()) {
        tmpPathMap.put(FILE_SEP_JOINER.join(renamedPathParts), FILE_SEP_JOINER.join(refPathParts));
        renamedPathParts.removeLast();
        refPathParts.removeLast();
      }
    }
    return ImmutableMap.copyOf(tmpPathMap);
  }

}
