/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.SystemUtils;

/**
 * This class is the executor for running all the steps of an analysis.
 * @author Laurent Jourdren
 */
public abstract class Executor {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private Command command;
  private SimpleExecutorInfo info = new SimpleExecutorInfo();

  private long startTimeCurrentStep;

  //
  // Getters
  //

  /**
   * Get the command object
   * @return Returns the command
   */
  protected Command getCommand() {
    return this.command;
  }
  
  /**
   * Get the information object
   * @return the information object
   */
  protected SimpleExecutorInfo getInfo() {
    
    return this.info;
  }

  //
  // Setters
  //

  /**
   * Set the command object.
   * @param command The command to set
   */
  protected void setCommand(final Command command) {
    this.command = command;
  }

  //
  // Abstract methods
  //

  /**
   * Load design object.
   */
  protected abstract Design loadDesign() throws EoulsanException;

  /**
   * Get a Step object from its name.
   * @param stepName name of the step
   * @return a Step object
   * @throws EoulsanException if the step does not exits
   */
  protected abstract Step getStep(final String stepName);

  /**
   * Write the log file of the result of a step
   * @param result Step result
   */
  protected abstract void writeStepLogs(final StepResult result);

  //
  // Private methods
  //

  /**
   * Get a Step object from its name.
   * @param stepName name of the step
   * @return a Step object
   * @throws EoulsanException if the step does not exits
   */
  private final Step findStep(final String stepName) throws EoulsanException {

    if (stepName == null)
      throw new EoulsanException("Step name is null");

    final String lower = stepName.trim().toLowerCase();
    final Step result = getStep(lower);

    if (result == null)
      throw new EoulsanException("Unknown step: " + lower);

    return result;
  }

  /**
   * Create the list of steps
   * @return a list of steps
   * @throws EoulsanException if an error occurs while creating the step
   */
  private List<Step> createSteps() throws EoulsanException {

    final List<Step> result = new ArrayList<Step>();
    final Command c = this.command;

    for (String stepName : c.getStepNames()) {

      final Step s = findStep(stepName);
      logger.info("Configure " + stepName + " step.");
      s.configure(c.getStepParameters(stepName), c.getGlobalParameters());

      result.add(s);
    }

    return result;
  }

  /**
   * run Eoulsan.
   * @throws EoulsanException if an error occurs while creating of executing
   *           steps
   */
  public void execute() throws EoulsanException {

    // Check command object
    if (this.command == null)
      throw new EoulsanException("The command is null");

    // Check base path
    if (this.info.getBasePathname() == null)
      throw new EoulsanException("The base path is null");

    // Load design
    final Design design = loadDesign();

    // Check design
    if (design == null)
      throw new EoulsanException("The design is null");

    // Check samples count
    if (design.getSampleCount() == 0)
      throw new EoulsanException(
          "Nothing to do, no samples found in design file");

    // Create steps
    final List<Step> steps = createSteps();

    logger.info("Date: " + new Date(System.currentTimeMillis()));
    logger.info("Host: " + SystemUtils.getHostName());
    logger.info("Operating system name: " + System.getProperty("os.name"));
    logger.info("Operating system arch: " + System.getProperty("os.arch"));
    logger
        .info("Operating system version: " + System.getProperty("os.version"));
    logger.info("Java version: " + System.getProperty("java.version"));
    logger.info("Log level: " + logger.getLevel());

    boolean success = true;
    final long startTime = System.currentTimeMillis();

    // Execute steps
    for (Step s : steps) {

      logStartStep(s.getName());
      final StepResult r = s.execute(design, this.info);
      logEndStep(s.getName());

      // Write step logs
      writeStepLogs(r);

      // End of the analysis if the analysis fail
      if (!r.isSuccess()) {
        logger.severe("Fail of the analysis: " + r.getErrorMessage());
        success = false;
        break;
      }

    }

    final long endTime = System.currentTimeMillis();

    logger.info("End of the analysis in "
        + StringUtils.toTimeHumanReadable(endTime - startTime) + " s.");

    if (!success) {
      System.err.println("Error during analysis.");
      System.exit(1);
    }

  }

  //
  // Utility methods
  //

  /**
   * Add log entry for step phase.
   * @param stepName Name of current the phase
   */
  private void logStartStep(final String stepName) {

    this.startTimeCurrentStep = System.currentTimeMillis();
    logger.info("Start " + stepName + " step.");
  }

  /**
   * Add log entry for end step.
   * @param stepName Name of current the step
   */
  private void logEndStep(final String stepName) {

    final long endTimePhase = System.currentTimeMillis();

    logger.info("Process step "
        + stepName
        + " in "
        + StringUtils.toTimeHumanReadable(endTimePhase
            - this.startTimeCurrentStep) + " s.");
  }

}
