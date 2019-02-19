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

package com.google.devtools.moe.client.config;

import com.google.devtools.moe.client.gson.GsonUtil;
import com.google.devtools.moe.client.InvalidProject;

/**
 * Configuration for a MOE migration.
 */
public class MigrationConfig {
  private String name;
  private boolean separateRevisions;
  private String fromRepository;
  private String toRepository;
  private MetadataScrubberConfig metadataScrubberConfig;

  public MigrationConfig() {} // Constructed by gson

  public MigrationConfig copyWithFromRepository(String alternate) {
    // TODO(cgruber) Rip this mechanism of using string labels
    MigrationConfig clone = GsonUtil.cloneGsonObject(this);
    clone.fromRepository = alternate;
    return clone;
  }

  public String getName() {
    return name;
  }

  public boolean getSeparateRevisions() {
    return separateRevisions;
  }

  public String getFromRepository() {
    return fromRepository;
  }

  public String getToRepository() {
    return toRepository;
  }

  public MetadataScrubberConfig getMetadataScrubberConfig() {
    return metadataScrubberConfig;
  }

  public void validate() throws InvalidProject {
    InvalidProject.assertNotEmpty(name, "Missing name in migration");
    InvalidProject.assertNotEmpty(fromRepository, "Missing from_repository in migration");
    InvalidProject.assertNotEmpty(toRepository, "Missing to_repository in migration");
  }
}
