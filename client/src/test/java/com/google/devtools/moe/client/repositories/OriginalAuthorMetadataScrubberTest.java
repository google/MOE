/*
 * Copyright (c) 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.repositories;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.moe.client.testing.DummyRevisionHistory.parseLegacyFields;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.config.MetadataScrubberConfig;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import junit.framework.TestCase;
import org.joda.time.DateTime;

/** Unit test for the {@link OriginalAuthorMetadataScrubber}. */
public class OriginalAuthorMetadataScrubberTest extends TestCase {
  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final Ui ui = new Ui(out, null);
  private final OriginalAuthorMetadataScrubber oams = new OriginalAuthorMetadataScrubber(ui);

  private final AtomicBoolean shouldScrub = new AtomicBoolean(false);
  private final MetadataScrubberConfig config =
      new MetadataScrubberConfig() {
        @Override
        public boolean getRestoreOriginalAuthor() {
          return shouldScrub.get();
        }
      };

  public void testOriginalAuthorSubstitution() throws Exception {
    shouldScrub.set(true);
    RevisionMetadata initial =
        RevisionMetadata.builder()
            .id("100")
            .author("foo@bar.com")
            .date(new DateTime(1L))
            .description(
                "random description\n"
                    + "ORIGINAL_AUTHOR=Blah Foo <blah@foo.com>\n"
                    + "blahblahblah")
            .build();
    initial = parseLegacyFields(initial);

    RevisionMetadata expected =
        RevisionMetadata.builder()
            .id("100")
            .author("Blah Foo <blah@foo.com>")
            .date(new DateTime(1L))
            .description("random description\n\nblahblahblah")
            .build();
    expected = parseLegacyFields(expected);

    RevisionMetadata actual = oams.scrub(initial, config);
    assertThat(actual.author()).isEqualTo("Blah Foo <blah@foo.com>");
    assertThat(actual.description()).isEqualTo(expected.description());
  }

  public void testOriginalAuthorSubstitution_Disabled() throws Exception {
    shouldScrub.set(false);
    RevisionMetadata initial =
        RevisionMetadata.builder()
            .id("100")
            .author("foo@bar.com")
            .date(new DateTime(1L))
            .description(
                "random description\n"
                    + "ORIGINAL_AUTHOR=\"Blah Foo <blah@foo.com>\"\n"
                    + "blahblahblah")
            .build();
    initial = parseLegacyFields(initial);

    RevisionMetadata actual = oams.scrub(initial, config);
    assertThat(actual).isEqualTo(initial);
    assertThat(actual).isSameAs(initial);
  }

  public void testSanitizeAuthor_Clean_FullSpec() {
    assertThat(oams.sanitizeAuthor("Foo <foo@foo.com>")).isEqualTo("Foo <foo@foo.com>");
  }

  public void testSanitizeAuthor_Clean_OnlyEmail() {
    assertThat(oams.sanitizeAuthor("<foo@foo.com>")).isEqualTo("<foo@foo.com>");
  }

  public void testSanitizeAuthor_Clean_Username() {
    assertThat(oams.sanitizeAuthor("<user>")).isEqualTo("<user>");
  }

  public void testSanitizeAuthor_Dirty_Email() {
    assertThat(oams.sanitizeAuthor("user@blah.com")).isEqualTo("user <user@blah.com>");
    assertThat(oams.sanitizeAuthor("blah%foo+blah@blah.com"))
        .isEqualTo("blah%foo+blah <blah%foo+blah@blah.com>");
    assertThat(oams.sanitizeAuthor("  user@blah.com ")).isEqualTo("user <user@blah.com>");
  }

  public void testSanitizeAuthor_Dirty_Username() {
    assertThat(oams.sanitizeAuthor("userblah")).isEqualTo("userblah <userblah>");
  }

  public void testSanitizeAuthor_Dirty_Unknown() {
    assertThat(oams.sanitizeAuthor("user blah")).isEqualTo("\"user blah\" <undetermined_user>");
    assertThat(out.toString()).contains("WARNING: unknown author format");
    assertThat(out.toString()).contains("user blah");
  }

  public void testExtractField() {
    String desc = "blahfoo\nORIGINAL_AUTHOR=blah@foo=asdf\nblahblahblah";
    String after = oams.extractFirstOriginalAuthorField(desc);
    assertThat(after).isEqualTo("blahfoo\n\nblahblahblah");
  }
}
