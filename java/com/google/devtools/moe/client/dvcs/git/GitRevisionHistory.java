// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.git;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.AbstractRevisionHistory;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

/**
 * A Git implementation of {@link AbstractRevisionHistory}.
 */
public class GitRevisionHistory extends AbstractRevisionHistory {

  // Like ISO 8601 format, but without the 'T' character
  private static final DateTimeFormatter GIT_DATE_FMT =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z");

  @VisibleForTesting static final String LOG_DELIMITER = "---@MOE@---";

  /**
   * The default Git branch in which to look for revisions.
   */
  @VisibleForTesting static final String DEFAULT_BRANCH = "master";

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
    if (revId == null || revId.isEmpty()) {
      revId = DEFAULT_BRANCH;
    }

    String hashID;
    GitClonedRepository headClone = headCloneSupplier.get();
    try {
      hashID = headClone.runGitCommand("log", "--max-count=1", "--format=%H", revId);
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format(
              "Failed git log run: %d %s %s",
              e.returnStatus,
              e.stdout,
              e.stderr));
    }
    // Clean up output.
    hashID = hashID.replaceAll("\\W", "");

    return new Revision(hashID, headClone.getRepositoryName());
  }

  /**
   * Read the metadata for a given revision in the same repository.
   *
   * @param revision  the revision to parse metadata for
   */
  @Override
  public RevisionMetadata getMetadata(Revision revision) {
    GitClonedRepository headClone = headCloneSupplier.get();
    if (!headClone.getRepositoryName().equals(revision.repositoryName)) {
      throw new MoeProblem(
          String.format("Could not get metadata: Revision %s is in repository %s instead of %s",
                        revision.revId, revision.repositoryName, headClone.getRepositoryName()));
    }

    // Format: hash, author, ISO date, parents, full commit message (subject and body)
    String format = Joiner.on(LOG_DELIMITER).join("%H", "%an", "%ai", "%P", "%B");

    String log;
    try {
      log = headClone.runGitCommand(
          "log",
          // Ensure one revision only, to be safe.
          "--max-count=1",
          "--format=" + format,
          revision.revId);
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format("Failed git run: %d %s %s", e.returnStatus, e.stdout, e.stderr));
    }

    return parseMetadata(log);
  }

  /**
   * Parse the output of Git into RevisionMetadata.
   *
   * @param log  the output of getMetadata to parse
   */
  @VisibleForTesting RevisionMetadata parseMetadata(String log) {
    // Split on the log delimiter. Limit to 5 so that it will act correctly
    // even if the log delimiter happens to be in the commit message.
    List<String> split = ImmutableList.copyOf(Splitter.on(LOG_DELIMITER).limit(5).split(log));

    // The fourth item contains all of the parents, each separated by a space.
    ImmutableList.Builder<Revision> parentBuilder = ImmutableList.<Revision>builder();
    for (String parent : Splitter.on(' ').omitEmptyStrings().split(split.get(3))) {
      parentBuilder.add(new Revision(parent, headCloneSupplier.get().getRepositoryName()));
    }

    DateTime date = GIT_DATE_FMT.parseDateTime(split.get(2));
    return new RevisionMetadata(
        split.get(0),  // id
        split.get(1),  // author
        date,
        split.get(4),  // description
        parentBuilder.build());  // parents
  }

  @Override
  protected List<Revision> findHeadRevisions() {
    List<String> importBranches = headCloneSupplier.get().getConfig().getImportBranches();
    if (importBranches == null) {
      importBranches = ImmutableList.of("master");
    }

    ImmutableList.Builder<Revision> result = ImmutableList.<Revision>builder();
    for (String branchName : importBranches) {
      result.add(findHighestRevision(branchName));
    }
    return result.build();
  }
}
