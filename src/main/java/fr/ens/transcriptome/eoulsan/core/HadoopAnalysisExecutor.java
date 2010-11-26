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

package fr.ens.transcriptome.eoulsan.core;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.io.DesignReader;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class define an executor for hadoop mode.
 * @author Laurent Jourdren
 */
public class HadoopAnalysisExecutor extends Executor {

  private Configuration conf;
  private Path designPath;
  private Design design;
  private SimpleContext context;

  @Override
  protected SimpleContext getExecutorInfo() {

    return this.context;
  }

  @Override
  protected Design loadDesign() throws EoulsanException {

    if (this.design != null)
      return this.design;

    try {

      final DesignReader dr =
          new SimpleDesignReader(this.designPath.toUri().toURL().openStream());

      return dr.read();

    } catch (IOException e) {
      throw new EoulsanException("Error while reading design file: " + e.getMessage());
    }
  }

  @Override
  protected void writeStepLogs(final StepResult result) {

    if (result == null || result.getStep().getLogName() == null)
      return;

    try {

      final Path logPath = new Path(getExecutorInfo().getLogPathname());
      final Path basePath = new Path(getExecutorInfo().getBasePathname());
      final FileSystem logFs = logPath.getFileSystem(this.conf);
      final FileSystem baseFs = basePath.getFileSystem(this.conf);

      if (!logFs.exists(logPath))
        logFs.mkdirs(logPath);

      final String logFilename = result.getStep().getLogName();

      // Write logs in the log directory
      writeResultLog(new Path(logPath, logFilename + ".log"), logFs, result);
      writeErrorLog(new Path(logPath, logFilename + ".err"), logFs, result);

      // Write logs in the base directory
      writeResultLog(new Path(basePath, logFilename + ".log"), baseFs, result);
      writeErrorLog(new Path(basePath, logFilename + ".err"), baseFs, result);

      // Write the catlog of the base path
      writeDirectoryCatalog(new Path(logPath, logFilename + ".cat"), logFs);

    } catch (IOException e) {

      Common.showAndLogErrorMessage("Unable to create log file for "
          + result.getStep() + " step.");
    }

  }

  private void writeResultLog(final Path logPath, final FileSystem fs,
      final StepResult result) throws IOException {

    final Writer writer = new OutputStreamWriter(fs.create(logPath));

    final String data = result.getLogMessage();

    if (data != null)
      writer.write(data);
    writer.close();
  }

  private void writeErrorLog(final Path logPath, final FileSystem fs,
      final StepResult result) throws IOException {

    final Writer writer = new OutputStreamWriter(fs.create(logPath));

    final String data = result.getErrorMessage();
    final Exception e = result.getException();

    if (data != null)
      writer.write(data);

    if (e != null) {

      writer.write("\n");
      writer.write("Exception: " + e.getClass().getName() + "\n");
      writer.write("Message: " + e.getMessage() + "\n");
      writer.write("StackTrace:\n");
      e.printStackTrace(new PrintWriter(writer));
    }

    writer.close();
  }

  private void writeDirectoryCatalog(final Path catPath, final FileSystem fs)
      throws IOException {

    final Writer writer = new OutputStreamWriter(fs.create(catPath));

    final Path basePath = new Path(getExecutorInfo().getBasePathname());
    final FileSystem baseFs = basePath.getFileSystem(this.conf);

    final StringBuilder sb = new StringBuilder();

    final FileStatus[] files = baseFs.listStatus(basePath);

    long count = 0;

    if (files != null)
      for (FileStatus f : files) {

        if (f.isDir())
          sb.append("D ");
        else
          sb.append("  ");

        sb.append(new Date(f.getModificationTime()));
        sb.append("\t");

        sb.append(String.format("%10d", f.getLen()));
        sb.append("\t");

        sb.append(f.getPath().getName());
        sb.append("\n");

        count += f.getLen();
      }
    sb.append(count);
    sb.append(" bytes in ");
    sb.append(basePath);
    sb.append(" directory.\n");

    writer.write(sb.toString());
    writer.close();
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private HadoopAnalysisExecutor(final Configuration conf) {

    if (conf == null)
      throw new NullPointerException("The configuration is null.");

    this.conf = conf;
    this.context = new SimpleContext();
  }

  /**
   * Constructor
   * @param command command to execute
   * @param designPathname the path the design file
   */
  public HadoopAnalysisExecutor(final Configuration conf,
      final Command command, final Path designPath) {

    this(conf);

    if (command == null)
      throw new NullPointerException("The command is null");

    setCommand(command);

    if (designPath == null)
      throw new NullPointerException("The design path is null.");

    this.designPath = designPath;
    getExecutorInfo().setBasePathname(designPath.getParent().toString());

  }

  /**
   * Constructor
   * @param command command to execute
   * @param designPathname the path the design file
   * @throws IOException if cannot create log or output directory
   */
  public HadoopAnalysisExecutor(final Configuration conf,
      final Command command, final Design design, final Path designPath,
      final Path paramPath) throws IOException {

    this(conf);

    if (command == null)
      throw new NullPointerException("The command is null");

    setCommand(command);

    if (design == null)
      throw new NullPointerException("The design is null.");

    if (designPath == null)
      throw new NullPointerException("The design path is null.");

    this.design = design;
    this.designPath = designPath;

    final SimpleContext context = getExecutorInfo();

    context.setBasePathname(this.designPath.getParent().toString());

    final Path logPath =
        new Path(this.designPath.getParent().toString()
            + "/" + context.getExecutionName());

    final Path outputPath =
        new Path(this.designPath.getParent().toString()
            + "/" + context.getExecutionName());

    if (!logPath.getFileSystem(conf).exists(logPath))
      PathUtils.mkdirs(logPath, conf);
    if (!outputPath.getFileSystem(conf).exists(outputPath))
      PathUtils.mkdirs(outputPath, conf);

    context.setLogPathname(logPath.toString());
    context.setOutputPathname(outputPath.toString());
    context.setDesignPathname(designPath.toString());
    context.setParameterPathname(paramPath.toString());
  }

}
