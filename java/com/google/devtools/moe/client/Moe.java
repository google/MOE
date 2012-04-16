// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.directives.Directive;
import com.google.devtools.moe.client.directives.DirectiveFactory;
import com.google.devtools.moe.client.tasks.HelloTask;
import com.google.devtools.moe.client.tasks.Task;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private static final Logger logger = Logger.getLogger(Moe.class.getName());
  static final Logger allMoeLogger = Logger.getLogger("com.google.devtools.moe");

  /**
   * The name of the Task pushed to the Ui for clean-up when MOE is about to exit. This name is
   * used, for example, to set a temp dir's lifetime to "clean up when MOE is about to exit".
   */
  static final String MOE_TERMINATION_TASK_NAME = "moe_termination";

  private Moe() {}

  /**
   * a main() that works with the new Task framework.
   */
  public static void main(String[] args) {
    ConsoleHandler sysErrHandler = new ConsoleHandler();
    sysErrHandler.setLevel(Level.WARNING);
    allMoeLogger.addHandler(sysErrHandler);

    allMoeLogger.setUseParentHandlers(false);

    if (args.length < 1) {
      System.err.println("Usage: moe <directive>");
      System.exit(1);
    }

    // This needs to get called first, so that DirectiveFactory can report
    // errors appropriately.
    AppContext.init();

    TaskType t = taskMap.get(args[0]);
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
    // This mutates t.getOptions, and so has to be called before we create the injector.
    Flags.parseOptions(t.getOptions(), ImmutableList.copyOf(args).subList(1, args.length));
    Injector injector = Guice.createInjector(t, new MoeModule());
    Task task = injector.getInstance(Task.class);

    Task.Explanation result = task.executeAtTopLevel();
    if (!Strings.isNullOrEmpty(result.message)) {
      logger.info(result.message);
      System.out.println(result.message);
    }
    System.exit(result.exitCode);
  }

  public static class TaskType extends AbstractModule {
    private final String desc;
    private final MoeOptions options;
    private final Class<? extends Task.TaskCreator> taskCreatorClass;

    public MoeOptions getOptions() {
      return options;
    }

    protected void configure() {
      bind(MoeOptions.class).toInstance(options);
      bind(Task.TaskCreator.class).to(taskCreatorClass);
    }

    @Provides
    public Task provideTask(Task.TaskCreator tc, MoeOptions options) {
      return tc.createTaskFromCommandLine(options);
    }

    public TaskType(String desc, MoeOptions options,
                    Class<? extends Task.TaskCreator> taskCreatorClass) {
      this.desc = desc;
      this.options = options;
      this.taskCreatorClass = taskCreatorClass;
    }
  }

  public static Map<String, TaskType> taskMap = ImmutableMap.of(
      HelloTask.commandLineName, new TaskType(
          "Prints welcome message",
          new HelloTask.HelloOptions(),
          HelloTask.HelloTaskCreator.class));

  // TODO(dbentley): remove all of this code when we're finished moving to Task
  public static void directiveMain(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Usage: moe <directive>");
      System.exit(1);
    }

    Directive d = DirectiveFactory.makeDirective(args[0]);
    if (d == null) {
      // DirectiveFactory.makeDirective has failed and reported the error.
      System.exit(1);
    }

    MoeOptions flags = d.getFlags();
    CmdLineParser parser = new CmdLineParser(flags);
    boolean parseError = false;
    try {
      List<String> nextArgs = Lists.newArrayList(args);
      nextArgs.remove(0); // Remove the directive.
      parser.parseArgument(
          processArgs(nextArgs).toArray(new String[] {}));
    } catch (CmdLineException e) {
      logger.log(Level.SEVERE, "Failure", e);
      parseError = true;
    }

    if (flags.shouldDisplayHelp() || parseError) {
      parser.printUsage(System.err);
      System.exit(parseError ? 1 : 0);
    }

    try {
      int result = d.perform();
      Ui.Task terminateTask = AppContext.RUN.ui.pushTask(
          MOE_TERMINATION_TASK_NAME, "Final clean-up");
      AppContext.RUN.fileSystem.cleanUpTempDirs();
      AppContext.RUN.ui.popTask(terminateTask, "");
      System.exit(result);
    } catch (MoeProblem m) {
      // TODO(dbentley): have an option for verbosity; if it is above a threshold, print
      // a stack trace.
      AppContext.RUN.ui.error(
          m, "Moe encountered a problem; look above for explanation");
      System.exit(1);
    }
  }

  private static List<String> processArgs(List<String> args) {
    // Args4j has a different format that the old command-line parser.
    // So we use some voodoo to get the args into the format that args4j
    // expects.
    Pattern argPattern = Pattern.compile("(--[a-zA-Z_]+)=(.*)");
    Pattern quotesPattern = Pattern.compile("^['\"](.*)['\"]$");
    List<String> processedArgs = Lists.newArrayList();

    for (String arg : args) {
      Matcher matcher = argPattern.matcher(arg);
      if (matcher.matches()) {
        processedArgs.add(matcher.group(1));

        String value = matcher.group(2);
        Matcher quotesMatcher = quotesPattern.matcher(value);
        if (quotesMatcher.matches()) {
          processedArgs.add(quotesMatcher.group(1));
        } else {
          processedArgs.add(value);
        }
      } else {
        processedArgs.add(arg);
      }
    }

    return processedArgs;
  }
}
