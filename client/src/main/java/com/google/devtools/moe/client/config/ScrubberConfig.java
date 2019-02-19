package com.google.devtools.moe.client.config;

import com.google.devtools.moe.client.InvalidProject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Configuration for a scrubber.
 */
@SuppressWarnings("unused") // JSON will read/write private fields.
public class ScrubberConfig {

  // General options
  private String ignoreFilesRe;
  private String doNotScrubFilesRe;
  private JsonArray extensionMap;
  private String sensitiveStringFile;
  private List<String> sensitiveWords;
  private List<String> sensitiveRes;
  private List<JsonObject> whitelist;
  private boolean scrubSensitiveComments = true;
  private JsonObject rearrangingConfig;
  private List<Map<String, String>> stringReplacements;
  private List<Map<String, String>> regexReplacements;
  private boolean scrubNonDocumentationComments;
  private boolean scrubAllComments;

  // User options
  private List<String> usernamesToScrub = new ArrayList<>();
  private List<String> usernamesToPublish = new ArrayList<>();
  private String usernamesFile;
  private boolean scrubUnknownUsers;
  private boolean scrubAuthors = true;

  // C/C++ options
  private String cIncludesConfigFile;
  private JsonObject cIncludesConfig;

  // Java options
  private String emptyJavaFileAction;
  private int maximumBlankLines;
  private boolean scrubJavaTestsizeAnnotations;
  private List<Map<String, String>> javaRenames;

  // JavaScript options
  private Map<String, String> jsDirectoryRename;
  private List<Map<String, String>> jsDirectoryRenames;

  // Python options
  private List<JsonObject> pythonModuleRenames;
  private List<JsonObject> pythonModuleRemoves;
  private JsonObject pythonShebangReplace;

  // GWT options
  private List<String> scrubGwtInherits;

  // proto options
  private boolean scrubProtoComments;


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
    return scrubUnknownUsers
        ? !matchesUsername(author, usernamesToPublish)
        : matchesUsername(author, usernamesToScrub);
  }

  public String getUsernamesFile() {
    return usernamesFile;
  }

  /**
   * Called by ProjectContextFactory to update usernamesConfig with external usernames file.
   */
  public void updateUsernames(UsernamesConfig usernamesConfig) {
    addUsernames(usernamesToScrub, usernamesConfig.getScrubbableUsernames());
    addUsernames(usernamesToPublish, usernamesConfig.getPublishableUsernames());
    // reset usernamesFile to null so that we don't read the file again
    usernamesFile = null;
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
