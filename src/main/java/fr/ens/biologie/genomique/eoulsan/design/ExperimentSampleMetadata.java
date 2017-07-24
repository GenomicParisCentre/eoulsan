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

package fr.ens.biologie.genomique.eoulsan.design;

/**
 * This inteface defines the experiment sample metadata.
 * @author Xavier Bauquet
 * @since 2.0
 */
public interface ExperimentSampleMetadata extends Metadata {

  // constants
  String REP_TECH_GROUP_KEY = "RepTechGroup";
  String REFERENCE_KEY = "Reference";
  String CONDITION_KEY = "Condition";

  /**
   * Get the RepTechGroup.
   * @return the RepTechGroup
   */
  String getRepTechGroup();

  /**
   * Get the reference.
   * @return the reference
   */
  String getReference();

  /**
   * Get the reference.
   * @return the reference
   */
  boolean isReference();

  /**
   * Get the condition.
   * @return the condition
   */
  String getCondition();

  /**
   * Set the ReptechGroup.
   * @param newReptechGroup the new ReptechGroup
   */
  void setRepTechGroup(String newReptechGroup);

  /**
   * Set the reference.
   * @param newReference the new reference
   */
  void setReference(boolean newReference);

  /**
   * Set the reference.
   * @param newReference the new reference
   */
  void setReference(String newReference);

  /**
   * Set the condition.
   * @param newCondition the new condition
   */
  void setCondition(String newCondition);

  /**
   * Test if the RepTechGroup field exists.
   * @return true if the RepTechGroup field exists
   */
  boolean containsRepTechGroup();

  /**
   * Test if the reference field exists.
   * @return true if the reference field exists
   */
  boolean containsReference();

  /**
   * Test if the condition field exists.
   * @return true if the condition field exists
   */
  boolean containsCondition();

}
