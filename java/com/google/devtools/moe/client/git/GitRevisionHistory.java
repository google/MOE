// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.List;
import java.util.Set;

/**
 * A RevisionHistory implementation for Git repositories.
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitRevisionHistory implements RevisionHistory {

  private static final String LOG_DELIMITER = "<";

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
      revId = "HEAD";
    }

    ImmutableList<String> args = ImmutableList.of(
        "log",
        "--max-count=1",
        "--format=%H",
        revId);

    String hashID;
    GitClonedRepository headClone = headCloneSupplier.get();
    try {
      hashID = headClone.runGitCommand(args);
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format(
              "Failed git run: %s %d %s %s",
              args.toString(),
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

    // Construct the format.
    String format = Joiner.on(LOG_DELIMITER)
        .join(ImmutableList.of("%H", "%an", "%ad", "%P", "%s"));

    // Sadly, there are no escape filters in Git for log like their are in Hg.
    // There simply seems to be no way to escape the fields.
    ImmutableList<String> args = ImmutableList.of(
        "log",
        // Ensure one revision only, to be safe.
        "--max-count=1",
        // Format output as "hashID < author < date <  parents < description".
        "--format=" + format,
        revision.revId);
    String log;
    try {
      log = headClone.runGitCommand(args);
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format("Failed git run: %s %d %s %s",
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

  /**
   * Parse the output of Git into RevisionMetadata.
   *
   * @param log  the output of getMetadata to parse
   */
  public List<RevisionMetadata> parseMetadata(String log) {
    ImmutableList.Builder<RevisionMetadata> result = ImmutableList.<RevisionMetadata>builder();
    Splitter lineSplitter = Splitter.on('\n').omitEmptyStrings().trimResults();
    for (String line : lineSplitter.split(log)) {
      // Split on the log delimiter. Limit to 5 so that it will act correctly
      // even if the log delimiter happens to be in the subject.
      String[] split = Iterables.toArray(
          Splitter.on(LOG_DELIMITER).limit(5).split(line), String.class);
      ImmutableList.Builder<Revision> parentBuilder = ImmutableList.<Revision>builder();

      // The fourth item contains all of the parents, each separated by a space.
      for (String parent : Splitter.on(" ").split(split[3])) {
        if (!parent.isEmpty()) {
          parentBuilder.add(new Revision(parent, headCloneSupplier.get().getRepositoryName()));
        }
      }

      result.add(new RevisionMetadata(
          split[0],  // id
          split[1],  // author
          split[2],  // date
          split[4],  // description
          parentBuilder.build())); // parents
    }
    return result.build();
  }

  /**
   * Pull the hash IDs for all head revisions.
   * @return List of Revision objects corresponding to those hashes.
   */
  public List<Revision> findHeadRevisions() {
    String heads;
    GitClonedRepository headClone = headCloneSupplier.get();
    try {
      heads = headClone.runGitCommand(ImmutableList.of("branch"));
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format("Failed git run: %s %d %s %s",
                        "branch",
                        e.returnStatus,
                        e.stdout,
                        e.stderr));
    }
    ImmutableList.Builder<Revision> result = ImmutableList.<Revision>builder();
    for (String branchName : Splitter.on("\n").split(heads)) {
      // As far as I can tell there is no clean way to do the equivalent of
      // hg heads, so we have to loop over the output of git branch.
      branchName = branchName.replaceAll("[\\*\\s]+", ""); // trim away star
      result.add(findHighestRevision(branchName));
    }
    return result.build();
  }

  /**
   * Starting at specified revision, recur until a matching revision is found.
   *
   * @param revision  the Revision to start at.  If null, then start at head revision
   * @param matcher  the Matcher to apply
   */
  @Override
  public Set<Revision> findRevisions(Revision revision, RevisionMatcher matcher) {
    ImmutableSet.Builder<Revision> resultBuilder = ImmutableSet.builder();
    if (revision != null) {
      findRevisionsRecursiveHelper(revision, matcher, resultBuilder);
    } else {
      // Look through each head.
      for (Revision r : findHeadRevisions()) {
        findRevisionsRecursiveHelper(r, matcher, resultBuilder);
      }
    }
    return resultBuilder.build();
  }

  private void findRevisionsRecursiveHelper(Revision revision, RevisionMatcher matcher,
                                            ImmutableSet.Builder<Revision> resultBuilder) {
    //TODO(user):This may exhaust the stack space; consider switching to an iterative solution.
    if (!matcher.matches(revision)) {
      resultBuilder.add(revision);
      RevisionMetadata metadata = getMetadata(revision);
      for (Revision parent : metadata.parents) {
        findRevisionsRecursiveHelper(parent, matcher, resultBuilder);
      }
    }
  }
}
