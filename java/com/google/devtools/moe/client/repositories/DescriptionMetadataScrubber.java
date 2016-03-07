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
import com.google.common.collect.ImmutableList;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.inject.Inject;

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
 */
public class DescriptionMetadataScrubber extends MetadataScrubber {
  private static final DateTimeFormatter DATE_FMT = DateTimeFormat.forPattern("yyyy/MM/dd");

  @Inject
  public DescriptionMetadataScrubber() {}

  @Override
  public RevisionMetadata execute(RevisionMetadata rm, MetadataScrubberConfig config) {
    ImmutableList.Builder<String> parentRevIds = ImmutableList.builder();
    for (Revision parent : rm.parents) {
      parentRevIds.add(parent.revId());
    }
    String parentsString = Joiner.on(", ").join(parentRevIds.build());

    String scrubbedDescription =
        config
            .getLogFormat()
            .replace("{id}", rm.id)
            .replace("{author}", rm.author)
            .replace("{date}", DATE_FMT.print(rm.date))
            .replace("{description}", rm.description)
            .replace("{parents}", parentsString);

    return new RevisionMetadata(rm.id, rm.author, rm.date, scrubbedDescription, rm.parents);
  }
}
