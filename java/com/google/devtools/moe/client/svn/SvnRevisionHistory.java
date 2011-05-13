// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;

import com.google.common.collect.ImmutableList;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.List;

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
   * @param repositoryName  the name of the repository being parse
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
}
