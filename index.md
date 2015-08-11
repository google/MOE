Introduction
------------

MOE has several concepts which litter the code. It's useful to understand them.

A list of useful concepts
-------------------------

### Project

A MOE project is one project that wants to be both internal and open source.

### Codebase

A Codebase is a set of files in a filesystem and its metadata. E.g., foo/Bar.java and foo/Baz.java, and that it came from this repository at this revision. This roughly corresponds to a subversion “export” or a git “tree”.

### Writer

A Writer takes a Codebase and edits the Repository to create a DraftRevision in which the Repository has the contents of the Codebase.

### Repository

MOE interacts with a repository in three ways:

  * Determining what revisions there are
  * Getting the contents at one revision
  * Making a change (that can be submitted as a revision)

Thus, a Repository object has three members to fulfill these roles, respectively:

  * RevisionHistory
  * CodebaseCreator
  * WriterCreator

### Project Spaces

A Project Space is one way of viewing a project's code. The most common project spaces are "public" and "internal". A Codebase knows what Project Space it is in.

### Translators

A Translator takes a Codebase in one Project Space and returns the equivalent Codebase in another Project Space. This is the central concept of MOE: **instead of scrubbing code once, the user describes the scrubbing in a repeatable manner so we can do it automatically.** A Translator must be configured in the config, and it performs translation by applying a preconfigured series of editors.

For instance, to translate from "internal" to "public", we may have to rearrange, then scrub, then run a shell command. We don't want to have to say the same series of editors every time we translate, so instead we configure it as part of the Translator from "internal" to "public".

### Editor

An Editor takes a Codebase and returns a new Codebase. (This is very similar to the Translator interface described above. The difference is that the Editor doesn't change the project space.)

Examples of Editors are:

  * RearrangingEditory: rearranges the files according to a config. E.g., java/. -> src/.
  * PatchingEditor: applies a patch file to the Codebase.
  * ShellEditor: runs a shell command (e.g., a sed command)

Editors must be configured in the config.

### Equivalence

In MOE, two revisions are equivalent when they represent the same files as they appear in their respective repositories. The purpose of MOE is to get projects into equivalence whenever they get out of it.

### Migration

A Migration represents the series of changes needed to make one repository equivalent to another. It has four properties:

  * name: String. The name of the migration, as given in the MOE config.
  * fromRepository: String. The name of the repository to make equivalent.
  * toRepository: String. The name of the repository to make fromRepository equivalent to.
  * separateRevisions: Boolean.
    * false: Combine all separate revisions in one repository into one big change in the other repository.
    * true: Let all separate revisions in one repository remain separate changes in the other repository [NOTE: this is not yet supported].
  * metadataScrubberConfig:
    * usernamesToScrub: List<String>. All names to scrub from the metadata of a MOE commit.
    * scrubConfidentialWords: Boolean. You must implement this yourself.
