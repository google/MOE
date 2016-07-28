/*
 * Copyright (c) 2016 Google, Inc.
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

package com.google.devtools.moe.client;

import static com.google.devtools.moe.client.Ui.MOE_TERMINATION_TASK_NAME;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import com.google.devtools.moe.client.directives.Directive;
import com.google.devtools.moe.client.directives.Directives;
import com.google.devtools.moe.client.options.OptionsParser;
import com.google.devtools.moe.client.project.InvalidProject;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * MOE (Make Open Easy) abstract executable.
 *
 * This class exists to implement MOE's core program flow, and by default the Moe executable
 * is a simple subclass of AbstractMoeExecutable.
 *
 * Moe's object graph is managed by <a href="http://google.github.io/dagger">Dagger</a>.
 * To allow for local customizations (such as local usage of otherwise unsupported repository types,
 * or special company-specific scrubbers) clients will subclass this type similar to the way
 * {@link Moe} does, but should declare a dagger component interface that (a) inherits from
 * {@link AbstractMoeExecutable.Component} and (b) declares alternative module sets that provide
 * different injections.
 *
 * The basic entry-point method is {@link #run(String...)}.  Alternative clients should subclass
 * {@link AbstractMoeExecutable} and implement their own main method roughly along these
 * lines (possibly surrounding the call to run() with any custom setup/tear-down static logic,
 * like custom local logging, etc.:<pre>{@code
 *   public static void main(String... args) {
 *     System.exit(new CustomMoeExcutable().run(args));
 *   }
 * }</pre>
 *
 * <p>Progress is written to STDOUT (by default).  Errors are logged to a default logger.  The
 * logger's handler is {@code com.google.devtools.moe} and by default logging levels are set to
 * WARNING unless {@code --debug} is passed in as a flag, in which case logging is set to FINE
 *
 * <p>Errors are SEVERE logs written (by default) to STDERR.
 *
 * @see AbstractMoeExecutable.Component
 */
public abstract class AbstractMoeExecutable<T extends AbstractMoeExecutable<T>> {
  public static final Logger globalLogger = Logger.getLogger("com.google.devtools.moe");
  private static final ConsoleHandler sysErrHandler = new ConsoleHandler();

  {
    sysErrHandler.setLevel(Level.WARNING); // set default
    globalLogger.addHandler(sysErrHandler);
    globalLogger.setUseParentHandlers(false);
  }

  @Inject Injector services; // Legacy context object for static initialization.
  @Inject OptionsParser optionsParser;
  @Inject Directives directives;
  @Inject Ui ui;
  @Inject FileSystem filesystem;

  private boolean debug = false;

  @SuppressWarnings("unchecked")
  private int init(String... args) {
    if (args.length < 1) {
      System.err.println("Usage: moe <directive>");
      return 64; // Unix usage error code
    }

    initializeComponent(args).inject((T) this);
    Injector.INSTANCE = services; // TODO(cgruber): Eliminate this.
    debug = optionsParser.debug();
    if (debug) {
      // This should be the only static state set.
      sysErrHandler.setLevel(Level.FINE);
    }
    return 0;
  }

  public final int run(String... args) {
    try {
      int result = init(args);
      if (result != 0) {
        return result;
      }

      Directive directive = directives.getSelectedDirective(); // Can't be null
      boolean parseError = !optionsParser.parseFlags(directive);
      if (directive.shouldDisplayHelp() || parseError) {
        return parseError ? 64 : 0;
      }

      result = directive.perform();
      Ui.Task terminateTask = ui.pushTask(MOE_TERMINATION_TASK_NAME, "Final clean-up");
      try {
        filesystem.cleanUpTempDirs();
      } catch (IOException e) {
        ui.message(
            "WARNING: Moe enocuntered a problem cleaning up temporary directories: %s",
            e.getMessage());
      }
      ui.popTask(terminateTask, "");
      return result;
    } catch (InvalidProject ip) {
      ui.message("ERROR: Invalid project configuration: %s", ip.getMessage());
      if (debug) {
        globalLogger.log(SEVERE, "", ip);
      }
    } catch (MoeUserProblem mup) {
      mup.reportTo(ui);
      if (debug) {
        globalLogger.log(WARNING, "", mup);
      }
    } catch (MoeProblem mp) {
      ui.message("ERROR: Moe encountered a problem: %s", mp.getMessage());
      if (debug) {
        globalLogger.log(SEVERE, "", mp);
      }
    } catch (Throwable t) {
      globalLogger.log(SEVERE, "Unhandled exception " + t.getClass().getSimpleName(), t);
    }
    return 1;
  }

  /**
   * A method that extenders can override to supply their specific component.
   *
   * TODO(cgruber): Add example of a local customization.
   */
  protected abstract Component<T> initializeComponent(String[] args);

  /**
   * The Dagger surface for a MOE application.
   *
   * Local variants of Moe should create a sub-interface of this and mark it as a
   * {@code @}{@link dagger.Component}.  The  {@link AbstractMoeExecutable#run(String...)} method
   * will, among other things, call the subclass-supplied component and inject the main Object.
   *
   * This is the key entry point of the <a href="http://google.github.io/dagger">dagger</a>-managed
   * object graph.
   *
   * @see dagger.Component
   */
  public interface Component<T extends AbstractMoeExecutable<T>> {
    void inject(T instance);
  }
}
