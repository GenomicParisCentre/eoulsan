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

package fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.DF;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.DistributedFileSystem.DiskStatus;
import org.apache.hadoop.hdfs.server.namenode.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.util.VersionInfo;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.LinuxCpuInfo;
import fr.ens.transcriptome.eoulsan.util.LinuxMemInfo;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class initialize the global logger
 * @author Laurent Jourdren
 */
@HadoopOnly
public class InitGlobalLoggerStep extends AbstractStep {

  /** Logger. */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  /** Step name. */
  public static final String STEP_NAME = "_init_global_logger";

  private static final String CPUINFO_FILE_PATH = "/proc/cpuinfo";
  private static final String MEMINFO_FILE_PATH = "/proc/meminfo";
  private static final String ROOT_PATH = "/";
  private static final String VAR_PATH = "/var";
  private static final String TMP_PATH = "/tmp";

  private Configuration conf;

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "Initialize global logger";
  }

  @Override
  public String getLogName() {

    return null;
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    this.conf = CommonHadoop.createConfiguration(EoulsanRuntime.getSettings());
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    final Configuration conf = this.conf;
    final Path loggerPath =
        new Path(context.getLogPathname(), Globals.APP_NAME_LOWER_CASE + ".log");

    try {

      final FileSystem loggerFs = loggerPath.getFileSystem(conf);
      logger.addHandler(new StreamHandler(loggerFs.create(loggerPath),
          Globals.LOG_FORMATTER));
      logger.setLevel(Globals.LOG_LEVEL);

      logger.info(Globals.WELCOME_MSG + " Hadoop mode.");
      context.logInfo();

    } catch (IOException e) {
      logger.severe("Unable to configure global logger: " + loggerPath);
    }

    HadoopInfo();

    try {
      copyCpuinfoAndMeminfo(context, conf);
      sysInfo(conf);
      dfsInfo(context, conf);
    } catch (IOException e) {
      logger
          .severe("Error while getting system information: " + e.getMessage());

      e.printStackTrace();
    }

    return new StepResult(context, true, "");
  }

  //
  // Other methods
  //

  private void copyFile(final File file, final Path outputDirPath,
      final Configuration conf) throws IOException {

    // Test if file exists
    if (!file.exists()) {
      logger.severe("Can't copy " + file + " to log directory");
      return;
    }

    final Path output = new Path(outputDirPath, file.getName());

    // Copy file
    PathUtils.copyLocalFileToPath(file, output, conf);
  }

  private void copyCpuinfoAndMeminfo(final Context context,
      final Configuration conf) throws IOException {

    final Path logPath = new Path(context.getLogPathname());

    final File cpuinfo = new File(CPUINFO_FILE_PATH);
    final File meminfo = new File(MEMINFO_FILE_PATH);

    copyFile(cpuinfo, logPath, conf);
    copyFile(meminfo, logPath, conf);
  }

  private void sysInfo(final Configuration conf) throws IOException {

    parseCpuinfo();
    parseMeminfo();

    df(new File(ROOT_PATH), conf);
    df(new File(TMP_PATH), conf);
    df(new File(VAR_PATH), conf);

    // Log the usage of the hadoop temporary directory partition
    final String hadoopTmp = conf.get("hadoop.tmp.dir");
    if (hadoopTmp != null)
      df(new File(hadoopTmp), conf);

    // Log the usage of the Java temporary directory partition
    final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    if (tmpDir != null && tmpDir.exists() && tmpDir.isDirectory())
      df(tmpDir, conf);
  }

  private static final void parseCpuinfo() throws IOException {

    final LinuxCpuInfo cpuinfo = new LinuxCpuInfo();

    final String modelName = cpuinfo.getModelName();
    final String processor = cpuinfo.getProcessor();
    final String cpuMHz = cpuinfo.getCPUMHz();
    final String bogomips = cpuinfo.getBogoMips();
    final String cores = cpuinfo.getCores();

    logger.info("SYSINFO CPU model name: "
        + (modelName == null ? "NA" : modelName));
    logger.info("SYSINFO CPU count: "
        + (processor == null ? "NA" : ""
            + (Integer.parseInt(processor.trim()) + 1)));
    logger.info("SYSINFO CPU cores: " + (cores == null ? "NA" : cores));
    logger.info("SYSINFO CPU clock: "
        + (cpuMHz == null ? "NA" : cpuMHz) + " MHz");
    logger.info("SYSINFO Bogomips: " + (bogomips == null ? "NA" : bogomips));
  }

  private static final void parseMeminfo() throws IOException {

    final LinuxMemInfo meminfo = new LinuxMemInfo();
    final String memTotal = meminfo.getMemTotal();

    logger.info("SYSINFO Mem Total: " + (memTotal == null ? "NA" : memTotal));
  }

  private static final void df(final File f, final Configuration conf)
      throws IOException {

    DF df = new DF(f, conf);

    logger.info("SYSINFO "
        + f + " " + StringUtils.sizeToHumanReadable(df.getCapacity())
        + " capacity, " + StringUtils.sizeToHumanReadable(df.getUsed())
        + " used, " + StringUtils.sizeToHumanReadable(df.getAvailable())
        + " available, " + df.getPercentUsed() + "% used");

  }

  private static final void dfsInfo(final Context context,
      final Configuration conf) throws IOException {

    final Path basePath = new Path(context.getBasePathname());
    final FileSystem fs = basePath.getFileSystem(conf);

    if (fs instanceof DistributedFileSystem) {

      final DistributedFileSystem dfs = (DistributedFileSystem) fs;
      final DiskStatus ds = dfs.getDiskStatus();

      final int percent = (int) (100.0 * ds.getDfsUsed() / ds.getCapacity());

      logger.info("SYSINFO "
          + fs.getUri() + " "
          + StringUtils.sizeToHumanReadable(ds.getCapacity()) + " capacity, "
          + StringUtils.sizeToHumanReadable(ds.getDfsUsed()) + " used, "
          + StringUtils.sizeToHumanReadable(ds.getRemaining()) + " available, "
          + percent + "% used");

      logger.info("SYSINFO Block size: " + dfs.getDefaultBlockSize());
      logger.info("SYSINFO Replication: " + dfs.getDefaultReplication());

      FSNamesystem fsn = FSNamesystem.getFSNamesystem();

      ArrayList<DatanodeDescriptor> live = new ArrayList<DatanodeDescriptor>();
      ArrayList<DatanodeDescriptor> dead = new ArrayList<DatanodeDescriptor>();

      if (fsn != null) {
        fsn.DFSNodesStatus(live, dead);

        logger.info("SYSINFO DFS live nodes: " + live.size());
        logger.info("SYSINFO DFS dead nodes: " + dead.size());
      }

    }
  }

  private static final void HadoopInfo() {

    logger.info("SYSINFO Hadoop version: " + VersionInfo.getVersion());
    logger.info("SYSINFO Hadoop revision: " + VersionInfo.getRevision());
    logger.info("SYSINFO Hadoop date: " + VersionInfo.getDate());
    logger.info("SYSINFO Hadoop user: " + VersionInfo.getUser());
    logger.info("SYSINFO Hadoop url: " + VersionInfo.getUrl());
  }
}
