// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

/**
 * A type used to wrap logging.
 *
 * TODO(cgruber): Replace with fluent logger when it is released (go/flogger)
 *
 * @author cgruber@google.com (Christian Gruber)
 */
public interface Messenger {

  /** Sends an informational message to the user. */
  void info(String msg);

  /** Reports an error to the user. */
  void error(String msg);

  /** Reports an error to the user, logging additional information about the error. */
  void error(Throwable e, String msg);

  /** Sends a debug message to the logs. */
  void debug(String msg);
}
