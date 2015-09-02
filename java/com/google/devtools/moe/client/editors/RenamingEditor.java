// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeModule;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.EditorConfig;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The renaming editor reorganizes the project's hierarchy.
 *
 */
public class RenamingEditor implements Editor {

  /** CharMatcher for trimming leading and trailing file path separators. */
  private static final CharMatcher SEP_CHAR_MATCHER = CharMatcher.is(File.separatorChar);

  private final FileSystem filesystem = Injector.INSTANCE.fileSystem(); // TODO(cgruber) @Inject

  private final String editorName;
  private final Map<Pattern, String> regexMappings;

  RenamingEditor(String editorName, Map<String, String> mappings, boolean useRegex) {
    this.editorName = editorName;

    ImmutableMap.Builder<Pattern, String> regexMappingsBuilder = ImmutableMap.builder();
    for (String mapping : mappings.keySet()) {
      regexMappingsBuilder.put(
          Pattern.compile(useRegex ? mapping : Pattern.quote(mapping)), mappings.get(mapping));
    }
    this.regexMappings = regexMappingsBuilder.build();
  }

  /**
   * Returns a description of what this editor will do.
   */
  @Override
  public String getDescription() {
    return "rename step " + editorName;
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
        return SEP_CHAR_MATCHER.trimLeadingFrom(renamed);
      }
    }
    throw new MoeProblem(
        "Cannot find a rename mapping that covers file %s. "
            + "Every file needs an applicable renaming rule.",
        inputFilename);
  }

  /**
   * Copies the input Codebase's contents, renaming the files according to this.mappings and
   * returns a new Codebase with the results.
   *
   * @param input the Codebase to edit
   * @param context the ProjectContext for this project
   * @param options a map containing any command line options such as a specific revision
   */
  @Override
  public Codebase edit(Codebase input, ProjectContext context, Map<String, String> options) {
    File tempDir = filesystem.getTemporaryDirectory("rename_run_");
    try {
      copyDirectoryAndRename(
          input.getPath().getAbsoluteFile(),
          input.getPath().getAbsoluteFile(),
          tempDir.getAbsoluteFile());
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }
    return new Codebase(filesystem, tempDir, input.getProjectSpace(), input.getExpression());
  }

  public static RenamingEditor makeRenamingEditor(String editorName, EditorConfig config) {
    if (config.getMappings() == null) {
      throw new MoeProblem("No mappings object found in the config for editor %s", editorName);
    }
    return new RenamingEditor(
        editorName, RenamingEditor.parseJsonMap(config.getMappings()), config.getUseRegex());
  }

  /**
   * Converts Json representing a map to a Java Map.
   *
   * @param jsonMappings  the JsonObject representing the renaming rules
   */
  static Map<String, String> parseJsonMap(JsonObject jsonMappings) {
    Type type = new TypeToken<Map<String, String>>() {}.getType();
    return MoeModule.provideGson().fromJson(jsonMappings, type);
  }
}
