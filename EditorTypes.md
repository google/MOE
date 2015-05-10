# Introduction #

What follows are descriptions of the different editors and any special fields that they require. Refer to the [Config](Config.md) example to see editors in a config file.

# Editors #

## shell ##
This editor lets you run shell commands on your code.
  * `command_string` is a string containing all the commands you wish you run. If you have multiple, you can concatenate them with && as seen in the example.

## renamer ##
This editor lets you reorganize your project’s files since the file hierarchy of your project will most likely be different between internal and external repositories.
  * `mappings` is a JSON object specifying the renaming rules. In the renaming step of the example config, files with "`java/foo/bar/`" in their name will have it replaced with "`src/`" and files with "`javatests/foo/bar/`" will have it replaced with "`tests/`". A file called "`java/foo/bar/baz/hello.txt`" will become "`src/baz/hello.txt`" after the renaming step. The editor looks at each file, finds the first mapping rule that fits the file’s name and makes the change. This means that if the mappings looked like this:
```
{"old/": "new/", "old/older/": "new/newer/"}
```
> then the file "`old/older/test.txt`" would become "`new/older/test.txt`" which is probably not what the user expected. This is because the file is changed based on the first applicable rule. To remedy this, specify the most specific rules first, like this:
```
{"old/older/": "new/newer/", "old/": "new/"}
```