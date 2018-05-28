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

import static com.google.common.base.Strings.repeat;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.devtools.moe.client.FileSystem.Lifetime;
import com.google.devtools.moe.client.qualifiers.Flag;
import dagger.Provides;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * Represents the command-line user interface for MOE.
 */
@Singleton
public class Ui {
  /**
   * The name of the Task pushed to the Ui for clean-up when MOE is about to exit. This name is
   * used, for example, to set a temp dir's lifetime to "clean up when MOE is about to exit".
   */
  public static final String MOE_TERMINATION_TASK_NAME = "moe_termination";

  private final PrintStream out;
  private final Deque<Task> tasks;
  private final boolean shouldTrace;

  // We store the task that is the current output, if any, so that we can special case a Task that
  // is popped right after it is pushed. In this case, we can output: "Doing...Done" on one line.
  private Ui.Task currentOutput;

  protected final FileSystem fileSystem;

  public Ui(OutputStream out) {
    this(out, new NoopFileSystem(), false);
  }

  public Ui(OutputStream out, FileSystem fileSystem) {
    this(out, fileSystem, false);
  }

  @Inject
  public Ui(OutputStream out, FileSystem fileSystem, @Flag("trace") boolean shouldTrace) {
    this(out, fileSystem, shouldTrace, new ArrayDeque<>());
  }

  @VisibleForTesting
  public Ui(
      OutputStream out,
      FileSystem fileSystem,
      @Flag("trace") boolean shouldTrace,
      Deque<Task> tasks) {
    try {
      this.out = new PrintStream(out, /*autoFlush*/ true, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new MoeProblem(e, "Invalid character set.");
    }
    this.shouldTrace = shouldTrace;
    this.fileSystem = fileSystem;
    this.tasks = tasks;
  }

  /**
   * Clears the current output, if applicable.
   */
  private void clearOutput() {
    if (currentOutput != null) {
      // We're in the middle of a line, so start a new one.
      out.println();
    }
    currentOutput = null;
  }

  private String indent(CharSequence msg) {
    String indentation =
        this.shouldTrace
            ? repeat("  ", tasks.size()) // use all tasks, including trace tasks.
            : repeat("  ", (int) tasks.stream().filter(t -> !t.traceOnly).count());
    return indentation + Joiner.on("\n" + indentation).join(Splitter.on('\n').split(msg));
  }

  public void message(String msg, Object... args) {
    clearOutput();
    out.println(indent(String.format(msg, args)));
  }

  public static class Task implements Closeable {
    public final Ui ui;
    public final String taskName;
    public final String description;
    public final boolean traceOnly;
    public final DateTime start = DateTime.now();
    public final StringBuilder result = new StringBuilder();
    private final List<File> kept = new ArrayList<>();

    Task(Ui ui, String taskName, boolean traceOnly, String descriptionFormat, Object... args) {
      ui.tasks.push(this);
      this.ui = ui;
      this.taskName = taskName;
      this.traceOnly = traceOnly;
      this.description = String.format(descriptionFormat, args);
    }

    @Override
    public String toString() {
      return taskName;
    }

    public Duration duration() {
      return new Duration(start, DateTime.now());
    }

    public StringBuilder result() {
      return result;
    }

    /**
     * Pops a task from the Task Stack, persisting any "kept" Files beyond this Task. In general,
     * use this if you call {@link FileSystem#getTemporaryDirectory(String, Lifetime)} within a Task
     * and need to keep the resulting temp dir past completion of this Task.
     *
     * @see #keep(File)
     */
    @Override
    public void close() {
      result.append(kept.stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(",")));
      if (ui.tasks.isEmpty()) {
        throw new MoeProblem("Tried to end task %s, but stack is empty", taskName);
      }

      Task current = ui.tasks.pop();

      if (current != this) {
        throw new MoeProblem(
            "Tried to end task %s, but the current task was: %s", taskName, current);
      }

      if (ui.fileSystem != null && !this.traceOnly) {
        try {
          ui.fileSystem.cleanUpTempDirs();
        } catch (IOException ioEx) {
          throw new MoeProblem(ioEx, "Error cleaning up temp dirs.");
        }
      }

      if (ui.shouldTrace || !this.traceOnly) {
        if (result.length() == 0) {
          result.append("Done");
        }
        if (ui.shouldTrace) {
          result.append(" [").append(duration().getMillis()).append("ms]");
        }
        String output =
            ui.currentOutput == this
                ? this.result.toString() // The last thing we printed was starting this task
                : ui.indent("DONE: " + description + ": " + result);

        ui.out.println(output);
      }
      ui.currentOutput = null;
    }

    /**
     * Keep the key files of a resource. This will execute {@link #keep(File)} on each path in the
     * returned iterable.
     */
    public <T extends Keepable<T>> T keep(T toKeep) {
      Collection<Path> paths = toKeep.toKeep();
      paths.forEach(path -> this.keep(path));
      return toKeep;
    }

    /**
     * Keep a file past the life of this task (specifically don't clean up the supplied file when
     * this task is closed).
     *
     * <p>If there is a parent Task on the stack after the one being popped here, then {@code file}
     * will be cleaned up when the parent Task is popped (unless it's kept within that Task too). If
     * there is no parent Task, then {@code file} will be persisted beyond MOE execution. So any
     * results persisted beyond a top-level Task constitute outputs of MOE execution.
     */
    public Path keep(Path toKeep) {
      return keep(toKeep.toFile()).toPath();
    }

    /**
     * Keep a file past the life of this task (specifically don't clean up the supplied file when
     * this task is closed).
     *
     * <p>If there is a parent Task on the stack after the one being popped here, then {@code file}
     * will be cleaned up when the parent Task is popped (unless it's kept within that Task too). If
     * there is no parent Task, then {@code file} will be persisted beyond MOE execution. So any
     * results persisted beyond a top-level Task constitute outputs of MOE execution.
     */
    public File keep(File toKeep) {
      kept.add(toKeep);
      if (ui.fileSystem != null) {
        Lifetime newLifetime;
        if (ui.tasks.size() == 1) {
          newLifetime = Lifetimes.persistent();
        } else {
          Task parentTask = Iterables.get(ui.tasks, 1);
          newLifetime = new TaskLifetime(parentTask, ui);
        }
        ui.fileSystem.setLifetime(toKeep, newLifetime);
      }
      return toKeep;
    }
  }

