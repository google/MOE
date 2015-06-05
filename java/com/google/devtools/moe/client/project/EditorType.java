// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

/**
 * Enum of all possible editors.
 *
 * All values are valid JSON editor types.
 *
 * @author dbentley@google.com (Dan Bentley)
 */
public enum EditorType {
  identity,
  scrubber,
  patcher,
  shell,
  renamer;
}
