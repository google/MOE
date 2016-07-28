/*
 * Copyright (c) 2011 Google, Inc.
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

import dagger.Provides;
import dagger.Provides.Type;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Abstract superclass defining what any MetadataScrubber should be able to do. A MetadataScrubber
 * is an object that can take in a RevisionMetadata object and then return a new one which has been
 * "scrubbed" of any undesirable information.
 *
 * {@code MetadataScrubber} subclasses may be called in different contexts during the life of the
 * application (say, in {@code moe magic} where separate migrations are configured), and so
 * are not injected with their configuration, but receive it on the call stack.
 */
public abstract class MetadataScrubber {

  /**
   * The primary method of a MetadataScrubber. It returns a copy of the RevisionMetadata whose
   * data fields have been scrubbed in the prescribed way.  If no alterations are made, this
   * method may return the original instance.
   *
   * By default, if config is null, scrub will perform no scrubbing, but simply return the
   * un-scrubbed metadata.
   */
  public final RevisionMetadata scrub(
      RevisionMetadata rm, @Nullable MetadataScrubberConfig config) {
    if (shouldScrub(config)) {
      return execute(rm, config);
    }
    return rm;
  }

  /**
   * Determines whether {@link #execute(RevisionMetadata, MetadataScrubberConfig)} is executed.
   *
   * <p>Subclasses should override this in order to selectively execute.  By default, a scrubber
   * will not be invoked if the scruber configuration is null, though scrubbers that are "always
   * on" and which need no configuration (say, are company specific and added for all projects
   * regardless of configuration) may override this behavior.
   *
   * @param config a configuration object which is used to conditionally execute its logic.
   */
  protected boolean shouldScrub(@Nullable MetadataScrubberConfig config) {
    return config != null;
  }

  /**
   * The body of the scrubbing logic.  Subclasses should override this method to transform
   * {@link RevisionMetadata} instances.  The returned metadata instance should be a copy of
   * the supplied metadata, if any transformations have been performed.
   */
  protected abstract RevisionMetadata execute(RevisionMetadata rm, MetadataScrubberConfig config);

  /**
   * A utility method that is useful for stripping a list of words from all the fields of the
   * RevisionMetadata.
   *
   * @param rm the RevisionMetadata to scrub
   * @param words the list of words to replace
   * @param replacement the String to replace the target words with
   * @param wordAlone true if the words to match must surrounded by word boundaries
   * @return a copy representing the RevisionMetadata resulting from the scrub
   */
  public static RevisionMetadata stripFromAllFields(
      RevisionMetadata rm, List<String> words, String replacement, boolean wordAlone) {
    String newId = new String(rm.id);
    String newAuthor = new String(rm.author);
    String newDescription = new String(rm.description);
    for (String word : words) {
      String regex = (wordAlone) ? ("(?i)(\\b)" + word + "(\\b)") : ("(?i)" + word);
      newId = newId.replaceAll(regex, replacement);
      newAuthor = newAuthor.replaceAll(regex, replacement);
      newDescription = newDescription.replaceAll(regex, replacement);
    }
    return new RevisionMetadata(newId, newAuthor, rm.date, newDescription, rm.parents);
  }

  /** Provides the set of default metadata scrubbers */
  @dagger.Module
  public static class Module {
    @Provides(type = Type.SET)
    static MetadataScrubber usernameScrubber(MetadataUsernameScrubber impl) {
      return impl;
    }

    @Provides(type = Type.SET)
    static MetadataScrubber publicSectionScrubber(PublicSectionMetadataScrubber impl) {
      return impl;
    }

    @Provides(type = Type.SET)
    static MetadataScrubber descScrubber(DescriptionMetadataScrubber impl) {
      return impl;
    }
  }
}
