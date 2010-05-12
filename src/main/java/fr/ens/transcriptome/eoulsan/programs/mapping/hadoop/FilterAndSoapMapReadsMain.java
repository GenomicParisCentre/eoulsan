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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.IdentityReducer;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.hadoop.CommonHadoop;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.io.hadoop.FastqInputFormat;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class is the main class for the filter and mapping program of the reads
 * in hadoop mode.
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public class FilterAndSoapMapReadsMain {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  // Configure URL handler for hdfs protocol
  static {
    URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());
  }

  private static final String UNMAP_CHUNK_PREFIX = "soap-unmap-";

  /**
   * Create the JobConf object for a sample
   * @param basePath base path of data
   * @param sample sample to process
   * @return a new JobConf object
   */
  private static JobConf createJobConf(final Path basePath,
      final Sample sample, final int lengthThreshold,
      final double qualityThreshold) {

    final JobConf conf = new JobConf(FilterReadsMain.class);

    final int sampleId = CommonHadoop.getSampleId(sample);
    final int genomeId =
        CommonHadoop.getSampleId(sample.getMetadata().getGenome());

    final Path inputPath = new Path(basePath, sample.getSource());

    // Set Job name
    conf.setJobName("Filter and map reads with SOAP ("
        + sample.getName() + ", " + inputPath.getName() + ")");

    if (lengthThreshold >= 0)
      conf.set(Globals.PARAMETER_PREFIX + ".filter.reads.length.threshold", ""
          + lengthThreshold);

    if (qualityThreshold >= 0)
      conf.set(Globals.PARAMETER_PREFIX + ".filter.reads.quality.threshold", ""
          + qualityThreshold);

    // Set genome reference path
    conf
        .set(Globals.PARAMETER_PREFIX + ".soap.indexzipfilepath", new Path(
            basePath, CommonHadoop.GENOME_SOAP_INDEX_FILE_PREFIX
                + genomeId + CommonHadoop.GENOME_SOAP_INDEX_FILE_SUFFIX)
            .toString());

    // Set unmap chuck dir path
    conf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix.dir",
        new Path(basePath, CommonHadoop.SOAP_UNMAP_FILE_PREFIX + sampleId)
            .toString());

    // Set unmap chuck prefix
    conf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix",
        UNMAP_CHUNK_PREFIX);

    // Set unmap output file path
    conf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.path", PathUtils
        .newPathWithOtherExtension(new Path(basePath, sample.getSource()),
            CommonHadoop.UNMAP_EXTENSION).toString());

    // Set the number of threads for soap
    conf.set(Globals.PARAMETER_PREFIX + ".soap.nb.threads", ""
        + Runtime.getRuntime().availableProcessors());

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // timeout
    conf.set("mapred.task.timeout", "" + 20 * 60 * 1000);

    // Set the jar
    conf.setJarByClass(FilterAndSoapMapReadsMain.class);

    // Set input path

    FileInputFormat.setInputPaths(conf, inputPath);

    // Set the input format
    conf.setInputFormat(FastqInputFormat.class);

    // Set the Mapper class
    conf.setMapperClass(FilterAndSoapMapReadsMapper.class);

    // Set the reducer class
    conf.setReducerClass(IdentityReducer.class);

    // Set the output key class
    conf.setOutputKeyClass(Text.class);

    // Set the output value class
    conf.setOutputValueClass(Text.class);

    // Set the number of reducers
    conf.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(conf, new Path(basePath,
        CommonHadoop.SOAP_ALIGNMENT_FILE_PREFIX + sampleId));

    return conf;
  }

  //
  // Main method
  //

  /**
   * Main method
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    logger.info("Start SOAP map reads.");

    if (args == null)
      throw new NullPointerException("The arguments of import data is null");

    if (args.length < 1)
      throw new IllegalArgumentException(
          "Filter reads need one or two arguments");

    // Set the design path
    final String designPathname = args[0];

    // Set the thresholds
    int lengthThreshold = -1;
    double qualityThreshold = -1;

    if (args.length > 1)
      try {
        lengthThreshold = Integer.parseInt(args[1]);
      } catch (NumberFormatException e) {
        CommonHadoop.error("Invalid length threshold: " + args[1]);
      }

    if (args.length > 2)
      try {
        qualityThreshold = Double.parseDouble(args[2]);
      } catch (NumberFormatException e) {
        CommonHadoop.error("Invalid quality threshold: " + args[2]);
      }

    final Path designPath = new Path(designPathname);
    final Path basePath = designPath.getParent();
    Design design = null;

    // Read design file
    try {

      final DesignReader dr =
          new SimpleDesignReader(designPath.toUri().toURL().openStream());

      design = dr.read();

    } catch (IOException e) {
      CommonHadoop.error("Error while reading design file: ", e);
    } catch (EoulsanIOException e) {
      CommonHadoop.error("Error while reading design file: ", e);
    }

    // Create the list of jobs to run
    final List<JobConf> jobconfs =
        new ArrayList<JobConf>(design.getSampleCount());
    for (Sample s : design.getSamples())
      jobconfs
          .add(createJobConf(basePath, s, lengthThreshold, qualityThreshold));

    try {
      final long startTime = System.currentTimeMillis();
      CommonHadoop.writeLog(new Path(basePath, "filtersoapmapreads.log"),
          startTime, MapReduceUtils.submitandWaitForJobs(jobconfs,
              CommonHadoop.CHECK_COMPLETION_TIME,
              FilterAndSoapMapReadsMapper.COUNTER_GROUP));

    } catch (IOException e) {
      CommonHadoop.error("Error while running job: ", e);
    } catch (InterruptedException e) {
      CommonHadoop.error("Error while running job: ", e);
    } catch (ClassNotFoundException e) {
      CommonHadoop.error("Error while running job: ", e);
    }

  }

}
