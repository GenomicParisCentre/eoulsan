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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.Globals.TASK_LOG_EXTENSION;
import static fr.ens.biologie.genomique.eoulsan.annotations.EoulsanAnnotationUtils.isNoLog;
import static fr.ens.biologie.genomique.eoulsan.annotations.EoulsanAnnotationUtils.isReuseStepInstance;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.PARTIALLY_DONE;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.WORKING;
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.stackTraceToString;
import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepType;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * This class allow to run a task context.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TaskRunner {

  private final TaskContextImpl context;
  private final Module step;
  private final TaskStatusImpl status;
  private volatile StepResult result;
  private boolean isTokensSent;
  private boolean forceStepInstanceReuse;

  //
  // Getter
  //

  /**
   * Get the context result.
   * @return a TaskResult object
   */
  public TaskResult getResult() {

    checkState(this.result != null, "The context has not been run");

    return (TaskResult) this.result;
  }

  //
  // Setter
  //

  /**
   * Force the TaskRunner to reuse the original step instance when execute the
   * task.
   * @param reuse true if the step instance must be reuse when execute the task
   */
  public void setForceStepInstanceReuse(final boolean reuse) {

    this.forceStepInstanceReuse = reuse;
  }

  //
  // Execute methods
  //

  /**
   * Run the task context.
   * @return a task result object
   */
  public TaskResult run() {

    // Check if task has been already executed
    checkState(this.result == null, "task has been already executed");

    // Thread group name
    final String threadGroupName = "TaskRunner_"
        + this.context.getStep().getId() + "_#" + this.context.getId();

    // Define thread group
    final ThreadGroup threadGroup = new ThreadGroup(threadGroupName);

    // Create Log handler and register it
    final Logger logger = isNoLog(this.step)
        ? null : createStepLogger(this.context.getStep(), threadGroupName);

    // Register the logger
    if (logger != null) {
      EoulsanLogger.registerThreadGroupLogger(threadGroup, logger);
    }

    // We use here a thread to execute the step
    // This allow to save log of step in distinct files
    final Runnable r = new Runnable() {

      @Override
      public void run() {

        getLogger().info("Start of task #" + TaskRunner.this.context.getId());
        final long startTime = System.currentTimeMillis();

        final Module stepInstance;
        final StepType stepType =
            TaskRunner.this.context.getWorkflowStep().getType();
        final boolean reuseAnnot = isReuseStepInstance(TaskRunner.this.step);

        final String stepDescLog =
            String.format("step (id: %s, name: %s, class: %s) for task #%d",
                TaskRunner.this.context.getWorkflowStep().getId(),
                TaskRunner.this.step.getName(),
                TaskRunner.this.step.getClass().getName(),
                TaskRunner.this.context.getId());

        try {

          // If step is a standard step and reuse of step instance is not
          // required by step
          // Create a new instance of the step for the task
          if (stepType == StepType.STANDARD_STEP
              && !reuseAnnot && !TaskRunner.this.forceStepInstanceReuse) {

            // Create the new instance of the step
            getLogger().fine("Create new instance of " + stepDescLog);

            final String stepName = TaskRunner.this.step.getName();
            final Version stepVersion = TaskRunner.this.step.getVersion();

            stepInstance = ModuleRegistry.getInstance().loadStep(stepName,
                stepVersion.toString());

            // Log step parameters
            logStepParameters();

            // Configure the new step instance
            getLogger().fine("Configure step instance");
            stepInstance.configure(
                new StepConfigurationContextImpl(
                    TaskRunner.this.context.getStep()),
                TaskRunner.this.context.getCurrentStep().getParameters());

          } else {

            // Use the original step instance for the task
            getLogger().fine("Reuse original instance of " + stepDescLog);
            stepInstance = TaskRunner.this.step;

            // Log step parameters
            logStepParameters();
          }

          // Execute task
          getLogger().info("Execute task");
          TaskRunner.this.result = stepInstance.execute(TaskRunner.this.context,
              TaskRunner.this.status);

        } catch (Throwable t) {

          getLogger()
              .severe("Exception while executing task: " + t.getMessage());

          // Handle exception not catch by step code
          TaskRunner.this.result = TaskRunner.this.status.createStepResult(t);
        }

        final long duration = System.currentTimeMillis() - startTime;
        final StepResult result = TaskRunner.this.result;
        final boolean success = result.isSuccess();

        getLogger().info("End of task #" + TaskRunner.this.context.getId());
        getLogger().info("Duration: " + toTimeHumanReadable(duration));
        getLogger().info("Result: " + (success ? "Success" : "Fail"));

        if (!success) {

          final String errorMessage = result.getErrorMessage();
          final Throwable exception = result.getException();

          if (errorMessage != null) {
            getLogger().severe("Error message: " + errorMessage);
          }

          if (exception != null) {
            getLogger().severe("Exception: " + stackTraceToString(exception));
          }
        }
      }

      /**
       * Log step parameters.
       */
      private void logStepParameters() {

        final Set<Parameter> parameters =
            context.getCurrentStep().getParameters();

        if (parameters.isEmpty()) {
          getLogger().fine("Step has no parameter");
        } else {
          for (Parameter p : parameters) {
            getLogger()
                .fine("Step parameter: " + p.getName() + "=" + p.getValue());
          }
        }
      }

    };

    // Set the progress of the task to 0%
    this.status.setProgress(0);

    // Start the time watch
    this.status.durationStart();

    try {

      // Create thread, reuse the thread group name as thread name
      final Thread thread = new Thread(threadGroup, r, threadGroupName);

      // Start thread
      thread.start();

      // Wait the end of the thread
      thread.join();

    } catch (InterruptedException e) {
      getLogger().severe(e.getMessage() == null
          ? "Interruption of the thread " + threadGroupName : e.getMessage());

      // Inform the step token manager of the failed output data
      TokenManagerRegistry.getInstance().getTokenManager(this.context.getStep())
          .addFailedOutputData(this.context);

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

    if (this.result == null) {

      this.result =
          this.status.createStepResult(new EoulsanException("The step "
              + this.context.getStep().getId()
              + " has not generate a result object"));
    }

    // Send the tokens
    sendTokens();

    return (TaskResult) this.result;
  }

  /**
   * Send token.
   */
  private void sendTokens() {

    // Check if result has been created
    checkState(this.result != null, "Cannot send tokens of a null result task");

    // Check if tokens has been already sent
    checkState(!this.isTokensSent, "Cannot send tokens twice");

    this.isTokensSent = true;

    // Do not send data if the task has not been successful
    if (!this.result.isSuccess()) {
      return;
    }

    // For all output ports
    for (String portName : this.context.getCurrentStep().getOutputPorts()
        .getPortNames()) {

      // Get data required for token creation
      final StepOutputPort port =
          this.context.getStep().getWorkflowOutputPorts().getPort(portName);
      final Data data = this.context.getOutputData(port);

      // Send the token
      this.context.getStep().sendToken(new Token(port, data));
    }

    // Change the state of the step to PARTIALY_DONE if it the end first task of
    // the step
    final AbstractStep step = this.context.getWorkflowStep();
    if (step.getState() == WORKING) {
      step.setState(PARTIALLY_DONE);
    }

  }

  /**
   * Create default context name.
   * @return a string with the default context name
   */
  private String createDefaultContextName() {

    final List<String> namedData = new ArrayList<>();
    final List<String> fileNames = new ArrayList<>();
    final List<String> otherDataNames = new ArrayList<>();

    // Collect the names of the data and files names
    for (String inputPortName : this.context.getCurrentStep().getInputPorts()
        .getPortNames()) {

      final AbstractData data =
          ((UnmodifiableData) this.context.getInputData(inputPortName))
              .getData();

      if (!data.isList()) {

        if (!data.isDefaultName()) {
          namedData.add(data.getName());
        } else {

          for (DataFile file : DataUtils.getDataFiles(data)) {
            fileNames.add(file.getName());
          }
        }

      } else {
        otherDataNames.add(data.getName());
      }
    }

    // Choose the name of the context
    if (namedData.size() > 0) {
      return Joiner.on('-').join(namedData);
    } else if (fileNames.size() > 0) {
      return Joiner.on('-').join(fileNames);
    } else {
      return Joiner.on('-').join(otherDataNames);
    }
  }

  /**
   * Create the logger for a step.
   * @param step the step
   * @param threadGroupName the name of the thread group
   * @return a Logger instance
   */
  private Logger createStepLogger(final AbstractStep step,
      final String threadGroupName) {

    // Define the log file for the step
    final DataFile logDir =
        this.context.getStep().getAbstractWorkflow().getTaskDirectory();
    final DataFile logFile = new DataFile(logDir,
        this.context.getTaskFilePrefix() + TASK_LOG_EXTENSION);

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

    // Get the Log level on command line
    String logLevel = Main.getInstance().getLogLevelArgument();
    if (logLevel == null) {
      logLevel = Globals.LOG_LEVEL.getName();
    }

    // Set log level
    handler.setLevel(Level.parse(logLevel.toUpperCase()));

    return logger;
  }

  //
  // Static methods
  //

  /**
   * Create a step result for an exception.
   * @param taskContext task context
   * @param exception exception
   * @return a new TaskResult object
   */
  public static TaskResult createStepResult(final TaskContextImpl taskContext,
      final Throwable exception) {

    return createStepResult(taskContext, exception,
        exception != null ? exception.getMessage() : null);
  }

  /**
   * Create a step result for an exception.
   * @param taskContext task context
   * @param exception exception
   * @param errorMessage error message
   * @return a new TaskResult object
   */
  public static TaskResult createStepResult(final TaskContextImpl taskContext,
      final Throwable exception, final String errorMessage) {

    final TaskRunner runner = new TaskRunner(taskContext);

    // Start the time watch
    runner.status.durationStart();

    // Create the result object
    return (TaskResult) runner.status.createStepResult(exception, errorMessage);
  }

  /**
   * Send tokens for a serialized task result.
   * @param taskContext task context
   * @param taskResult task result
   */
  public static void sendTokens(final TaskContextImpl taskContext,
      final TaskResult taskResult) {

    new TaskRunner(taskContext, taskResult).sendTokens();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param taskContext task context to execute
   */
  public TaskRunner(final TaskContextImpl taskContext) {

    this(taskContext, (StepStatus) null);
  }

  /**
   * Constructor.
   * @param taskContext task context to execute
   * @param stepStatus step status
   */
  public TaskRunner(final TaskContextImpl taskContext,
      final StepStatus stepStatus) {

    checkNotNull(taskContext, "taskContext cannot be null");

    this.context = taskContext;
    this.step =
        StepInstances.getInstance().getStep(taskContext.getCurrentStep());

    this.status = new TaskStatusImpl(taskContext, stepStatus);

    // Set the task context name for the status
    this.context.setContextName(createDefaultContextName());
  }

  /**
   * Private constructor used to send token for serialized result.
   * @param taskContext task context
   * @param taskResult task result
   */
  private TaskRunner(final TaskContextImpl taskContext,
      final TaskResult taskResult) {

    checkNotNull(taskContext, "taskContext cannot be null");
    checkNotNull(taskResult, "taskResult cannot be null");

    // Check if the task result has been created for the task context
    checkArgument(taskContext.getId() == taskResult.getContext().getId(), "");

    this.context = taskContext;
    this.result = taskResult;

    // Step object and status are not necessary in this case
    this.step = null;
    this.status = null;
  }

}
