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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters;

/**
 * This class define a HTCondor cluster scheduler using a Bpipe script.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class HTCondorTaskScheduler extends BundledScriptBpipeTaskScheduler {

  public static final String SCHEDULER_NAME = "htcondor";
  private static final String COMMAND_WRAPPER_SCRIPT = "bpipe-htcondor.sh";

  //
  // Constructor
  //

  /**
   * Constructor.
   */
  public HTCondorTaskScheduler() {
    super(SCHEDULER_NAME, COMMAND_WRAPPER_SCRIPT);
  }

}
