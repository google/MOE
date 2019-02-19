/*
 * Copyright (c) 2015 Google, Inc.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.devtools.moe.client.GsonModule;
import com.google.gson.Gson;

import junit.framework.TestCase;

public class RevisionTest extends TestCase {
  private final Gson gson = GsonModule.provideGson();
  private final String legacyRevisionJson = "{\"revId\":\"12345\",\"repositoryName\":\"foo\"}";
  private final String newRevisionJson =
      "{\n  \"repository_name\": \"foo\",\n  \"rev_id\": \"12345\"\n}";
  private final Revision testRevision = Revision.create(12345, "foo");

  public void testRead() {
    Revision legacyRevision = gson.fromJson(legacyRevisionJson, Revision.class);
    Revision newRevision = gson.fromJson(newRevisionJson, Revision.class);
    assertThat(legacyRevision).isEqualTo(newRevision);
    assertThat(newRevision.revId()).isEqualTo("12345");
    assertThat(newRevision.repositoryName()).isEqualTo("foo");
    assertThat(newRevision).isEqualTo(testRevision);
  }

  public void testWrite() {
    assertThat(gson.toJson(testRevision)).isEqualTo(newRevisionJson);
  }
}
