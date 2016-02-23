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

package com.google.devtools.moe.client.editors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.EditorConfig;
import com.google.devtools.moe.client.project.ProjectContext;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An {@link InverseEditor} that undoes renaming. It takes a {@link RenamingEditor} and inverts its
 * renaming of the reference to-codebase given in {@link InverseEditor#inverseEdit}.
 *
 * <p>Given a renamed file in the input codebase, how is an inverse-renamed path determined? First,
 * a map of renamed paths to reference paths is built. Do this by, for each file in the reference
 * to-codebase, (1) renaming and (2) putting a mapping for each parent dir of the renamed path.
 * For example, if {@code internal/mylib/java/MyClass.java} in the reference to-codebase is renamed
 * to {@code public/src/MyClass.java} in the input codebase, a mapping is put for each of:
 * <ul>
 * <li>{@code public/src/MyClass.java} to {@code internal/mylib/java/MyClass.java}
 * <li>{@code public/src} to {@code internal/mylib/java}
 * <li>{@code public} to {@code internal/mylib}
 * </ul>
 *
 * <p>With this map built, a file is inverse-renamed by looking for any of its path prefixes (in
 * order of deepest to shallowest) in this map. With the above example,
 * {@code public/src/MyClass.java} is inverse-renamed to {@code internal/mylib/java/MyClass.java}
 * by the first mapping, and {@code public/src/NewClass.java} is renamed to
 * {@code internal/mylib/java/NewClass.java} by the second mapping. If no mapping is found, the
 * whole file path is used without modification.
 */
public class InverseRenamingEditor implements InverseEditor {

  private static final Joiner FILE_SEP_JOINER = Joiner.on(File.separator);
  private static final Splitter FILE_SEP_SPLITTER = Splitter.on(File.separator);

  private final FileSystem filesystem = Injector.INSTANCE.fileSystem(); // TODO(cgruber) @Inject

  public static InverseRenamingEditor makeInverseRenamingEditor(
      String editorName, EditorConfig config) {
    return new InverseRenamingEditor(RenamingEditor.makeRenamingEditor(editorName, config));
  }

  private final RenamingEditor renamer;

  @VisibleForTesting
  // TODO(user): Make tests use the config factory method above, and make this ctor private.
  InverseRenamingEditor(RenamingEditor renamer) {
    this.renamer = renamer;
  }

  @Override
  public Codebase inverseEdit(
      Codebase input,
      Codebase referenceFrom,
      Codebase referenceTo,
      ProjectContext context,
      Map<String, String> options) {
    File tempDir = filesystem.getTemporaryDirectory("inverse_rename_run_");
    inverseRenameAndCopy(input, tempDir, referenceTo);
    return new Codebase(
        filesystem, tempDir, referenceTo.getProjectSpace(), referenceTo.getExpression());
  }

  private void inverseRenameAndCopy(Codebase input, File destination, Codebase reference) {
    Set<String> renamedFilenames = input.getRelativeFilenames();
    Map<String, String> renamedToReferenceMap =
        makeRenamedToReferenceMap(reference.getRelativeFilenames());

    for (String renamedFilename : renamedFilenames) {
      String inverseRenamedFilename = inverseRename(renamedFilename, renamedToReferenceMap);
      copyFile(renamedFilename, inverseRenamedFilename, input.getPath(), destination);
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
   * Returns mappings (renamed path, original/reference path) for all paths in the renamed/input
   * Codebase.
   */
  private Map<String, String> makeRenamedToReferenceMap(Set<String> referenceFilenames) {
    // Use a HashMap instead of ImmutableMap.Builder because we may put the same key (e.g. a
    // high-level dir) multiple times. We may want to complain if trying to put a new value for a
    // dir (i.e. if two different reference paths are renamed to the same path), but we don't now.
    HashMap<String, String> tmpPathMap = Maps.newHashMap();
    for (String refFilename : referenceFilenames) {

      String renamed = renamer.renameFile(refFilename);
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
