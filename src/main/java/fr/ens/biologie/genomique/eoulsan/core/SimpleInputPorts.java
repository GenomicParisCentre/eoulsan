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

package fr.ens.biologie.genomique.eoulsan.core;

import java.io.Serializable;
import java.util.Set;

/**
 * This class define a class that handles a set of input ports.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SimpleInputPorts extends AbstractPorts<InputPort>
    implements InputPorts, Serializable {

  private static final long serialVersionUID = -6661589274392547355L;

  SimpleInputPorts(final Set<InputPort> ports) {
    super(ports);
  }

}
