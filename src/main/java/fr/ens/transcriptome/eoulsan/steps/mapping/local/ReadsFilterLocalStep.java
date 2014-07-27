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

package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_RAW_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.READS_REJECTED_BY_FILTERS_COUNTER;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.bio.io.FastqWriter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.ReadFilter;
import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsFilterStep;
import fr.ens.transcriptome.eoulsan.util.LocalReporter;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a step for reads filtering.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
@LocalOnly
public class ReadsFilterLocalStep extends AbstractReadsFilterStep {

  /** Logger. */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Create the reporter
    final Reporter reporter = new LocalReporter();

    try {

      // Get input and output data
      final Data inData = context.getInputData(READS_FASTQ);
      final Data outData = context.getOutputData(READS_FASTQ, inData);

      // Get FASTQ format
      // TODO Use metadata
      final FastqFormat fastqFormat =
          FastqFormat.valueOf(inData.getMetadata().get("fastq.format"));

      // get input file count for the sample
      final int inFileCount = inData.getDataFileCount();

      if (inFileCount < 1)
        throw new IOException("No reads file found.");

      if (inFileCount > 2)
        throw new IOException(
            "Cannot handle more than 2 reads files at the same time.");

      // Get the read filter
      final MultiReadFilter filter = getReadFilter(reporter, COUNTER_GROUP);
      LOGGER.info("Reads filters to apply: "
          + Joiner.on(", ").join(filter.getFilterNames()));

      // Run the filter in single or pair-end mode
      if (inFileCount == 1) {
        singleEnd(inData, outData, fastqFormat, reporter, status, filter);
      } else {
        pairedEnd(inData, outData, fastqFormat, reporter, status, filter);
      }

    } catch (FileNotFoundException e) {
      return status.createStepResult(e, "File not found: " + e.getMessage());
    } catch (IOException e) {
      return status.createStepResult(e,
          "Error while filtering: " + e.getMessage());
    } catch (EoulsanException e) {
      return status.createStepResult(e,
          "Error while initializing filter: " + e.getMessage());
    }

    return status.createStepResult();
  }

  /**
   * Filter a sample data in single end mode.
   * @param inData input Data
   * @param outData output Data
   * @param fastqFormat FASTQ format
   * @param reporter reporter to use
   * @param status step status
   * @param filter reads filter to use
   * @throws IOException if an error occurs while filtering reads
   */
  private static void singleEnd(final Data inData, final Data outData,
      final FastqFormat fastqFormat, final Reporter reporter,
      final StepStatus status, final ReadFilter filter) throws IOException {

    // Get the source
    final DataFile inFile = inData.getDataFile(0);

    // Get the dest
    final DataFile outFile = outData.getDataFile(0);

    // Filter reads
    filterFile(inFile, outFile, reporter, filter, fastqFormat);

    // Add counters for this sample to log file
    status.setCounters(reporter, COUNTER_GROUP,
        "Filter reads (" + inData.getName() + ", " + inFile.getName() + ")");
  }

  /**
   * Filter a sample data in paired-end mode.
   * @param inData input Data
   * @param outData output Data
   * @param fastqFormat FASTQ formatt
   * @param reporter reporter to use
   * @param filter reads filter to use
   * @throws IOException if an error occurs while filtering reads
   */
  private static void pairedEnd(final Data inData, final Data outData,
      final FastqFormat fastqFormat, final Reporter reporter,
      final StepStatus status, final ReadFilter filter) throws IOException {

    // Filter reads
    filterFile(inData.getDataFile(0), inData.getDataFile(1),
        outData.getDataFile(0), outData.getDataFile(1), reporter, filter,
        fastqFormat);

    // Add counters for this sample to log file
    status.setCounters( reporter, COUNTER_GROUP,
        "Filter reads ("
            + inData.getName() + ", " + inData.getDataFile(0).getName() + ", "
            + inData.getDataFile(1).getName() + ")");
  }

  /**
   * Filter a file in single end mode.
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @param filter reads filter to use
   * @param fastqFormat FastqFormat
   * @throws IOException if an error occurs while filtering data
   */
  private static void filterFile(final DataFile inFile, final DataFile outFile,
      final Reporter reporter, final ReadFilter filter,
      final FastqFormat fastqFormat) throws IOException {

    LOGGER.info("Filter file: " + inFile);
    LOGGER.info("FastqFormat: " + fastqFormat);

    final FastqReader reader = new FastqReader(inFile.open());
    final FastqWriter writer = new FastqWriter(outFile.create());

    try {
      for (final ReadSequence read : reader) {

        // Set Fastq format
        read.setFastqFormat(fastqFormat);

        reporter.incrCounter(COUNTER_GROUP,
            INPUT_RAW_READS_COUNTER.counterName(), 1);

        if (filter.accept(read)) {

          writer.write(read);
          reporter.incrCounter(COUNTER_GROUP,
              OUTPUT_FILTERED_READS_COUNTER.counterName(), 1);
        } else {
          reporter.incrCounter(COUNTER_GROUP,
              READS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
        }

      }
      reader.throwException();

    } catch (BadBioEntryException e) {

      throw new IOException("Invalid Fastq format: " + e.getEntry());

    } finally {

      reader.close();
      writer.close();
    }

  }

  /**
   * Filter a file in pair-end mode.
   * @param inFile1 first input file
   * @param inFile2 second input file
   * @param outFile1 first output file
   * @param outFile2 second output file
   * @param reporter reporter to use
   * @param filter reads filter to use
   * @param fastqFormat FastqFormat
   * @throws IOException if an error occurs while filtering data
   */
  private static void filterFile(final DataFile inFile1,
      final DataFile inFile2, final DataFile outFile1, final DataFile outFile2,
      final Reporter reporter, final ReadFilter filter,
      final FastqFormat fastqFormat) throws IOException {

    LOGGER.info("Filter files: "
        + inFile1 + ", " + inFile2 + ", Fastq format: " + fastqFormat);

    final FastqReader reader1 = new FastqReader(inFile1.open());
    final FastqReader reader2 = new FastqReader(inFile2.open());
    final FastqWriter writer1 = new FastqWriter(outFile1.create());
    final FastqWriter writer2 = new FastqWriter(outFile2.create());

    try {
      for (final ReadSequence read1 : reader1) {

        // Test if the second read exists
        if (!reader2.hasNext()) {
          reader2.throwException();
          throw new IOException("Excepted end of the second reads file.");
        }

        // Get the second read
        final ReadSequence read2 = reader2.next();

        // Set fastq format
        read1.setFastqFormat(fastqFormat);
        read2.setFastqFormat(fastqFormat);

        reporter.incrCounter(COUNTER_GROUP,
            INPUT_RAW_READS_COUNTER.counterName(), 1);

        if (filter.accept(read1, read2)) {
          writer1.write(read1);
          writer2.write(read2);
          reporter.incrCounter(COUNTER_GROUP,
              OUTPUT_FILTERED_READS_COUNTER.counterName(), 1);
        } else {
          reporter.incrCounter(COUNTER_GROUP,
              READS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
        }

      }
      reader1.throwException();
      reader2.throwException();

      if (reader2.hasNext())
        throw new IOException("Excepted end of the first reads file.");

    } catch (BadBioEntryException e) {

      throw new IOException("Invalid Fastq format: " + e.getEntry());

    } finally {

      reader1.close();
      reader2.close();
      writer1.close();
      writer2.close();
    }

  }

}
