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

package fr.ens.transcriptome.eoulsan.programs.mgmt.upload;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.programs.mgmt.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.programs.mgmt.Parameter;
import fr.ens.transcriptome.eoulsan.programs.mgmt.Step;
import fr.ens.transcriptome.eoulsan.programs.mgmt.StepResult;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

public class HDFSDataDownloadStep implements Step {

  // Logger
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  public static final String STEP_NAME = "_download";

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

  }

  @Override
  public StepResult execute(Design design, ExecutorInfo info) {

    logger.info("Start copying results.");
    logger.info("inpath="
        + info.getBasePathname() + "\toutpath=" + info.getOutputPathname());

    final long startTime = System.currentTimeMillis();
    final Configuration conf = new Configuration();

    if (info.getBasePathname() == null)
      throw new NullPointerException("The input path is null");

    if (info.getOutputPathname() == null)
      throw new NullPointerException("The output path is null");

    try {

      final Path inPath = new Path(info.getBasePathname());
      final Path outPath = new Path(info.getOutputPathname());

      if (!PathUtils.isExistingDirectoryFile(inPath, conf))
        throw new EoulsanException("The base directory is not a directory: "
            + inPath);

      final FileSystem inFs = inPath.getFileSystem(conf);
      final FileSystem outFs = outPath.getFileSystem(conf);

      final FileStatus[] files = inFs.listStatus(inPath, new PathFilter() {

        @Override
        public boolean accept(final Path p) {

          final String filename = p.getName();

          if (filename.startsWith("sample_expression_")
              && filename.endsWith(".txt"))
            // || filename.endsWith(".log"))
            return true;

          return false;

        }
      });

      // Create output path is does not exists
      if (!outFs.exists(outPath))
        outFs.mkdirs(outPath);

      final StringBuilder logMsg = new StringBuilder();

      if (files != null)
        for (FileStatus f : files) {

          final Path ip = f.getPath();
          final Path op = new Path(outPath, ip.getName());

          String msg = "Copy " + ip + " to " + op;
          logger.info(msg);
          logMsg.append(msg);
          logMsg.append("\n");
          PathUtils.copy(ip, op, conf);
        }

      return new StepResult(this, startTime, logMsg.toString());

    } catch (EoulsanException e) {

      return new StepResult(this, e, "Error while download results: "
          + e.getMessage());
    } catch (IOException e) {

      return new StepResult(this, e, "Error while download results: "
          + e.getMessage());
    }
  }

  @Override
  public String getDescription() {

    return "Download output data from HDFS filesystem";
  }

  @Override
  public String getLogName() {

    return "download";
  }

  @Override
  public String getName() {

    return STEP_NAME;
  }

}
