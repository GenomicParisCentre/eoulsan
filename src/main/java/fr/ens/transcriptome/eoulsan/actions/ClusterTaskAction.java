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

package fr.ens.transcriptome.eoulsan.actions;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_RESULT_EXTENSION;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.filenameWithoutExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskResult;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskRunner;

/**
 * This class define a action to launch a task on a cluster.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ClusterTaskAction extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "clustertask";

  @Override
  public String getName() {

    return ACTION_NAME;
  }

  @Override
  public String getDescription() {

    return "Execute a cluster task";
  }

  @Override
  public boolean isHidden() {

    return true;
  }

  @Override
  public void action(final List<String> arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line =
          parser.parse(options, arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
    }

    if (arguments.size() != argsOptions + 1) {
      help(options);
    }

    final File contextFile = new File(arguments.get(0));

    // Execute task
    run(contextFile);
  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   * @return an Options object
   */
  private Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Help option
    options.addOption("h", "help", false, "display this help");

    return options;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + getName() + " [options] stepcontext.context", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Execute the task.
   * @param contextFile context file
   */
  private static final void run(final File contextFile) {

    checkNotNull(contextFile, "contextFile is null");

    try {

      // Test if param file exists
      if (!contextFile.exists())
        throw new FileNotFoundException(contextFile.toString());

      // Load context file
      final TaskContext context =
          TaskContext.deserialize(contextFile);

      // Create the context runner
      final TaskRunner runner =
          new TaskRunner(context);

      // Get the result
      final TaskResult result = runner.run();

      final String fileBaseName =
          filenameWithoutExtension(contextFile.getName());

      // Save task result
      saveTaskResult(result, new File(contextFile.getParent(), fileBaseName
          + TASK_RESULT_EXTENSION));

      // Create done file
      createDoneFile(new File(contextFile.getParent(), fileBaseName
          + Globals.TASK_DONE_EXTENSION));

    } catch (FileNotFoundException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    } catch (IOException e) {
      Common.errorExit(e, "IOException: " + e.getMessage());
    } catch (EoulsanException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    } catch (EoulsanRuntimeException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }

  /**
   * Save the result of a task.
   * @param taskResult task result
   * @param taskResultFile task result file
   * @throws EoulsanException if the result object is null
   * @throws IOException if an error occurs while creating the result file
   */
  private static final void saveTaskResult(
      final TaskResult taskResult, final File taskResultFile)
      throws EoulsanException, IOException {

    if (taskResult == null)
      throw new EoulsanException("Task result is null");

    final ObjectOutputStream out =
        new ObjectOutputStream(new FileOutputStream(taskResultFile));
    out.writeObject(taskResult);
    out.close();
  }

  /**
   * Create the done file
   * @param doneFile done file to create
   * @throws IOException if an error occurs while creating the done file
   */
  private static final void createDoneFile(final File doneFile)
      throws IOException {

    final OutputStream out = new FileOutputStream(doneFile);
    out.close();
  }

}
