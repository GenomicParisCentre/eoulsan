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

package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import static fr.ens.transcriptome.eoulsan.core.CommonHadoop.createConfiguration;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;

import java.io.IOException;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastQFormatNew;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep;
import fr.ens.transcriptome.eoulsan.util.hadoop.MapReduceUtils;

/**
 * This class define an mapper step in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class ReadsMapperHadoopStep extends AbstractReadsMapperStep {

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort(READS_PORT_NAME, READS_FASTQ, true);
    builder.addPort(MAPPER_INDEX_PORT_NAME, getMapper().getArchiveFormat(),
        true);

    return builder.create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    super.configure(context, stepParameters);

    // Check if the mapper can be used with Hadoop
    if (!getMapper().isSplitsAllowed()) {
      throw new EoulsanException(
          "The selected mapper cannot be used in hadoop mode as "
              + "computation cannot be parallelized: "
              + getMapper().getMapperName());
    }
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Create configuration object
    final Configuration conf = createConfiguration();

    try {

      // Get input and output data
      final Data readsData = context.getInputData(READS_FASTQ);
      final Data mapperIndexData =
          context.getInputData(getMapper().getArchiveFormat());
      final Data outData = context.getOutputData(MAPPER_RESULTS_SAM, readsData);

      // Get FASTQ format
      final FastqFormat fastqFormat = readsData.getMetadata().getFastqFormat();

      // Create the job to run
      final Job job =
          createJobConf(conf, context, readsData, fastqFormat, mapperIndexData,
              outData);

      // Launch jobs
      MapReduceUtils.submitAndWaitForJob(job, readsData.getName(),
          CommonHadoop.CHECK_COMPLETION_TIME, status, COUNTER_GROUP);

      return status.createStepResult();

    } catch (IOException | InterruptedException | ClassNotFoundException e) {

      return status.createStepResult(e,
          "Error while running job: " + e.getMessage());
    }

  }

  /**
   * Create the JobConf object for a sample
   * @param readsData reads data
   * @param fastqFormat FASTQ format
   * @param mapperIndexData mapper index data
   * @param outData output data
   * @return a new JobConf object
   * @throws IOException
   */
  private Job createJobConf(final Configuration parentConf,
      final StepContext context, final Data readsData,
      final FastqFormat fastqFormat, final Data mapperIndexData,
      final Data outData) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    final Path inputPath = new Path(readsData.getDataFile().getSource());

    // Set mapper name
    jobConf.set(ReadsMapperMapper.MAPPER_NAME_KEY, getMapperName());

    // Set mapper version
    jobConf.set(ReadsMapperMapper.MAPPER_VERSION_KEY, getMapperVersion());

    // Set mapper flavor
    jobConf.set(ReadsMapperMapper.MAPPER_FLAVOR_KEY, getMapperFlavor());

    // Set pair end or single end mode
    if (readsData.getDataFileCount() == 2) {
      jobConf.set(ReadsMapperMapper.PAIR_END_KEY, Boolean.TRUE.toString());
    } else {
      jobConf.set(ReadsMapperMapper.PAIR_END_KEY, Boolean.FALSE.toString());
    }

    // Set the number of threads for the mapper
    if (getMapperLocalThreads() < 0) {
      jobConf.set(ReadsMapperMapper.MAPPER_THREADS_KEY, ""
          + getMapperHadoopThreads());
    }

    // Set mapper arguments
    if (getMapperArguments() != null) {
      jobConf.set(ReadsMapperMapper.MAPPER_ARGS_KEY, getMapperArguments());
    }

    // Set Mapper fastq format
    jobConf.set(ReadsMapperMapper.FASTQ_FORMAT_KEY, "" + fastqFormat);

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // timeout
    jobConf.set("mapreduce.task.timeout", "" + HADOOP_TIMEOUT);

    // No JVM task resuse
    jobConf.set("mapreduce.job.jvm.numtasks", "" + 1);

    // Set ZooKeeper client configuration
    setZooKeeperJobConfiguration(jobConf, context);

    // Create the job and its name
    final Job job =
        Job.getInstance(jobConf,
            "Map reads with "
                + getMapperName() + " (" + readsData.getName() + ", "
                + inputPath.getName() + ")");

    // Set genome index reference path in the distributed cache
    final Path genomeIndex =
        new Path(mapperIndexData.getDataFile().getSource());

    job.addCacheFile(genomeIndex.toUri());

    // Set the jar
    job.setJarByClass(ReadsMapperHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the input format
    job.setInputFormatClass(FastQFormatNew.class);

    // Set the Mapper class
    job.setMapperClass(ReadsMapperMapper.class);

    // Set the reducer class
    // job.setReducerClass(IdentityReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    job.setNumReduceTasks(0);

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(outData.getDataFile()
        .getSource()));

    return job;
  }

  /**
   * Configure ZooKeeper client.
   * @param jobConf job configuration
   * @param context Eoulsan context
   */
  static void setZooKeeperJobConfiguration(final Configuration jobConf,
      final StepContext context) {

    final Settings settings = context.getSettings();

    String connectString = settings.getZooKeeperConnectString();

    if (connectString == null) {

      connectString =
          jobConf.get("yarn.resourcemanager.hostname").split(":")[0]
              + ":" + settings.getZooKeeperDefaultPort();

    }

    jobConf.set(ReadsMapperMapper.ZOOKEEPER_CONNECT_STRING_KEY, connectString);
    jobConf.set(ReadsMapperMapper.ZOOKEEPER_SESSION_TIMEOUT_KEY,
        "" + settings.getZooKeeperSessionTimeout());
  }

}
