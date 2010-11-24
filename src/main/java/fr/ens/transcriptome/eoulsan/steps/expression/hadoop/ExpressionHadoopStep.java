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

package fr.ens.transcriptome.eoulsan.steps.expression.hadoop;

import static fr.ens.transcriptome.eoulsan.datatypes.DataFormats.ANNOTATION_SERIAL;
import static fr.ens.transcriptome.eoulsan.datatypes.DataFormats.EXPRESSION_RESULTS_TXT;
import static fr.ens.transcriptome.eoulsan.datatypes.DataFormats.SOAP_RESULTS_TXT;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextInputFormat;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.expression.ExpressionStep;
import fr.ens.transcriptome.eoulsan.steps.expression.FinalExpressionTranscriptsCreator;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.JobsResults;
import fr.ens.transcriptome.eoulsan.util.OldAPIJobsResults;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class is the main class for the expression program of the reads in
 * hadoop mode.
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public class ExpressionHadoopStep extends ExpressionStep {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private Configuration conf;

  /**
   * Create JobConf object.
   * @param basePath base path
   * @param sample sample of the job
   * @param genomicType genomic type
   * @throws IOException if an error occurs while creating job
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  private static final JobConf createJobConf(final ExecutorInfo info,
      final Sample sample, final String genomicType) throws IOException,
      BadBioEntryException {

    // Create JobConf
    final JobConf conf = new JobConf(ExpressionHadoopStep.class);

    final Path inputPath =
        CommonHadoop.selectDirectoryOrFile(new Path(info.getDataFile(
            SOAP_RESULTS_TXT, sample).getSourceWithoutExtension()),
            SOAP_RESULTS_TXT.getDefaultExtention());

    logger.fine("sample: " + sample);
    logger.fine("inputPath.getName(): " + inputPath.getName());
    logger.fine("sample.getMetadata(): " + sample.getMetadata());
    logger.fine("sample.getMetadata().getAnnotation(): "
        + sample.getMetadata().getAnnotation());

    // Set Job name
    conf.setJobName("Expression computation ("
        + sample.getName() + ", " + inputPath.getName() + ", "
        + sample.getMetadata().getAnnotation() + ", " + genomicType + ")");

    conf.set("mapred.child.java.opts", "-Xmx1024m");

    final Path exonsIndexPath =
        new Path(info.getDataFilename(ANNOTATION_SERIAL, sample));

    if (!PathUtils.isFile(exonsIndexPath, conf))
      createExonsIndex(new Path(info.getBasePathname(), sample.getMetadata()
          .getAnnotation()), genomicType, exonsIndexPath, conf);

    // Set the path to the exons index
    // conf.set(Globals.PARAMETER_PREFIX + ".expression.exonsindex.path",
    // exonsIndexPath.toString());
    DistributedCache.addCacheFile(exonsIndexPath.toUri(), conf);

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // Set input path
    FileInputFormat.setInputPaths(conf, inputPath);

    // Set the input format
    conf.setInputFormat(TextInputFormat.class);

    // Set the Mapper class
    conf.setMapperClass(ExpressionMapper.class);

    // Set the reducer class
    conf.setReducerClass(ExpressionReducer.class);

    // Set the output key class
    conf.setOutputKeyClass(Text.class);

    // Set the output value class
    conf.setOutputValueClass(Text.class);

    // Set the number of reducers
    conf.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(conf, new Path(info.getDataFilename(
        EXPRESSION_RESULTS_TXT, sample)));

    return conf;
  }

  /**
   * Create exon index.
   * @param gffPath gff path
   * @param expressionType expression type
   * @param exonsIndexPath output exon index path
   * @param conf configuration object
   * @throws IOException if an error occurs while creating the index
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  private static final Path createExonsIndex(final Path gffPath,
      final String expressionType, final Path exonsIndexPath,
      final Configuration conf) throws IOException, BadBioEntryException {

    final FileSystem fs = gffPath.getFileSystem(conf);
    final FSDataInputStream is = fs.open(gffPath);

    final TranscriptAndExonFinder ef =
        new TranscriptAndExonFinder(is, expressionType);
    final File exonIndexFile =
        FileUtils.createFileInTempDir(StringUtils.basename(gffPath.getName())
            + ANNOTATION_SERIAL.getDefaultExtention());
    ef.save(exonIndexFile);

    PathUtils.copyLocalFileToPath(exonIndexFile, exonsIndexPath, conf);
    if (!exonIndexFile.delete())
      logger.warning("Can not delete exon index file: "
          + exonIndexFile.getAbsolutePath());

    return exonsIndexPath;
  }

  private static final void createFinalExpressionTranscriptsFile(
      final ExecutorInfo info, final Map<Sample, RunningJob> jobconfs,
      final Configuration conf) throws IOException {

    int lastGenomeId = -1;
    FinalExpressionTranscriptsCreator fetc = null;

    for (Map.Entry<Sample, RunningJob> e : jobconfs.entrySet()) {

      final Sample sample = e.getKey();
      final RunningJob rj = e.getValue();

      final int genomeId = Common.getSampleId(sample.getMetadata().getGenome());
      final long readsUsed =
          rj.getCounters().getGroup(ExpressionMapper.COUNTER_GROUP).getCounter(
              "reads used");

      final FileSystem fs =
          new Path(info.getBasePathname()).getFileSystem(conf);

      // Load the annotation index
      if (genomeId != lastGenomeId) {

        final Path exonsIndexPath =
            new Path(info
                .getDataFilename(ANNOTATION_SERIAL, sample));

        fetc = new FinalExpressionTranscriptsCreator(fs.open(exonsIndexPath));

        lastGenomeId = genomeId;
      }

      final Path outputDirPath =
          new Path(info.getDataFile(EXPRESSION_RESULTS_TXT, sample)
              .getSourceWithoutExtension());

      final Path resultPath =
          new Path(info.getDataFilename(EXPRESSION_RESULTS_TXT,
              sample));

      fetc.initializeExpressionResults();

      for (FileStatus fstatus : fs.listStatus(outputDirPath))
        if (!fstatus.getPath().getName().startsWith("_"))
          fetc.loadPreResults(fs.open(fstatus.getPath()), readsUsed);

      fetc.saveFinalResults(fs.create(resultPath));
    }

  }

  //
  // Step methods
  //

  @Override
  public ExecutionMode getExecutionMode() {

    return Step.ExecutionMode.HADOOP;
  }

  @Override
  public String getLogName() {

    return "expression";
  }

  @Override
  public void configure(Set<Parameter> stepParameters,
      Set<Parameter> globalParameters) throws EoulsanException {

    super.configure(stepParameters, globalParameters);
    this.conf = CommonHadoop.createConfiguration(globalParameters);
  }

  @Override
  public StepResult execute(final Design design, final ExecutorInfo info) {

    // Create the list of jobs to run
    final Map<Sample, RunningJob> jobsRunning =
        new HashMap<Sample, RunningJob>();

    try {
      final long startTime = System.currentTimeMillis();

      JobClient jc = null;

      logger.info("Genomic type: " + getGenomicType());

      for (Sample s : design.getSamples()) {

        final JobConf jconf = createJobConf(info, s, getGenomicType());

        if (jc == null)
          jc = new JobClient(jconf);
        jobsRunning.put(s, jc.submitJob(jconf));
      }

      final JobsResults jobsResults =
          new OldAPIJobsResults(jobsRunning.values(),
              CommonHadoop.CHECK_COMPLETION_TIME,
              ExpressionMapper.COUNTER_GROUP);

      createFinalExpressionTranscriptsFile(info, jobsRunning, this.conf);

      return jobsResults.getStepResult(this, startTime);

    } catch (IOException e) {

      return new StepResult(this, e, "Error while running job: "
          + e.getMessage());
    } catch (InterruptedException e) {

      return new StepResult(this, e, "Error while running job: "
          + e.getMessage());
    } catch (BadBioEntryException e) {

      return new StepResult(this, e, "Invalid annotation entry: "
          + e.getEntry());
    }

  }

}
