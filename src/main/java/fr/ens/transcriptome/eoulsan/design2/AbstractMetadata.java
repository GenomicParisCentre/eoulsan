/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.design2;

import static org.python.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This abstract class defines methods for metadata.
 * @author Laurent Jourdren
 * @since 2.0
 */

public abstract class AbstractMetadata {

  private Map<String, String> md = new LinkedHashMap<>();

  //
  // Methods
  //

  /**
   * Get the value according the key.
   * @param key the key
   * @return the value
   */
  public String get(final String key) {

    checkNotNull(key, "key argument cannot be null");

    return this.md.get(key.trim());
  }

  /**
   * Set the value according the key.
   * @param key the key
   * @param value the value
   */
  public void set(final String key, final String value) {

    checkNotNull(key, "key argument cannot be null");
    checkNotNull(value, "value argument cannot be null");

    this.md.put(key, value);
  }

  /**
   * Test if the key is in md.
   * @param key the key
   * @return true if the key is in md
   */
  public boolean contains(final String key) {

    checkNotNull(key, "key argument cannot be null");

    return this.md.containsKey(key.trim());
  }

  /**
   * Get the value according the key as a list.
   * @param key the key
   * @return the value as a list
   */
  public List<String> getAsList(final String key) {

    checkNotNull(key, "key argument cannot be null");

    return StringUtils.deserializeStringArray(get(key.trim()));
  }

  /**
   * Set the key.
   * @return
   */
  public Set<String> keySet() {

    return Collections.unmodifiableSet(this.md.keySet());
  }

  /**
   * Set the entry.
   * @return
   */
  public Set<Map.Entry<String, String>> entrySet() {

    return Collections.unmodifiableSet(this.md.entrySet());
  }

  /**
   * Remove the value according the key.
   * @param key the key
   */
  public void remove(final String key) {

    checkNotNull(key, "key argument cannot be null");

    this.md.remove(key.trim());
  }

}
