// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.collect.Sets;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.util.Set;

public class BooleanOptionHandler extends OptionHandler<Boolean> {
  private static final Set<String> TRUES = Sets.newHashSet("true", "on", "yes", "1");
  private static final Set<String> FALSES = Sets.newHashSet("false", "off", "no", "0");

  public BooleanOptionHandler(
      CmdLineParser parser, OptionDef option, Setter<? super Boolean> setter) {
    super(parser, option, setter);
  }

  @Override
  public int parseArguments(Parameters params) throws CmdLineException {
    String param = null;
    try {
      param = params.getParameter(0);
    } catch (CmdLineException expected) {
    }

    if (param == null) {
      setter.addValue(true);
      return 0;
    } else {
      String lowerParam = param.toLowerCase();
      if (TRUES.contains(lowerParam)) {
        setter.addValue(true);
      } else if (FALSES.contains(lowerParam)) {
        setter.addValue(false);
      } else {
        setter.addValue(true);
        return 0;
      }
      return 1;
    }
  }

  @Override
  public String getDefaultMetaVariable() {
    return null;
  }
}
