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

package fr.ens.transcriptome.eoulsan.bio.readsfilters;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This builder allow to create a MultiReadFilter object.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class MultiReadFilterBuilder {

  private final Map<String, ReadFilter> mapFilters = new HashMap<>();
  private final List<ReadFilter> listFilter = new ArrayList<>();

  private final Map<String, String> mapParameters = new LinkedHashMap<>();

  /**
   * Add a parameter to the builder
   * @param key key of the parameter
   * @param value value of the parameter
   * @return true if the parameter has been successfully added
   * @throws EoulsanException if the filter reference in the key does not exist
   *           or if an error occurs while setting the parameter in the
   *           dedicated filter
   */
  public boolean addParameter(final String key, final String value)
      throws EoulsanException {

    return addParameter(key, value, false);
  }

  /**
   * Add a parameter to the builder
   * @param key key of the parameter
   * @param value value of the parameter
   * @param noExceptionIfFilterNotExists do not thrown an exception if the
   *          filter does not exists.
   * @return true if the parameter has been successfully added
   * @throws EoulsanException if the filter reference in the key does not exist
   *           or if an error occurs while setting the parameter in the
   *           dedicated filter
   */
  public boolean addParameter(final String key, final String value,
      final boolean noExceptionIfFilterNotExists) throws EoulsanException {

    if (key == null || value == null)
      return false;

    // Get first dot position
    final String keyTrimmed = key.trim();
    final int index = keyTrimmed.indexOf('.');

    final String filterName;
    final String filterKey;

    // Get the the filter name and parameter name
    if (index == -1) {
      filterName = keyTrimmed;
      filterKey = null;
    } else {
      filterName = keyTrimmed.substring(0, index);
      filterKey = keyTrimmed.substring(index + 1);
    }

    final ReadFilter filter;

    // Get the filter object, load it if necessary
    if (mapFilters.containsKey(filterName))
      filter = mapFilters.get(filterName);
    else {
      filter = ReadFilterService.getInstance().newService(filterName);

      if (filter == null) {

        if (noExceptionIfFilterNotExists)
          return false;

        throw new EoulsanException("Unable to find "
            + filterName + " read filter.");
      }
      this.mapFilters.put(filterName, filter);
      this.listFilter.add(filter);
    }

    // Set the parameter
    if (filterKey != null) {
      final String valueTrimmed = value.trim();
      filter.setParameter(filterKey, valueTrimmed);
      this.mapParameters.put(keyTrimmed, valueTrimmed);
      getLogger().info(
          "Set read filter \""
              + filterName + "\" with parameter: " + filterKey + "="
              + valueTrimmed);
    } else {
      this.mapParameters.put(filterName, "");
      getLogger().info(
          "Set read filter \"" + filterName + "\" with no parameter");
    }

    return true;
  }

  /**
   * Add parameters to the builder.
   * @param parameters parameters to add
   * @throws EoulsanException if the filter reference in the key does not exist
   *           or if an error occurs while setting the parameter in the
   *           dedicated filter
   */
  public void addParameters(final Map<String, String> parameters)
      throws EoulsanException {

    if (parameters == null)
      return;

    for (Map.Entry<String, String> e : parameters.entrySet())
      addParameter(e.getKey(), e.getValue());
  }

  /**
   * Create the final MultiReadFilter.
   * @return a new MultiReadFilter object
   * @throws EoulsanException if an error occurs while initialize one of the
   *           filter
   */
  public MultiReadFilter getReadFilter() throws EoulsanException {

    for (ReadFilter f : this.listFilter)
      f.init();

    final MultiReadFilter mrf = new MultiReadFilter(this.listFilter);

    return mrf;
  }

  /**
   * Create the final MultiReadFilter.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @return a new MultiReadFilter object
   * @throws EoulsanException if an error occurs while initialize one of the
   *           filter
   */
  public MultiReadFilter getReadFilter(final ReporterIncrementer incrementer,
      final String counterGroup) throws EoulsanException {

    for (ReadFilter f : this.listFilter)
      f.init();

    final MultiReadFilter mrf =
        new MultiReadFilter(incrementer, counterGroup, this.listFilter);

    return mrf;
  }

  /**
   * Get a map with all the parameters used to create the MultiReadFilter.
   * @return an ordered map object
   */
  public Map<String, String> getParameters() {

    return Collections.unmodifiableMap(this.mapParameters);
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   */
  public MultiReadFilterBuilder() {
  }

  /**
   * Public constructor.
   * @param parameters parameters to add to the builder
   * @throws EoulsanException if the filter reference in the key does not exist
   *           or if an error occurs while setting the parameter in the
   *           dedicated filter
   */
  public MultiReadFilterBuilder(final Map<String, String> parameters)
      throws EoulsanException {

    addParameters(parameters);
  }

}
