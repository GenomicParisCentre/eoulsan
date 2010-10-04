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

package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.io.LogReader;
import fr.ens.transcriptome.eoulsan.steps.mapping.FilterSamplesStep;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class is the main class for filtering samples after mapping in local
 * mode.
 * @author Laurent Jourdren
 */
public class FilterSamplesLocalStep extends FilterSamplesStep {

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
  public StepResult execute(final Design design, final ExecutorInfo info) {

    final long startTime = System.currentTimeMillis();

    final double threshold = getThreshold() / 100.0;
    final File baseDir = new File(info.getBasePathname());

    try {
      // Read filterreads.log
      LogReader logReader = new LogReader(new File(baseDir, "filterreads.log"));
      final Reporter filterReadsReporter = logReader.read();

      // Read soapmapreads.log
      logReader = new LogReader(new File(baseDir, "soapmapreads.log"));
      final Reporter soapMapReadsReporter = logReader.read();

      // Get the input reads for each sample
      final Map<String, Long> sampleInputMapReads =
          parseReporter(filterReadsReporter,
              Common.READS_AFTER_FILTERING_COUNTER);

      // Get the number of match with onlt one locus for each sample
      final Map<String, Long> soapAlignementWithOneLocus =
          parseReporter(soapMapReadsReporter,
              Common.SOAP_ALIGNEMENT_WITH_ONLY_ONE_HIT_COUNTER);

      int removedSampleCount = 0;
      final StringBuilder sb = new StringBuilder();

      // Compute ration and filter samples
      for (String sample : sampleInputMapReads.keySet()) {

        if (!soapAlignementWithOneLocus.containsKey(sample))
          continue;

        final long inputReads = sampleInputMapReads.get(sample);
        final long oneLocus = soapAlignementWithOneLocus.get(sample);

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

  //
  // Other method
  // 

  private static final Map<String, Long> parseReporter(final Reporter reporter,
      final String counter) {

    final Map<String, Long> result = new HashMap<String, Long>();

    final Set<String> groups = reporter.getCounterGroups();

    for (String group : groups) {

      final int pos1 = group.indexOf('(');
      final int pos2 = group.indexOf(',');

      if (pos1 == -1 || pos2 == -1)
        continue;

      final String sample = group.substring(pos1 + 1, pos2).trim();

      result.put(sample, reporter.getCounterValue(group, counter));
    }

    return result;
  }

}
