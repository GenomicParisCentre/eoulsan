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

package fr.ens.transcriptome.eoulsan.checkers;

import java.util.HashMap;
import java.util.Map;

/**
 * This class define a storage where some results of the checker can be save for
 * later reuse by other checkers.
 * @author Laurent Jourdren
 */
public class CheckStore {

  private Map<String, Object> info = new HashMap<String, Object>();

  /**
   * Store some data.
   * @param key key of the data
   * @param value the data to store
   */
  public void add(final String key, Object value) {

    if (key == null || value == null)
      return;

    this.info.put(key, value);
  }

  /**
   * Get some data.
   * @param key key of the data to retrieve
   * @return data if stored
   */
  public Object get(final String key) {

    return this.info.get(key);
  }

}
