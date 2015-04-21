// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import static com.google.devtools.moe.client.Ui.MOE_TERMINATION_TASK_NAME;

import com.google.devtools.moe.client.directives.Directive;
import com.google.devtools.moe.client.directives.Directives;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;

/**
 * MOE (Make Open Easy) client.
 *
 * Progress is written to STDOUT, then logged as INFO logs. We suppress INFO logs
 * by default unless you programmatically set a handler on com.google.devtools.moe.
 *
 * Errors are SEVERE logs written to STDERR.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Moe {
  static final Logger allMoeLogger = Logger.getLogger("com.google.devtools.moe");

  /**
   * The Dagger surface for the MOE application.
   */
  // TODO(cgruber): Turn Injector into the component.
  @Singleton
  @dagger.Component(modules = MoeModule.class)
  public abstract static class Component {
    public abstract Injector context(); // Legacy context object for static initialization.

    public abstract Directives directives();
  }

  /**
   * a main() that works with the new Task framework.
   */
  public static void main(String[] args) {
    ConsoleHandler sysErrHandler = new ConsoleHandler();
    sysErrHandler.setLevel(Level.WARNING);
    allMoeLogger.addHandler(sysErrHandler);
    allMoeLogger.setUseParentHandlers(false);
    System.exit(doMain(args));
  }

  /** Implements the main method logic for Moe, returning an error code if there is any */
  static int doMain(String... args) {
    if (args.length < 1) {
      System.err.println("Usage: moe <directive>");
      return 1;
    }
    // This needs to get called first, so that DirectiveFactory can report errors appropriately.
    Moe.Component component = DaggerMoe_Component.create();
    Injector.INSTANCE = component.context();

    try {
      Directive d = component.directives().getDirective(args[0]);
      if (d == null) {
        return 1; // Directive lookup will have reported the error already..
      }
      boolean parseError = !d.parseFlags(args);
      if (d.getFlags().shouldDisplayHelp() || parseError) {
        return parseError ? 1 : 0;
      }
      int result = d.perform();
      Ui.Task terminateTask =
          Injector.INSTANCE.ui().pushTask(MOE_TERMINATION_TASK_NAME, "Final clean-up");
      Injector.INSTANCE.fileSystem().cleanUpTempDirs();
      Injector.INSTANCE.ui().popTask(terminateTask, "");
      return result;
    } catch (MoeProblem m) {
      // TODO(dbentley): implement verbose mode; if it is above a threshold, print a stack trace.
      Injector.INSTANCE.ui().error(m, "Moe encountered a problem; look above for explanation");
      return 1;
    } catch (IOException e) {
      return 1;
    }
    // Exit early since we're postponing Task usage until post-dagger2.

    /*
    // Tasks are a work in progress, which are only currently used to implement a
    // HelloWorld style trivial task.  For now, leave them be until we get directive/task
    // scopes hooked up in dagger.  Then this can be re-examined.

    TaskType t = TaskType.TASK_MAP.get(args[0]);
    if (t == null) {
      // We did not find a task for this name. We should print help and quit.
      // But because we are in the process of converting from the old Directive framework to
      // the new Task framework, we may instead have to run oldMain. Therefore, don't
      // System.exit; just return.
      // TODO(dbentley): kill all Directives, print the relevant help, and exit instead
      // of calling directiveMain().
      try {
        directiveMain(args);
      } catch (IOException e) {
        System.exit(1);
        return;
      }
      return;
    }

    // Strip off the task name
    // This mutates t.getOptions, and so has to be called before we create the graph.
    Flags.parseOptions(t.getOptions(), ImmutableList.copyOf(args).subList(1, args.length));
    ObjectGraph injector = ObjectGraph.create(t, new MoeModule());
    Task task = injector.get(Task.class);

    Task.Explanation result = task.executeAtTopLevel();
    if (!Strings.isNullOrEmpty(result.message)) {
      logger.info(result.message);
      System.out.println(result.message);
    }
    System.exit(result.exitCode);
    */
  }

  private Moe() {}
}
