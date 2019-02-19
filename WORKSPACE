workspace(name = "moe")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Rules and tooling versions
RULES_KOTLIN_VERSION = "porque_no_les_dos"
MAVEN_REPOSITORY_RULES_VERSION = "1.0-rc4"
MAVEN_REPOSITORY_RULES_SHA = "57357874e88da816857a8ec1f8e6ffa059161ac26fecdd35202e2488dba72d57"
KOTLIN_VERSION = "1.2.71"
KOTLINC_RELEASE = {
    "urls": ["https://github.com/JetBrains/kotlin/releases/download/v{version}/kotlin-compiler-{version}.zip".format(version = KOTLIN_VERSION)],
    "sha256": "e48292fdfed42f44230bc01f92ffd17002101d8c5aeedfa3dba3cb29c5b6ea7b",
}

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
kotlin_repositories(compiler_release = KOTLINC_RELEASE)
kt_register_toolchains()


# Setup maven repository handling.
load("@maven_repository_rules//maven:maven.bzl", "maven_repository_specification")
load(":workspace_maven_substitutes.bzl", "snippets")
maven_repository_specification(
    name = "maven",
    artifacts = {
        "args4j:args4j:2.32": { "insecure": True },
        "cglib:cglib-nodep:2.2.2": { "insecure": True },
        "com.google.auto.factory:auto-factory:1.0-beta6": {
            "insecure": True,
            "build_snippet": snippets.AUTO_FACTORY.format(version = "1.0-beta6"),
        },
        "com.google.auto.value:auto-value-annotations:1.6.3": { "insecure": True },
        "com.google.auto.value:auto-value:1.6.3": {
            "insecure": True,
            "build_snippet": snippets.AUTO_VALUE.format(version = "1.6.3"),
        },
        "com.google.auto:auto-common:0.10": { "insecure": True },
        "com.google.code.findbugs:jsr305:3.0.2": { "insecure": True },
        "com.google.code.gson:gson:2.8.1": { "insecure": True },
        "com.google.dagger:dagger-compiler:2.21": { "insecure": True },
        "com.google.dagger:dagger-producers:2.21": { "insecure": True },
        "com.google.dagger:dagger-spi:2.21": { "insecure": True },
        "com.google.dagger:dagger:2.21": {
            "insecure": True,
            "build_snippet": snippets.DAGGER.format(version = "2.21"),
        },
        "com.google.errorprone:error_prone_annotations:2.3.1": { "insecure": True },
        "com.google.errorprone:javac-shaded:9+181-r4173-1": { "insecure": True },
        "com.google.googlejavaformat:google-java-format:1.6": { "insecure": True },
        "com.google.guava:guava:27.0.1-jre": { "insecure": True },
        "com.google.guava:failureaccess:1.0.1": { "insecure": True },
        "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava": { "insecure": True },
        "com.google.j2objc:j2objc-annotations:1.1": { "insecure": True },
        "com.google.truth:truth:0.42": { "insecure": True },
        "com.googlecode.java-diff-utils:diffutils:1.3.0": { "insecure": True },
        "com.mikesamuel:json-sanitizer:1.1": { "insecure": True },
        "com.nhaarman:mockito-kotlin:1.6.0": { "insecure": True },
        "com.ryanharter.auto.value:auto-value-gson-annotations:0.8.0": { "insecure": True },
        "com.ryanharter.auto.value:auto-value-gson:0.8.0": { "insecure": True },
        "com.squareup.okhttp:okhttp:2.5.0": { "insecure": True },
        "com.squareup.okio:okio:1.6.0": { "insecure": True },
        "com.squareup:javapoet:1.11.1": { "insecure": True },
        "javax.annotation:jsr250-api:1.0": { "insecure": True },
        "javax.inject:javax.inject:1": { "insecure": True },
        "joda-time:joda-time:2.9.9": { "insecure": True },
        "junit:junit:4.13-beta-1": { "insecure": True },
        "net.bytebuddy:byte-buddy:1.9.7": { "insecure": True },
        "net.bytebuddy:byte-buddy-agent:1.9.7": { "insecure": True },
        "org.checkerframework:checker-compat-qual:2.5.3": { "insecure": True },
        "org.checkerframework:checker-qual:2.5.3": { "insecure": True },
        "org.codehaus.mojo:animal-sniffer-annotations:1.14": { "insecure": True },
        "org.easymock:easymock:3.1": { "insecure": True },
        "org.jetbrains:annotations:13.0": { "insecure": True },
        "org.jetbrains.kotlin:kotlin-reflect:%s" % KOTLIN_VERSION: { "insecure": True },
        "org.jetbrains.kotlin:kotlin-stdlib:%s" % KOTLIN_VERSION: { "insecure": True },
        "org.jetbrains.kotlin:kotlin-stdlib-common:%s" % KOTLIN_VERSION: { "insecure": True },
        "org.mockito:mockito-core:2.24.0": { "insecure": True },
        "org.objenesis:objenesis:2.6": { "insecure": True },
        "org.hamcrest:hamcrest-core:1.3": { "insecure": True },
    },
    dependency_target_substitutes = {
        "com.google.dagger": {"@maven//com/google/dagger:dagger": "@maven//com/google/dagger:dagger_api"},
    },
)
