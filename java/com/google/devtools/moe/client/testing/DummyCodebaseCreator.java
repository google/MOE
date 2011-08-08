// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.parser.Term;

import java.io.File;
import java.util.Map;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DummyCodebaseCreator implements CodebaseCreator {

  private final String name;
  private final String projectSpace;

  public DummyCodebaseCreator(String repositoryName, String projectSpace) {
    this.name = repositoryName;
    this.projectSpace = projectSpace;
  }

  public Codebase create(Map<String, String> options) throws CodebaseCreationError{
    String revId = options.get("revision");
    if (revId == null) {
      revId = "HEAD";
    }


    return new Codebase(
        new File("/dummy/path"), projectSpace,
        new CodebaseExpression(new Term(name, ImmutableMap.<String, String>of())));
  }

  public String getProjectSpace() {
    return projectSpace;
  }
}
