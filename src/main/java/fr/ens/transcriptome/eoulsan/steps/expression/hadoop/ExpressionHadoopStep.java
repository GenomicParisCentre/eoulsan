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

package fr.ens.transcriptome.eoulsan.steps.expression.hadoop;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATION_INDEX_SERIAL;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_MAPPER_RESULTS_SAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_MAPPER_RESULTS_TSAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GFFEntry;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.OverlapMode;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.StrandUsage;
import fr.ens.transcriptome.eoulsan.bio.io.GFFReader;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep;
import fr.ens.transcriptome.eoulsan.steps.expression.FinalExpressionFeaturesCreator;
import fr.ens.transcriptome.eoulsan.steps.expression.FinalExpressionTranscriptsCreator;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder;
import fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.ReadsMapperHadoopStep;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;
import fr.ens.transcriptome.eoulsan.util.hadoop.HadoopJobsResults;
import fr.ens.transcriptome.eoulsan.util.hadoop.MapReduceUtils;
import fr.ens.transcriptome.eoulsan.util.hadoop.NewAPIJobsResults;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * This class is the main class for the expression program of the reads in
 * hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class ExpressionHadoopStep extends AbstractExpressionStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private Configuration conf;

  // @Override
  // public DataFormat[] getInputFormats() {
  // return new DataFormat[] {FILTERED_MAPPER_RESULTS_SAM,
  // TAB_FILTERED_MAPPER_RESULTS_SAM};
  // }
  //
  // @Override
  // public DataFormat[] getOutputFormats() {
  // return new DataFormat[] {EXPRESSION_RESULTS_TXT};
  // }

  /**
   * Create JobConf object for the Eoulsan counter.
   * @param basePath base path
   * @param sample sample of the job
   * @param genomicType genomic type
   * @param attributeIg GFF attribute Id
   * @throws IOException if an error occurs while creating job
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  private static final Job createJobEoulsanCounter(
      final Configuration parentConf, final Context context,
      final Sample sample, final String genomicType, final String attributeId)
      throws IOException, BadBioEntryException {

    final Configuration jobConf = new Configuration(parentConf);

    // Create JobConf
    // final JobConf conf = new JobConf(ExpressionHadoopStep.class);

    final Path inputPath =
        new Path(context.getInputDataFilename(
            DataFormats.FILTERED_MAPPER_RESULTS_SAM, sample));

    // Get annotation DataFile
    final DataFile annotationDataFile =
        context.getInputDataFile(DataFormats.ANNOTATION_GFF, sample);

    LOGGER.fine("sample: " + sample);
    LOGGER.fine("inputPath.getName(): " + inputPath.getName());
    LOGGER.fine("sample.getMetadata(): " + sample.getMetadata());
    LOGGER.fine("annotationDataFile: " + annotationDataFile.getSource());

    jobConf.set("mapred.child.java.opts", "-Xmx1024m");

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set Genome description path
    jobConf.set(ExpressionMapper.GENOME_DESC_PATH_KEY, context
        .getInputDataFile(DataFormats.GENOME_DESC_TXT, sample).getSource());

    final Path exonsIndexPath =
        new Path(context.getOtherDataFilename(ANNOTATION_INDEX_SERIAL, sample));
    LOGGER.info("exonsIndexPath: " + exonsIndexPath);

    if (!PathUtils.isFile(exonsIndexPath, jobConf))
      createExonsIndex(context, new Path(annotationDataFile.getSource()),
          genomicType, attributeId, exonsIndexPath, jobConf);

    // Set the path to the exons index
    // conf.set(Globals.PARAMETER_PREFIX + ".expression.exonsindex.path",
    // exonsIndexPath.toString());
    DistributedCache.addCacheFile(exonsIndexPath.toUri(), jobConf);

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // Create the job and its name
    final Job job =
        new Job(jobConf, "Expression computation with Eoulsan counter ("
            + sample.getName() + ", " + inputPath.getName() + ", "
            + annotationDataFile.getSource() + ", " + genomicType + ","
            + attributeId + ")");

    // Set the jar
    job.setJarByClass(ReadsMapperHadoopStep.class);

    // Set input path
    FileInputFormat.setInputPaths(job, inputPath);

    // Set the Mapper class
    job.setMapperClass(ExpressionMapper.class);

    // Set the reducer class
    job.setReducerClass(ExpressionReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    // job.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(
        job,
        new Path(context.getOutputDataFile(DataFormats.EXPRESSION_RESULTS_TXT,
            sample).getSourceWithoutExtension()
            + ".tmp"));

    return job;
  }

  /**
   * Create JobConf object for HTSeq-count.
   * @param basePath base path
   * @param sample sample of the job
   * @param genomicType genomic type
   * @param attributeId attributeId
   * @param stranded stranded mode
   * @param overlapMode overlap mode
   * @param removeAmbiguousCases true to remove ambiguous cases
   * @throws IOException if an error occurs while creating job
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   * @throws EoulsanException
   */
  private static final Job createJobHTSeqCounter(
      final Configuration parentConf, final Context context,
      final Sample sample, final String genomicType, final String attributeId,
      final StrandUsage stranded, final OverlapMode overlapMode,
      final boolean removeAmbiguousCases) throws IOException,
      BadBioEntryException, EoulsanException {

    final Configuration jobConf = new Configuration(parentConf);

    // Get input DataFile
    DataFile inputDataFile = null;
    inputDataFile =
        context.getExistingInputDataFile(
            new DataFormat[] {FILTERED_MAPPER_RESULTS_TSAM}, sample);
    if (inputDataFile == null)
      inputDataFile =
          context.getExistingInputDataFile(
              new DataFormat[] {FILTERED_MAPPER_RESULTS_SAM}, sample);

    if (inputDataFile == null)
      throw new IOException("No input file found.");

    // Set input path
    final Path inputPath = new Path(inputDataFile.getSource());

    // Get annotation DataFile
    final DataFile annotationDataFile =
        context.getInputDataFile(DataFormats.ANNOTATION_GFF, sample);

    LOGGER.fine("sample: " + sample);
    LOGGER.fine("inputPath.getName(): " + inputPath.getName());
    LOGGER.fine("sample.getMetadata(): " + sample.getMetadata());
    LOGGER.fine("annotationDataFile: " + annotationDataFile.getSource());

    jobConf.set("mapred.child.java.opts", "-Xmx1024m");

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set Genome description path
    final DataFile genomeDescDataFile =
        context.getInputDataFile(DataFormats.GENOME_DESC_TXT, sample);
    jobConf.set(ExpressionMapper.GENOME_DESC_PATH_KEY,
        genomeDescDataFile.getSource());

    // Set the "stranded" parameter
    jobConf.set(HTSeqCountMapper.STRANDED_PARAM, stranded.getName());

    // Set the "overlap mode" parameter
    jobConf.set(HTSeqCountMapper.OVERLAPMODE_PARAM, overlapMode.getName());

    // Set the "remove ambiguous cases" parameter
    jobConf.setBoolean(HTSeqCountMapper.REMOVE_AMBIGUOUS_CASES,
        removeAmbiguousCases);

    final Path featuresIndexPath =
        new Path(context.getOtherDataFilename(ANNOTATION_INDEX_SERIAL, sample));
    LOGGER.info("featuresIndexPath: " + featuresIndexPath);

    if (!PathUtils.isFile(featuresIndexPath, jobConf))
      createFeaturesIndex(context, new Path(annotationDataFile.getSource()),
          genomicType, attributeId, stranded, genomeDescDataFile,
          featuresIndexPath, jobConf);

    // Set the path to the features index
    DistributedCache.addCacheFile(featuresIndexPath.toUri(), jobConf);

    // Create the job and its name
    final Job job =
        new Job(jobConf, "Expression computation with htseq-count ("
            + sample.getName() + ", " + inputPath.getName() + ", "
            + annotationDataFile.getSource() + ", " + genomicType + ", "
            + attributeId + ", stranded : " + stranded + ")");

    // Set the jar
    job.setJarByClass(ExpressionHadoopStep.class);

    // Set input path
    FileInputFormat.setInputPaths(job, inputPath);

    // Set the mapper class
    job.setMapperClass(HTSeqCountMapper.class);

    // Set the combiner class
    // job.setCombinerClass(HTSeqCountReducer.class);

    // Set the reducer class
    job.setReducerClass(HTSeqCountReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set output path
    FileOutputFormat.setOutputPath(
        job,
        new Path(context.getOutputDataFile(DataFormats.EXPRESSION_RESULTS_TXT,
            sample).getSourceWithoutExtension()
            + ".tmp"));

    return job;
  }

  // private static final Job createJobGFFReader(final Configuration parentConf,
  // final Context context, final Sample sample, final String genomicType)
  // throws IOException, BadBioEntryException {
  //
  // final Configuration jobConf = new Configuration(parentConf);
  //
  // // Set counter group
  // jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);
  //
  // // Get annotation DataFile
  // final DataFile annotationDataFile =
  // context.getInputDataFile(DataFormats.ANNOTATION_GFF, sample);
  //
  // // Set input path
  // final Path inputPath = new Path(annotationDataFile.getSource());
  //
  // // Create the job and its name
  // final Job job =
  // new Job(jobConf, "Annotation file reading ("
  // + annotationDataFile.getSource() + ", " + genomicType + ")");
  //
  // // Set the jar
  // job.setJarByClass(ExpressionHadoopStep.class);
  //
  // // Set input path
  // FileInputFormat.setInputPaths(job, inputPath);
  //
  // // Set the Mapper class
  // job.setMapperClass(GFFReaderMapper.class);
  //
  // // Set the reducer class
  // job.setReducerClass(GFFReaderReducer.class);
  //
  // // Set the output key class
  // job.setOutputKeyClass(Text.class);
  //
  // // Set the output value class
  // job.setOutputValueClass(Text.class);
  //
  // // Output name
  // String outputName =
  // StringUtils.filenameWithoutExtension(inputPath.getName());
  // // outputName = outputName.substring(0, outputName.length() - 1);
  // outputName += ".tsam";
  //
  // // Set output path
  // FileOutputFormat.setOutputPath(job, new Path(inputPath.getParent(),
  // outputName));
  //
  // return job;
  // }

  private static final Job createJobPairedEnd(final Configuration parentConf,
      final Context context, final Sample sample) throws IOException,
      BadBioEntryException {

    final Configuration jobConf = new Configuration(parentConf);

    // get input file count for the sample
    final int inFileCount =
        context.getDataFileCount(DataFormats.READS_FASTQ, sample);

    if (inFileCount < 1)
      throw new IOException("No input file found.");

    if (inFileCount > 2)
      throw new IOException(
          "Cannot handle more than 2 reads files at the same time.");

    // Get the source
    final DataFile inputDataFile =
        context.getInputDataFile(DataFormats.FILTERED_MAPPER_RESULTS_SAM,
            sample);

    // Set input path
    final Path inputPath = new Path(inputDataFile.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set Genome description path
    jobConf.set(ExpressionMapper.GENOME_DESC_PATH_KEY, context
        .getInputDataFile(DataFormats.GENOME_DESC_TXT, sample).getSource());

    // Create the job and its name
    final Job job =
        new Job(jobConf, "Pretreatment for the expression estimation step ("
            + sample.getName() + ", " + inputDataFile.getSource() + ")");

    // Set the jar
    job.setJarByClass(ExpressionHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the Mapper class
    job.setMapperClass(PreTreatmentExpressionMapper.class);

    // Set the Reducer class
    job.setReducerClass(PreTreatmentExpressionReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Output name
    String outputName =
        StringUtils.filenameWithoutExtension(inputPath.getName());
    outputName = outputName.substring(0, outputName.length());
    outputName += ".tsam";

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(inputPath.getParent(),
        outputName));
    // FileOutputFormat.setOutputPath(
    // job,
    // new Path(context.getOutputDataFile(
    // DataFormats.TAB_FILTERED_MAPPER_RESULTS_SAM, sample).getSource()));

    return job;
  }

  /**
   * Create exon index.
   * @param gffPath gff path
   * @param expressionType expression type
   * @param attributeId GFF attribute id
   * @param exonsIndexPath output exon index path
   * @param conf configuration object
   * @throws IOException if an error occurs while creating the index
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  private static final Path createExonsIndex(final Context context,
      final Path gffPath, final String expressionType,
      final String attributeId, final Path exonsIndexPath,
      final Configuration conf) throws IOException, BadBioEntryException {

    final FileSystem fs = gffPath.getFileSystem(conf);
    final FSDataInputStream is = fs.open(gffPath);

    final TranscriptAndExonFinder ef =
        new TranscriptAndExonFinder(is, expressionType, attributeId);
    final File exonIndexFile =
        context.getRuntime().createFileInTempDir(
            StringUtils.basename(gffPath.getName())
                + ANNOTATION_INDEX_SERIAL.getDefaultExtention());
    ef.save(exonIndexFile);

    PathUtils.copyLocalFileToPath(exonIndexFile, exonsIndexPath, conf);
    if (!exonIndexFile.delete())
      LOGGER.warning("Can not delete exon index file: "
          + exonIndexFile.getAbsolutePath());

    return exonsIndexPath;
  }

  /**
   * @param context Eoulsan context
   * @param gffPath GFF annotation file path
   * @param featureType feature type to use
   * @param stranded strand mode
   * @param genomeDescDataFile genome description DataFile
   * @param featuresIndexPath feature index output path
   * @param conf Hadoop configuration object
   * @return the feature index output path
   * @throws IOException if an error occurs while creating the feature index
   *           file
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   * @throws EoulsanException if an error occurs with feature types and feature
   *           identifiants
   */
  private static final Path createFeaturesIndex(final Context context,
      final Path gffPath, final String featureType, final String attributeId,
      final StrandUsage stranded, final DataFile genomeDescDataFile,
      final Path featuresIndexPath, final Configuration conf)
      throws IOException, BadBioEntryException, EoulsanException {

    final GenomicArray<String> features = new GenomicArray<String>();
    final GenomeDescription genomeDescription =
        GenomeDescription.load(genomeDescDataFile.open());
    final Map<String, Integer> counts = Utils.newHashMap();

    final FileSystem fs = gffPath.getFileSystem(conf);
    final FSDataInputStream is = fs.open(gffPath);
    final GFFReader gffReader = new GFFReader(is);

    // Read the annotation file
    for (final GFFEntry gff : gffReader) {

      if (featureType.equals(gff.getType())) {

        final String featureId = gff.getAttributeValue(attributeId);
        if (featureId == null)
          throw new EoulsanException("Feature "
              + featureType + " does not contain a " + attributeId
              + " attribute");

        if ((stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE)
            && '.' == gff.getStrand())
          throw new EoulsanException("Feature "
              + featureType
              + " does not have strand information but you are running "
              + "htseq-count in stranded mode.");

        // Addition to the list of features of a GenomicInterval object
        // corresponding to the current annotation line
        features.addEntry(
            new GenomicInterval(gff, stranded.isSaveStrandInfo()), featureId);
        counts.put(featureId, 0);
      }
    }
    gffReader.throwException();
    gffReader.close();

    if (counts.size() == 0)
      throw new EoulsanException("Warning: No features of type '"
          + featureType + "' found.\n");

    final File featuresIndexFile =
        context.getRuntime().createFileInTempDir(
            StringUtils.basename(gffPath.getName())
                + ANNOTATION_INDEX_SERIAL.getDefaultExtention());

    // Add all chromosomes even without annotations to the feature object
    features.addChromosomes(genomeDescription);

    // Save the annotation
    features.save(featuresIndexFile);

    PathUtils.copyLocalFileToPath(featuresIndexFile, featuresIndexPath, conf);

    if (!featuresIndexFile.delete())
      LOGGER.warning("Can not delete features index file: "
          + featuresIndexFile.getAbsolutePath());

    return featuresIndexPath;
  }

  private static final void createFinalExpressionTranscriptsFile(
      final Context context, final Map<Sample, Job> jobconfs,
      final Configuration conf) throws IOException, InterruptedException {

    FinalExpressionTranscriptsCreator fetc = null;

    for (Map.Entry<Sample, Job> e : jobconfs.entrySet()) {

      final Sample sample = e.getKey();
      final Job rj = e.getValue();

      final long readsUsed =
          rj.getCounters().findCounter(COUNTER_GROUP, "reads used").getValue();

      final FileSystem fs =
          new Path(context.getBasePathname()).getFileSystem(conf);

      // Load the annotation index
      final Path exonsIndexPath =
          new Path(
              context.getOtherDataFilename(ANNOTATION_INDEX_SERIAL, sample));
      fetc = new FinalExpressionTranscriptsCreator(fs.open(exonsIndexPath));

      // Set the result path
      final Path resultPath =
          new Path(context.getOtherDataFilename(EXPRESSION_RESULTS_TXT, sample));

      fetc.initializeExpressionResults();

      // Load map-reduce results
      fetc.loadPreResults(
          new DataFile(context
              .getOutputDataFile(EXPRESSION_RESULTS_TXT, sample)
              .getSourceWithoutExtension()
              + ".tmp").open(), readsUsed);

      fetc.saveFinalResults(fs.create(resultPath));
    }

  }

  private static final void createFinalExpressionFeaturesFile(
      final Context context, final Map<Sample, Job> jobconfs,
      final Configuration conf) throws IOException {

    FinalExpressionFeaturesCreator fefc = null;

    for (Map.Entry<Sample, Job> e : jobconfs.entrySet()) {

      final Sample sample = e.getKey();
      // final Job rj = e.getValue();

      final FileSystem fs =
          new Path(context.getBasePathname()).getFileSystem(conf);

      // Load the annotation index
      final Path featuresIndexPath =
          new Path(
              context.getOtherDataFilename(ANNOTATION_INDEX_SERIAL, sample));
      fefc = new FinalExpressionFeaturesCreator(fs.open(featuresIndexPath));

      // Set the result path
      final Path resultPath =
          new Path(context.getOtherDataFilename(EXPRESSION_RESULTS_TXT, sample));

      fefc.initializeExpressionResults();

      // Load map-reduce results
      fefc.loadPreResults(new DataFile(context.getOutputDataFile(
          EXPRESSION_RESULTS_TXT, sample).getSourceWithoutExtension()
          + ".tmp").open());

      fefc.saveFinalResults(fs.create(resultPath));
    }
  }

  //
  // Step methods
  //

  @Override
  public void configure(Set<Parameter> stepParameters) throws EoulsanException {

    super.configure(stepParameters);
    this.conf = CommonHadoop.createConfiguration(EoulsanRuntime.getSettings());
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    if (getCounter().getCounterName().equals("eoulsanCounter"))
      return executeJobEoulsanCounter(design, context);
    else if (getCounter().getCounterName().equals("htseq-count"))
      return executeJobHTSeqCounter(design, context);

    return new StepResult(context, new EoulsanException("Unknown counter: "
        + getCounter().getCounterName()), "Unknown counter: "
        + getCounter().getCounterName());
  }

  /**
   * Execute Eoulsan counter as an Hadoop job.
   * @param design design object
   * @param context Eoulsan context
   * @return a StepResult object
   */
  private StepResult executeJobEoulsanCounter(final Design design,
      final Context context) {

    // Create configuration object
    final Configuration conf = new Configuration(false);

    // Create the list of jobs to run
    final Map<Sample, Job> jobsRunning = new HashMap<Sample, Job>();

    try {
      final long startTime = System.currentTimeMillis();

      LOGGER.info("Genomic type: " + getGenomicType());

      // Create the list of jobs to run
      for (Sample s : design.getSamples()) {

        final Job jconf =
            createJobEoulsanCounter(conf, context, s, getGenomicType(),
                getAttributeId());

        jconf.submit();
        jobsRunning.put(s, jconf);
      }

      // Compute map-reduce part of the expression computation
      final HadoopJobsResults jobsResults =
          new NewAPIJobsResults(jobsRunning.values(),
              CommonHadoop.CHECK_COMPLETION_TIME, COUNTER_GROUP);

      final long mapReduceEndTime = System.currentTimeMillis();
      LOGGER.info("Finish the part of the expression computation in "
          + ((mapReduceEndTime - startTime) / 1000) + " seconds.");

      // Create the final expression files
      createFinalExpressionTranscriptsFile(context, jobsRunning, this.conf);

      LOGGER.info("Finish the create of the final expression files in "
          + ((System.currentTimeMillis() - mapReduceEndTime) / 1000)
          + " seconds.");

      return jobsResults.getStepResult(context, startTime);

    } catch (IOException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    } catch (InterruptedException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    } catch (BadBioEntryException e) {

      return new StepResult(context, e, "Invalid annotation entry: "
          + e.getEntry());
    } catch (ClassNotFoundException e) {
      return new StepResult(context, e, "Class not found: " + e.getMessage());
    }
  }

  /**
   * Execute HTSeq-count counter as an Hadoop job.
   * @param design design object
   * @param context Eoulsan context
   * @return a StepResult object
   */
  private StepResult executeJobHTSeqCounter(final Design design,
      final Context context) {

    // Create configuration object
    final Configuration conf = new Configuration(false);

    // Create the list of jobs to run
    final Map<Sample, Job> jobsRunning = new HashMap<Sample, Job>();

    try {
      final long startTime = System.currentTimeMillis();

      LOGGER.info("Genomic type: " + getGenomicType());

      // Create the list of paired-end jobs to run
      final List<Job> jobsPairedEnd = new ArrayList<Job>();
      for (Sample s : design.getSamples()) {
        if (context.getDataFileCount(READS_FASTQ, s) == 2)
          jobsPairedEnd.add(createJobPairedEnd(conf, context, s));
      }

      MapReduceUtils.submitAndWaitForJobs(jobsPairedEnd,
          CommonHadoop.CHECK_COMPLETION_TIME, COUNTER_GROUP);

      // Create the list of jobs to run
      for (Sample s : design.getSamples()) {

        final Job jconf =
            createJobHTSeqCounter(conf, context, s, getGenomicType(),
                getAttributeId(), getStranded(), getOverlapMode(),
                isRemoveAmbiguousCases());

        jconf.submit();
        jobsRunning.put(s, jconf);
      }

      // Compute map-reduce part of the expression computation
      final HadoopJobsResults jobsResults =
          new NewAPIJobsResults(jobsRunning.values(),
              CommonHadoop.CHECK_COMPLETION_TIME, COUNTER_GROUP);

      final long mapReduceEndTime = System.currentTimeMillis();
      LOGGER.info("Finish the part of the expression computation in "
          + ((mapReduceEndTime - startTime) / 1000) + " seconds.");

      // Create the final expression files
      createFinalExpressionFeaturesFile(context, jobsRunning, this.conf);

      LOGGER.info("Finish the create of the final expression files in "
          + ((System.currentTimeMillis() - mapReduceEndTime) / 1000)
          + " seconds.");

      return jobsResults.getStepResult(context, startTime);

    } catch (IOException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    } catch (InterruptedException e) {

      return new StepResult(context, e, "Error while running job: "
          + e.getMessage());
    } catch (BadBioEntryException e) {

      return new StepResult(context, e, "Invalid annotation entry: "
          + e.getEntry());
    } catch (ClassNotFoundException e) {

      return new StepResult(context, e, "Class not found: " + e.getMessage());
    } catch (EoulsanException e) {

      return new StepResult(context, e,
          "Error while reading the annotation file: " + e.getMessage());
    }
  }

}
