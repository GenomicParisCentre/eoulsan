/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.programs.mapping.hadoop;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.io.LogReader;
import fr.ens.transcriptome.eoulsan.programs.mapping.FilterSamplesStep;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class is the main class for filtering samples after mapping in hadoop
 * mode.
 * @author Laurent Jourdren
 */
public class FilterSamplesHadoopStep extends FilterSamplesStep {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  //
  // Step methods
  //

  @Override
  public String getLogName() {

    return "filtersamples";
  }

  @Override
  public StepResult execute(Design design, final ExecutorInfo info) {

    final long startTime = System.currentTimeMillis();

    final double threshold = getThreshold() / 100.0;
    final Path basePath = new Path(info.getBasePathname());

    try {

      final Configuration conf = new Configuration();
      final FileSystem fs = basePath.getFileSystem(conf);
      // Read soapmapreads.log
      Path logPath = new Path(basePath, "soapmapreads.log");
      if (!PathUtils.exists(logPath, conf))
        logPath = new Path(basePath, "filtersoapmapreads.log");

      logger.info("Read log: " + logPath);
      LogReader logReader = new LogReader(fs.open(logPath));
      final Reporter reporter = logReader.read();

      int removedSampleCount = 0;
      final StringBuilder sb = new StringBuilder();

      // Compute ration and filter samples
      for (String group : reporter.getCounterGroups()) {

        final int pos1 = group.indexOf('(');
        final int pos2 = group.indexOf(',');

        if (pos1 == -1 || pos2 == -1)
          continue;

        final String sample = group.substring(pos1 + 1, pos2).trim();

        final long inputReads =
            reporter.getCounterValue(group, Common.SOAP_INPUT_READS_COUNTER);
        final long oneLocus =
            reporter.getCounterValue(group,
                Common.SOAP_ALIGNEMENT_WITH_ONLY_ONE_HIT_COUNTER);

        final double ratio = (double) oneLocus / (double) inputReads;

        logger.info("Check Reads with only one match: "
            + sample + " " + oneLocus + "/" + inputReads + "=" + ratio
            + " threshold=" + threshold);

        if (ratio < threshold) {
          design.removeSample(sample);
          logger.info("Remove sample: " + sample);
          sb.append("Remove sample: " + sample + "\n");
          removedSampleCount++;
        }
      }

      return new StepResult(this, startTime, "Sample(s) removed: "
          + removedSampleCount + "\n" + sb.toString());

    } catch (IOException e) {

      return new StepResult(this, e, "Error while filtering samples: "
          + e.getMessage());

    }

  }
}
