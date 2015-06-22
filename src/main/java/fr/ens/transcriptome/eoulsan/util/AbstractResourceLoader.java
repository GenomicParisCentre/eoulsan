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

package fr.ens.transcriptome.eoulsan.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class define an abstract resource loader.
 * @param <S> Type of the data to load
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractResourceLoader<S> implements ResourceLoader<S> {

  private final Multimap<String, String> resources = ArrayListMultimap.create();

  //
  // Abstract methods
  //

  /**
   * Get the input stream related to a resource.
   * @param resourcePath resource path
   * @return a resource object
   * @throws IOException if an error occurs while creating the input stream
   */
  protected abstract InputStream getResourceAsStream(final String resourcePath)
      throws IOException;

  /**
   * Load a resource.
   * @param in input stream
   * @return a resource object
   * @throws IOException if an error occurs while loading the resource
   * @throws EoulsanException if an error occurs while creating the resource
   *           object
   */
  protected abstract S load(final InputStream in) throws IOException,
      EoulsanException;

  /**
   * Get the resource name.
   * @param resource the resource
   * @return the name of the resource
   */
  protected abstract String getResourceName(S resource);

  //
  // Resources loading
  //

  /**
   * Add a resource.
   * @param resourceName resource name
   * @param resourcePath resource path
   */
  protected void addResource(final String resourceName,
      final String resourcePath) {

    checkNotNull(resourceName, "resourceName argument cannot be null");
    checkNotNull(resourcePath, "resourcePath argument cannot be null");

    this.resources.put(resourceName, resourcePath);
  }

  @Override
  public List<S> loadAllResources() {

    final List<S> result = new ArrayList<>();

    for (String resourcePath : this.resources.values()) {

      try {
        result.add(load(getResourceAsStream(resourcePath)));
      } catch (IOException | EoulsanException e) {
        throw new ServiceConfigurationError("Unable to load resource", e);
      }
    }

    return Collections.unmodifiableList(result);
  }

  @Override
  public List<S> loadResources(final String resourceName) {

    checkNotNull(resourceName, "resourceName argument cannot be null");

    if (!this.resources.containsKey(resourceName)) {
      return Collections.emptyList();
    }

    final List<S> result = new ArrayList<>();

    for (String resourcePath : this.resources.get(resourceName)) {

      try {
        result.add(load(getResourceAsStream(resourcePath)));
      } catch (IOException | EoulsanException e) {
        throw new ServiceConfigurationError("Unable to load resource", e);
      }
    }

    return Collections.unmodifiableList(result);
  }

}
