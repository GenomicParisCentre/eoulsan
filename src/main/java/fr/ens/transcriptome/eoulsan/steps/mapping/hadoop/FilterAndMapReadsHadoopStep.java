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

import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // Don't reuse JVM
    jobConf.set("mapred.job.reuse.jvm.num.tasks", "" + 1);

    //
    // Reads filters parameters
    //

    // Set reads filter PHRED offset
    jobConf.set(ReadsFilterMapper.PHRED_OFFSET_KEY, ""
        + sample.getMetadata().getPhredOffset());

    // Set read filter parameters
    for (Map.Entry<String, String> e : getReadFilterParameters().entrySet()) {

      jobConf.set(
          ReadsFilterMapper.READ_FILTER_PARAMETER_KEY_PREFIX + e.getKey(),
          e.getValue());
    }

    // Set pair end mode
    jobConf.set(ReadsMapperMapper.PAIR_END_KEY, "" + isPairend());

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

    // Set Mapper PHRED offset
    jobConf.set(ReadsMapperMapper.PHRED_OFFSET_KEY, ""
        + sample.getMetadata().getPhredOffset());
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
