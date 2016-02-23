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

package com.google.devtools.moe.client;

import static com.google.devtools.moe.client.Ui.MOE_TERMINATION_TASK_NAME;
import static com.google.devtools.moe.client.options.OptionsParser.debugFlagPresent;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import com.google.devtools.moe.client.directives.Directive;
import com.google.devtools.moe.client.directives.Directives;
import com.google.devtools.moe.client.options.OptionsModule;
import com.google.devtools.moe.client.options.OptionsParser;
import com.google.devtools.moe.client.project.InvalidProject;

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
    abstract Injector context(); // Legacy context object for static initialization.
    public abstract OptionsParser optionsParser();
    public abstract Directives directives();
  }
  /**
   * a main() that works with the new Task framework.
   */
  public static void main(String... args) {
    System.exit(doMain(args));
  }

  /** Implements the main method logic for Moe, returning an error code if there is any */
  public static int doMain(String... args) {
    ConsoleHandler sysErrHandler = new ConsoleHandler();
    sysErrHandler.setLevel(debugFlagPresent(args) ? FINE : WARNING);
    allMoeLogger.addHandler(sysErrHandler);
    allMoeLogger.setUseParentHandlers(false);
    if (args.length < 1) {
      System.err.println("Usage: moe <directive>");
      return 1;
    }
    // This needs to get called first, so that DirectiveFactory can report errors appropriately.
    Moe.Component component =
        DaggerMoe_Component.builder().optionsModule(new OptionsModule(args)).build();
    boolean debug = component.optionsParser().debug();
    Injector.INSTANCE = component.context();
    Ui ui = component.context().ui();

    try {
      Directive directive = component.directives().getSelectedDirective();
      if (directive == null) {
        return 1; // Directive lookup will have reported the error already..
      }

      boolean parseError = !component.optionsParser().parseFlags(directive);
      if (directive.shouldDisplayHelp() || parseError) {
        return parseError ? 1 : 0;
      }

      int result = directive.perform();
      Ui.Task terminateTask = ui.pushTask(MOE_TERMINATION_TASK_NAME, "Final clean-up");
      try {
        component.context().fileSystem().cleanUpTempDirs();
      } catch (IOException e) {
        logHelper(debug, WARNING, "Moe enocuntered a problem cleaning up temporary directories", e);

      }
      ui.popTask(terminateTask, "");
      return result;
    } catch (InvalidProject e) {
      logHelper(debug, SEVERE, "Couldn't create project", e);
    } catch (MoeUserProblem e) {
      e.reportTo(ui);
      if (debug) {
        allMoeLogger.log(WARNING, "Moe encountered a problem", e);
      }
    } catch (MoeProblem m) {
      logHelper(debug, SEVERE, "Moe encountered a problem", m);
    }
    return 1;
  }

  private static void logHelper(boolean debug, Level level, String message, Throwable t) {
    allMoeLogger.log(level, message);
    if (!debug) {
      allMoeLogger.log(level, t.getMessage());
    } else {
      allMoeLogger.log(level, "", t);
    }
  }

  private Moe() {}
}
