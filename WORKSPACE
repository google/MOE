workspace(name = "moe")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Rules and tooling versions
RULES_KOTLIN_VERSION = "porque_no_les_dos"
MAVEN_REPOSITORY_RULES_VERSION = "1.0-rc4"
MAVEN_REPOSITORY_RULES_SHA = "57357874e88da816857a8ec1f8e6ffa059161ac26fecdd35202e2488dba72d57"

# Rules and tools repositories
http_archive(
    name = "io_bazel_rules_kotlin",
    urls = ["https://github.com/cgruber/rules_kotlin/archive/%s.zip" % RULES_KOTLIN_VERSION],
    type = "zip",
    strip_prefix = "rules_kotlin-%s" % RULES_KOTLIN_VERSION
)
#local_repository(name = "io_bazel_rules_kotlin", path="../../bazelbuild/rules_kotlin")
http_archive(
    name = "maven_repository_rules",
    urls = ["https://github.com/square/bazel_maven_repository/archive/%s.zip" % MAVEN_REPOSITORY_RULES_VERSION],
    type = "zip",
    strip_prefix = "bazel_maven_repository-%s" % MAVEN_REPOSITORY_RULES_VERSION,
    sha256 = MAVEN_REPOSITORY_RULES_SHA,
)
#local_repository(name = "maven_repository_rules", path="../../bazel_maven_repository")

# Setup Kotlin
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
kt_register_toolchains()


# Description:
#   Substitutes the naive maven_jvm_artifact for com.google.dagger:dagger with a flagor that exports the compiler
#   plugin.  Contains the `dagger_version` substitution variable.
DAGGER_BUILD_SUBSTITUTE_WITH_PLUGIN = """
java_library(
   name = "dagger",
   exports = [
       ":dagger_api",
       "@maven//javax/inject:javax_inject",
   ],
   exported_plugins = [":dagger_plugin"],
   visibility = ["//visibility:public"],
)

maven_jvm_artifact(
   name = "dagger_api",
   artifact = "com.google.dagger:dagger:{dagger_version}",
)

java_plugin(
   name = "dagger_plugin",
   processor_class = "dagger.internal.codegen.ComponentProcessor",
   generates_api = True,
   deps = [":dagger_compiler"],
)
"""

# Setup maven repository handling.
load("@maven_repository_rules//maven:maven.bzl", "maven_repository_specification")
maven_repository_specification(
    name = "maven",
    artifacts = {
        "args4j:args4j:2.32": { "insecure": True },
        "cglib:cglib-nodep:2.2.2": { "insecure": True },
        "com.google.auto.factory:auto-factory:1.0-beta6": { "insecure": True },
        "com.google.auto.value:auto-value-annotations:1.6.3": { "insecure": True },
        "com.google.auto.value:auto-value:1.6.3": { "insecure": True },
        "com.google.auto:auto-common:0.10": { "insecure": True },
        "com.google.code.findbugs:jsr305:3.0.2": { "insecure": True },
        "com.google.code.gson:gson:2.8.1": { "insecure": True },
        "com.google.dagger:dagger-compiler:2.20": { "insecure": True },
        "com.google.dagger:dagger-producers:2.20": { "insecure": True },
        "com.google.dagger:dagger-spi:2.20": { "insecure": True },
        "com.google.dagger:dagger:2.20": {
            "insecure": True,
            "build_snippet": DAGGER_BUILD_SUBSTITUTE_WITH_PLUGIN.format(dagger_version = "2.20")
        },
        "com.google.errorprone:error_prone_annotations:2.3.1": { "insecure": True },
        "com.google.errorprone:javac-shaded:9+181-r4173-1": { "insecure": True },
        "com.google.googlejavaformat:google-java-format:1.6": { "insecure": True },
        "com.google.j2objc:j2objc-annotations:1.1": { "insecure": True },
        "com.google.truth:truth:0.42": { "insecure": True },
        "com.googlecode.java-diff-utils:diffutils:1.3.0": { "insecure": True },
        "com.mikesamuel:json-sanitizer:1.1": { "insecure": True },
        "com.ryanharter.auto.value:auto-value-gson-annotations:0.8.0": { "insecure": True },
        "com.ryanharter.auto.value:auto-value-gson:0.8.0": { "insecure": True },
        "com.squareup.okhttp:okhttp:2.5.0": { "insecure": True },
        "com.squareup.okio:okio:1.6.0": { "insecure": True },
        "com.squareup:javapoet:1.11.1": { "insecure": True },
        "javax.annotation:jsr250-api:1.0": { "insecure": True },
        "javax.inject:javax.inject:1": { "insecure": True },
        "joda-time:joda-time:2.9.9": { "insecure": True },
        "junit:junit:4.13-beta-1": { "insecure": True },
        "org.checkerframework:checker-compat-qual:2.5.3": { "insecure": True },
        "org.checkerframework:checker-qual:2.5.3": { "insecure": True },
        "org.codehaus.mojo:animal-sniffer-annotations:1.14": { "insecure": True },
        "org.easymock:easymock:3.1": { "insecure": True },
        "org.mockito:mockito-core:1.9.5": { "insecure": True },
        "org.objenesis:objenesis:1.2": { "insecure": True },
        "org.hamcrest:hamcrest-core:1.3": { "insecure": True },
        "com.google.guava:guava:25.1-jre": { "sha256": "6db0c3a244c397429c2e362ea2837c3622d5b68bb95105d37c21c36e5bc70abf" },
    },
    dependency_target_substitutes = {
        "com.google.dagger": {"@maven//com/google/dagger:dagger": "@maven//com/google/dagger:dagger_api"},
    },
)
