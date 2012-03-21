// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.hg;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.dvcs.AbstractDvcsRevisionHistory;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An Hg implementation of {@link AbstractDvcsRevisionHistory}.
 *
 */
public class HgRevisionHistory extends AbstractDvcsRevisionHistory {

  /**
   * The Hg revision ID for a repo at tip.
   */
  static final String HG_TIP_REVID = "tip";

  private final Supplier<HgClonedRepository> tipCloneSupplier;

  HgRevisionHistory(Supplier<HgClonedRepository> tipCloneSupplier) {
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
  public Revision findHighestRevision(String revId) {
    if (revId == null || revId.isEmpty()) {
      revId = HG_TIP_REVID;
    }

    ImmutableList<String> args = ImmutableList.of(
        "log",
        "--rev=" + revId,
        // Ensure one revision only, to be safe.
        "--limit=1",
        // Format output as "changesetID" alone.
        "--template={node}");

    String changesetID;
    HgClonedRepository tipClone = tipCloneSupplier.get();
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
   * Reads the metadata for a given revision in the same repository
   *
   * @param revision  the revision to parse metadata for
   */
  @Override
  public RevisionMetadata getMetadata(Revision revision) {
    HgClonedRepository tipClone = tipCloneSupplier.get();
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
   * Unescapes text that was escaped by hg log escaping.
   */
  private static String unescape(String text) {
    // TODO(user): Replace with URLEncoder or something from HtmlEscapers.
    return text.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");
  }

  /**
   * Parses the output of Hg into RevisionMetadata
   *
   * @param log  a single log entry to parse. This is expected to be in the format given by
   *             the output of a call to getMetadata().
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
            parentBuilder.add(new Revision(parent, tipCloneSupplier.get().getRepositoryName()));
          }
        }
      }
      result.add(new RevisionMetadata(unescape(m.group(1)), unescape(m.group(2)),
                                      unescape(m.group(3)), unescape(m.group(4)),
                                      parentBuilder.build()));
    }
    return result.build();
  }

  @Override
  protected List<Revision> findHeadRevisions() {
    ImmutableList<String> args = ImmutableList.of(
        "heads",
        // Format output as "changesetID"s alone.
        "--template={node}\n");
    String heads;
    HgClonedRepository tipClone = tipCloneSupplier.get();
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
    for (String changesetID : Splitter.on("\n").omitEmptyStrings().split(heads)) {
      result.add(new Revision(changesetID, tipClone.getRepositoryName()));
    }
    return result.build();
  }
}
