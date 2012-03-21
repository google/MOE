// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.EquivalenceMatcher;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnRevisionHistory implements RevisionHistory {

  private String name;
  private String url;

  public SvnRevisionHistory(String name, String url) {
    this.name = name;
    this.url = url;
  }

  @Override
  public Revision findHighestRevision(String revId) {
    if (revId == null || revId.isEmpty()) {
      revId = "HEAD";
    }
    ImmutableList<String> args = ImmutableList.of("log", "--xml", "-l", "1", "-r",
        revId + ":1", url);

    String log;
    try {
      log = SvnRepository.runSvnCommand(args, "");
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format("Failed svn run: %s %d %s %s", args.toString(), e.returnStatus,
              e.stdout, e.stderr));
    }

    List<Revision> revisions = parseRevisions(log, name);
    // TODO(dbentley): we should log when the Revision's revId is different than
    // what was passed in, as this is often suprising to users.
    return revisions.get(0);
  }

  /**
   * Parse the output of svn log into Revisions.
   *
   * @param log  the output of svn to parse
   * @param repositoryName  the name of the repository being parsed
   */
  public static List<Revision> parseRevisions(String log, String repositoryName) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
          new InputSource(new StringReader(log)));
      NodeList nl = doc.getElementsByTagName("logentry");
      ImmutableList.Builder<Revision> resultBuilder = ImmutableList.builder();
      for (int i = 0; i < nl.getLength(); i++) {
        String revId = nl.item(i).getAttributes().getNamedItem("revision").getNodeValue();
        resultBuilder.add(new Revision(revId, repositoryName));
      }
      return resultBuilder.build();
    } catch (Exception e) {
      throw new MoeProblem("Could not parse xml log: " + log + e.getMessage());
    }
  }

  /**
   * Read the metadata for a given revision in the same repository
   *
   * @param revision  the revision to get metadata for
   */
  @Override
  public RevisionMetadata getMetadata(Revision revision) throws MoeProblem {
    if (!name.equals(revision.repositoryName)) {
      throw new MoeProblem(
          String.format("Could not get metadata: Revision %s is in repository %s instead of %s",
                        revision.revId, revision.repositoryName, name));
    }
    // svn log command for output in xml format for 2 log entries, for revision and its parent
    ImmutableList<String> args = ImmutableList.of("log", "--xml", "-l", "2", "-r",
        revision.revId + ":1", url);
    String log;
    try {
      log = SvnRepository.runSvnCommand(args, "");
    } catch (CommandException e) {
      throw new MoeProblem(
          String.format("Failed svn run: %s %d %s %s", args.toString(), e.returnStatus,
              e.stdout, e.stderr));
    }
    List<RevisionMetadata> metadata = parseMetadata(log);
    return metadata.get(0);
  }

  /**
   * Parse the output of svn log into Metadata
   *
   * @param log  the output of svn to parse
   */
  public List<RevisionMetadata> parseMetadata(String log) {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
          new InputSource(new StringReader(log)));
      NodeList nl = doc.getElementsByTagName("logentry");
      ImmutableList.Builder<RevisionMetadata> resultBuilder = ImmutableList.builder();
      for (int i = 0; i < nl.getLength(); i++) {
        String revId = nl.item(i).getAttributes().getNamedItem("revision").getNodeValue();
        NodeList nlEntries = nl.item(i).getChildNodes();
        if (i + 1 >= nl.getLength()) {
          resultBuilder.add(parseMetadataNodeList(revId, nlEntries, ImmutableList.<Revision>of()));
        } else {
          String parentId = nl.item(i + 1).getAttributes().getNamedItem("revision").getNodeValue();
          resultBuilder.add(parseMetadataNodeList(revId, nlEntries,
              ImmutableList.of(new Revision(parentId, name))));
        }
      }
      return resultBuilder.build();
    } catch (Exception e) {
      throw new MoeProblem("Could not parse xml log: " + log + e.getMessage());
    }
  }

  /**
   * Helper function for parseMetadata
   */
  public RevisionMetadata parseMetadataNodeList(String revId, NodeList nlEntries,
                                                 ImmutableList<Revision> parents) {
    String author = "None";
    String date = "None"; 
    String description = "None";
    for (int i = 0; i < nlEntries.getLength(); i++) {
      Node currNode = nlEntries.item(i);
      if (currNode.getNodeName().equals("author")) {
        author = currNode.getTextContent();
      }
      if (currNode.getNodeName().equals("date")) {
        date = currNode.getTextContent();
      }
      if (currNode.getNodeName().equals("msg")) {
        description = currNode.getTextContent();
      }
    }
    return new RevisionMetadata(revId, author, date, description, parents);
  }

  /**
   * Starting at specified revision, recur until a matching revision is found
   *
   * @param revision  the revision to start at.  If null, then start at head revision
   * @param matcher  the matcher to apply
   */
  @Override
  public Set<Revision> findRevisions(Revision revision, RevisionMatcher matcher) {
    ImmutableSet.Builder<Revision> resultBuilder = ImmutableSet.builder();
    if (revision == null) {
      revision = findHighestRevision("");
    }
    while (!matcher.matches(revision)) {
      resultBuilder.add(revision);
      RevisionMetadata metadata = getMetadata(revision);
      // Revisions in svn have at most one parent
      if (!metadata.parents.isEmpty()) {
        revision = metadata.parents.get(0);
      } else {
        return resultBuilder.build();
      }
    }
    return resultBuilder.build();
  }

  /**
   * Starting at specified revision, check for an equivalence in the matcher's other repository. If
   * there isn't one, find the revision's parent and check that for equivalence. Continue until you
   * find an equivalence, or the revision you are examining has no parents, or if the number of
   * parents that have been examined exceeds RevisionHistory.MAX_PARENTS_TO_EXAMINE.
   *
   * @param revision  the Revision to start at
   * @param matcher  the RevisionMatcher to apply
   *
   * @return the most recent Equivalence
   */
  @Override
  public Equivalence findLastEquivalence(Revision revision, EquivalenceMatcher matcher) {
    int parentsExamined = 0;
    AppContext.RUN.ui.info(String.format("Looking for an equivalence with repository %s starting "
        + "from revision %s...", matcher.repositoryName, revision.toString()));
    while (!matcher.matches(revision) && (parentsExamined < MAX_PARENTS_TO_EXAMINE)) {
      RevisionMetadata metadata = getMetadata(revision);
      // Revisions in svn have at most one parent
      if (!metadata.parents.isEmpty()) {
        revision = metadata.parents.get(0);
        parentsExamined++;
      } else {
        // The beginning of the history was reached and no equivalence was found
        return null;
      }
    }
    // The maximum number of parents where examined and an equivalence was not found.
    if (parentsExamined >= MAX_PARENTS_TO_EXAMINE) {
      AppContext.RUN.ui.info(String.format("No equivalence with repository %s starting from "
          + "revision %s was found after examining %d parent revisions. Null was returned.", 
          matcher.repositoryName, revision.toString(), MAX_PARENTS_TO_EXAMINE));
      return null;
    }
    return matcher.getEquivalence(revision);
  }
}
