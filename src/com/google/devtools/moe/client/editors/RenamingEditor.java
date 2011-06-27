// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.project.EditorConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * The renaming editor reorganizes the project's hierarchy.
 *
 */
public class RenamingEditor implements Editor {

  private final String editorName;
  private final Map<String, String> mappings;

  RenamingEditor(String editorName, Map<String, String> mappings) {
    this.editorName = editorName;
    this.mappings = mappings;
  }

  /**
   * Returns a description of what this editor will do.
   */
  @Override
  public String getDescription() {
    return String.format("rename step %s", editorName);
  }

  /**
   * Recursively copies files from src to dest, changing the filenames as specified
   * in mappings.
   */
  public static void copyDirectoryAndRename(File src, File dest, Map<String, String> mappings)
      throws IOException, CommandException {
    if (src == null) {
      return;
    }
    if (AppContext.RUN.fileSystem.isFile(src)) {
      String renamedFilename = RenamingEditor.renameFile(src.getAbsolutePath(), mappings);
      if (renamedFilename == null) {
        throw new MoeProblem(String.format(
            "Cannot find a rename mapping that covers file %s."
            + " Every file needs an applicable renaming rule.", src.getAbsolutePath()));
      }
      File renamedFile = new File(renamedFilename);
      AppContext.RUN.fileSystem.makeDirsForFile(renamedFile);
      AppContext.RUN.fileSystem.copyFile(src, renamedFile);
      return;
    }
    File[] files = AppContext.RUN.fileSystem.listFiles(src);
    if (files != null) {
      for (File subFile : files) {
        File newFile = new File(dest, AppContext.RUN.fileSystem.getName(subFile));
        if (AppContext.RUN.fileSystem.isDirectory(subFile)) {
          copyDirectoryAndRename(subFile, newFile, mappings);
        } else {
          String renamedFilename = RenamingEditor.renameFile(newFile.getAbsolutePath(), mappings);
          if (renamedFilename == null) {
            throw new MoeProblem(String.format(
                "Cannot find a rename mapping that covers file %s. "
                + "Every file needs an applicable renaming rule.", src.getAbsolutePath()));
          }
          File renamedFile = new File(renamedFilename);
          AppContext.RUN.fileSystem.makeDirsForFile(renamedFile);
          AppContext.RUN.fileSystem.copyFile(subFile, renamedFile);
        }
      }
    }
  }

  /**
   * Returns the filename according to the rules in mappings.
   *
   * @param inputFilename  the filename to be renamed
   * @param mappings  a map mapping the old filepath Strings to the new ones
   *
   * @return the new filename or null if no renaming took place
   */
  public static String renameFile(String inputFilename, Map<String, String> mappings) {
    for (String prefix : mappings.keySet()) {
      if (inputFilename.indexOf(prefix) != -1) {
        return inputFilename.replaceFirst(prefix, mappings.get(prefix));
      }
    }
    return null;
  }

  /**
   * Edits a Directory and returns the result.
   *
   * @param input  the directory to edit
   * @param options  command-line parameters
   */
  @Override
  public File edit(File input, Map<String, String> options) {
    File tempDir = AppContext.RUN.fileSystem.getTemporaryDirectory("rename_run_");
    try {
      RenamingEditor.copyDirectoryAndRename(input, tempDir, this.mappings);
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    } catch (CommandException e) {
      throw new MoeProblem(e.getMessage());
    }
    return tempDir;
  }

  public static RenamingEditor makeRenamingEditor(String editorName, EditorConfig config) {
    if (config.getMappings() == null) {
      throw new MoeProblem(String.format("No mappings object found in the config for editor %s",
          editorName));
    }
    return new RenamingEditor(editorName, RenamingEditor.parseJsonMap(config.getMappings()));
  }

  /**
   * Converts Json representing a map to a Java Map.
   *
   * @param jsonMappings  the JsonObject representing the renaming rules
   */
  public static Map<String, String> parseJsonMap(JsonObject jsonMappings) {
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    return new Gson().fromJson(jsonMappings, type);
  }
}
