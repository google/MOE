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

import java.util.List;

/**
 * This MetadataScrubber replaces any of the target usernames with the replacement string.
 */
public class MetadataUsernameScrubber extends MetadataScrubber {

  private List<String> usernamesToScrub;
  private final String replacement = "<user>";

  public MetadataUsernameScrubber(List<String> usernames) {
    this.usernamesToScrub = usernames;
  }

  @Override
  public RevisionMetadata scrub(RevisionMetadata rm) {
    return MetadataScrubber.stripFromAllFields(rm, usernamesToScrub, replacement, true);
  }
}
