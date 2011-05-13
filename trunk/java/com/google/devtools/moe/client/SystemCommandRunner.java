// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.lang.Thread;
import java.util.List;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SystemCommandRunner implements CommandRunner {

  public String runCommand(String cmd, List<String> args, String stdinData, String workingDirectory)
      throws CommandException {
    ImmutableList<String> cmdArgs = (new ImmutableList.Builder<String>()).add(cmd).
        addAll(args).build();

    ProcessBuilder pb = new ProcessBuilder(cmdArgs);
    if (workingDirectory != null && !workingDirectory.isEmpty()) {
      pb.directory(new File(workingDirectory));
    }
    Process p;
    int returnStatus;
    String stdoutData, stderrData;
    try {
      p = pb.start();
      if (stdinData != null && !stdinData.isEmpty()) {
        p.getOutputStream().write(stdinData.getBytes());
      }
      p.getOutputStream().close();
      // We need to read data from the output steams.
      // Why? Because if we don't read from them, then the process we have started will fill the
      // buffers and block. We will be in a deadlock.
      // If there were only one stream, we could just do repeated calls to read() until we got
      // EOF.
      // But because there are two streams (both stdout and stderr), we have to read from each.
      // read() is blocking, so we use available().

      Sink stdoutSink = new Sink(p.getInputStream());
      Sink stderrSink = new Sink(p.getErrorStream());

      // Sleep in longer increments when it's not generating output.
      // When it is, reset this value.
      int timeToSleep = 1;
      while(true) {
        while (stdoutSink.isAvailable()) {
          if (stdoutSink.consumeByte()) {
            timeToSleep = 1;
          } else {
            stdoutSink.closeStream();
          }
        }
        while (stderrSink.isAvailable()) {
          if (stderrSink.consumeByte()) {
            timeToSleep = 1;
          } else {
            stderrSink.closeStream();
          }
        }
        try {
          returnStatus = p.exitValue();
          break;
        } catch (IllegalThreadStateException e) {}
        timeToSleep++;
        // Never sleep more than half a second.
        if (timeToSleep > 500) {
          timeToSleep = 500;
        }
        Thread.sleep(timeToSleep);
      }
      if (stdoutSink.isAvailable()) {
        while (stdoutSink.consumeByte()) {}
      }
      if (stderrSink.isAvailable()) {
        while (stderrSink.consumeByte()) {}
      }

      stdoutData = stdoutSink.getData();
      stderrData = stderrSink.getData();
    } catch (IOException e) {
      throw new MoeProblem(String.format("Cannot run process: %s", e.getMessage()));
    } catch (InterruptedException e) {
      throw new MoeProblem(String.format("Interrupted while running process: %s", cmdArgs));
    }
    if (returnStatus == 0) {
      return stdoutData;
    }
    throw new CommandException(cmd, args, stdoutData, stderrData, returnStatus);
  }


  private static class Sink {
    private final List<Byte> bytes = Lists.newArrayList();
    private InputStream stream;

    Sink(InputStream stream) {
      this.stream = stream;
    }

    boolean isAvailable() throws IOException {
      return stream != null && stream.available() > 0;
    }

    void closeStream() {
      stream = null;
    }

    boolean consumeByte() throws IOException {
      int data = stream.read();
      if (data == -1) {
        return false;
      } else {
        bytes.add((byte)data);
        return true;
      }
    }

    String getData() {
      byte[] byteArray = new byte[bytes.size()];
      int i = 0;
      for (Byte b : bytes) {
        byteArray[i++] = b;
      }
      return new String(byteArray);
    }
  }
}
