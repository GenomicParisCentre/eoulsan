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

import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;

public interface StepConfigurationContext {

  /**
   * Get the command name.
   * @return the command name
   */
  String getCommandName();

  /**
   * Get the UUID of the job.
   * @return the job UUID
   */
  String getJobUUID();

  /**
   * Get the job description.
   * @return the job description
   */
  String getJobDescription();

  /**
   * Get the job environment.
   * @return the job environment
   */
  String getJobEnvironment();

  /**
   * Get command description.
   * @return the command description
   */
  String getCommandDescription();

  /**
   * Get the command author.
   * @return the command author
   */
  String getCommandAuthor();

  /**
   * Get the output path.
   * @return Returns the output Path
   */
  String getOutputPathname();

  /**
   * Get the job id.
   * @return the job id
   */
  String getJobId();

  /**
   * Get the host of the job.
   * @return a string with the host of the job
   */
  String getJobHost();

  /**
   * Get the design file path.
   * @return the design file path
   */
  String getDesignPathname();

  /**
   * Get the workflow file path.
   * @return the workflow file path
   */
  String getWorkflowPathname();

  /**
   * Get EoulsanRuntime.
   * @return the EoulsanRuntime
   */
  AbstractEoulsanRuntime getRuntime();

  /**
   * Get Eoulsan settings.
   * @return the Settings
   */
  Settings getSettings();

  /**
   * Get the logger.
   * @return the logger
   */
  Logger getLogger();

  /**
   * Get the current step.
   * @return the current Step or null if no Step is currently running.
   */
  WorkflowStep getCurrentStep();

}
