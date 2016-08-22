Contributing
============

If you would like to contribute code to MOE you can do so through GitHub
by forking the repository and sending a pull request.

When submitting code, please make every effort to follow existing conventions
and style in order to keep the code as readable as possible.  

Where appropriate, please provide unit tests or integration tests. Unit tests
should be JUnit based tests and can use either standard JUnit assertions or
Truth assertions and be added to `<project>/javatests`. 

Please make sure your code compiles by running `ant test` which will
execute any available tests.  Additionally, consider using  <http://travis-ci.org>
to validate your branches before you even put them into pull requests.  All
pull requests will be validated by Travis-ci in any case and must pass before
being merged.

If you are adding or modifying files you may add your own copyright line, but
please ensure that the form is consistent with the existing files, and please
note that a Google, Inc. copyright line must appear in every copyright notice.
All files are released with the Apache 2.0 license and any new files may only
be accepted under the terms of that license.

Before your code can be accepted into the project you must sign the
[Individual Contributor License Agreement (CLA)][1].

 [1]: https://developers.google.com/open-source/cla/individual
