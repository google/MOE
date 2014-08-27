package com.google.devtools.moe.client.project;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuration for a scrubber.
 */
public class ScrubberConfig {

  // General options
  @SerializedName("ignore_files_re") private String ignoreFilesRe;
  @SerializedName("do_not_scrub_files_re") private String doNotScrubFilesRe;
  @SerializedName("extension_map") private JsonArray extensionMap;
  @SerializedName("sensitive_string_file") private String sensitiveStringFile;
  @SerializedName("sensitive_words") private List<String> sensitiveWords;
  @SerializedName("sensitive_res") private List<String> sensitiveRes;
  private List<JsonObject> whitelist;
  @SerializedName("scrub_sensitive_comments") private boolean scrubSensitiveComments = true;
  @SerializedName("rearranging_config") private JsonObject rearrangingConfig;
  @SerializedName("string_replacements") private List<Map<String, String>> stringReplacements;
  @SerializedName("regex_replacements") private List<Map<String, String>> regexReplacements;
  @SerializedName("scrub_non_documentation_comments") private boolean scrubNonDocumentationComments;
  @SerializedName("scrub_all_comments") private boolean scrubAllComments;

  // User options
  @SerializedName("usernames_to_scrub") private List<String> usernamesToScrub = new ArrayList<>();
  @SerializedName("usernames_to_publish")
  private List<String> usernamesToPublish = new ArrayList<>();
  @SerializedName("usernames_file") private String usernamesFile;
  @SerializedName("scrub_unknown_users") private boolean scrubUnknownUsers;
  @SerializedName("scrub_authors") private boolean scrubAuthors = true;

  // C/C++ options
  @SerializedName("c_includes_config_file") private String cIncludesConfigFile;

  // Java options
  @SerializedName("empty_java_file_action") private String emptyJavaFileAction;
  @SerializedName("maximum_blank_lines") private int maximumBlankLines;
  @SerializedName("scrub_java_testsize_annotations") private boolean scrubJavaTestsizeAnnotations;
  @SerializedName("java_renames") private List<Map<String, String>> javaRenames;

  // JavaScript options
  @SerializedName("js_directory_rename") private Map<String, String> jsDirectoryRename;
  @SerializedName("js_directory_renames") private List<Map<String, String>> jsDirectoryRenames;

  // Python options
  @SerializedName("python_module_renames") private List<JsonObject> pythonModuleRenames;
  @SerializedName("python_module_removes") private List<JsonObject> pythonModuleRemoves;
  @SerializedName("python_shebang_replace") private JsonObject pythonShebangReplace;

  // GWT options
  @SerializedName("scrub_gwt_inherits") private List<String> scrubGwtInherits;

  // proto options
  @SerializedName("scrub_proto_comments") private boolean scrubProtoComments;

  private ScrubberConfig() { // Instantiated by GSON.
  }

  /**
   * @param author Author in the format "Name <username@domain>".
   */
  public boolean shouldScrubAuthor(String author) throws InvalidProject {
    if (!scrubAuthors) {
      return false;
    }
    // TODO(cgruber): Create a custom deserializer that does this logic once, on deserialization.
    if (usernamesFile != null) {
      try {
        UsernamesConfig usernamesConfig = ProjectConfig.makeGson().fromJson(
            new BufferedReader(new FileReader(usernamesFile)),
            UsernamesConfig.class);
        addUsernames(usernamesToScrub, usernamesConfig.getScrubbableUsernames());
        addUsernames(usernamesToPublish, usernamesConfig.getPublishableUsernames());
      } catch (FileNotFoundException exception) {
        throw new InvalidProject(
            "File " + usernamesFile + " referenced by usernames_file not found.");
      }
      usernamesFile = null;
    }
    return scrubUnknownUsers
        ? !matchesUsername(author, usernamesToPublish)
        : matchesUsername(author, usernamesToScrub);
  }

  private void addUsernames(List<String> local, List<String> global) {
    if (global != null) {
      for (String username : global) {
        if (!local.contains(username)) {
          local.add(username);
        }
      }
    }
  }

  // TODO(cgruber): Parse out the actual usernames in a custom deserializer.
  private boolean matchesUsername(String author, List<String> usernames) {
    if (usernames != null) {
      for (String username : usernames) {
        if (author.matches(".*<" + Pattern.quote(username) + "@.*")) {
          return true;
        }
      }
    }
    return false;
  }

}
