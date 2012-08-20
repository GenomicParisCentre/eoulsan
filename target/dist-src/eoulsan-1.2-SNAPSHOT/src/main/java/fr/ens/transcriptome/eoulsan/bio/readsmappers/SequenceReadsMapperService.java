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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * This class define a service to retrieve a SequenceReadsMapper
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SequenceReadsMapperService {

  private static SequenceReadsMapperService service;
  private final ServiceLoader<SequenceReadsMapper> loader;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of SequenceReadsMapperService.
   * @return A SequenceReadsMapperService instance
   */
  public static synchronized SequenceReadsMapperService getInstance() {

    if (service == null) {
      service = new SequenceReadsMapperService();
    }

    return service;
  }

  //
  // Instance methods
  //

  /**
   * Get SequenceReadsMapper.
   * @param mapperName name of the mapper to get
   * @return a SequenceReadsMapper
   */
  public SequenceReadsMapper getMapper(final String mapperName) {

    if (mapperName == null) {
      return null;
    }

    final String mapperNameLower = mapperName.toLowerCase();

    final Iterator<SequenceReadsMapper> it = this.loader.iterator();

    while (it.hasNext()) {

      final SequenceReadsMapper mapper = it.next();

      if (mapperNameLower.equals(mapper.getMapperName().toLowerCase())) {
        return mapper;
      }
    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private SequenceReadsMapperService() {

    loader = ServiceLoader.load(SequenceReadsMapper.class);
  }

}
