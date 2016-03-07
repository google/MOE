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

import javax.inject.Inject;

/**
 * This MetadataScrubber replaces any of the target usernames with the replacement string.
 */
public class MetadataUsernameScrubber extends MetadataScrubber {
  private static final String REPLACEMENT = "<user>";

  @Inject
  public MetadataUsernameScrubber() {}

  @Override
  protected boolean shouldScrub(MetadataScrubberConfig config) {
    return config != null && !config.getUsernamesToScrub().isEmpty();
  }

  @Override
  public RevisionMetadata execute(RevisionMetadata rm, MetadataScrubberConfig config) {
    return MetadataScrubber.stripFromAllFields(
        rm, config.getUsernamesToScrub(), REPLACEMENT, /*wordAlone*/ true);
  }
}
