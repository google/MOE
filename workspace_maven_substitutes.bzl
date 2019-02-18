
_DAGGER = """
java_library(
    name = "dagger",
    exports = [
        ":dagger_api",
        "@maven//javax/inject:javax_inject",
    ],
    exported_plugins = [":plugin"],
    visibility = ["//visibility:public"],
)

maven_jvm_artifact(
    name = "dagger_api",
    artifact = "com.google.dagger:dagger:{version}",
    visibility = ["//visibility:private"],
)

java_plugin(
    name = "plugin",
    processor_class = "dagger.internal.codegen.ComponentProcessor",
    generates_api = True,
    deps = [":dagger_compiler"],
)
"""

_AUTO_FACTORY = """
java_library(
    name = "factory",
    exports = [
        "@maven//com/google/auto/factory:auto_factory",
    ],
    exported_plugins = [":plugin"],
    neverlink = True, # this is only needed at compile-time, for code-gen.
    visibility = ["//visibility:public"],
)

maven_jvm_artifact(
    name = "auto_factory",
    artifact = "com.google.auto.factory:auto-factory:{version}",
    visibility = ["//visibility:private"],
)

java_plugin(
    name = "plugin",
    processor_class = "com.google.auto.factory.processor.AutoFactoryProcessor",
    generates_api = True,
    deps = [":auto_factory"],
)
"""

_AUTO_VALUE = """
java_library(
    name = "value",
    exports = [
        ":auto_value_annotations",
        "@maven//com/ryanharter/auto/value:auto_value_gson_annotations",
    ],
    exported_plugins = [":plugin", ":gson_plugin"],
    visibility = ["//visibility:public"],
)

maven_jvm_artifact(
    name = "auto_value",
    artifact = "com.google.auto.value:auto-value:{version}",
    visibility = ["@maven//com/ryanharter/auto/value:__subpackages__"],
)

java_plugin(
    name = "plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    generates_api = True,
    deps = [":auto_value"],
)

java_plugin(
    name = "gson_plugin",
    processor_class = "com.ryanharter.auto.value.gson.AutoValueGsonAdapterFactoryProcessor",
    generates_api = True,
    deps = [
        "@//tools/auto/generated/javax/annotation:generated", # For compiling with java9+
        "@maven//com/ryanharter/auto/value:auto_value_gson",
    ],
)
"""

snippets = struct(
    AUTO_VALUE = _AUTO_VALUE,
    AUTO_FACTORY = _AUTO_FACTORY,
    DAGGER = _DAGGER,
)