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

package com.google.devtools.moe.client.options;

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
