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

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.AbstractRevisionHistory;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.repositories.RevisionMetadata.FieldParsingResult;
import java.io.StringReader;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * A revision history backed by a subversion repository
 */
public class SvnRevisionHistory extends AbstractRevisionHistory {

  private final String name;
  private final String url;
  private final SvnUtil util;

  public SvnRevisionHistory(String name, String url, SvnUtil util) {
    this.name = name;
    this.url = url;
    this.util = util;
  }

  @Override
  public Revision findHighestRevision(String revId) {
    if (revId == null || revId.isEmpty()) {
      revId = "HEAD";
    }

    String log;
    try {
      log = util.runSvnCommand("log", "--xml", "-l", "1", "-r", revId + ":1", url);
    } catch (CommandException e) {
      throw new MoeProblem("Failed svn run: %s", e);
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
      Document doc =
          DocumentBuilderFactory.newInstance()
              .newDocumentBuilder()
              .parse(new InputSource(new StringReader(log)));
      NodeList nl = doc.getElementsByTagName("logentry");
      ImmutableList.Builder<Revision> resultBuilder = ImmutableList.builder();
      for (int i = 0; i < nl.getLength(); i++) {
        String revId = nl.item(i).getAttributes().getNamedItem("revision").getNodeValue();
        resultBuilder.add(Revision.create(revId, repositoryName));
      }
      return resultBuilder.build();
    } catch (Exception e) {
      throw new MoeProblem(e, "Could not parse xml log: %s", log);
    }
  }

  /**
   * Read the metadata for a given revision in the same repository
   *
   * @param revision the revision to get metadata for
   */
  @Override
  public RevisionMetadata createMetadata(Revision revision) {
    if (!name.equals(revision.repositoryName())) {
      throw new MoeProblem(
          "Could not get metadata: Revision %s is in repository %s instead of %s",
          revision.revId(),
          revision.repositoryName(),
          name);
    }
    String log;
    try {
      log = util.runSvnCommand("log", "--xml", "-l", "2", "-r", revision.revId() + ":1", url);
    } catch (CommandException e) {
      throw new MoeProblem("Failed svn run: %s", e);
    }
    List<RevisionMetadata> metadata = parseMetadata(log);
    return metadata.get(0);
  }

  /**
   * Parse the output of svn log into Metadata
   *
   * @param log the output of svn to parse
   */
  List<RevisionMetadata> parseMetadata(String log) {
    try {
      Document doc =
          DocumentBuilderFactory.newInstance()
              .newDocumentBuilder()
              .parse(new InputSource(new StringReader(log)));
      NodeList nl = doc.getElementsByTagName("logentry");
      ImmutableList.Builder<RevisionMetadata> resultBuilder = ImmutableList.builder();
      for (int i = 0; i < nl.getLength(); i++) {
        String revId = nl.item(i).getAttributes().getNamedItem("revision").getNodeValue();
        NodeList nlEntries = nl.item(i).getChildNodes();
        if (i + 1 >= nl.getLength()) {
          resultBuilder.add(parseMetadataNodeList(revId, nlEntries, ImmutableList.<Revision>of()));
        } else {
          String parentId =
              nl.item(i + 1).getAttributes().getNamedItem("revision").getNodeValue();
          resultBuilder.add(
              parseMetadataNodeList(
                  revId, nlEntries, ImmutableList.of(Revision.create(parentId, name))));
        }
      }
      return resultBuilder.build();
    } catch (Exception e) {
      throw new MoeProblem(e, "Could not parse xml log: %s", log);
    }
  }

  /**
   * Helper function for parseMetadata
   */
  public RevisionMetadata parseMetadataNodeList(
      String revId, NodeList nlEntries, ImmutableList<Revision> parents) {
    String author = "None";
    DateTime date = new DateTime(0L); // Unix epoch
    String description = "None";
    for (int i = 0; i < nlEntries.getLength(); i++) {
      Node currNode = nlEntries.item(i);
      if (currNode.getNodeName().equals("author")) {
        author = currNode.getTextContent();
      }
      if (currNode.getNodeName().equals("date")) {
        date = ISODateTimeFormat.dateTime().parseDateTime(currNode.getTextContent());
      }
      if (currNode.getNodeName().equals("msg")) {
        description = currNode.getTextContent();
      }
    }
    return RevisionMetadata.builder()
        .id(revId)
        .author(author)
        .date(date)
        .description(description)
        .withParents(parents)
        .build();
  }

  @Override
  protected List<Revision> findHeadRevisions() {
    return ImmutableList.of(findHighestRevision(null));
  }

  /** The tag parsing logic for svn commits. */
  @Override
  protected FieldParsingResult parseFields(RevisionMetadata metadata) {
    // TODO(cgruber) implement header parsing
    return RevisionMetadata.legacyFieldParser(metadata.description());
  }
}
