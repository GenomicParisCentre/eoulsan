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

package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastQFormatNew;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractFilterAndMapReadsStep;
import fr.ens.transcriptome.eoulsan.util.JobsResults;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;

@HadoopOnly
public class FilterAndMapReadsHadoopStep extends AbstractFilterAndMapReadsStep {

  @Override
  public StepResult execute(Design design, final Context context) {

    // Create configuration object
    final Configuration conf = new Configuration();// this.conf;

    try {

      // Create the list of jobs to run
      final List<Job> jobs = new ArrayList<Job>(design.getSampleCount());
      for (Sample s : design.getSamples())
        jobs.add(createJobConf(conf, context, s));

      final long startTime = System.currentTimeMillis();

      final JobsResults jobsResults =
          MapReduceUtils.submitAndWaitForJobs(jobs,
              CommonHadoop.CHECK_COMPLETION_TIME, getCounterGroup());

      return jobsResults.getStepResult(context, startTime);

    } catch (IOException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    } catch (InterruptedException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    } catch (ClassNotFoundException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    }

  }

  /**
   * Create a filter reads job
   * @param basePath bas epath
   * @param sample Sample to filter
   * @return a JobConf object
   * @throws IOException
   */
  private Job createJobConf(final Configuration parentConf,
      final Context context, final Sample sample) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // Set input path
    final Path inputPath =
        new Path(context.getBasePathname(), sample.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, getCounterGroup());

    // timeout
    jobConf.set("mapred.task.timeout", "" + HADOOP_TIMEOUT);

    //
    // Reads filters parameters
    //

    // Set length threshold
    if (getLengthThreshold() >= 0)
      jobConf.set(ReadsFilterMapper.LENGTH_THRESHOLD_KEY, ""
          + getLengthThreshold());

    // Set quality threshold
    if (getQualityThreshold() >= 0)
      jobConf.set(ReadsFilterMapper.QUALITY_THRESHOLD_KEY, ""
          + getQualityThreshold());

    // Set pair end mode
    jobConf.set(ReadsFilterMapper.PAIR_END_KEY, "" + isPairend());

    //
    // Reads mapping parameters
    //

    // Set genome index reference path
    final Path genomeIndex =
        new Path(context.getDataFile(getMapper().getArchiveFormat(), sample)
            .getSource());

    DistributedCache.addCacheFile(genomeIndex.toUri(), jobConf);

    // Set Mapper name
    jobConf.set(ReadsMapperMapper.MAPPER_NAME_KEY, getMapperName());

    // Set the number of threads for the mapper
    if (getMapperThreads() < 0) {
      jobConf
          .set(ReadsMapperMapper.MAPPER_THREADS_KEY, "" + getMapperThreads());
    }

    // Set mapper arguments
    if (getMapperArguments() != null) {
      jobConf.set(ReadsMapperMapper.MAPPER_ARGS_KEY, getMapperArguments());
    }

    //
    // Alignment filtering
    //

    // Set counter group
    jobConf.set(SAMFilterMapper.MAPPING_QUALITY_THRESOLD_KEY,
        Integer.toString(getMappingQualityThreshold()));

    // Set Genome description path
    jobConf.set(SAMFilterMapper.GENOME_DESC_PATH_KEY,
        context.getDataFile(DataFormats.GENOME_DESC_TXT, sample).getSource());

    // Set Job name
    // Create the job and its name
    final Job job =
        new Job(jobConf, "Filter and map reads ("
            + sample.getName() + ", " + sample.getSource() + ")");

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the input format
    if (sample.getSource().endsWith(READS_FASTQ.getDefaultExtention()))
      job.setInputFormatClass(FastQFormatNew.class);

    // Set the Mapper class
    job.setMapperClass(FilterAndMapMapper.class);

    // Set the reducer class
    job.setReducerClass(SAMFilterReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    job.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(
        job,
        new Path(context.getDataFile(DataFormats.FILTERED_MAPPER_RESULTS_SAM,
            sample).getSource()));

    return job;
  }
}
