// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A RevisionHistory implementation for Hg repositories.
 *
 */
public class HgRevisionHistory implements RevisionHistory {

  private final HgClonedRepository repo;

  /*package*/ HgRevisionHistory(HgClonedRepository repo) {
    this.repo = repo;
  }

  /**
   * Confirm the existence of the given changeset ID via 'hg log', or pull the most recent
   * changeset ID if none is given. Returns a Revision object corresponding to that changeset.
   *
   * NB: In Hg, revision numbers are local-only, i.e. not valid across clones of a repository. So
   * MOE deals only in full Hg changeset IDs (hex strings).
   */
  @Override
  public Revision findHighestRevision(String revId) {
    if (revId == null || revId.isEmpty()) {
      revId = "tip";
    }

    ImmutableList<String> args = ImmutableList.of(
        "log",
        "--rev=" + revId,
        // Ensure one revision only, to be safe.
        "--limit=1",
        // Format output as "changesetID" alone.
        "--template='{node}'",
        repo.getLocalTempDir().getAbsolutePath());

    String changesetID;
    try {
      changesetID = HgRepository.runHgCommand(args, "");
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format(
              "Failed hg run: %s %d %s %s",
              args.toString(),
              e.returnStatus,
              e.stdout,
              e.stderr));
    }

    return new Revision(changesetID, repo.getRepositoryName());
  }

  /**
   * Read the metadata for a given revision in the same repository
   *
   * @param revision  the revision to parse metadata for
   */
  public RevisionMetadata getMetadata(Revision revision) {
    if (!repo.getRepositoryName().equals(revision.repositoryName)) {
      throw new MoeProblem(
          String.format("Could not get metadata: Revision %s is in repository %s instead of %s",
                        revision.revId, revision.repositoryName, repo.getRepositoryName()));
    }
    ImmutableList<String> args = ImmutableList.of(
        "log",
        "--rev=" + revision.revId,
        // Ensure one revision only, to be safe.
        "--limit=1",
        // Format output as "changesetID < author < date < description < parents".
        "--template='{node|escape} < {author|escape} < {date|date|escape} < " +
                    "{desc|escape} < {parents|escape}'",
        // Use the debug option to get all parents
        "--debug",
        repo.getLocalTempDir().getAbsolutePath());
    String log;
    try {
      log = HgRepository.runHgCommand(args, "");
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format("Failed hg run: %s %d %s %s",
                        args.toString(),
                        e.returnStatus,
                        e.stdout,
                        e.stderr));
    }
    RevisionMetadata metadata;
    try {
      metadata = parseMetadata(log).get(0);
    } catch (Exception e) {
      throw new MoeProblem("No metadata read");
    }
    return metadata;
  }

  private static final String BEGIN_LOG_PATTERN = "^(.*) < (.*) < (.*) < (.*) < (.*)$";
  private static final Pattern BEGIN_LOG_RE = Pattern.compile(BEGIN_LOG_PATTERN);

  /**
   * Unescape text that was escaped by hg escape
   */
  private static String unescape(String text) {
    return text.replaceAll("&lt", "<").replaceAll("&gt", ">").replaceAll("&amp", "&");
  }

  /** Parse the output of Hg into RevisionMetadata
   *
   * @param log  the output of srcrr to parse
   */
  public List<RevisionMetadata> parseMetadata(String log) {
    ImmutableList.Builder<RevisionMetadata> result = ImmutableList.<RevisionMetadata>builder();
    for (String line : Splitter.on("\n").split(log)) {
      Matcher m = BEGIN_LOG_RE.matcher(line);
      if (m.matches()) {
        ImmutableList.Builder<Revision> parentBuilder = ImmutableList.<Revision>builder();
        // group 5 contains all of the parents, each separated by a space
        for (String parent : Splitter.on(" ").split(unescape(m.group(5)))) {
          if (!parent.isEmpty()) {
            parentBuilder.add(new Revision(parent, repo.getRepositoryName()));
          }
        }
        result.add(new RevisionMetadata(unescape(m.group(1)), unescape(m.group(2)),
                                        unescape(m.group(3)), unescape(m.group(4)),
                                        parentBuilder.build()));
      }
    }
    return result.build();
  }
}
