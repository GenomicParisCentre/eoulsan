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

package fr.ens.transcriptome.eoulsan.ui;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.workflow.Workflow;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepEvent;

public class BasicUI implements WorkflowStepEvent {

  private final Workflow workflow;
  private final Map<WorkflowStep, Double> steps = Maps.newHashMap();

  private int lastMessageLength = 0;

  //
  // WorkflowStepEvent methods
  //

  @Override
  public void updateStepState(final WorkflowStep step) {

    if (step == null || step.getWorkflow() != this.workflow)
      return;

    // System.out.println("Step "
    // + step.getId() + " (" + step.getNumber() + ") status changed to "
    // + step.getState());

    if (step.getState() == StepState.WORKING)
      updateStepState(step, 0.0);

  }

  @Override
  public void updateStepState(final WorkflowStep step, double progress) {

    if (step == null
        || step.getWorkflow() != this.workflow
        || step.getState() != StepState.WORKING
        || !this.steps.containsKey(step))
      return;

    final double globalProgress = computeGlobalProgress(step, progress);

    if (globalProgress == 0 && this.lastMessageLength == 0)
      System.out.println(Globals.WELCOME_MSG);

    final String msg =
        String
            .format(
                "%.0f%% workflow done (currently process step %s #%d, %.0f%% done)",
                globalProgress * 100.0, step.getId(), step.getNumber(),
                progress * 100.0);

    // Clear previous message
    System.out.print(Strings.repeat("\r", this.lastMessageLength));
    System.out.print(Strings.repeat(" ", this.lastMessageLength));
    System.out.print(Strings.repeat("\r", this.lastMessageLength));
    this.lastMessageLength = msg.length();

    System.out.print(msg);

    // At the end of the workflow print \n
    if (globalProgress == 1.0)
      System.out.println();

    System.out.flush();

  }

  @Override
  public void updateStepState(final WorkflowStep step, final String note) {

    if (step == null || step.getWorkflow() != this.workflow)
      return;

    System.out.printf("Step "
        + step.getId() + " " + step.getState().toString() + " note: " + note);
  }

  //
  // Other methods
  //

  /**
   * Search steps to follow.
   */
  private void searchSteps() {

    for (WorkflowStep step : this.workflow.getSteps()) {

      if (step == null)
        continue;

      switch (step.getType()) {
      case CHECKER_STEP:
      case GENERATOR_STEP:
      case STANDARD_STEP:

        if (!step.isSkip())
          this.steps.put(step, 0.0);

        break;

      default:
        break;
      }
    }
  }

  /**
   * Compute global progress.
   * @param step step to update progress
   * @param progress progress value of the step
   * @return global progress as percent
   */
  private double computeGlobalProgress(final WorkflowStep step,
      final double progress) {

    if (!this.steps.containsKey(step))
      return -1;

    // Update progress
    this.steps.put(step, progress);

    double sum = 0;
    for (double p : this.steps.values())
      sum += p;

    return sum / this.steps.size();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param workflow workflow to show
   */
  public BasicUI(final Workflow workflow) {

    Preconditions.checkNotNull(workflow, "workflow is null");
    this.workflow = workflow;

    // Search step to follow
    searchSteps();
  }

}
