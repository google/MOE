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
  void info(String msgfmt, Object... args);

  /** Reports an error to the user. */
  void error(String msgfmt, Object... args);

  /** Reports an error to the user, logging additional information about the error. */
  void error(Throwable e, String msgfmt, Object... args);

  /** Sends a debug message to the logs. */
  void debug(String msgfmt, Object... args);
}
