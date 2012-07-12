// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Scrubs a {@code RevisionMetadata} by formatting its {@link RevisionMetadata#description} per
 * {@link MetadataScrubberConfig#getLogFormat()}.
 *
 * <p>Example log_format:
 * "Change made in internal repo at original date {date} with original description:\n{description}"
 *
 * <p>Possible fields:
 * <p>{id} Original revision commit identifier
 * <p>{author} Original author
 * <p>{date} Original revision date
 * <p>{description} Original revision changelog
 * <p>{parents} Original revision's parent revision numbers
 *
 */
public class DescriptionMetadataScrubber extends MetadataScrubber {
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormat.forPattern("yyyy/MM/dd");
  private final String logFormat;

  public DescriptionMetadataScrubber(String logFormat) {
    this.logFormat = logFormat;
  }

  @Override public RevisionMetadata scrub(RevisionMetadata rm) {
    ImmutableList.Builder<String> parentRevIds = ImmutableList.builder();
    for (Revision parent : rm.parents) {
      parentRevIds.add(parent.revId);
    }
    String parentsString = Joiner.on(", ").join(parentRevIds.build());

    String scrubbedDescription = logFormat
        .replace("{id}", rm.id)
        .replace("{author}", rm.author)
        .replace("{date}", DATE_FMT.print(rm.date))
        .replace("{description}", rm.description)
        .replace("{parents}", parentsString);

    return new RevisionMetadata(rm.id, rm.author, rm.date, scrubbedDescription, rm.parents);
  }
}
