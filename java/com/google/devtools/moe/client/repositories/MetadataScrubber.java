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

import java.util.List;

/**
 * Abstract superclass defining what any MetadataScrubber should be able to do. A MetadataScrubber
 * is an object that can take in a RevisionMetadata object and then return a new one which has been
 * "scrubbed" of any undesirable information.
 *
 */
public abstract class MetadataScrubber {

  /**
   * The primary method of a MetadataScrubber. It returns a copy of the RevisionMetadata whose
   * data fields have been scrubbed in the prescribed way.
   */
  public abstract RevisionMetadata scrub(RevisionMetadata rm);

  /**
   * A helper method that is useful for stripping a list of words from all the fields of the
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
}
