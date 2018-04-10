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

package com.google.devtools.moe.client.dvcs.git;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.AbstractRevisionHistory;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.repositories.RevisionMetadata.FieldParsingResult;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * A Git implementation of {@link AbstractRevisionHistory}.
 */
public class GitRevisionHistory extends AbstractRevisionHistory {

  // Like ISO 8601 format, but without the 'T' character
  private static final DateTimeFormatter GIT_DATE_FMT =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z");

  @VisibleForTesting static final String LOG_DELIMITER = "---@MOE@---";

  private final Supplier<GitClonedRepository> headCloneSupplier;

  GitRevisionHistory(Supplier<GitClonedRepository> headCloneSupplier) {
    this.headCloneSupplier = headCloneSupplier;
  }

  /**
   * Confirm the existence of the given hash ID via 'git log', or pull the most recent
   * hash ID if none is given.
   *
   * @param revId a revision ID (or the name of a branch)
   * @return a Revision corresponding to the given revId hash
   */
  @Override
  public Revision findHighestRevision(String revId) {
    if (Strings.isNullOrEmpty(revId)) {
      revId = "HEAD";
    }

    String hashID;
    GitClonedRepository headClone = headCloneSupplier.get();
    try {
      hashID = headClone.runGitCommand("log", "--max-count=1", "--format=%H", revId, "--").trim();
    } catch (CommandException e) {
      throw new MoeProblem(e, "Failed git log run: %d %s %s", e.returnStatus, e.stdout, e.stderr);
    }
    return Revision.create(hashID, headClone.getRepositoryName());
  }

  /**
   * Read the metadata for a given revision in the same repository.
   *
   * @param revision the revision to parse metadata for
   */
  @Override
  public RevisionMetadata createMetadata(Revision revision) {
    GitClonedRepository headClone = headCloneSupplier.get();
    if (!headClone.getRepositoryName().equals(revision.repositoryName())) {
      throw new MoeProblem(
          "Could not get metadata: Revision %s is in repository %s instead of %s",
          revision.revId(),
          revision.repositoryName(),
          headClone.getRepositoryName());
    }

    // Format: hash, author, ISO date, parents, full commit message (subject and body)
    String format = Joiner.on(LOG_DELIMITER).join("%H", "%an", "%ai", "%P", "%B");

    String log;
    try {
      log =
          headClone.runGitCommand(
              "log",
              // Ensure one revision only, to be safe.
              "--max-count=1",
              "--format=" + format,
              "--ignore-missing",
              revision.revId());
    } catch (CommandException e) {
      throw new MoeProblem("Failed git run: %d %s %s", e.returnStatus, e.stdout, e.stderr);
    }

    return parseMetadata(log);
  }

  /**
   * Parse the output of Git into RevisionMetadata.
   *
   * @param log  the output of getMetadata to parse
   */
  @VisibleForTesting
  RevisionMetadata parseMetadata(String log) {
    if (Strings.isNullOrEmpty(log.trim())) {
      return null;
    }
    // Split on the log delimiter. Limit to 5 so that it will act correctly
    // even if the log delimiter happens to be in the commit message.
    List<String> split = Splitter.on(LOG_DELIMITER).limit(5).splitToList(log);

    // The fourth item contains all of the parents, each separated by a space.
    ImmutableList.Builder<Revision> parentBuilder = ImmutableList.<Revision>builder();
    for (String parent : Splitter.on(' ').omitEmptyStrings().split(split.get(3))) {
      parentBuilder.add(Revision.create(parent, headCloneSupplier.get().getRepositoryName()));
    }

    DateTime date = GIT_DATE_FMT.parseDateTime(split.get(2));

    // TODO(cgruber): scan for fields from git metadata rather than description?
    return RevisionMetadata.builder()
        .id(split.get(0))
        .author(split.get(1))
        .date(date)
        .description(split.get(4))
        .withParents(parentBuilder.build())
        .build();
  }

  /** The tag parsing logic for git commits. */
  @Override
  protected FieldParsingResult parseFields(RevisionMetadata metadata) {
    // TODO(cgruber) implement trailer parsing
    return RevisionMetadata.legacyFieldParser(metadata.description());
  }

  @Override
  protected List<Revision> findHeadRevisions() {
    // As distinct from Mercurial, the head (current branch) in Git can only ever point to a
    // single commit.
    return ImmutableList.of(findHighestRevision("HEAD"));
  }
}
