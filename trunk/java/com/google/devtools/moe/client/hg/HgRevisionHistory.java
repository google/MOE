// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A RevisionHistory implementation for Hg repositories.
 *
 */
public class HgRevisionHistory implements RevisionHistory {

  private final HgClonedRepository tipClone;

  HgRevisionHistory(HgClonedRepository tipClone) {
    this.tipClone = tipClone;
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
        "--template={node}");

    String changesetID;
    try {
      changesetID = HgRepository.runHgCommand(args, tipClone.getLocalTempDir().getAbsolutePath());
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format(
              "Failed hg run: %s %d %s %s",
              args.toString(),
              e.returnStatus,
              e.stdout,
              e.stderr));
    }

    return new Revision(changesetID, tipClone.getRepositoryName());
  }

  /**
   * Read the metadata for a given revision in the same repository
   *
   * @param revision  the revision to parse metadata for
   */
  public RevisionMetadata getMetadata(Revision revision) {
    if (!tipClone.getRepositoryName().equals(revision.repositoryName)) {
      throw new MoeProblem(
          String.format("Could not get metadata: Revision %s is in repository %s instead of %s",
                        revision.revId, revision.repositoryName, tipClone.getRepositoryName()));
    }
    ImmutableList<String> args = ImmutableList.of(
        "log",
        "--rev=" + revision.revId,
        // Ensure one revision only, to be safe.
        "--limit=1",
        // Format output as "changesetID < author < date < description < parents".
        // Since parents is a list, need to use stringify before applying another filter.
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
                    "{desc|escape} < {parents|stringify|escape}",
        // Use the debug option to get all parents
        "--debug");
    String log;
    try {
      log = HgRepository.runHgCommand(args, tipClone.getLocalTempDir().getAbsolutePath());
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
  private static final Pattern BEGIN_LOG_RE = Pattern.compile(BEGIN_LOG_PATTERN, Pattern.DOTALL);

  /**
   * Unescape text that was escaped by hg escape
   */
  private static String unescape(String text) {
    return text.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");
  }

  /** Parse the output of Hg into RevisionMetadata
   *
   * @param log a single log entry to parse. This is expected to be in the format given by
   *            the output of a call to getMetadata().
   */
  public List<RevisionMetadata> parseMetadata(String log) {
    ImmutableList.Builder<RevisionMetadata> result = ImmutableList.<RevisionMetadata>builder();
    Matcher m = BEGIN_LOG_RE.matcher(log);
    if (m.matches()) {
      ImmutableList.Builder<Revision> parentBuilder = ImmutableList.<Revision>builder();
      // group 5 contains all of the parents, each separated by a space
      for (String parent : Splitter.on(" ").split(unescape(m.group(5)))) {
        if (!parent.isEmpty()) {
          // A parent is of the form revisionId:changesetId. When null, a parent is denoted by
          // revisionId of -1 and changesetId of 0000000000000000000000000000000000000000.
          String[] parentParts = parent.split(":");
          if (!parentParts[0].equals("-1")) {
            parent = parentParts[1];
            parentBuilder.add(new Revision(parent, tipClone.getRepositoryName()));
          }
        }
      }
      result.add(new RevisionMetadata(unescape(m.group(1)), unescape(m.group(2)),
                                      unescape(m.group(3)), unescape(m.group(4)),
                                      parentBuilder.build()));
    }
    return result.build();
  }

  /**
   * Pull the changeset IDs for all head revisions.
   * Returns a List of Revision objects corresponding to those changesets.
   */
  public List<Revision> findHeadRevisions() {
    ImmutableList<String> args = ImmutableList.of(
        "heads",
        // Format output as "changesetID"s alone.
        "--template='{node}\n'");
    String heads;
    try {
      heads = HgRepository.runHgCommand(args, tipClone.getLocalTempDir().getAbsolutePath());
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format("Failed hg run: %s %d %s %s",
                        args.toString(),
                        e.returnStatus,
                        e.stdout,
                        e.stderr));
    }
    ImmutableList.Builder<Revision> result = ImmutableList.<Revision>builder();
    for (String changesetID : Splitter.on("\n").split(heads)) {
      result.add(new Revision(changesetID, tipClone.getRepositoryName()));
    }
    return result.build();
  }

  /**
   * Starting at specified revision, recur until a matching revision is found
   *
   * @param revision  the revision to start at.  If null, then start at head revision
   * @param matcher  the matcher to apply
   */
  public Set<Revision> findRevisions(Revision revision, RevisionMatcher matcher) {
    ImmutableSet.Builder<Revision> resultBuilder = ImmutableSet.builder();
    if (revision != null) {
      findRevisionsRecursiveHelper(revision, matcher, resultBuilder);
    } else {
      // may be many heads in hg
      for (Revision r : findHeadRevisions()) {
        findRevisionsRecursiveHelper(r, matcher, resultBuilder);
      }
    }
    return resultBuilder.build();
  }

  private void findRevisionsRecursiveHelper(Revision revision, RevisionMatcher matcher,
                                            ImmutableSet.Builder<Revision> resultBuilder) {
    //TODO(user): this may exhaust the stack space; consider switching to an iterative solution
    if (!matcher.matches(revision)) {
      resultBuilder.add(revision);
      RevisionMetadata metadata = getMetadata(revision);
      for (Revision parent : metadata.parents) {
        findRevisionsRecursiveHelper(parent, matcher, resultBuilder);
      }
    }
  }
}
