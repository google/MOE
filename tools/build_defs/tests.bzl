#
# Convenience test rules.
#
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_binary")

def jvm_unit_test(name, srcs = [], size = "small", **kwargs):
    """Wraps a kt_jvm_library and java_test."""

    # If unspecified, try to get the test source named after the test file.
    if not bool(srcs):
        srcs = native.glob(["%s.*" % name])
    srcs = [x for x in srcs if x.endswith(".java") or x.endswith(".kt")]
    if bool(srcs) and len(srcs) > 1:
        fail("More than one test src file %s: %s" % (name, srcs))
    elif bool(srcs) and srcs[0].endswith(".kt"):
        test_file = srcs[0]
        lib_name = "%s_testlib" % name
        kt_jvm_library(
            name = lib_name,
            srcs = srcs,
            testonly = 1,
            **kwargs
        )
    elif bool(srcs) and srcs[0].endswith(".java"):
        test_file = srcs[0]
        lib_name = "%s_testlib" % name
        native.java_library(
            name = lib_name,
            srcs = srcs,
            testonly = 1,
            **kwargs
        )
    elif bool(srcs):
        fail("Invalid test file %s. Must be a java or kotlin file." % srcs)
    else:
        fail("Missing a test file for %s" % name)

    native.java_test(
      name = name,
      size = size,
      runtime_deps = [lib_name],
    )