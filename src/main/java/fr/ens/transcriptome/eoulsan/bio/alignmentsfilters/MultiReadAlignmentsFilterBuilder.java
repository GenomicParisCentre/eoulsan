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

package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import static fr.ens.transcriptome.eoulsan.util.Utils.newArrayList;
import static fr.ens.transcriptome.eoulsan.util.Utils.newHashMap;
import static fr.ens.transcriptome.eoulsan.util.Utils.newLinkedHashMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This builder allow to create a MultiAlignmentsFilter object.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class MultiReadAlignmentsFilterBuilder {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final Map<String, ReadAlignmentsFilter> mapFilters = newHashMap();
  private final List<ReadAlignmentsFilter> listFilter = newArrayList();
  private final Map<String, String> mapParameters = newLinkedHashMap();

  /**
   * Add a parameter to the builder
   * @param key key of the parameter
   * @param value value of the parameter
   * @throws EoulsanException if the filter reference in the key does not exist
   *           or if an error occurs while setting the parameter in the
   *           dedicated filter
   */
  public void addParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null)
      return;

    // Get first dot position
    final String keyTrimmed = key.trim();
    final int index = keyTrimmed.indexOf('.');

    final String filterName;
    final String filterKey;

    // Get the filter name and parameter name
    if (index == -1) {
      filterName = keyTrimmed;
      filterKey = null;
    } else {
      filterName = keyTrimmed.substring(0, index);
      filterKey = keyTrimmed.substring(index + 1);
    }

    final ReadAlignmentsFilter filter;

    // Get the filter object, load it if necessary
    if (mapFilters.containsKey(filterName))
      filter = mapFilters.get(filterName);
    else {
      filter =
          ReadAlignmentsFilterService.getInstance().getAlignmentsFilter(
              filterName);

      if (filter == null)
        throw new EoulsanException("Unable to find "
            + filterName + " alignments filter.");

      this.mapFilters.put(filterName, filter);
      this.listFilter.add(filter);
    }

    // Set the parameter
    if (filterKey != null) {
      final String valueTrimmed = value.trim();
      filter.setParameter(filterKey, valueTrimmed);
      this.mapParameters.put(keyTrimmed, valueTrimmed);
      LOGGER
          .info("Set alignments filter \""
              + filterName + "\" with parameter: " + filterKey + "="
              + valueTrimmed);
    } else
      LOGGER.info("Set alignments filter \""
          + filterName + "\" with no parameter");

  }

  /**
   * Create the final MultiAlignmentsFilter.
   * @return a new MultiAlignmentsFilter object
   * @throws EoulsanException if an error occurs while initialize one of the
   *           filter
   */
  public ReadAlignmentsFilter getAlignmentsFilter() throws EoulsanException {

    for (ReadAlignmentsFilter f : this.listFilter)
      f.init();

    final MultiReadAlignmentsFilter mrf =
        new MultiReadAlignmentsFilter(this.listFilter);

    return mrf;
  }

  /**
   * Create the final MultiAlignmentsFilter.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @return a new MultiAlignmentsFilter object
   * @throws EoulsanException if an error occurs while initialize one of the
   *           filter
   */
  public ReadAlignmentsFilter getAlignmentsFilter(
      final ReporterIncrementer incrementer, final String counterGroup)
      throws EoulsanException {

    for (ReadAlignmentsFilter f : this.listFilter)
      f.init();

    final MultiReadAlignmentsFilter maf =
        new MultiReadAlignmentsFilter(incrementer, counterGroup,
            this.listFilter);

    return maf;
  }

  /**
   * Get a map with all the parameters used to create the
   * MultiAlignementsFilter.
   * @return an ordered map object
   */
  public Map<String, String> getParameters() {

    return Collections.unmodifiableMap(this.mapParameters);
  }

}
