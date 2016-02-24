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

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Interface for MOE to interact with the local filesystem.
 */
public interface FileSystem {

  /**
   * Finds a Temporary Directory starting with prefix that lasts as long as the current task. This
   * is equivalent to calling {@link #getTemporaryDirectory(String, Lifetime)} with
   * {@link Lifetimes#currentTask()}.
   *
   * @param prefix a prefix for the basename of the created directory.
   * 
   * @return a path to an uncreated, available temporary directory.
   */
  // TODO(user): Delete all usages of this method in favor of the explicit form.
  public File getTemporaryDirectory(String prefix);

  /**
   * Finds a Temporary Directory starting with prefix, with the given lifetime.
   *
   * @param prefix a prefix for the basename of the created directory.
   * @param lifetime a {@code Lifetime} specifying the clean-up behavior of this temp dir.
   * 
   * @return a path to an uncreated, available temporary directory.
   */
  public File getTemporaryDirectory(String prefix, Lifetime lifetime);

  /**
   * Deletes files/directories created by {@link #getTemporaryDirectory(String, Lifetime)} whose
   * {@code Lifetime}s specify deletion at this juncture.
   *
   * @throws java.io.IOException if some error occurs while cleaning up the 
   * temporary directories.
   * 
   * @see #setLifetime(File, Lifetime)
   */
  public void cleanUpTempDirs() throws IOException;

  /**
   * Sets the {@link Lifetime} for a path. The path must have been provided by
   * {@link #getTemporaryDirectory(String, Lifetime)}.
   * 
   * @param path path of the lifetime.
   * @param lifetime new value for the lifetime.
   */
  public void setLifetime(File path, Lifetime lifetime);

  /**
   * Find the relative names of files under some path.
   * NB: returns only files, not directories
   * 
   * @param path path to find the files.
   * 
   * @return the set of files under {@code path}.
   */
  public Set<File> findFiles(File path);

  /**
   * Lists the files and directories under some path.
   * 
   * @param path path to get the files list.
   * 
   * @return an array of files.
   */
  // TODO(user): Return List instead of array.
  public File[] listFiles(File path);

  /**
   * Returns whether the file exists.
   * 
   * @param file file to be checked.
   * 
   * @return true if the file exists or false, otherwise.
   */
  public boolean exists(File file);

  /**
   * Gets the name of a file.
   * 
   * @param file file to get the name.
   * 
   * @return the name of the file.
   */
  public String getName(File file);

  /**
   * Checks if a file is a file.
   * 
   * @param file file to be checked.
   * 
   * @return true if the file is a file or false, otherwise.
   */
  public boolean isFile(File file);

  /**
   * Checks if a file is a directory.
   * 
   * @param file file to be checked.
   * 
   * @return true if the file is a directory or false, otherwise.
   */
  public boolean isDirectory(File file);

  /**
   * Checks if a file is executable.
   * 
   * @param file file to be checked.
   * 
   * @return true if the file is executable or false, otherwise.
   */
  public boolean isExecutable(File file);

  /**
   * Checks if a file is readable.
   * 
   * @param file file to be checked.
   * 
   * @return true if the file is readable or false, otherwise.
   */
  public boolean isReadable(File file);

  /**
   * Makes a file executable for all users.
   * 
   * @param file file to be set as executable.
   */
  public void setExecutable(File file);

  /**
   * Makes a file non-executable for all users.
   * 
   * @param file file to be set as non-executable.
   */
  public void setNonExecutable(File file);

  /**
   * Make the parent directory for file exist.
   * 
   * @param file parent directory to be created.
   * 
   * @throws java.io.IOException if some error occurs during the directory creation.
   */
  public void makeDirsForFile(File file) throws IOException;

  /**
   * Make the directory exist.
   * 
   * @param file directory to be created.
   * 
   * @throws java.io.IOException if some error occurs during the directory creation.
   */
  public void makeDirs(File file) throws IOException;

  /**
   * Copy the source file contents into destination file.
   * 
   * @param source source of the contents to be copied.
   * @param destination destination of the content.
   * 
   * @throws java.io.IOException if some error occurs while copping the content.
   */
  public void copyFile(File source, File destination) throws IOException;

  /**
   * Write contents to a file.
   * 
   * @param contents content to be written to the file.
   * @param file file where the content will be written.
   * 
   * @throws java.io.IOException if some error occurs while writing to the file.
   */
  public void write(String contents, File file) throws IOException;

  /**
   * Deletes a file or directory and all contents recursively.
   * 
   * @param file file to be deleted.
   * @throws java.io.IOException if some error occurs while deleting the file.
   * 
   */
  public void deleteRecursively(File file) throws IOException;

  /**
   * Extracts a resource into a file.
   *
   * @param resource  the name of the resource to extract
   *
   * @return a path to the resource in the file system
   * 
   * @throws java.io.IOException if some error occurs while getting the file.
   */
  public File getResourceAsFile(String resource) throws IOException;

  /**
   * Reads all characters from a file into a String.
   * 
   * @param file file to be read.
   * 
   * @return returns the characters from a file into a String.
   * @throws java.io.IOException
   */
  public String fileToString(File file) throws IOException;

}
