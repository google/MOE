/*
 * Copyright (c) 2016 Google, Inc.
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

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.config.MetadataScrubberConfig;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Scrubs a {@code RevisionMetadata} by consuming a commit metadata field, if present, named {@code
 * ORIGINAL_AUTHOR} a substituting its value as the named author of the commit.
 *
 * MOE assumes that author metadata will be in "blah &lt;some@email.com>" format. This scrubber
 * will attempt to do a best-effort cleanup of the field if it doesn't match that format.
 */
public class OriginalAuthorMetadataScrubber extends MetadataScrubber {
  private static final String ORIGINAL_AUTHOR_FIELD_KEY = "ORIGINAL_AUTHOR";
  private static final String ORIGINAL_AUTHOR_REPLACEMENT_RE = ORIGINAL_AUTHOR_FIELD_KEY + "=.*";
  private static final String SIMPLE_RAW_EMAIL_RE = "[A-Z0-9._%+-]+@[A-Z0-9.-]+[.][A-Z]{2,}";
  private static final String SIMPLE_EMAIL_RE = ".*(?i:\\b" + SIMPLE_RAW_EMAIL_RE+ "\\b).*";
  private static final String SIMPLE_USERNAME_RE = "(?i:\\b[A-Z0-9._%+-]*\\b)";
  private static final String GIT_AUTHOR_RE = ".*<.*>.*";

  private final Ui ui;

  @Inject
  public OriginalAuthorMetadataScrubber(Ui ui) {
    this.ui = ui;
  }

  @Override
  protected boolean shouldScrub(@Nullable MetadataScrubberConfig config) {
    return config != null && config.getRestoreOriginalAuthor();
  }

  @Override
  public RevisionMetadata execute(RevisionMetadata rm, MetadataScrubberConfig unused) {
    if (rm.fields().containsKey(ORIGINAL_AUTHOR_FIELD_KEY)) {
      return rm.toBuilder()
          .author(sanitizeAuthor(rm.fields().get(ORIGINAL_AUTHOR_FIELD_KEY).iterator().next()))
          .description(extractFirstOriginalAuthorField(rm.description()))
          .build();
    } else {
      return rm;
    }
  }

  String sanitizeAuthor(String author) {
    author = author.trim();
    if (author.matches(GIT_AUTHOR_RE)) {
      return author;
    } else if (author.matches(SIMPLE_EMAIL_RE)) {
      return author.split("@")[0] + " <" + author + ">";
    } else if (author.matches(SIMPLE_USERNAME_RE)) {
      return author + " <" + author + ">";
    } else {
      // TODO(cgruber): Find a safer handling of degenerate cases.
      ui.message("WARNING: unknown author format found in commit metadata: \"%s\"", author);
      return "\"" + author + "\" <undetermined_user>";
    }
  }

  /** Extract the original author field from the description, in case it's still there. */
  String extractFirstOriginalAuthorField(String desc) {
    // TODO(cgruber) remove the newline implicit in this check.
    return desc.replaceFirst(ORIGINAL_AUTHOR_REPLACEMENT_RE, "");
  }
}
