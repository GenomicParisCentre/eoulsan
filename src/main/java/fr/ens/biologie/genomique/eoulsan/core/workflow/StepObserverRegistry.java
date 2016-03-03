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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class allow to relay workflow step events to other observers. This class
 * avoid storing observers WorkflowStep objects that are serialized.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class StepObserverRegistry {

  private static StepObserverRegistry singleton;

  private final Set<StepObserver> observers = new HashSet<>();

  /**
   * Add a listener.
   * @param observer listener to add
   */
  public void addObserver(final StepObserver observer) {

    if (observer == null || this.observers.contains(observer)) {
      return;
    }

    this.observers.add(observer);
  }

  /**
   * Remove a listener.
   * @param observer listener to remove
   */
  public void removeObserver(final StepObserver observer) {

    if (observer == null) {
      return;
    }

    this.observers.remove(observer);
  }

  public Set<StepObserver> getObservers() {

    return Collections.unmodifiableSet(this.observers);
  }

  //
  // Static methods
  //

  /**
   * Get the singleton instance of WorkflowStepObserverRegistry.
   * @return the singleton instance of WorkflowStepObserverRegistry
   */
  public static StepObserverRegistry getInstance() {

    if (singleton == null) {
      singleton = new StepObserverRegistry();
    }

    return singleton;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private StepObserverRegistry() {
  }

}
