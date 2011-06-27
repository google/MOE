// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.writer;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.testing.DummyRepository;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class WriterEvaluatorTest extends TestCase {

  public void testBadExpression() throws Exception {
    try {
      WriterEvaluator.parseAndEvaluate("peanut butter hula hoops", null);
      fail();
    } catch (WritingError e) {}
  }

  public void testNoRepository() throws Exception {
    try {
      WriterEvaluator.parseAndEvaluate(
          "internal",
          ProjectContext.builder().build());
      fail();
    } catch (WritingError e) {}
  }

  public void testUneditableRepository() throws Exception {
    try {
      WriterEvaluator.parseAndEvaluate(
          "internal",
          ProjectContext.builder().withRepositories(
              ImmutableMap.of("internal", new Repository("internal", null, null, null))).build());
      fail();
    } catch (WritingError e) {}
  }

  public void testWorking() throws Exception {
    WriterEvaluator.parseAndEvaluate(
        "internal",
        ProjectContext.builder().withRepositories(
            ImmutableMap.of("internal",
                            DummyRepository.makeDummyRepository("internal", null))).build());
  }

}
