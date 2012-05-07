// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.parser;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.FileSystem.Lifetime;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.editors.ForwardTranslator;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.editors.TranslatorStep;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.Map;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ExpressionTest extends TestCase {
  
  private static final Map<String, String> EMPTY_MAP = ImmutableMap.of();

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testNoSuchCreator() throws Exception {
    try {
      new RepositoryExpression("foo").createCodebase(ProjectContext.builder().build());
      fail();
    } catch (CodebaseCreationError expected) {
      assertEquals("no repository foo", expected.getMessage());
    }
  }

  public void testFileCodebaseCreator() throws Exception {
    IMocksControl control = EasyMock.createControl();
    FileSystem mockFs = control.createMock(FileSystem.class);
    AppContext.RUN.fileSystem = mockFs;
    expect(mockFs.exists(new File("/foo"))).andReturn(true);
    expect(mockFs.isDirectory(new File("/foo"))).andReturn(true);
    File copyLocation = new File("/tmp/copy");
    expect(mockFs.getTemporaryDirectory("file_codebase_copy_")).andReturn(copyLocation);
    // Short-circuit Utils.copyDirectory().
    mockFs.makeDirsForFile(copyLocation);
    expect(mockFs.isFile(new File("/foo"))).andReturn(true);
    mockFs.copyFile(new File("/foo"), copyLocation);
    mockFs.setLifetime(EasyMock.eq(copyLocation), EasyMock.<Lifetime>anyObject());
    mockFs.cleanUpTempDirs();

    RepositoryExpression repoEx = new RepositoryExpression("file").withOption("path", "/foo");

    control.replay();
    Codebase c = repoEx.createCodebase(ProjectContext.builder().build());
    control.verify();

    assertEquals(copyLocation, c.getPath());
    assertEquals("public", c.getProjectSpace());
    assertEquals(repoEx, c.getExpression());
  }

  public void testNoSuchEditor() throws Exception {
    try {
      ProjectContext context = ProjectContext.builder()
          .withEditors(ImmutableMap.<String, Editor>of())
          .build();
      
      IMocksControl control = EasyMock.createControl();
      RepositoryExpression mockRepoEx = control.createMock(RepositoryExpression.class);
      expect(mockRepoEx.createCodebase(context)).andReturn(null);  // Codebase unneeded
      
      Expression ex = new EditExpression(
          mockRepoEx,
          new Operation(Operator.EDIT, new Term("noSuchEditor", EMPTY_MAP)));
      
      control.replay();
      ex.createCodebase(context);
      fail();
    } catch (CodebaseCreationError expected) {
      assertEquals("no editor noSuchEditor", expected.getMessage());
    }
  }

  public void testNoSuchTranslator() throws Exception {
    try {
      ProjectContext context = ProjectContext.builder()
          .withEditors(ImmutableMap.<String, Editor>of())
          .build();
      
      IMocksControl control = EasyMock.createControl();
      RepositoryExpression mockRepoEx = control.createMock(RepositoryExpression.class);
      Codebase mockRepoCodebase = control.createMock(Codebase.class);
      expect(mockRepoCodebase.getProjectSpace()).andReturn("internal").times(2);
      expect(mockRepoEx.createCodebase(context)).andReturn(mockRepoCodebase);
      
      Expression ex = new TranslateExpression(
          mockRepoEx,
          new Operation(Operator.TRANSLATE, new Term("public", EMPTY_MAP)));

      control.replay();
      ex.createCodebase(context);
      fail();
    } catch (CodebaseCreationError expected) {
      assertEquals("Could not find translator from project space \"internal\" to \"public\"",
                   expected.getMessage());
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

    TranslatorPath tPath = new TranslatorPath("foo", "public");
    Translator t = new ForwardTranslator(ImmutableList.<TranslatorStep>of(
        new TranslatorStep("quux", translatorEditor)));
    
    ProjectContext context = ProjectContext.builder()
        .withRepositories(ImmutableMap.of("foo", new Repository("foo", null, cc, null)))
        .withTranslators(ImmutableMap.of(tPath, t))
        .withEditors(ImmutableMap.of("bar", e)).build();

    Codebase firstCb = new Codebase(new File("/first"), "foo",
        new RepositoryExpression(new Term("foo", EMPTY_MAP)));
    
    Codebase secondCb = new Codebase(new File("/second"), "public",
        new RepositoryExpression(new Term("foo2", EMPTY_MAP)));
    
    Codebase finalCb = new Codebase(new File("/final"), "public",
        new RepositoryExpression(new Term("foo3", EMPTY_MAP)));

    expect(cc.create(EMPTY_MAP)).andReturn(firstCb);
    expect(translatorEditor.edit(firstCb, context, EMPTY_MAP)).andReturn(secondCb);
    expect(e.getDescription()).andReturn("");
    expect(e.edit(secondCb, context, EMPTY_MAP)).andReturn(finalCb);

    control.replay();
    Codebase c = Parser.parseExpression("foo>public|bar").createCodebase(context);

    control.verify();
    assertEquals(finalDir, c.getPath());
    assertEquals("public", c.getProjectSpace());
    assertEquals("foo>public|bar", c.getExpression().toString());
  }
}
