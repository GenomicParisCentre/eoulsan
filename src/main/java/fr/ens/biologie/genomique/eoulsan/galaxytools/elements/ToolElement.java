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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.galaxytools.elements;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

/**
 * The ToolElement interface.
 * @author Sandrine Perrin
 * @since 2.0
 */
public interface ToolElement {

  /** The Constant SEP. */
  String SEP = ".";

  /**
   * Gets the name.
   * @return the name
   */
  String getName();

  /**
   * Gets the short name.
   * @return the short name
   */
  String getShortName();

  /**
   * Gets the name which respect Eoulsan's syntax.
   * @return the name
   */
  String getValidatedName();

  /**
   * Gets the value.
   * @return the value
   */
  String getValue();

  /**
   * Sets the value.
   * @param value the value
   * @throws EoulsanException the eoulsan exception
   */
  void setValue(final String value) throws EoulsanException;

}
