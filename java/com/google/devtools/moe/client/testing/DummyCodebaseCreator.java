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

package com.google.devtools.moe.client.testing;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.parser.Term;

import java.io.File;
import java.util.Map;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DummyCodebaseCreator implements CodebaseCreator {
  private final FileSystem filesystem;
  private final String name;
  private final String projectSpace;

  public DummyCodebaseCreator(FileSystem filesystem, String repositoryName, String projectSpace) {
    this.filesystem = filesystem;
    this.name = repositoryName;
    this.projectSpace = projectSpace;
  }

  @Override
  public Codebase create(Map<String, String> options) throws CodebaseCreationError {
    String revId = options.get("revision");
    if (revId == null) {
      revId = "1";
    }

    return new Codebase(
        filesystem,
        new File("/dummy/codebase/" + name + "/" + revId),
        projectSpace,
        new RepositoryExpression(new Term(name, ImmutableMap.<String, String>of())));
  }
}
