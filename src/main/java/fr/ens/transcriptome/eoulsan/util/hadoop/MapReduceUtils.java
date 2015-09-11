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

package fr.ens.transcriptome.eoulsan.util.hadoop;

import java.io.IOException;

import org.apache.hadoop.mapreduce.Job;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.StepStatus;

/**
 * This class contains utility method to easily manipulate the new Hadoop
 * MapReduce API.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class MapReduceUtils {

  /**
   * Wait the completion of a job.
   * @param job the job to submit
   * @param jobDescription the description of the job
   * @param waitTimeInMillis waiting time between 2 checks of the completion of
   *          jobs
   * @param status step status
   * @param counterGroup group of the counter to log
   * @throws EoulsanException if the job fail or if an exception occurs while
   *           submitting or waiting the end of the job
   */
  public static void submitAndWaitForJob(final Job job,
      final String jobDescription, final int waitTimeInMillis,
      final StepStatus status, final String counterGroup)
          throws EoulsanException {

    if (job == null) {
      throw new NullPointerException("The job is null");
    }

    if (jobDescription == null) {
      throw new NullPointerException("The jobDescription is null");
    }

    try {

      // Set the description of the context
      status.setDescription(job.getJobName());

      // Submit the job
      job.submit();

      // Add the Hadoop job to the list of job to kill if workflow fails
      HadoopJobEmergencyStopTask.addHadoopJobEmergencyStopTask(job);

      // Job the completion of the job (non verbose mode)
      job.waitForCompletion(false);

      // Remove the Hadoop job to the list of job to kill if workflow fails
      HadoopJobEmergencyStopTask.removeHadoopJobEmergencyStopTask(job);

      // Check if the job has been successfully executed
      if (!job.isSuccessful()) {

        status.setProgressMessage("FAILED");

        throw new EoulsanException(
            "Fail of the Hadoop job: " + job.getJobFile());
      }

      // Set the counters
      status.setCounters(new HadoopReporter(job.getCounters()), counterGroup);

    } catch (ClassNotFoundException | InterruptedException | IOException e) {
      throw new EoulsanException(e);
    }
  }

  //
  // Constructor
  //

  private MapReduceUtils() {
  }

}
