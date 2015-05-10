# Introduction #

The MOE config file is where all the magic happens. It is a JSON file where you define the repositories of your project as well as any editors, translators, and migrations that you need.

Create a file and name it something like `moe_config.txt`.

A config file consists of three main sections. An example MOE configuration follows.

  1. REPOSITORIES: Here is where you declare any repositories your project will work with. The example project’s code is both internal in an hg repository and shared publicly on an svn repository. Valid types are svn, hg, and git.
  1. EDITORS: Editors are useful if you wish to explicitly perform a single step. For example, if I just wanted to rename the files in my example project, I could invoke the editor named “renamey” using the create\_codebase directive which is explained in [Directives](Directives.md). Editors are useful if there is a particular edit that is only needed occasionally, or if you want to test that a step in a translator is working properly. They are not mandatory and are separate from the editors specified in a translator’s steps. (See the main page [here](EditorTypes.md).)
  1. TRANSLATORS: Translators let you specify a sequence of editors to perform. The example project has a translator that can translate from the internal repository to the external repository. The process involves renaming the files, scrubbing the code, and running some shell commands. See EditorTypes for a more detailed description of the editor options.
  1. MIGRATIONS: See the Migration section of [MOE Concepts](Concepts.md) for more information about migrations.

# Example #

```
{
  "#": "This is the MOE config file"    **COMMENT**
  "name": "myproject”,    **PROJECT NAME**
  "repositories": {    **REPOSITORY SECTION**
    "internal”: {
      "type": "hg",
      "project_space": "internal",
      "url": "/path/to/repo/",
      "ignore_file_res": [
        "#": "Regexes for files in the internal repo that shouldn't be migrated to the public one",
        "^docs/secret_doc\\.txt",
        "^secret_folder/.*"
      ]
    },
    "svn": {
      "type": "svn",
      "project_space": "public",
      "url": "https://example.googlecode.com/svn/trunk/"
    }
  },
  "editors": {    **EDITOR SECTION**
    "renamey": {
      "type": "renamer",
      "mappings": {
        "java/": "src/",
        "javatests/": "tests/"
      }
    }
  },
  "translators": [{    **TRANSLATOR SECTION**
    "from_project_space": "internal",
    "to_project_space": "public",
    "steps": [{
      "name": "renamestep",
      "editor": {
        "type": "renamer",
        "mappings": {
          "java/foo/bar/": "src/",
          "javatests/foo/bar/": "tests/"
        }
      }
    },{
      "name": "shellstep",
            "editor": {
        "type": "shell",
        "command_string":
          "rename s/foo/bar/g * baz/* &&
           sed -i s/foo/bar/g {README.txt} &&
           sed -i s/foo/bar/g *.{cc,h,h.in} &&
           sed -i s/Foo/Bar/g *.{cc,h,h.in} &&
           sed -i s/FOO/BAR/g *.{cc,h,h.in,ac,am}"
      }
    }]
  }],
  "migrations":[{    **MIGRATION SECTION**
    "name":"publicize_project",
    "from_repository":"internal",
    "to_repository":"googlecode",
    "separate_revisions":false,
    "metadata_scrubber_config":{
      "usernames_to_scrub":["privateuser"],
      "scrub_confidential_words":false
    }
  }]
}
```