// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.editors.TranslatorStep;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.easymock.IMocksControl;
import junit.framework.TestCase;

import java.io.File;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class EvaluatorTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testNoSuchCreator() throws Exception {
    try {
      Evaluator.create(
          new Term("foo", ImmutableMap.<String, String>of()),
          ImmutableMap.<String, Repository>of());
      fail();
    } catch (CodebaseCreationError e) {
      assertEquals("no repository foo", e.getMessage());
    }
  }

  public void testNoSuchEditor() throws Exception {
    try {
      Evaluator.edit(
          new File("/dev/null"),
          new Term("foo", ImmutableMap.<String, String>of()),
          ImmutableMap.<String, Editor>of());
      fail();
    } catch (CodebaseCreationError e) {
      assertEquals("no editor foo", e.getMessage());
    }
  }

  public void testNoSuchTranslator() throws Exception {
    try {
      Evaluator.translate(
          new File("/dev/null"),
          "internal", new Term("public", ImmutableMap.<String, String>of()),
          ImmutableMap.<TranslatorPath, Translator>of());
      fail();
    } catch (CodebaseCreationError e) {
      assertEquals("Could not find translator from project space \"internal\" to \"public\"",
                   e.getMessage());
    }
  }

  public void testParseAndEvaluate() throws Exception {
    IMocksControl control = EasyMock.createControl();
    CodebaseCreator cc = control.createMock(CodebaseCreator.class);
    Editor e = control.createMock(Editor.class);
    Editor translatorEditor = control.createMock(Editor.class);

    File firstDir = new File("/first");
    File secondDir = new File("/second");
    File finalDir = new File("/final");

    Translator t = new Translator(
        "foo", "public", ImmutableList.<TranslatorStep>of(
            new TranslatorStep("quux", translatorEditor)));

    Codebase created = new Codebase(
        firstDir, "foo",
        new CodebaseExpression(new Term("foo", ImmutableMap.<String, String>of())));

    expect(cc.create(ImmutableMap.<String, String>of())).andReturn(created);
    expect(translatorEditor.getDescription()).andReturn("");
    expect(translatorEditor.edit(firstDir)).andReturn(secondDir);
    expect(e.getDescription()).andReturn("");
    expect(e.edit(secondDir)).andReturn(finalDir);

    control.replay();
    Codebase c = Evaluator.parseAndEvaluate(
        "foo>public|bar",
        ProjectContext.builder()
        .withRepositories(ImmutableMap.of("foo", new Repository("foo", null, cc, null)))
        .withTranslators(ImmutableMap.of(t.getTranslatorPath(), t))
        .withEditors(ImmutableMap.of("bar", e)).build());

    control.verify();
    assertEquals(finalDir, c.getPath());
    assertEquals("public", c.getProjectSpace());
    assertEquals("foo>public|bar", c.getExpression().toString());
  }
}
