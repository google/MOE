# Introduction #
MOE has two major expression types:

## Codebase Expressions ##
Any place in MOE that takes a Codebase as a string takes a Codebase Expression and evaluates it.

(IDENT means a String literal)

BNF:

Expression -> Term OperationList

Term -> IDENT

Term -> IDENT Options

Options -> '( OptionList ')'

OptionList -> Option

Option -> IDENT '=' IDENT

OptionList -> Option ',' OptionList

OperationList ->

OperationList -> Operation OperationList

Operation -> Operator Term

Operator -> '|'

Operator -> '>'


An expression is evaluated by (com.google.devtools.moe.client.codebase.Evaluator):
  * Evaluate the "Creator" term. This generates a codebase.
  * Evaluate each Operation. An operation:
    * invokes a translator if the operator is ">", in which case the identifier is the project space to translate to
    * invokes an editor if the operator is "|", in which case the identifier is the name of the editor to invoke

Example Codebase Expressions:
  * `'googleinternal'`
> the repository "googleinternal" at head
  * `'googlecode'`
> the repository "googlecode" at head
  * `'googlecode(revision=45)'`
> the repository "googlecode" at [revision 45](https://code.google.com/p/moe-java/source/detail?r=45)
  * `'googlecode(revision=45)>internal'`
> translate the repository "googlecode" at [revision 45](https://code.google.com/p/moe-java/source/detail?r=45) to the project space "internal"
  * `'googlecode|patch(file="/path/to/patch.txt")'`
> apply the patch file "/path/to/patch.txt" to the repository "googlecode" at [revision 45](https://code.google.com/p/moe-java/source/detail?r=45)
  * `'googlecode|patch(file="/path/to/patch.txt")>internal'`
> apply the patch file then translate
  * `'googlecode>internal|patch(file="/path/to/patch.txt")'`
> apply the patch file after translating (e.g. if the patch file was made by someone developing against the internal project space)

Special Codebase Expressions:
  * `'file(path="path/to/repo", projectspace="internal")'`
> The special codebase identifier 'file' will tell MOE to build the codebase from the files in the location specified by the path option. A projectspace can also be given. The default is public. The 'file' codebase can be useful for testing MOE on files stored locally rather than in a version control system. As of August 2011, you can only create 'file' codebases. Writing to a 'file' codebase is not supported. Specifying a 'file' codebase as the --destination in a MOE change directive will cause an error.

## Revision Expressions ##
Related to Codebase Expressions are Revision Expressions. These describe one or more revisions in a particular repository.

A Revision Expression consists of:

  * Repository name: an alphanumeric (underscores allowed) identifier for a repository
  * Revision name(s): (optional) a list of one or more revisions separated by commas

Example Revision Expressions:
  * `'googlecode{123,456,789}'`
> revisions 123, 456, and 789 of repository "googlecode"
  * `'internal'`
> the head revision of repository "internal"