  /**
   * Represents a resource that has specific paths which may need to be omitted from filesystem
   * cleanup.
   */
  public interface Keepable<T extends Keepable<T>> {
    /** Return any resources that need to be omitted from cleanup by a task. */
    Collection<Path> toKeep();

    /** A convenience method to allow chaining to work cleanly, while still using the keep apis. */
    @SuppressWarnings("unchecked") // self type.
    default T keep(Task task) {
      return task.keep((T) this);
    }
  }

  /**
   * Pushes a task onto the Task Stack.
   *
   * <p>MOE's UI operates on a stack model. Tasks get pushed onto the stack and then what is popped
   * must be the top task on the stack, allowing nesting only.
   *
   * @param taskName the name of the task; should be sensical to a computer
   * @param descriptionFormat a String.format() template for the description of what MOE is about to
   *     do, suitable for a user.
   * @param args arguments which will be used to format the descriptionFormat template
   * @return the Task created
   */
  public Task newTask(String taskName, String descriptionFormat, Object... args) {
    return newTask(taskName, false, descriptionFormat, args);
  }

  /**
   * Pushes a task onto the Task Stack.
   *
   * <p>MOE's UI operates on a stack model. Tasks get pushed onto the stack and then what is popped
   * must be the top task on the stack, allowing nesting only.
   *
   * <p>Tasks generally clean up after themselves, unless their completion preserves a file created.
   * The {@code trace} parameter both disables this cleanup. It also disables output unless the
   * --trace flag is set.
   *
   * @param taskName the name of the task; should be sensical to a computer
   * @param traceTask Whether this task should report output only with --trace
   * @param descriptionFormat a String.format() template for the description of what MOE is about to
   *     do, suitable for a user.
   * @param args arguments which will be used to format the descriptionFormat template
   * @return the Task created
   */
  public Task newTask(
      String taskName, boolean traceTask, String descriptionFormat, Object... args) {
    // If not a trace task, or if --trace is enabled.
    if (this.shouldTrace || !traceTask) {
      clearOutput();
      String description = String.format(descriptionFormat, args);
      String indented = indent(description + "... ");
      out.print(indented);
    }
    return currentOutput = new Task(this, taskName, traceTask, descriptionFormat, args);
  }

  Lifetime currentTaskLifetime() {
    Preconditions.checkState(!tasks.isEmpty());
    return new TaskLifetime(tasks.peek(), this);
  }

  Lifetime moeExecutionLifetime() {
    return new MoeExecutionLifetime();
  }

  /**
   * A {@code Lifetime} for a temp dir that should be cleaned up when the given Task is completed.
   */
  private static class TaskLifetime implements Lifetime {

    private final Task task;
    private final Ui ui;

    TaskLifetime(Task task, Ui ui) {
      this.ui = ui;
      this.task = task;
    }

    @Override
    public boolean shouldCleanUp() {
      return !ui.tasks.contains(task);
    }
  }

  /**
   * A {@code Lifetime} for a temp dir that should be cleaned up when MOE completes execution.
   */
  private class MoeExecutionLifetime implements Lifetime {

    @Override
    public boolean shouldCleanUp() {
      return !tasks.isEmpty() && tasks.peek().taskName.equals(MOE_TERMINATION_TASK_NAME);
    }
  }

  /**
   * A module to supply the OutputStream used in the UI.  In testing, this can be overridden
   * passing in a {@code ByteArrayOutputStream} or some other string-bearing stream.
   */
  @dagger.Module
  public static class UiModule {
    @Provides
    @Singleton
    @SuppressWarnings("CloseableProvides") // Provided for the life of the application.
    public OutputStream uiOutputStream() {
      return System.err;
    }
  }
}
