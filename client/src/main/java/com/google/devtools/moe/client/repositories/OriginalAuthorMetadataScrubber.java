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

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Scrubs a {@code RevisionMetadata} by consuming a commit metadata field, if present, named {@code
 * ORIGINAL_AUTHOR} a substituting its value as the named author of the commit.
 */
public class OriginalAuthorMetadataScrubber extends MetadataScrubber {
  private static final String ORIGINAL_AUTHOR_FIELD_KEY = "ORIGINAL_AUTHOR";
  private static final String ORIGINAL_AUTHOR_REPLACEMENT_RE = ORIGINAL_AUTHOR_FIELD_KEY + "=.*";

  @Inject
  public OriginalAuthorMetadataScrubber() {}

  @Override
  protected boolean shouldScrub(@Nullable MetadataScrubberConfig config) {
    return config != null && config.getRestoreOriginalAuthor();
  }

  @Override
  public RevisionMetadata execute(RevisionMetadata rm, MetadataScrubberConfig unused) {
    if (rm.fields().containsKey(ORIGINAL_AUTHOR_FIELD_KEY)) {
      return rm.toBuilder()
          .author(rm.fields().get(ORIGINAL_AUTHOR_FIELD_KEY).iterator().next())
          .description(extractFirstOriginalAuthorField(rm.description()))
          .build();
    } else {
      return rm;
    }
  }

  String extractFirstOriginalAuthorField(String desc) {
    return desc.replaceFirst(ORIGINAL_AUTHOR_REPLACEMENT_RE, "");
  }
}
