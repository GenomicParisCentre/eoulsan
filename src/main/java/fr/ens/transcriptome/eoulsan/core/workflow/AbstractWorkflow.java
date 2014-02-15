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
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.READY;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState.WAITING;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.DESIGN_STEP;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.GENERATOR_STEP;
import static fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType.STANDARD_STEP;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Main;
import fr.ens.transcriptome.eoulsan.core.ExecutorArguments;
import fr.ens.transcriptome.eoulsan.core.OutputPort;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a Workflow. This class must be extended by a class to be
 * able to work with a specific worklow file format.
 * @author Laurent Jourdren
 * @since 1.3
 */
public abstract class AbstractWorkflow implements Workflow {

  /** Serialization version UID. */
  private static final long serialVersionUID = 4865597995432347155L;

  private static final String STEP_RESULT_FILE_EXTENSION = ".result";

  private final DataFile localWorkingDir;
  private final DataFile hadoopWorkingDir;
  private final DataFile outputDir;
  private final DataFile logDir;

  private final Design design;
  private final WorkflowContext workflowContext;
  private final Set<String> stepIds = Sets.newHashSet();
  private final Map<AbstractWorkflowStep, StepState> steps = Maps.newHashMap();
  private final Multimap<StepState, AbstractWorkflowStep> states =
      ArrayListMultimap.create();

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
  DataFile getLocalWorkingDir() {
    return this.localWorkingDir;
  }

  /**
   * Get the local working directory.
   * @return Returns the local working directory
   */
  DataFile getHadoopWorkingDir() {
    return this.hadoopWorkingDir;
  }

  /**
   * Get the output directory.
   * @return Returns the output directory
   */
  DataFile getOutputDir() {
    return this.outputDir;
  }

  /**
   * Get the log directory.
   * @return Returns the log directory
   */
  DataFile getLogDir() {
    return this.logDir;
  }

  @Override
  public Design getDesign() {

    return this.design;
  }

