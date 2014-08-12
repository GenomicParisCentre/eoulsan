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
import fr.ens.transcriptome.eoulsan.core.workflow.Workflow;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * This interface define the context of a step.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface StepContext {

  /**
   * Get the context name.
   * @return a String with the context name
   */
  String getContextName();

  /**
   * Set the context name.
   * @param contextName the name of the context
   */
  void setContextName(String contextName);

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
   * Get the local working path.
   * @return Returns the local working Path
   */
  String getLocalWorkingPathname();

  /**
   * Get the Hadoop working path.
   * @return Returns the Hadoop working Path
   */
  String getHadoopWorkingPathname();

  /**
   * Get the log path.
   * @return Returns the log Path
   */
  String getLogPathname();

  /**
   * Get the task path.
   * @return Returns the task Path
   */
  String getTaskPathname();

  /**
   * Get the output path.
   * @return Returns the output Path
   */
  String getOutputPathname();

  /**
   * Get the step working path.
   * @return Returns the step working path
   */
  String getStepWorkingPathname();

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
   * Get the creation time of the context.
   * @return the creation time of the context in milliseconds since epoch
   *         (1.1.1970)
   */
  long getContextCreationTime();

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
   * Get the application jar path.
   * @return Returns the jar path
   */
  String getJarPathname();

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
   * Get the workflow description
   * @return the workflow description
   */
  Workflow getWorkflow();

  /**
   * Get the current step.
   * @return the current Step or null if no Step is currently running.
   */
  WorkflowStep getCurrentStep();

  /**
   * Get the input data for an input DataType and a Sample.
   * @param format the DataFormat of the source
   * @return a String with the pathname
   */
  Data getInputData(DataFormat format);

  /**
   * Get the input data for a port name and a Sample.
   * @param portName the name of the port
   * @return a String with the pathname
   */
  Data getInputData(String portName);

  /**
   * Get the output data for an input DataType and a Sample.
   * @param format the DataFormat of the source
   * @param dataName the name of the data
   * @return a String with the pathname
   */
  Data getOutputData(DataFormat format, String dataName);

  /**
   * Get the output data for an input DataType and a Sample.
   * @param format the DataFormat of the source
   * @param dataName the name of the data
   * @param part data part
   * @return a String with the pathname
   */
  Data getOutputData(DataFormat format, String dataName, int part);

  /**
   * Get the output data for an input DataType and a Sample.
   * @param format the DataFormat of the source
   * @param origin origin of the new data
   * @return a String with the pathname
   */
  Data getOutputData(DataFormat format, Data origin);

  /**
   * Get the output data for a port name and a Sample.
   * @param portName the name of the port
   * @param dataName the name of the data
   * @return a String with the pathname
   */
  Data getOutputData(String portName, String dataName);

  /**
   * Get the output data for a port name and a Sample.
   * @param portName the name of the port
   * @param dataName the name of the data
   * @param part data part
   * @return a String with the pathname
   */
  Data getOutputData(String portName, String dataName, int part);

  /**
   * Get the output data for a port name and a Sample.
   * @param portName the name of the port
   * @param origin origin of the new data
   * @return a String with the pathname
   */
  Data getOutputData(String portName, Data origin);

}
