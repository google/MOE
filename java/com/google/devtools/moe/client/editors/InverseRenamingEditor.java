// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.EditorConfig;
import com.google.devtools.moe.client.project.ProjectContext;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>An Editor that undoes renaming. It takes a RenamingEditor and a "referenceToCodebase" edit()
 * option and undoes the action of the RenamingEditor on the reference codebase.
 *
 * <p>Given a renamed file in the input codebase, how is an inverse-renamed path determined? First,
 * a map of renamed paths -> reference paths is built. Do this by, for each file in the reference
 * codebase, renaming and putting a mapping for each parent dir of the renamed path. For example, if
 * {@code internal/mylib/java/MyClass.java} in the reference codebase is renamed to
 * {@code public/src/MyClass.java} in the input codebase, a mapping
 * is put for each of:
 * <ul>
 * <li> {@code public/src/MyClass.java} -> {@code internal/mylib/java/MyClass.java}
 * <li> {@code public/src} -> {@code internal/mylib/java}
 * <li> {@code public} -> {@code internal/mylib}
 * </ul>
 *
 * <p>With this map built, a file is inverse-renamed by looking for any of its path prefixes (in
 * order of deepest to shallowest) in this map. With the above example,
 * {@code public/src/MyClass.java} is inverse-renamed to {@code internal/mylib/java/MyClass.java}
 * by the first mapping, and {@code public/src/NewClass.java} is renamed to
 * {@code internal/mylib/java/NewClass.java} by the second mapping. If no mapping is found, the
 * whole file path is used without modification.
 *
 */
public class InverseRenamingEditor implements InverseEditor {

  private static final Joiner SEP_JOIN = Joiner.on(File.separator);
  private static final Splitter SEP_SPLIT = Splitter.on(File.separator);

  private final RenamingEditor renamer;

  InverseRenamingEditor(RenamingEditor renamer) {
    this.renamer = renamer;
  }

  @Override
  public Codebase inverseEdit(Codebase input, Codebase referenceFrom, Codebase referenceTo,
      ProjectContext context, Map<String, String> options) {
    File tempDir = AppContext.RUN.fileSystem.getTemporaryDirectory("inverse_rename_run_");
    inverseRenameAndCopy(input, tempDir, referenceTo);
    return new Codebase(tempDir, referenceTo.getProjectSpace(), referenceTo.getExpression());
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
   * Walk backwards through the dir prefixes of renamedFilename looking for a match in
   * renamedToReferenceMap.
   */
  private String inverseRename(String renamedFilename, Map<String, String> renamedToReferenceMap) {
    List<String> renamedAllParts = ImmutableList.copyOf(SEP_SPLIT.split(renamedFilename));
    for (int i = renamedAllParts.size(); i > 0; i--) {
      String renamedParts = SEP_JOIN.join(renamedAllParts.subList(0, i));
      String partsToSubstitute = renamedToReferenceMap.get(renamedParts);
      if (partsToSubstitute != null) {
        return renamedFilename.replace(renamedParts, partsToSubstitute);
      }
    }
    // No inverse renaming found.
    return renamedFilename;
  }

  private void copyFile(String inputFilename, String destFilename, File input, File destination) {
    try {
      File destFile = new File(destination, destFilename);
      AppContext.RUN.fileSystem.makeDirsForFile(destFile);
      AppContext.RUN.fileSystem.copyFile(new File(input, inputFilename), destFile);
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }
  }

  /**
   * Returns mappings (renamed path -> original/reference path) for all paths in the renamed/input
   * Codebase.
   */
  private Map<String, String> makeRenamedToReferenceMap(Set<String> referenceFilenames) {
    // Use a HashMap instead of ImmutableMap.Builder because we may put the same key (e.g. a
    // high-level dir) multiple times. We may want to complain if trying to put a new value for a
    // dir (i.e. if two different reference paths are renamed to the same path), but we don't now.
    HashMap<String, String> mapBuilder = Maps.newHashMap();
    for (String referenceFilename : referenceFilenames) {
      String renamed = renamer.renameFile(referenceFilename);
      List<String> renamedAllParts = ImmutableList.copyOf(SEP_SPLIT.split(renamed));
      List<String> referenceAllParts = ImmutableList.copyOf(SEP_SPLIT.split(referenceFilename));
      // Put a mapping for each directory prefix of the renaming, stopping at the root of either
      // path. For example, a renaming a/b/c/file -> x/y/file creates mappings for each dir prefix:
      // - x/y/file -> a/b/c/file
      // - x/y -> a/b/c
      // - x -> a/b
      for (int i = 0; i < Math.min(renamedAllParts.size(), referenceAllParts.size()); i++) {
        List<String> renamedParts = renamedAllParts.subList(0, renamedAllParts.size() - i);
        List<String> refParts = referenceAllParts.subList(0, referenceAllParts.size() - i);
        mapBuilder.put(SEP_JOIN.join(renamedParts), SEP_JOIN.join(refParts));
      }
    }
    return ImmutableMap.copyOf(mapBuilder);
  }

  public static InverseRenamingEditor makeInverseRenamingEditor(
      String editorName, EditorConfig config) {
    return new InverseRenamingEditor(RenamingEditor.makeRenamingEditor(editorName, config));
  }
}
