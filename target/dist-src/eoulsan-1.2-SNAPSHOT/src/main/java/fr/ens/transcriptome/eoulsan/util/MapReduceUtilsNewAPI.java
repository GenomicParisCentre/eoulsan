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

package fr.ens.transcriptome.eoulsan.util;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;

/**
 * This class contains utility method to easily manipulate the new Hadoop
 * MapReduce API.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class MapReduceUtilsNewAPI {

  /**
   * Move the content of the result of a map reduce into a new place
   * @param srcDirPath path of the output directory of map reduce
   * @param destPath new path of the result
   * @param overwrite true if must overwrite existing file
   * @param conf Configuration
   * @throws IOException if an error occurs while moving file
   */
  public static boolean moveMapredOutput(final Path srcDirPath,
      final Path destPath, final boolean overwrite, final Configuration conf)
      throws IOException {

    final List<Path> paths =
        PathUtils.listPathsByPrefix(srcDirPath, "part-", conf);

    if (paths == null)
      throw new NullPointerException("The list of output path is null");

    if (paths.size() == 0)
      return false;

    if (paths.size() == 1) {

      final Path p = paths.get(0);
      final FileSystem srcFs = p.getFileSystem(conf);
      final FileSystem destFs = destPath.getFileSystem(conf);

      if (destFs.exists(destPath))
        if (overwrite)
          destFs.delete(destPath, false);
        else
          return false;

      return srcFs.rename(p, destPath)
          && PathUtils.fullyDelete(srcDirPath, conf);
    }

    return PathUtils.concat(
        PathUtils.listPathsByPrefix(srcDirPath, "part-r-", conf), destPath,
        true, overwrite, conf) && PathUtils.fullyDelete(srcDirPath, conf);
  }

  /**
   * Submit a collection of jobs and wait the completion of all jobs.
   * @param jobs Collection of jobs to submit
   * @param waitTimeInMillis waiting time between 2 checks of the completion of
   *          jobs
   * @throws IOException if an IO error occurs while waiting for jobs
   * @throws InterruptedException if an error occurs while waiting for jobs
   * @throws ClassNotFoundException if a class needed for map reduce execution
   *           is not found
   */
  public static void submitandWaitForJobs(final Collection<Job> jobs,
      final int waitTimeInMillis) throws IOException, InterruptedException,
      ClassNotFoundException {

    if (jobs == null)
      throw new NullPointerException("The list of jobs is null");

    for (Job j : jobs)
      j.submit();

    waitForJobs(jobs, waitTimeInMillis);
  }

  /**
   * Wait the completion of a collection of jobs.
   * @param jobs Collection of jobs to submit
   * @param waitTimeInMillis waiting time between 2 checks of the completion of
   *          jobs
   * @throws IOException if an IO error occurs while waiting for jobs
   * @throws InterruptedException if an error occurs while waiting for jobs
   * @throws ClassNotFoundException if a class needed for map reduce execution
   *           is not found
   */
  public static void waitForJobs(final Collection<Job> jobs,
      final int waitTimeInMillis) throws InterruptedException, IOException {

    if (jobs == null)
      throw new NullPointerException("The list of jobs is null");

    final int totalJobs = jobs.size();
    int completedJobs = 0;

    while (completedJobs != totalJobs) {

      Thread.sleep(waitTimeInMillis);
      completedJobs = 0;
      for (Job j : jobs)
        if (j.isComplete())
          completedJobs++;
    }
  }

  /**
   * Parse a tabulated line to fill out text key and values.
   * @param line line to parse
   * @param outKey text out key
   * @param outValue text out value
   * @return true if the parsing is ok
   */
  public static final boolean parseKeyValue(final String line,
      final Text outKey, final Text outValue) {

    if (line == null) {

      return false;
    }

    final int posTab = line.indexOf('\t');

    if (posTab == -1) {

      outKey.set(line);
      outValue.clear();

      return false;
    }

    outKey.set(line.substring(0, posTab));
    outValue.set(line.substring(posTab + 1));

    return true;
  }

  //
  // Constructor
  //

  private MapReduceUtilsNewAPI() {
  }

}
