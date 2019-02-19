package com.google.devtools.moe.client.project;


import com.google.devtools.moe.client.config.ScrubberConfig;
import com.google.devtools.moe.client.GsonModule;

import junit.framework.TestCase;



/**
 * Test case for {@link ScrubberConfig}.
 */
public class ScrubberConfigTest extends TestCase {
  private static final String PUBLISHABLE_USERS = "[\"publishable\"]";
  private static final String SCRUBBABLE_USERS = "[\"scrubbable\"]";
  private static final String UNKNOWN_AUTHOR = "Name <unknown@domain>";
  private static final String PUBLISHABLE_AUTHOR = "Name <publishable@domain>";
  private static final String SCRUBBABLE_AUTHOR = "Name <scrubbable@domain>";


  public void testShouldScrubAuthor_doesntScrubAuthors() throws Exception {
    ScrubberConfig scrubberConfig =
        GsonModule.provideGson()
            .fromJson("{\"scrub_authors\":false,\"usernames_file\":null}", ScrubberConfig.class);
    assertFalse(scrubberConfig.shouldScrubAuthor(UNKNOWN_AUTHOR));
  }

  public void testShouldScrubAuthor_scrubsUnknownAuthors() throws Exception {
    ScrubberConfig scrubberConfig =
        GsonModule.provideGson()
            .fromJson(
                "{\"scrub_unknown_users\":true,\"usernames_file\":null}", ScrubberConfig.class);
    assertTrue(scrubberConfig.shouldScrubAuthor(UNKNOWN_AUTHOR));
  }

  public void testShouldScrubAuthor_scrubsAuthors() throws Exception {
    ScrubberConfig scrubberConfig =
        GsonModule.provideGson()
            .fromJson(
                "{\"usernames_to_scrub\":"
                    + SCRUBBABLE_USERS
                    + ",\"usernames_to_publish\":"
                    + PUBLISHABLE_USERS
                    + ",\"usernames_file\":null}",
                ScrubberConfig.class);
    assertTrue(scrubberConfig.shouldScrubAuthor(SCRUBBABLE_AUTHOR));
    assertFalse(scrubberConfig.shouldScrubAuthor(PUBLISHABLE_AUTHOR));
    assertFalse(scrubberConfig.shouldScrubAuthor(UNKNOWN_AUTHOR));
  }
}
