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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.core.workflow.FileNaming;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * Abstract class that define a port.
 * @since 2.0
 * @author Laurent Jourdren
 */
public abstract class AbstractPort implements Port, Serializable {

  private static final long serialVersionUID = 1773398938012180465L;

  private final String name;
  private final boolean list;
  private final DataFormat format;

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public boolean isList() {

    return this.list;
  }

  @Override
  public DataFormat getFormat() {

    return this.format;
  }

  @Override
  public Set<WorkflowStep> getLinkedSteps() {
    return Collections.emptySet();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param name name of the port
   * @param list true if the port requires a list as value
   * @param format format of the port
   */
  AbstractPort(final String name, final boolean list, final DataFormat format) {

    checkNotNull(name, "The name of the port is null");
    checkNotNull(format, "The format of the port " + name + " is null");
    checkArgument(FileNaming.isPortNameValid(name),
        "Invalid port name (only ascii letters and digits are allowed): "
            + name.trim());

    this.name = name.trim().toLowerCase();
    this.list = list;
    this.format = format;
  }

}
