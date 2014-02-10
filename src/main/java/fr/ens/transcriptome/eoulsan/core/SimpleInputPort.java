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

package fr.ens.transcriptome.eoulsan.core;

import java.io.Serializable;
import java.util.EnumSet;

import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class define an input port of a step.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class SimpleInputPort extends AbstractPort implements InputPort,
    Serializable {

  private static final long serialVersionUID = 4663590179211976634L;

  private final EnumSet<CompressionType> compressionsAccepted;
  private final boolean requieredInWorkingDirectory;

  @Override
  public EnumSet<CompressionType> getCompressionsAccepted() {

    return this.compressionsAccepted;
  }

  @Override
  public boolean isRequieredInWorkingDirectory() {

    return this.requieredInWorkingDirectory;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param name name of the port
   * @param format format of the port
   */
  SimpleInputPort(final String name, final DataFormat format) {

    this(name, format, null);
  }

  /**
   * Constructor.
   * @param name name of the port
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   */
  SimpleInputPort(final String name, final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted) {

    this(name, format, compressionsAccepted, false);
  }

  /**
   * Constructor.
   * @param name name of the port
   * @param format format of the port
   * @param requieredInWorkingDirectory if data is required in working directory
   */
  SimpleInputPort(final String name, final DataFormat format,
      boolean requieredInWorkingDirectory) {

    this(name, format, null, requieredInWorkingDirectory);
  }

  /**
   * Constructor.
   * @param name name of the port
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   * @param requieredInWorkingDirectory if data is required in working directory
   */
  SimpleInputPort(final String name, final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted,
      boolean requieredInWorkingDirectory) {

    // Set the name and the format
    super(name, format);

    // Set the compressions accepted
    if (compressionsAccepted == null)
      this.compressionsAccepted = EnumSet.allOf(CompressionType.class);
    else
      this.compressionsAccepted = compressionsAccepted;

    // Set if input data is required in working directory
    this.requieredInWorkingDirectory = requieredInWorkingDirectory;
  }

  /**
   * Constructor.
   * @param inputport input port to clone
   */
  public SimpleInputPort(final InputPort inputport) {

    this(inputport.getName(), inputport.getFormat(), inputport
        .getCompressionsAccepted(), inputport.isRequieredInWorkingDirectory());
  }

}
