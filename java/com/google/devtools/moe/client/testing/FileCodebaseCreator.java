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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.parser.Term;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Creates a codebase based upon a local existing directory. Primarily used for testing purposes.
 * Works with single files and directories.
 */
public class FileCodebaseCreator implements CodebaseCreator {
  private static final String PATH_OPTION = "path";
  private static final String PROJECT_SPACE_OPTION = "projectspace";

  private final FileSystem filesystem;

  public FileCodebaseCreator(FileSystem filesystem) {
    this.filesystem = filesystem;
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
    RepositoryExpression expression = new RepositoryExpression(new Term("file", options));
    return new Codebase(filesystem, codebasePath, projectSpace, expression);
  }

  /**
   * Returns a folder reference to the codebase described by the source file.
   * Will extract .tar/.tar.gz automatically.
   * @param sourceFile The file describing a codebase, or a directory.
   * @return A reference to a codebase directory
   * @throws CodebaseCreationError
   */
  public static File getCodebasePath(File sourceFile) throws CodebaseCreationError {
    // Check whether the specified path is valid.
    if (!Injector.INSTANCE.getFileSystem().exists(sourceFile)) {
      throw new CodebaseCreationError(
          "The specified codebase path \"%s\" does not exist.", sourceFile);
    }

    try {
      // Get the target path based upon whether we are dealing with a directory or a file.
      if (Injector.INSTANCE.getFileSystem().isDirectory(sourceFile)) {
        // If it is a directory, make a copy and return the path of the copy.
        File destFile = Injector.INSTANCE.getFileSystem().getTemporaryDirectory("file_codebase_copy_");
        Utils.copyDirectory(sourceFile, destFile);
        return destFile;
      } else if (Injector.INSTANCE.getFileSystem().isFile(sourceFile)) {
        // If it is a file, assume that it is an archive and try to extract it.
        File extractedArchive = Utils.expandToDirectory(sourceFile);
        if (extractedArchive != null) {
          return extractedArchive;
        }
      }
    } catch (CommandException exception) {
      throw new CodebaseCreationError(
          String.format("Could not extract archive: '%s' %s", sourceFile, exception.getMessage()));
    } catch (IOException exception) {
      throw new CodebaseCreationError(
          String.format("Could not extract archive '%s': %s", sourceFile, exception.getMessage()));
    }

    // If we did not return a codebase-path by now, we have no way of handling it.
    throw new CodebaseCreationError(
        String.format(
            "The '%s'-option of a FileCodebaseCreator must specify either a directory "
                + "or a .tar/.tar.gz-archive",
            PATH_OPTION));
  }
}
