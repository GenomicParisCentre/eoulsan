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

package fr.ens.transcriptome.eoulsan.data.storages;

import java.util.Map;

import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This interface define a genome index storage.
 * @since 1.1
 * @author Laurent Jourdren
 */
public interface GenomeIndexStorage {

  /**
   * Get the DataFile that corresponds to a mapper and a genome
   * @param mapper mapper
   * @param genome genome description object for the genome
   * @param additionalDescription description of the additional parameters
   * @return a DataFile that contains the path to the index or null if the index
   *         has not yet been computed
   */
  DataFile get(SequenceReadsMapper mapper, GenomeDescription genome,
      Map<String, String> additionalDescription);

  /**
   * Put the index archive in the storage.
   * @param mapper mapper
   * @param genome genome description object
   * @param additionalDescription description of the additional parameters
   * @param indexArchive the DataFile that contains the index
   */
  void put(SequenceReadsMapper mapper, GenomeDescription genome,
      Map<String, String> additionalDescription, DataFile indexArchive);

}
