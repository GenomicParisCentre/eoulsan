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

package fr.ens.transcriptome.eoulsan.core.executors;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;

/**
 * This class define a muti thread executor.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class MultiThreadTaskExecutor extends AbstractTaskExecutor {

  private static final int WAIT_SHUTDOWN_MINUTES = 60;

  private final PausableThreadPoolExecutor executor;
  private final List<Future<ContextThread>> threads = Lists.newArrayList();

  /**
   * Wrapper class around a call to AbstractTaskExecutor.execute() method.
   * @author Laurent Jourdren
   */
  private final class ContextThread implements Runnable {

    private final TaskContext context;

    @Override
    public void run() {

      execute(this.context);
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param context context to execute
     */
    ContextThread(final TaskContext context) {
      this.context = context;
    }
  }

  @Override
  public void submit(final WorkflowStep step, final TaskContext context) {

    // Call to the super method
    super.submit(step, context);

    // Create context thread
    final ContextThread st = new ContextThread(context);

    // Submit the context thread the thread executor
    synchronized (this.threads) {
      this.threads.add(executor.submit(st, st));
    }
  }

  @Override
  public void start() {
    super.start();
  }

  @Override
  public void stop() {

    try {

      // Shutdown the executor
      this.executor.shutdownNow();

      // Wait the termination of current running task
      executor.awaitTermination(WAIT_SHUTDOWN_MINUTES, TimeUnit.MINUTES);

    } catch (InterruptedException e) {
      getLogger().severe(e.getMessage());
    }
  }

  @Override
  public void pause() {

    this.executor.pause();
  }

  @Override
  public void resume() {

    this.executor.resume();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param threadNumber number of thread to use by the task executor
   */
  public MultiThreadTaskExecutor(final int threadNumber) {

    checkArgument(threadNumber > 0, "threadNumber must be > 0");

    // Create executor service
    this.executor = new PausableThreadPoolExecutor(threadNumber);
  }

}