  @Override
  public Set<WorkflowStep> getSteps() {

    final Set<WorkflowStep> result = Sets.newHashSet();
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

    if (step.getWorkflow() != this)
      throw new IllegalStateException(
          "step cannot be part of more than one workflow");

    if (this.stepIds.contains(step.getId()))
      throw new IllegalStateException("2 step cannot had the same id: "
          + step.getId());

    // Register root step
    if (step.getType() == StepType.ROOT_STEP) {

      if (this.rootStep != null && step != this.rootStep)
        throw new IllegalStateException(
            "Cannot add 2 root steps to the workflow");
      this.rootStep = step;
    }

    // Register design step
    if (step.getType() == StepType.DESIGN_STEP) {

      if (this.designStep != null && step != this.designStep)
        throw new IllegalStateException(
            "Cannot add 2 design steps to the workflow");
      this.designStep = step;
    }

    // Register checker step
    if (step.getType() == StepType.CHECKER_STEP) {

      if (this.checkerStep != null && step != this.checkerStep)
        throw new IllegalStateException(
            "Cannot add 2 checkers steps to the workflow");
      this.checkerStep = step;
    }

    // Register first step
    if (step.getType() == StepType.FIRST_STEP) {

      if (this.firstStep != null && step != this.firstStep)
        throw new IllegalStateException(
            "Cannot add 2 first steps to the workflow");
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

    if (step.getWorkflow() != this)
      throw new IllegalStateException("step is not part of the workflow");

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

    final WorkflowFiles files = getWorkflowFilesAtRootStep();

    // Location where find files, null means the expected outputFile
    final List<DataFile> directories =
        Lists.newArrayList(getLocalWorkingDir(), getHadoopWorkingDir(),
            getOutputDir(), null);

    for (WorkflowStepOutputDataFile file : files.getOutputFiles()) {

      // Do not check output of skipped steps and outputs of design step
      if (file.getStep().isSkip() || file.getStep().getType() == DESIGN_STEP)
        continue;

      for (DataFile dir : directories) {

        // Set the file to test
        final DataFile test =
            dir == null ? file.getDataFile() : new DataFile(dir, file
                .getDataFile().getName());

        if (test.exists())
          throw new EoulsanException("For sample "
              + file.getSample().getId() + ", generated \""
              + file.getFormat().getName() + "\" already exists (" + test
              + ").");
      }
    }

    for (WorkflowStepOutputDataFile file : files.getReusedFiles()) {

      // Do not check output of skipped steps and outputs of design step
      if (file.getStep().isSkip() || file.getStep().getType() == DESIGN_STEP)
        continue;

      for (DataFile dir : directories) {

        // Set the file to test
        final DataFile test =
            dir == null ? file.getDataFile() : new DataFile(dir, file
                .getDataFile().getName());

        if (test.exists())
          throw new EoulsanException("For sample "
              + file.getSample().getId() + " in step " + file.getStep().getId()
              + ", generated \"" + file.getFormat().getName()
              + "\" already exists (" + test + ").");
      }
    }
  }

  /**
   * Check if the input file of the workflow already exists.
   * @throws EoulsanException if input files of the workflow already exists
   */
  private void checkExistingInputFiles() throws EoulsanException {

    final WorkflowFiles files = getWorkflowFilesAtRootStep();

    for (WorkflowStepOutputDataFile file : files.getInputFiles()) {
      if (!file.getStep().isSkip()
          && !file.isMayNotExist() && !file.getDataFile().exists()) {
        throw new EoulsanException("For sample "
            + file.getSample().getId() + " in step " + file.getStep().getId()
            + ", input file for " + file.getFormat().getName()
            + " not exists (" + file.getDataFile() + ").");
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

    // check if output files does not exists
    checkExistingOutputFiles();

    // check if input files exists
    checkExistingInputFiles();

    // Set Steps to WAITING state
    for (AbstractWorkflowStep step : this.steps.keySet())
      step.setState(WAITING);

    final Stopwatch stopwatch = new Stopwatch();
    stopwatch.start();

    while (!getSortedStepsByState(READY, WAITING).isEmpty()) {

      final List<AbstractWorkflowStep> stepsToExecute =
          getSortedStepsByState(READY);

      if (!stepsToExecute.isEmpty()) {

        final AbstractWorkflowStep step = stepsToExecute.get(0);

        // Skip step is necessary
        if (step.isSkip()) {
          step.setState(StepState.DONE);
          continue;
        }

        getLogger().info("Execute step: " + step.getId());

        // Execute step
        final StepResult result = executeStep(step);

        if (step.getType() == GENERATOR_STEP || step.getType() == STANDARD_STEP) {

          // Write step result in a file
          if (step.isCreateLogFiles())
            writeStepResult(step, result);

          // Create symlink in output directory
          createSymlinksInOutputDirectory(step);

          // End of the analysis if the analysis fail
          if (!result.isSuccess()) {

            getLogger().severe(
                "Fail of the analysis: " + result.getErrorMessage());
            logEndAnalysis(false, stopwatch);

            if (result.getException() != null)
              Common.errorExit(result.getException(), result.getErrorMessage());
            else
              Common.errorExit(new EoulsanException("Fail of the analysis."),
                  result.getErrorMessage());
          }
        }

        // If the step is terminal step, end of the execution of the workflow
        if (step.getStep() != null && step.getStep().isTerminalStep())
          break;

      }

    }
    logEndAnalysis(true, stopwatch);

  }

  /**
   * Launch the step and create a log file for each step.
   * @param step step
   * @return the step result object
   * @throws EoulsanException if an Interrupted exception occurs
   */
  private StepResult executeStep(final AbstractWorkflowStep step)
      throws EoulsanException {

    // Thread group name
    final String threadGroupName = "eoulsan-step-#" + step.getNumber();

    // Define thread group
    final ThreadGroup threadGroup = new ThreadGroup(threadGroupName);

    // Create Log handler and register it
    final Logger logger =
        step.isCreateLogFiles()
            ? createStepLogger(step, threadGroupName) : null;

    // Register the logger
    if (logger != null)
      EoulsanLogger.registerThreadGroupLogger(threadGroup, logger);

    // Wrapper for step result
    final List<StepResult> resultWrapper = Lists.newArrayList();

    // Runnable object
    final Runnable r = new Runnable() {

      @Override
      public void run() {
        resultWrapper.add(step.execute());
        System.out.println("coucou");
      }
    };

    try {
      // Create thread
      final Thread thread = new Thread(threadGroup, r);

      // Start thread
      thread.start();
      System.out.println("wait 1");
      // Wait the end of the thread
      thread.join();
      System.out.println("wait 2");

    } catch (InterruptedException e) {
      throw new EoulsanException(e.getMessage());
    } finally {

      if (logger != null) {

        Handler handler = logger.getHandlers()[0];

        // Close handler
        handler.close();

        // Remove logger from EoulsanLogger registry
        EoulsanLogger.removeThreadGroupLogger(threadGroup);

        // Remove handler
        logger.removeHandler(handler);
      }
    }

    return resultWrapper.get(0);
  }

  /**
   * Create the logger for a step.
   * @param step the step
   * @param threadGroupName the name of the thread group
   * @return a Logger instance
   */
  private Logger createStepLogger(final AbstractWorkflowStep step,
      final String threadGroupName) {

    // Define the log file for the step
    final DataFile logFile = new DataFile(getLogDir(), step.getId() + ".log");
    OutputStream logOut;
    try {

      logOut = logFile.create();

    } catch (IOException e) {
      return null;
    }

    // Get the logger for the step
    final Logger logger = Logger.getLogger(threadGroupName);

    final Handler handler = new StreamHandler(logOut, Globals.LOG_FORMATTER);

    // Disable parent Handler
    logger.setUseParentHandlers(false);

    // Set log level to all before setting the real log level
    logger.setLevel(Level.ALL);

    // Set the Handler
    logger.addHandler(handler);

    // Set log level
    handler.setLevel(Level.parse(Main.getInstance().getLogLevelArgument()
        .toUpperCase()));

    return logger;
  }

  /**
   * This method write the step result in a file.
   * @param step step that has been executed
   * @param result result object
   */
  private void writeStepResult(final WorkflowStep step, final StepResult result) {

    if (result == null)
      return;

    // Step result file
    final DataFile logFile =
        new DataFile(this.logDir, step.getId() + STEP_RESULT_FILE_EXTENSION);

    try {

      result.write(logFile);

    } catch (IOException e) {

      Common.showAndLogErrorMessage("Unable to create log file for "
          + step.getId() + " step.");
    }
  }

  /**
   * Create symbolic
   * @param step
   */

  private void createSymlinksInOutputDirectory(final AbstractWorkflowStep step) {

    if (step == null)
      return;

    final DataFile stepDir = step.getStepWorkingDir();
    final DataFile outputDir = getOutputDir();
    final StepContext context = step.getContext();

    try {
      if (!outputDir.getProtocol().canSymlink()
          || !stepDir.getProtocol().canSymlink() || outputDir.equals(stepDir))
        return;

      for (OutputPort port : step.getOutputPorts()) {
        for (Sample sample : getDesign().getSamples()) {

          final DataFormat format = port.getFormat();

          // Test the number of files by data
          if (format.getMaxFilesCount() == 1) {
            final DataFile file = context.getOutputDataFile(format, sample);
            final DataFile link = new DataFile(getOutputDir(), file.getName());

            // Remove existing file/symlink
            if (link.exists())
              link.delete();

            // Create symbolic link
            file.symlink(link);
          } else {

            // Handle multi files
            final int count = context.getOutputDataFileCount(format, sample);
            for (int i = 0; i < count; i++) {
              final DataFile file =
                  context.getOutputDataFile(format, sample, i);
              final DataFile link =
                  new DataFile(getOutputDir(), file.getName());

              // Remove existing file/symlink
              if (link.exists())
                link.delete();

              // Create symbolic link
              file.symlink(link);
            }
          }
          // if one file per analysis for the format, there only one symlink to
          // create
          if (format.isOneFilePerAnalysis())
            break;
        }
      }
    } catch (IOException e) {
      getLogger().warning(
          "Error while creating symlink of output step ("
              + step.getId() + ") file: " + e.getMessage());
    }
  }

  //
  // Utility methods
  //

  /**
   * Get the steps which has some step status. The step are ordered.
   * @param states step status to retrieve
   * @return a sorted list with the steps
   */
  private List<AbstractWorkflowStep> getSortedStepsByState(
      final StepState... states) {

    Preconditions.checkNotNull(states, "states argument is null");

    final List<AbstractWorkflowStep> result = Lists.newArrayList();

    for (StepState state : states)
      result.addAll(getSortedStepsByState(state));

    // Sort steps
    sortListSteps(result);

    return result;
  }

  /**
   * Get the steps which has a step status. The step are ordered.
   * @param states step status to retrieve
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
  private static final void sortListSteps(final List<AbstractWorkflowStep> list) {

    if (list == null)
      return;

    Collections.sort(list, new Comparator<AbstractWorkflowStep>() {

      @Override
      public int compare(AbstractWorkflowStep a, AbstractWorkflowStep b) {

        int result = a.getType().getPriority() - b.getType().getPriority();

        if (result != 0)
          return result;

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
  private static final DataFile newDataFile(final String path) {

    if (path == null)
      return null;

    return new DataFile(path);
  }

  /**
   * Check directories needed by the workflow.
   * @throws EoulsanException if an error about the directories is found
   */
  public void checkDirectories() throws EoulsanException {

    checkNotNull(this.logDir, "the log directory is null");
    checkNotNull(this.outputDir, "the output directory is null");
    checkNotNull(this.localWorkingDir, "the local working directory is null");

    try {
      for (DataFile dir : new DataFile[] { this.logDir, this.outputDir,
          this.localWorkingDir, this.hadoopWorkingDir }) {

        if (dir == null)
          continue;

        if (dir.exists() && !dir.getMetaData().isDir())
          throw new EoulsanException("the directory is not a directory: " + dir);

        if (!dir.exists())
          dir.mkdirs();

      }
    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
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

    if (tempDir == null)
      throw new EoulsanException("Temporary directory is null");

    if ("".equals(tempDir.getAbsolutePath()))
      throw new EoulsanException("Temporary directory is null");

    if (!tempDir.exists())
      throw new EoulsanException("Temporary directory does not exists: "
          + tempDir);

    if (!tempDir.isDirectory())
      throw new EoulsanException("Temporary directory is not a directory: "
          + tempDir);

    if (!tempDir.canRead())
      throw new EoulsanException("Temporary directory cannot be read: "
          + tempDir);

    if (!tempDir.canWrite())
      throw new EoulsanException("Temporary directory cannot be written: "
          + tempDir);

    if (!tempDir.canExecute())
      throw new EoulsanException("Temporary directory is not executable: "
          + tempDir);
  }

  /**
   * Log the state and the time of the analysis
   * @param success true if analysis was successful
   * @param stopwatch stopwatch of the workflow epoch
   */
  private void logEndAnalysis(final boolean success, final Stopwatch stopwatch) {

    stopwatch.stop();

    final String successString = success ? "Successful" : "Unsuccessful";

    // Log the end of the analysis
    getLogger().info(
        successString
            + " end of the analysis in "
            + StringUtils.toTimeHumanReadable(stopwatch
                .elapsedTime(MILLISECONDS)) + " s.");

    // Send a mail

    final String mailSubject =
        "["
            + Globals.APP_NAME + "] " + successString + " end of your job "
            + workflowContext.getJobId() + " on "
            + workflowContext.getJobHost();

    final String mailMessage =
        "THIS IS AN AUTOMATED MESSAGE.\n\n"
            + successString
            + " end of your job "
            + workflowContext.getJobId()
            + " on "
            + workflowContext.getJobHost()
            + ".\nJob finished at "
            + new Date(System.currentTimeMillis())
            + " in "
            + StringUtils.toTimeHumanReadable(stopwatch
                .elapsedTime(MILLISECONDS))
            + " s.\n\nOutput files and logs can be found in the following location:\n"
            + workflowContext.getOutputPathname() + "\n\nThe "
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
   */
  protected AbstractWorkflow(final ExecutorArguments executionArguments,
      final Design design) {

    Preconditions.checkNotNull(executionArguments, "Argument cannot be null");
    Preconditions.checkNotNull(design, "Design argument cannot be null");

    this.workflowContext = new WorkflowContext(executionArguments, this);
    this.design = design;

    this.logDir = newDataFile(executionArguments.getLogPathname());

    this.localWorkingDir =
        newDataFile(executionArguments.getLocalWorkingPathname());
    this.hadoopWorkingDir =
        newDataFile(executionArguments.getHadoopWorkingPathname());
    this.outputDir = newDataFile(executionArguments.getOutputPathname());
  }
}
