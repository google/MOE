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

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Scrubs a {@code RevisionMetadata} by replacing the description with just the "Public:" section if
 * one exists.
 */
public class PublicSectionMetadataScrubber extends MetadataScrubber {

  private static final Pattern PUBLIC_SECTION_PATTERN = Pattern.compile("^\\s*Public:\\s*$");
  private static final Pattern END_PUBLIC_SECTION_PATTERN = Pattern.compile("^\\s*$");

  @Inject
  public PublicSectionMetadataScrubber() {}

  @Override
  protected boolean shouldScrub(@Nullable MetadataScrubberConfig config) {
    return true;
  }

  @Override
  public RevisionMetadata execute(RevisionMetadata rm, MetadataScrubberConfig unused) {
    List<String> lines = Splitter.on('\n').splitToList(rm.description);
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

    String newDesc =
        (startPublicSection >= 0)
            ? Joiner.on("\n").join(lines.subList(startPublicSection + 1, endPublicSection))
            : rm.description;
    return new RevisionMetadata(rm.id, rm.author, rm.date, newDesc, rm.parents);
  }
}
