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
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.ABORTED;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.FAILED;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.PARTIALLY_DONE;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.READY;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.WAITING;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.WORKING;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.ExecutorArguments;
import fr.ens.transcriptome.eoulsan.core.schedulers.TaskSchedulerFactory;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.io.DesignWriter;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a Workflow. This class must be extended by a class to be
 * able to work with a specific workflow file format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractWorkflow implements Workflow {

  /** Serialization version UID. */
  private static final long serialVersionUID = 4865597995432347155L;

  private static final String DESIGN_COPY_FILENAME = "design.txt";
  protected static final String WORKFLOW_COPY_FILENAME = "workflow.xml";
  private static final String WORKFLOW_GRAPHVIZ_FILENAME = "workflow.gv";

  private final DataFile localWorkingDir;
  private final DataFile hadoopWorkingDir;
  private final DataFile outputDir;
  private final DataFile jobDir;
  private final DataFile taskDir;
  private final DataFile tmpDir;

  private final Design design;
  private final WorkflowContext workflowContext;
  private final Set<String> stepIds = new HashSet<>();
  private final Map<AbstractWorkflowStep, StepState> steps = new HashMap<>();
  private final Multimap<StepState, AbstractWorkflowStep> states =
      ArrayListMultimap.create();
  private final SerializableStopwatch stopwatch = new SerializableStopwatch();

  private AbstractWorkflowStep rootStep;
  private AbstractWorkflowStep designStep;
  private AbstractWorkflowStep checkerStep;
  private AbstractWorkflowStep firstStep;

  //
  // Getters
  //

  /**
   * Get the local working directory.
   * @return Returns the local working directory
   */
  DataFile getLocalWorkingDirectory() {
    return this.localWorkingDir;
  }

  /**
   * Get the local working directory.
   * @return Returns the local working directory
   */
  DataFile getHadoopWorkingDirectory() {
    return this.hadoopWorkingDir;
  }

  /**
   * Get the output directory.
   * @return Returns the output directory
   */
  DataFile getOutputDirectory() {
    return this.outputDir;
  }

  /**
   * Get the job directory.
   * @return Returns the log directory
   */
  DataFile getJobDirectory() {
    return this.jobDir;
  }

  /**
   * Get the task directory.
   * @return Returns the task directory
   */
  DataFile getTaskDirectory() {
    return this.taskDir;
  }

  @Override
  public Design getDesign() {

    return this.design;
  }

  @Override
  public Set<WorkflowStep> getSteps() {

    final Set<WorkflowStep> result = new HashSet<>();
    result.addAll(this.steps.keySet());

    return Collections.unmodifiableSet(result);
  }

  @Override
  public WorkflowStep getRootStep() {

    return this.rootStep;
  }

  @Override
  public WorkflowStep getDesignStep() {

    return this.designStep;
  }

  @Override
  public WorkflowStep getFirstStep() {

    return this.firstStep;
  }

  /**
   * Get checker step.
   * @return the checker step
   */
  protected WorkflowStep getCheckerStep() {

    return this.checkerStep;
  }

  /**
   * Get the real Context object. This method is useful to redefine context
   * values like base directory.
   * @return The Context object
   */
  public WorkflowContext getWorkflowContext() {

    return this.workflowContext;
  }

  //
  // Setters
  //

  /**
   * Register a step of the workflow.
   * @param step step to register
   */
  protected void register(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step cannot be null");

    if (step.getWorkflow() != this) {
      throw new IllegalStateException(
          "step cannot be part of more than one workflow");
    }

    if (this.stepIds.contains(step.getId())) {
      throw new IllegalStateException("2 step cannot had the same id: "
          + step.getId());
    }

    // Register root step
    if (step.getType() == StepType.ROOT_STEP) {

      if (this.rootStep != null && step != this.rootStep) {
        throw new IllegalStateException(
            "Cannot add 2 root steps to the workflow");
      }
      this.rootStep = step;
    }

    // Register design step
    if (step.getType() == StepType.DESIGN_STEP) {

      if (this.designStep != null && step != this.designStep) {
        throw new IllegalStateException(
            "Cannot add 2 design steps to the workflow");
      }
      this.designStep = step;
    }

    // Register checker step
    if (step.getType() == StepType.CHECKER_STEP) {

      if (this.checkerStep != null && step != this.checkerStep) {
        throw new IllegalStateException(
            "Cannot add 2 checkers steps to the workflow");
      }
      this.checkerStep = step;
    }

    // Register first step
    if (step.getType() == StepType.FIRST_STEP) {

      if (this.firstStep != null && step != this.firstStep) {
        throw new IllegalStateException(
            "Cannot add 2 first steps to the workflow");
      }
      this.firstStep = step;
    }

    synchronized (this) {
      this.steps.put(step, step.getState());
      this.states.put(step.getState(), step);
    }
  }

  /**
   * Update the status of a step. This method is used by steps to inform the
   * workflow object that the status of the step has been changed.
   * @param step Step that the status has been changed.
   */
  void updateStepState(final AbstractWorkflowStep step) {

    Preconditions.checkNotNull(step, "step argument is null");

    if (step.getWorkflow() != this) {
      throw new IllegalStateException("step is not part of the workflow");
    }

    synchronized (this) {

      StepState oldState = this.steps.get(step);
      StepState newState = step.getState();

      this.states.remove(oldState, step);
      this.states.put(newState, step);
      this.steps.put(step, newState);
    }
  }

  //
  // Check methods
  //

  /**
   * Check if the output file of the workflow already exists.
   * @throws EoulsanException if output files of the workflow already exists
   */
  private void checkExistingOutputFiles() throws EoulsanException {

    // For each step
    for (AbstractWorkflowStep step : this.steps.keySet()) {

      // that is a standard step that is not skip
      if (step.getType() == StepType.STANDARD_STEP && !step.isSkip()) {

        // and for each port
        for (WorkflowOutputPort port : step.getWorkflowOutputPorts()) {

          // Check if files that can generate the port already exists
          List<DataFile> files = port.getExistingOutputFiles();
          if (!files.isEmpty()) {

            throw new EoulsanException("For the step "
                + step.getId() + " data generated by the port "
                + port.getName() + " already exists: " + files.get(0));
          }
        }
      }
    }
  }

  /**
   * Check if the input file of the workflow already exists.
   * @throws EoulsanException if input files of the workflow already exists
   */
  private void checkExistingInputFiles() throws EoulsanException {

    // For each step
    for (AbstractWorkflowStep step : this.steps.keySet()) {

      // that is a standard step that is not skip
      if (step.getType() == StepType.STANDARD_STEP && !step.isSkip()) {

        // and for each port
        for (WorkflowInputPort port : step.getWorkflowInputPorts()) {

          // Get the link
          final WorkflowOutputPort link = port.getLink();

          // If the step that generate the data is skip
          if (link.getStep().getType() == StepType.STANDARD_STEP
              && link.getStep().isSkip()) {

            // Check if files that can generate the port already exists
            List<DataFile> files = link.getExistingOutputFiles();
            if (files.isEmpty()) {

              throw new EoulsanException("For the step \""
                  + step.getId() + "\" data needed by the port \""
                  + port.getName()
                  + "\" not exists (this data is generated by the port \""
                  + link.getName() + "\" of the step \""
                  + link.getStep().getId() + "\")");
            }
          }
        }
      }
    }
  }

  /**
   * Skip the generators that are only required by skipped steps.
   */
  private void skipGeneratorsIfNotNeeded() {

    for (AbstractWorkflowStep step : this.steps.keySet()) {

      // Search for generator steps
      if (step.getType() == StepType.GENERATOR_STEP) {

        boolean allStepSkipped = true;

        // Check if all linked step are skipped
        for (WorkflowOutputPort outputPort : step.getWorkflowOutputPorts()) {

          if (!outputPort.isAllLinksToSkippedSteps()) {
            allStepSkipped = false;
            break;
          }
        }

        // If all linked steps are skipped, skip the generator
        if (allStepSkipped) {
          step.setSkipped(true);
        }
      }
    }
  }

  //
  // Workflow lifetime methods
  //

  /**
   * Execute the workflow.
   * @throws EoulsanException if an error occurs while executing the workflow
   */
  public void execute() throws EoulsanException {

    // Skip generators if needed
    skipGeneratorsIfNotNeeded();

    // check if output files does not exists
    checkExistingOutputFiles();

    // check if input files exists
    checkExistingInputFiles();

    // Save configuration files (design and workflow files)
    saveConfigurationFiles();

    // Initialize scheduler
    TaskSchedulerFactory.initialize();

    // Start scheduler
    TaskSchedulerFactory.getScheduler().start();

    // Get the token manager registry
    final TokenManagerRegistry registry = TokenManagerRegistry.getInstance();

    // Set Steps to WAITING state
    for (AbstractWorkflowStep step : this.steps.keySet()) {

      // Create Token manager of each step
      registry.getTokenManager(step);

      // Set state to WAITING
      step.setState(WAITING);
    }

    // Start stop watch
    this.stopwatch.start();

    while (!getSortedStepsByState(READY, WAITING, PARTIALLY_DONE, WORKING)
        .isEmpty()) {

      try {
        // TODO 2000 must be a constant
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      // Get the step that had failed
      final List<AbstractWorkflowStep> failedSteps =
          getSortedStepsByState(StepState.FAILED);

      if (!failedSteps.isEmpty()) {

        WorkflowStepResult firstResult = null;

        // Log error messages
        for (AbstractWorkflowStep failedStep : failedSteps) {

          final WorkflowStepResult result =
              TaskSchedulerFactory.getScheduler().getResult(failedStep);
          getLogger().severe(
              "Fail of the analysis: " + result.getErrorMessage());

          if (firstResult == null) {
            firstResult = result;
          }
        }

        // Get the exception that cause the fail of the analysis
        final Throwable exception;
        if (firstResult.getException() != null) {
          exception = firstResult.getException();
        } else {
          exception = new EoulsanException("Fail of the analysis.");
        }

        // Stop the analysis
        emergencyStop(exception, firstResult.getErrorMessage());

        break;
      }
    }

    // Remove outputs to discard
    removeOutputsToDiscard();

    // Stop the workflow
    stop();

    logEndAnalysis(true);
  }

  /**
   * Stop the threads used by the workflow.
   */
  private void stop() {

    final TokenManagerRegistry registry = TokenManagerRegistry.getInstance();
    for (AbstractWorkflowStep step : this.steps.keySet()) {

      // Stop Token manager dedicated thread
      final TokenManager tokenManager = registry.getTokenManager(step);

      if (tokenManager.isStarted()) {
        tokenManager.stop();
      }
    }

    // Stop scheduler
    TaskSchedulerFactory.getScheduler().stop();
  }

  /**
   * Stop the workflow if the analysis failed.
   * @param exception exception
   * @param errorMessage error message
   */
  void emergencyStop(final Throwable exception, final String errorMessage) {

    // Change working step state to aborted
    for (AbstractWorkflowStep step : getSortedStepsByState(PARTIALLY_DONE,
        WORKING)) {
      step.setState(ABORTED);
    }

    // Stop the workflow
    stop();

    final TokenManagerRegistry registry = TokenManagerRegistry.getInstance();

    // Remove all outputs of failed steps
    for (AbstractWorkflowStep step : getSortedStepsByState(FAILED)) {
      registry.getTokenManager(step).removeAllOutputs();
    }

    // Remove all outputs of aborted steps
    for (AbstractWorkflowStep step : getSortedStepsByState(ABORTED)) {
      registry.getTokenManager(step).removeAllOutputs();
    }

    // Log end of analysis
    logEndAnalysis(false);

    // Exit Eoulsan
    Common.errorExit(exception, errorMessage);
  }

  /**
   * Remove outputs to discard.
   */
  private void removeOutputsToDiscard() {

    final TokenManagerRegistry registry = TokenManagerRegistry.getInstance();
    for (AbstractWorkflowStep step : this.steps.keySet()) {

      // Stop Token manager dedicated thread
      final TokenManager tokenManager = registry.getTokenManager(step);
      tokenManager.removeOutputsToDiscard();
    }
  }

  //
  // Utility methods
  //

  /**
   * Save configuration files.
   * @throws EoulsanException if an error while writing files
   */
  protected void saveConfigurationFiles() throws EoulsanException {

    try {
      DataFile jobDir = getWorkflowContext().getJobDirectory();

      if (!jobDir.exists()) {
        jobDir.mkdirs();
      }

      // Save design file
      DesignWriter designWriter =
          new SimpleDesignWriter(
              new DataFile(jobDir, DESIGN_COPY_FILENAME).create());
      designWriter.write(getDesign());

      // Save the workflow as a Graphviz file
      new Workflow2Graphviz(this).save(new DataFile(jobDir,
          WORKFLOW_GRAPHVIZ_FILENAME));

    } catch (IOException | EoulsanIOException e) {
      throw new EoulsanException(
          "Error while writing design file or Graphiviz workflow file: "
              + e.getMessage(), e);
    }
  }

  /**
   * Get the steps which has some step status. The step are ordered.
   * @param states step status to retrieve
   * @return a sorted list with the steps
   */
  private List<AbstractWorkflowStep> getSortedStepsByState(
      final StepState... states) {

    Preconditions.checkNotNull(states, "states argument is null");

    final List<AbstractWorkflowStep> result = new ArrayList<>();

    for (StepState state : states) {
      result.addAll(getSortedStepsByState(state));
    }

    // Sort steps
    sortListSteps(result);

    return result;
  }

  /**
   * Get the steps which has a step status. The step are ordered.
   * @param state step status to retrieve
   * @return a sorted list with the steps
   */
  private List<AbstractWorkflowStep> getSortedStepsByState(final StepState state) {

    Preconditions.checkNotNull(state, "state argument is null");

    final Collection<AbstractWorkflowStep> collection;

    synchronized (this) {
      collection = this.states.get(state);
    }

    final List<AbstractWorkflowStep> result = Lists.newArrayList(collection);

    sortListSteps(result);

    return result;
  }

  /**
   * Sort a list of step by priority and then by step number.
   * @param list the list of step to sort
   */
  private static void sortListSteps(final List<AbstractWorkflowStep> list) {

    if (list == null) {
      return;
    }

    Collections.sort(list, new Comparator<AbstractWorkflowStep>() {

      @Override
      public int compare(final AbstractWorkflowStep a,
          final AbstractWorkflowStep b) {

        int result = a.getType().getPriority() - b.getType().getPriority();

        if (result != 0) {
          return result;
        }

        return a.getNumber() - b.getNumber();
      }
    });

  }

  /**
   * Create a DataFile object from a path.
   * @param path the path
   * @return null if the path is null or a new DataFile object with the required
   *         path
   */
  private static DataFile newDataFile(final String path) {

    if (path == null) {
      return null;
    }

    return new DataFile(URI.create(path));
  }

  /**
   * Check directories needed by the workflow.
   * @throws EoulsanException if an error about the directories is found
   */
  public void checkDirectories() throws EoulsanException {

    checkNotNull(this.jobDir, "the job directory is null");
    checkNotNull(this.taskDir, "the task directory is null");
    checkNotNull(this.outputDir, "the output directory is null");
    checkNotNull(this.localWorkingDir, "the local working directory is null");

    // Get Eoulsan settings
    final Settings settings = EoulsanRuntime.getSettings();

    // Define the list of directories to create
    final List<DataFile> dirsToCheck =
        Lists.newArrayList(this.jobDir, this.outputDir, this.localWorkingDir,
            this.hadoopWorkingDir, this.taskDir);

    // If the temporary directory has not been defined by user
    if (!settings.isUserDefinedTempDirectory()) {

      // Set the temporary directory
      checkNotNull(this.tmpDir, "The temporary directory is null");
      settings.setTempDirectory(this.tmpDir.toFile().toString());
      dirsToCheck.add(this.tmpDir);
    }

    try {
      for (DataFile dir : dirsToCheck) {

        if (dir == null) {
          continue;
        }

        if (dir.exists() && !dir.getMetaData().isDir()) {
          throw new EoulsanException("the directory is not a directory: " + dir);
        }

        if (!dir.exists()) {
          dir.mkdirs();
        }

      }
    } catch (IOException e) {
      throw new EoulsanException(e);
    }

    // Check temporary directory
    checkTemporaryDirectory();
  }

  /**
   * Check temporary directory.
   * @throws EoulsanException
   */
  private void checkTemporaryDirectory() throws EoulsanException {

    final File tempDir = EoulsanRuntime.getSettings().getTempDirectoryFile();

    if (tempDir == null) {
      throw new EoulsanException("Temporary directory is null");
    }

    if ("".equals(tempDir.getAbsolutePath())) {
      throw new EoulsanException("Temporary directory is null");
    }

    if (!tempDir.exists()) {
      throw new EoulsanException("Temporary directory does not exists: "
          + tempDir);
    }

    if (!tempDir.isDirectory()) {
      throw new EoulsanException("Temporary directory is not a directory: "
          + tempDir);
    }

    if (!tempDir.canRead()) {
      throw new EoulsanException("Temporary directory cannot be read: "
          + tempDir);
    }

    if (!tempDir.canWrite()) {
      throw new EoulsanException("Temporary directory cannot be written: "
          + tempDir);
    }

    if (!tempDir.canExecute()) {
      throw new EoulsanException("Temporary directory is not executable: "
          + tempDir);
    }
  }

  /**
   * Log the state and the time of the analysis.
   * @param success true if analysis was successful
   */
  private void logEndAnalysis(final boolean success) {

    this.stopwatch.stop();

    final String successString = success ? "Successful" : "Unsuccessful";

    // Log the end of the analysis
    getLogger().info(
        successString
            + " end of the analysis in "
            + StringUtils.toTimeHumanReadable(this.stopwatch
                .elapsed(MILLISECONDS)) + " s.");

    // Inform observers of the end of the analysis
    for (WorkflowStepObserver o : WorkflowStepObserverRegistry.getInstance()
        .getObservers()) {
      o.notifyWorkflowSuccess(
          success,
          "(Job done in "
              + StringUtils.toTimeHumanReadable(this.stopwatch
                  .elapsed(MILLISECONDS)) + " s.)");
    }

    // Send a mail

    final String mailSubject =
        "["
            + Globals.APP_NAME + "] " + successString + " end of your job "
            + this.workflowContext.getJobId() + " on "
            + this.workflowContext.getJobHost();

    final String mailMessage =
        "THIS IS AN AUTOMATED MESSAGE.\n\n"
            + successString
            + " end of your job "
            + this.workflowContext.getJobId()
            + " on "
            + this.workflowContext.getJobHost()
            + ".\nJob finished at "
            + new Date(System.currentTimeMillis())
            + " in "
            + StringUtils.toTimeHumanReadable(this.stopwatch
                .elapsed(MILLISECONDS))
            + " s.\n\nOutput files and logs can be found in the following location:\n"
            + this.workflowContext.getOutputDirectory() + "\n\nThe "
            + Globals.APP_NAME + "team.";

    // Send mail
    Common.sendMail(mailSubject, mailMessage);
  }

  //
  // Constructor
  //

  /**
   * Protected constructor.
   * @param executionArguments execution arguments
   * @param design design to use for the workflow
   * @throws EoulsanException if an error occurs while configuring the workflow
   */
  protected AbstractWorkflow(final ExecutorArguments executionArguments,
      final Design design) throws EoulsanException {

    Preconditions.checkNotNull(executionArguments, "Argument cannot be null");
    Preconditions.checkNotNull(design, "Design argument cannot be null");

    this.design = design;

    this.jobDir = newDataFile(executionArguments.getJobPathname());

    this.taskDir = newDataFile(executionArguments.getTaskPathname());

    this.tmpDir = newDataFile(executionArguments.getTemporaryPathname());

    this.localWorkingDir =
        newDataFile(executionArguments.getLocalWorkingPathname());

    this.hadoopWorkingDir =
        newDataFile(executionArguments.getHadoopWorkingPathname());

    this.outputDir = newDataFile(executionArguments.getOutputPathname());

    this.workflowContext = new WorkflowContext(executionArguments, this);

  }
}
