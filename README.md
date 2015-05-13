# Moe
*Make Open Easy*

## Introduction

MOE is a system for synchronzing, translating, and scrubbing source code repositories.  Often, a project needs to exist in two forms, but maintaining code in two repositories is burdensome.  MOE allows users to:

  * synchronize (in either or both directions) between two source code repositories
  * use different types of repositories (svn, hg, git) in combinations
  * maintain "scrubbed" content in an internal or private repository.
  * transform project paths to support different layouts/structures in different repositories
  * propagate or hide individual commits, commit-authorship, and other metadata between repositories whild syncing.

## Project Status

MOE was created around 2011, but has not had a lot of love. Google teams that maintain open-source
releases (guava, dagger, auto, etc.) use it regularly, so we dusted it off to share fixes,
improvements, and help folks who use it outside of Google.

The project is currently undergoing a fair bit of re-factoring and needs a documentation update, which is forthcoming.

## Usage

### Building MOE

   1. Install Apache Ant if you don't have it already.
   2. Checkout the Java-MOE source `git clone git@github.com:google/MOE.git`
   3.  In the top-level directory that contains the build.xml file, run:
     - `ant jar` to compile the source and build the jar (outputs to build/jar/)
     - `ant test` to compile the tests and run them
     - `ant clean` to delete any generated files

### Running MOE

Once you have the .jar, you can run:
"java -jar path/to/java-moe.jar <arguments for MOE>"

Alternatively, you can make the .jar executable and then run:
"./java-moe.jar <arguments for MOE>"

## Contributing

Contributing to MOE is subject to the guidelines in the CONTRIBUTING.md file, which, in brief, requires that contributors sign the [Individual Contributor License Agreement (CLA)][1].

## License

```
  Copyright 2011 Google, Inc. All Rights Reserved.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
```

[1]: https://spreadsheets.google.com/spreadsheet/viewform?formkey=dDViT2xzUHAwRkI3X3k5Z0lQM091OGc6MQ&ndplr=1
