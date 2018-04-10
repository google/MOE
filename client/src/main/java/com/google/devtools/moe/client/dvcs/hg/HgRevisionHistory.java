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

package com.google.devtools.moe.client.dvcs.hg;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.AbstractRevisionHistory;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.repositories.RevisionMetadata.FieldParsingResult;
import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * An Hg implementation of {@link AbstractRevisionHistory}.
 */
public class HgRevisionHistory extends AbstractRevisionHistory {
  private static final DateTimeFormatter HG_DATE_FMT =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm Z");

  private final Supplier<HgClonedRepository> tipCloneSupplier;
  private final CommandRunner runner;
  private final File hgBinary;

  HgRevisionHistory(
      CommandRunner runner, File hgBinary, Supplier<HgClonedRepository> tipCloneSupplier) {
    this.runner = runner;
    this.hgBinary = hgBinary;
    this.tipCloneSupplier = tipCloneSupplier;
  }

  /**
   * Confirms the existence of the given changeset ID via 'hg log', or pulls the most recent
   * changeset ID if none is given. Returns a Revision object corresponding to that changeset.
   *
   * NB: In Hg, revision numbers are local-only, i.e. not valid across clones of a repository. So
   * MOE deals only in full Hg changeset IDs (hex strings).
   */
  @Override
  public Revision findHighestRevision(@Nullable String revId) {
    ImmutableList.Builder<String> argsBuilder =
        ImmutableList.<String>builder()
            .add("log")
            .add("--branch=" + tipCloneSupplier.get().getBranch())
            // Ensure one revision only, to be safe.
            .add("--limit=1")
            // Format output as "changesetID" alone.
            .add("--template={node}");
    if (!Strings.isNullOrEmpty(revId)) {
      argsBuilder.add("--rev=" + revId);
    }
    List<String> args = argsBuilder.build();

    String changesetID;
    HgClonedRepository tipClone = tipCloneSupplier.get();
    try {
      changesetID =
          runner.runCommand(
              tipClone.getLocalTempDir().getAbsolutePath(),
              hgBinary.getPath(),
              ImmutableList.copyOf(args));
    } catch (CommandException e) {
      throw new MoeProblem(
          e, "Failed hg run: %s %d %s %s", args, e.returnStatus, e.stdout, e.stderr);
    }

    return Revision.create(changesetID, tipClone.getRepositoryName());
  }

  /**
   * Reads the metadata for a given revision in the same repository
   *
   * @param revision the revision to parse metadata for
   */
  @Override
  public RevisionMetadata createMetadata(Revision revision) {
    HgClonedRepository tipClone = tipCloneSupplier.get();
    if (!tipClone.getRepositoryName().equals(revision.repositoryName())) {
      throw new MoeProblem(
          "Could not get metadata: Revision %s is in repository %s instead of %s",
          revision.revId(),
          revision.repositoryName(),
          tipClone.getRepositoryName());
    }
    ImmutableList<String> args =
        ImmutableList.of(
            "log",
            "--rev=" + revision.revId(),
            // Ensure one revision only, to be safe.
            "--limit=1",
            // Format output as "changesetID < author < date < description < parents".
            // Since parents is a list, need to use stringify before applying another filter.
            "--template={node|escape} < {author|escape} < {date|isodate|escape} < "
                + "{desc|escape} < {parents|stringify|escape}",
            // Use the debug option to get all parents
            "--debug");
    String log;
    try {
      log =
          runner.runCommand(
              tipClone.getLocalTempDir().getAbsolutePath(),
              hgBinary.getPath(),
              ImmutableList.copyOf(args));

    } catch (CommandException e) {
      throw new MoeProblem(
          e, "Failed hg run: %s %d %s %s", args, e.returnStatus, e.stdout, e.stderr);
    }

    return parseMetadata(log);
  }

  private static final String BEGIN_LOG_PATTERN = "^(.*) < (.*) < (.*) < (.*) < (.*)$";
  private static final Pattern BEGIN_LOG_RE = Pattern.compile(BEGIN_LOG_PATTERN, Pattern.DOTALL);

  /**
   * Unescapes text that was escaped by hg log escaping.
   */
  private static String unescape(String text) {
    // TODO(user): Replace with a proper escaping utility.
    return text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
  }

  /**
   * Parses the output of Hg into RevisionMetadata
   *
   * @param log  a single log entry to parse. This is expected to be in the format given by
   *             the output of a call to getMetadata().
   */
  @VisibleForTesting
  RevisionMetadata parseMetadata(String log) {
    Matcher m = BEGIN_LOG_RE.matcher(log);
    if (!m.matches()) {
      throw new IllegalArgumentException("Tried to parse unexpected Hg log entry: " + log);
    }
    ImmutableList.Builder<Revision> parentBuilder = ImmutableList.<Revision>builder();
    // group 5 contains all of the parents, each separated by a space
    for (String parent : Splitter.on(' ').split(unescape(m.group(5)))) {
      if (!parent.isEmpty()) {
        // A parent is of the form revisionId:changesetId. When null, a parent is denoted by
        // revisionId of -1 and changesetId of 0000000000000000000000000000000000000000.
        String[] parentParts = parent.split(":");
        if (!parentParts[0].equals("-1")) {
          parent = parentParts[1];
          parentBuilder.add(Revision.create(parent, tipCloneSupplier.get().getRepositoryName()));
        }
      }
    }

    // TODO(cgruber): scan for fields from hg metadata rather than only the description?
    return RevisionMetadata.builder()
        .id(unescape(m.group(1)))
        .author(unescape(m.group(2)))
        .date(HG_DATE_FMT.parseDateTime(unescape(m.group(3))))
        .description(unescape(m.group(4)))
        .withParents(parentBuilder.build())
        .build();
  }

  @Override
  protected List<Revision> findHeadRevisions() {
    HgClonedRepository tipClone = tipCloneSupplier.get();

    String heads;
    try {
      heads =
          runner.runCommand(
              tipClone.getLocalTempDir().getAbsolutePath(),
              hgBinary.getPath(),
              // Format output as "changesetID branch".
              ImmutableList.of("heads", tipClone.getBranch(), "--template={node} {branch}\n"));
    } catch (CommandException e) {
      throw new MoeProblem(
          e, "Failed hg run: %s %d %s %s", e.args, e.returnStatus, e.stdout, e.stderr);
    }

    ImmutableList.Builder<Revision> result = ImmutableList.<Revision>builder();
    for (String changesetIDAndBranch : Splitter.on('\n').omitEmptyStrings().split(heads)) {
      String changesetID = changesetIDAndBranch.split(" ")[0];
      result.add(Revision.create(changesetID, tipClone.getRepositoryName()));
    }
    return result.build();
  }

  /** The tag parsing logic for hg commits. */
  @Override
  protected FieldParsingResult parseFields(RevisionMetadata metadata) {
    // TODO(cgruber) implement hg field parsing
    return RevisionMetadata.legacyFieldParser(metadata.description());
  }
}
