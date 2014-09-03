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

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.data.DataMetadata;

/**
 * This class define an unmodifiable class for metadata of data objects.
 * @since 2.0
 * @author Laurent Jourdren
 */
class UnmodifiableDataMetadata extends AbstractDataMetaData {

  private static final long serialVersionUID = 2773004152416176374L;

  private final DataMetadata metadata;

  @Override
  public String get(final String key) {

    return this.metadata.get(key);
  }

  @Override
  public void set(final String key, final String value) {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsKey(final String key) {

    return this.metadata.containsKey(key);
  }

  @Override
  public boolean removeKey(final String key) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void set(final DataMetadata metadata) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {

    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> keySet() {

    return this.metadata.keySet();
  }

  @Override
  public String toString() {

    return this.metadata.toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param metadata the DataMetadata object to wrap
   */
  UnmodifiableDataMetadata(final DataMetadata metadata) {

    checkNotNull(metadata, "metadata argument cannot be null");

    this.metadata = metadata;
  }

}
