# Moe
*Make Open Easy*

[![LICENSE](https://img.shields.io/badge/license-Apache-blue.svg)](https://github.com/google/MOE/blob/master/LICENSE)
[![Travis CI](https://img.shields.io/travis/google/MOE.svg)](https://travis-ci.org/google/MOE)
[![GitHub Issues](https://img.shields.io/github/issues/google/MOE.svg)](https://github.com/google/MOE/issues)
[![GitHub Pull Requests](https://img.shields.io/github/issues-pr/google/MOE.svg)](https://github.com/google/MOE/pulls)

## Introduction

MOE is a system for synchronizing, translating, and scrubbing source code
repositories.  Often, a project needs to exist in two forms, typically because
it is released in open-source, which may use a different build system, only
be a subset of the wider project, etc.  Maintaining code in two repositories
is burdensome. MOE allows users to:

  * synchronize (in either or both directions) between two source code
    repositories
  * use different types of repositories (svn, hg, git) in combinations
  * maintain "scrubbed" content in an internal or private repository.
  * transform project paths to support different layouts/structures in
    different repositories
  * propagate or hide individual commits, commit-authorship, and other
    metadata between repositories while syncing.

## Project Status

MOE was created around 2011, but has not had a lot of love. Google teams that
maintain open-source releases (guava, dagger, auto, etc.) use it regularly,
so we dusted it off to share fixes, improvements, and help folks who use it
outside of Google.

The project is currently undergoing a fair bit of re-factoring and needs a
documentation update, which is forthcoming.

## Usage

### Building MOE

1. Install Apache Maven 3.1 if you don't already have it
   2. Checkout the Java-MOE source `git clone git@github.com:google/MOE.git`
   3.  In the top-level directory that contains the build.xml file, run:
     - `mvn install`
     - `util/make-binary.sh`
   4. The moe client binary should be created at `client/target/moe`


### Running MOE

Once you have the `moe` binary, you should be able to simply run:
`moe <arguments for MOE>`

## Contributing

Contributing to MOE is subject to the guidelines in the CONTRIBUTING.md file,
which, in brief, requires that contributors sign the [Individual Contributor
License Agreement (CLA)][CLA].

[CLA]: https://cla.developers.google.com/


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


