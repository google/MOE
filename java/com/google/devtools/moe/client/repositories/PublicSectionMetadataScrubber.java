// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Scrubs a {@code RevisionMetadata} by replacing the description with just the "Public:" section if
 * one exists.
 *
 */
public class PublicSectionMetadataScrubber extends MetadataScrubber {

  private static final Pattern PUBLIC_SECTION_PATTERN = Pattern.compile("^\\s*Public:\\s*$");
  private static final Pattern END_PUBLIC_SECTION_PATTERN = Pattern.compile("^\\s*$");

  public PublicSectionMetadataScrubber() {}

  @Override
  public RevisionMetadata scrub(RevisionMetadata rm) {
    List<String> lines = ImmutableList.copyOf(Splitter.on("\n").split(rm.description));
    int startPublicSection = -1;
    int endPublicSection = -1;
    int currentLine = 0;
    for (String line : lines) {
      if (PUBLIC_SECTION_PATTERN.matcher(line).matches()) {
        startPublicSection = currentLine;
        endPublicSection = lines.size();
      } else if (startPublicSection >= 0 && END_PUBLIC_SECTION_PATTERN.matcher(line).matches()) {
        endPublicSection = currentLine;
      }
      ++currentLine;
    }

    String newDesc = (startPublicSection >= 0)
        ? Joiner.on("\n").join(lines.subList(startPublicSection + 1, endPublicSection))
        : rm.description;
    return new RevisionMetadata(rm.id, rm.author, rm.date, newDesc, rm.parents);
  }
}
