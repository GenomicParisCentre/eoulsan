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
package fr.ens.transcriptome.eoulsan.it;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createSymbolicLink;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

public class ITSuite {
  // Singleton

  private static final String RUNNING_LINKNAME = "running";
  private static final String SUCCEEDED_LINKNAME = "succeeded";
  private static final String FAILED_LINKNAME = "failed";
  private static final String LATEST_LINKNAME = "latest";

  private final int testsCount;
  private static final Stopwatch GLOBAL_TIMER = Stopwatch.createStarted();

  private static boolean debug_enable = false;
  private static int failCount = 0;
  private static int successCount = 0;
  private static int testRunningCount = 0;
  private static boolean isFirstTest = true;

  /**
   * Create useful symbolic test to the latest and running test in output test
   * directory.
   * @param failTestsCount in case of start tests, create running test link
   *          otherwise recreate latest and remove running test
   */
  public static void createSymbolicLinkToTest(final File directory) {

    // Path to the running link
    File runningDirLink = new File(directory.getParentFile(), RUNNING_LINKNAME);

    // Path to the latest link
    File latestDirLink = new File(directory.getParentFile(), LATEST_LINKNAME);

    // Path to the last succeeded test link
    File succeededDirLink = new File(directory.getParentFile(), SUCCEEDED_LINKNAME);

    // Path to the last failed test link
    File failedDirLink = new File(directory.getParentFile(), FAILED_LINKNAME);

    // Remove old running test link
    runningDirLink.delete();

    // Create running test link
    if (testRunningCount == 0) {
      createSymbolicLink(directory, runningDirLink);

    } else {
      // Replace latest by running test link

      // Remove old link
      latestDirLink.delete();

      // Recreate the latest link
      createSymbolicLink(directory, latestDirLink);

      if (failCount == 0) {
        // Update succeed link
        succeededDirLink.delete();
        createSymbolicLink(directory, succeededDirLink);
      } else {
        // Update failed link
        failedDirLink.delete();
        createSymbolicLink(directory, failedDirLink);
      }
    }
  }

  /**
   * @param directory
   */
  public void startTest(final File directory) {

    if (isFirstTest) {
      createSymbolicLinkToTest(directory);
      isFirstTest = false;
    }

    // Count test running
    testRunningCount++;

  }

  /**
   * @param directory
   */
  public void endTest(final File directory, final ITResult itResult) {

    // Update counter
    if (itResult.isSuccess())
      successCount++;
    else
      failCount++;

    // For latest
    if (testRunningCount == testsCount) {
      createSymbolicLinkToTest(directory);
      closeLogger();
    }

  }

  /**
   * Close log file, add a summary on tests execution and update symbolic link
   * in output test directory.
   */
  private void closeLogger() {
    // Add summary of tests execution
    getLogger().info(
        "Summary tests execution: "
            + successCount + " successed tests and " + failCount
            + " failed tests.");

    // Add suffix to log global filename
    getLogger().fine(
        "End of configuration of "
            + testsCount + " integration tests in "
            + toTimeHumanReadable(GLOBAL_TIMER.elapsed(TimeUnit.MILLISECONDS)));

    GLOBAL_TIMER.stop();
  }

  //
  // Getter and setter
  //

  public static boolean isDebugEnable() {
    return debug_enable;
  }

  public void setDebugEnable(boolean b) {
    debug_enable = b;
  }

  //
  // Constructor
  //
  public ITSuite(final int testsCount) {
    this.testsCount = testsCount;
  }

}
