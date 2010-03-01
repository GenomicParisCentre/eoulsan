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

package fr.ens.transcriptome.eoulsan.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.hadoop.mapreads.SOAPCombiner;
import fr.ens.transcriptome.eoulsan.hadoop.mapreads.SOAPMapper;
import fr.ens.transcriptome.eoulsan.hadoop.mapreads.SOAPReducer;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

public class MapReads {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static final String MAP_READS_OUTPUT_DIR_EXTENSION =
      ".map.aln.temp.dir";
  private static final String UNMAP_READS_OUTPUT_DIR_EXTENSION =
      ".map.umap.temp.dir";

  public static void main(final String[] args) throws Exception {

    if (args == null)
      throw new NullPointerException("The arguments of import data is null");

    if (args.length != 1)
      throw new IllegalArgumentException("Import data needs only one argument");

    final String uri = args[0];

    // Create Configuration Object
    final Configuration conf = new Configuration();

    final Path basePath = new Path(uri);
    final Path inputDirPath = new Path(basePath, Common.READS_SUBDIR);

    logger.info("Map reads input path: " + inputDirPath);

    final List<Path> listSoapIndexZipPathsInBasePath =
        PathUtils.listPathsBySuffix(basePath,
            Common.SOAP_INDEX_ZIP_FILE_EXTENSION, conf);
    if (listSoapIndexZipPathsInBasePath.size() == 0)
      throw new IOException("SOAP index zip file not found.");
    if (listSoapIndexZipPathsInBasePath.size() > 1)
      throw new IOException("Found more than one SOAP index zip file.");

    final Path soapIndexZipPath = listSoapIndexZipPathsInBasePath.get(0);

    final FileSystem fs = PathUtils.getFileSystem(inputDirPath, conf);
    final FileStatus[] filesStatus =
        fs.listStatus(inputDirPath, new PathUtils.SuffixPathFilter(
            Common.READS_FILTERED_EXTENSION));

    final List<Job> jobs = new ArrayList<Job>();

    // Run the filter
    for (FileStatus fst : filesStatus) {

      final Path inputPath = fst.getPath();
      final Path outputPath =
          PathUtils.newPathWithOtherExtension(inputPath,
              MAP_READS_OUTPUT_DIR_EXTENSION);

      jobs.add(createMapReadJob(conf, soapIndexZipPath, inputPath, outputPath));
    }

    // Runs the jobs
    MapReduceUtils.submitandWaitForJobs(jobs, Common.CHECK_COMPLETION_TIME);

    // Rename the outputs of map reduce
    for (FileStatus fst : filesStatus) {
      final Path inputPath = fst.getPath();
      final Path outputPath =
          PathUtils.newPathWithOtherExtension(inputPath,
              MAP_READS_OUTPUT_DIR_EXTENSION);
      final Path resultPath =
          PathUtils.newPathWithOtherExtension(inputPath,
              Common.SOAP_RESULT_EXTENSION);

      MapReduceUtils.moveMapredOutput(outputPath, resultPath, true, conf);
    }

    //
    // Concat all unmap file into one at the root base dir
    //

    // The list of unmap files
    final List<Path> unmapPaths =
        PathUtils.listPathsBySuffix(inputDirPath, Common.UNMAP_EXTENSION, conf);

    // Get the name of the genome file
    final Path genomePath = Common.getGenomeFilePath(basePath, conf);

    PathUtils.concat(unmapPaths, PathUtils.newPathWithOtherExtension(
        genomePath, Common.UNMAP_EXTENSION), false, true, conf);

  }

  /**
   * Create a map reduce job
   * @param parentConf Configuration
   * @param soapIndexZipPath genome reference path
   * @param inputPath input path for reads
   * @param outputPath output directory
   * @return a new job
   * @throws IOException if an error occurs while creating job
   * @throws InterruptedException if an error occurs while creating job
   * @throws ClassNotFoundException if an error occurs while creating job
   */
  private static Job createMapReadJob(final Configuration parentConf,
      final Path soapIndexZipPath, final Path inputPath, final Path outputPath)
      throws IOException, InterruptedException, ClassNotFoundException {

    final Configuration jobConf = new Configuration(parentConf);

    // Force to use only one jvm by task
    //jobConf.set("mapred.job.reuse.jvm.num.task", "1");

    // Set genome reference path
    jobConf.set(Globals.PARAMETER_PREFIX + ".soap.indexzipfilepath",
        soapIndexZipPath.toString());

    // Set unmap chuck dir path
    jobConf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.temp.dir", PathUtils
        .newPathWithOtherExtension(inputPath, UNMAP_READS_OUTPUT_DIR_EXTENSION)
        .toString());

    // Set unmap output file path
    jobConf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.path", PathUtils
        .newPathWithOtherExtension(inputPath, Common.UNMAP_EXTENSION)
        .toString());

    // Set the number of threads for soap
    jobConf.set(Globals.PARAMETER_PREFIX + ".soap.nb.threads", "1");
    
    
    // Debug
    // jobConf.set("mapred.job.tracker", "local");

    // Create the job and its name
    final Job job = new Job(jobConf, "Map reads in " + inputPath.getName());

    // Set the jar of the map reduce classes
    job.setJarByClass(MapReads.class);

    // Set the mapper class
    job.setMapperClass(SOAPMapper.class);

    // Set the map key/values types
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);

    // Add input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set combiner
    job.setCombinerClass(SOAPCombiner.class);

    // Set Reducer
    job.setReducerClass(SOAPReducer.class);

    // Set the output Path
    FileOutputFormat.setOutputPath(job, outputPath);

    // Set the map key/values types

    return job;
  }

}
