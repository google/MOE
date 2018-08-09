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

package com.google.devtools.moe.client.testing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.tools.TarUtils;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Creates a codebase based upon a local existing directory. Primarily used for testing purposes.
 * Works with single files and directories.
 */
@Singleton
public class FileCodebaseCreator extends CodebaseCreator {
  private static final String PATH_OPTION = "path";
  private static final String PROJECT_SPACE_OPTION = "projectspace";

  private final FileSystem filesystem;
  private final TarUtils tarUtils;

  @Inject
  FileCodebaseCreator(FileSystem filesystem, TarUtils tarUtils) {
    this.filesystem = filesystem;
    this.tarUtils = tarUtils;
  }

  @Override
  public Codebase create(Map<String, String> options) throws CodebaseCreationError {
    // Validate that the options passed are valid.
    Utils.checkKeys(options, ImmutableSet.of(PATH_OPTION, PROJECT_SPACE_OPTION));

    // Get the source path to the file/directory described in the options list.
    String source = options.get(PATH_OPTION);
    if (Strings.isNullOrEmpty(source)) {
      throw new CodebaseCreationError(
          "Please specify the mandatory '%s' option for the FileCodebaseCreator", PATH_OPTION);
    }

    // Create the codebase instance.
    File codebasePath = getCodebasePath(new File(source));
    String projectSpace =
        options.containsKey(PROJECT_SPACE_OPTION) ? options.get(PROJECT_SPACE_OPTION) : "public";
    RepositoryExpression expression = new RepositoryExpression("file").withOptions(options);
    return Codebase.create(codebasePath, projectSpace, expression);
  }

  /**
   * Returns a folder reference to the codebase described by the source file. Will extract
   * .tar/.tar.gz automatically.
   *
   * @param sourceFile The file describing a codebase, or a directory.
   * @return A reference to a codebase directory
   * @throws CodebaseCreationError
   */
  @VisibleForTesting
  File getCodebasePath(File sourceFile) throws CodebaseCreationError {
    // Check whether the specified path is valid.
    if (!filesystem.exists(sourceFile)) {
      throw new CodebaseCreationError(
          "The specified codebase path \"%s\" does not exist.", sourceFile);
    }

    try {
      // Get the target path based upon whether we are dealing with a directory or a file.
      if (filesystem.isDirectory(sourceFile)) {
        // If it is a directory, make a copy and return the path of the copy.
        File destFile = filesystem.getTemporaryDirectory("file_codebase_copy_");
        filesystem.copyDirectory(sourceFile, destFile);
        return destFile;
      } else if (filesystem.isFile(sourceFile)) {
        // If it is a file, assume that it is an archive and try to extract it.
        File extractedArchive = expandToDirectory(sourceFile);
        if (extractedArchive != null) {
          return extractedArchive;
        }
      }
    } catch (CommandException exception) {
      throw new CodebaseCreationError(
          "Could not extract archive: '%s' %s", sourceFile, exception.getMessage());
    } catch (IOException exception) {
      throw new CodebaseCreationError(
          "Could not extract archive '%s': %s", sourceFile, exception.getMessage());
    }

    // If we did not return a codebase-path by now, we have no way of handling it.
    throw new CodebaseCreationError(
        "The '%s'-option of a FileCodebaseCreator must specify either a directory "
            + "or a .tar/.tar.gz-archive",
        PATH_OPTION);
  }

  /**
   * Expands the specified File to a new temporary directory, or returns null if the file type is
   * unsupported.
   *
   * @param inputFile The File to be extracted.
   * @return File pointing to a directory, or null.
   * @throws CommandException
   * @throws IOException
   */
  File expandToDirectory(File inputFile) throws IOException, CommandException {
    // If the specified path already is a directory, return it without modification.
    if (inputFile.isDirectory()) {
      return inputFile;
    }

    // Determine the file type by looking at the file extension.
    String lowerName = inputFile.getName().toLowerCase();
    if (lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tar")) {
      return tarUtils.expandTar(inputFile);
    }

    // If this file extension is unknown, return null.
    return null;
  }
}
