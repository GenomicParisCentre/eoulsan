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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.CREATED;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.READY;
import static fr.ens.biologie.genomique.eoulsan.core.Step.StepState.WAITING;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepState;

/**
 * This class define an observer for step states.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class StepStateObserver implements Serializable {

  private static final long serialVersionUID = -5734184849291521186L;

  private final AbstractStep step;
  private StepState stepState = CREATED;

  private final Set<AbstractStep> requiredSteps = new HashSet<>();
  private final Set<AbstractStep> stepsToInform = new HashSet<>();

  /**
   * Add a dependency.
   * @param step the dependency
   */
  public void addDependency(final AbstractStep step) {

    this.requiredSteps.add(step);
    step.getStepStateObserver().stepsToInform.add(this.step);
  }

  /**
   * Get the required steps.
   * @return a set with the required steps
   */
  public Set<AbstractStep> getRequiredSteps() {

    return Collections.unmodifiableSet(this.requiredSteps);
  }

  /**
   * Get the state of the step.
   * @return the state of the step
   */
  public StepState getState() {

    return this.stepState;
  }

  /**
   * Set the state of the step.
   * @param state the new state of the step
   */
  public void setState(final StepState state) {

    // Do nothing if the state has not changed or if the current state is a
    // final state
    if (state == null
        || state == CREATED || this.stepState == state
        || this.stepState.isFinalState()) {
      return;
    }

    // Do not change the state to READY if the step is already working
    if (state == READY && this.stepState.isWorkingState()) {
      return;
    }

    // Save current state
    final StepState previousState = this.stepState;

    // If is the root step, there is nothing to wait
    synchronized (this) {

      if (this.step.getType() == Step.StepType.ROOT_STEP && state == WAITING) {
        this.stepState = READY;
      } else {

        // Set the new state
        this.stepState = state;
      }
    }

    // Log the new state of the step
    getLogger().fine("Step #"
        + this.step.getNumber() + " " + this.step.getId() + " is now in state "
        + this.stepState + " (previous state was " + previousState + ")");

    // Log dependencies when step is in WAITING state
    if (this.stepState == WAITING) {
      logDependencies();
    }

    // Inform step that depend of this step
    if (this.stepState.isDoneState()) {
      for (AbstractStep step : this.stepsToInform) {
        step.getStepStateObserver().updateStatus();
      }
    }

    // Start Token manager thread for the step if state is READY
    if (this.stepState == READY) {
      TokenManagerRegistry.getInstance().getTokenManager(this.step).start();
    }

    // Inform workflow object
    this.step.getAbstractWorkflow().updateStepState(this.step);

    // Inform listeners
    for (StepObserver o : StepObserverRegistry.getInstance().getObservers()) {
      o.notifyStepState(this.step);
    }
  }

  /**
   * Update the status of the step to READY if all the dependency of this step
   * are in DONE state.
   */
  private void updateStatus() {

    // Do nothing if the step is already in READY state
    if (getState() == READY) {
      return;
    }

    for (AbstractStep step : this.requiredSteps) {
      if (!(step.getState().isDoneState())) {
        return;
      }
    }

    // Set the step to the READY state
    setState(READY);
  }

  /**
   * Log dependencies of step.
   */
  void logDependencies() {

    String msg = "Step #"
        + this.step.getNumber() + " " + this.step.getId()
        + " has the following dependencies: ";

    List<String> list = new ArrayList<>();

    for (AbstractStep step : this.requiredSteps) {
      list.add("step #" + step.getNumber() + " " + step.getId());
    }

    if (list.isEmpty()) {
      msg += "no dependencies";
    } else {
      msg += Joiner.on(", ").join(list);
    }
    getLogger().fine(msg);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step the step related to the instance
   */
  public StepStateObserver(final AbstractStep step) {

    checkNotNull(step, "step cannot be null");

    this.step = step;

    getLogger().fine("Step #"
        + this.step.getNumber() + " " + this.step.getId() + " is now in state "
        + this.stepState);
  }
}
