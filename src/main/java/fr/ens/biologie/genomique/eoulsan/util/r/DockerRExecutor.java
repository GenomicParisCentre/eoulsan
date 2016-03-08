package fr.ens.biologie.genomique.eoulsan.util.r;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Joiner;
import com.spotify.docker.client.DockerException;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;
import fr.ens.biologie.genomique.eoulsan.util.docker.DockerProcess;

/**
 * This class define a Docker RExecutor.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerRExecutor extends ProcessRExecutor {

  public static final String REXECUTOR_NAME = "docker";

  private final String dockerImage;

  @Override
  public String getName() {

    return REXECUTOR_NAME;
  }

  @Override
  protected void putFile(final DataFile inputFile, final String outputFilename)
      throws IOException {

    final DataFile outputFile =
        new DataFile(getOutputDirectory(), outputFilename);

    // Check if the input and output file are the same
    if (isSameLocalPath(inputFile, outputFile)) {
      return;
    }

    // If input file is not local file use the super implementation
    if (!inputFile.isLocalFile()) {
      super.putFile(inputFile, outputFilename);
      return;
    }

    final File inFile = inputFile.toFile();

    // Check if the file is in the output directory (or a subdir) or in the
    // temporary directory (or a subdir)
    if (isInSubDir(getOutputDirectory(), inFile)
        || isInSubDir(getTemporaryDirectory(), inFile)) {

      // If not, copy files
      if (outputFile.exists()) {
        throw new IOException("The output file already exists: " + outputFile);
      }
      DataFiles.copy(inputFile, outputFile);

    } else {

      // Else use default putFile implementation
      super.putFile(inputFile, outputFilename);
    }
  }

  private static boolean isInSubDir(File a, File b) {

    final File aAbs = a.getAbsoluteFile();
    final File bAbs = b.getAbsoluteFile();

    final URI aURI = aAbs.toURI();
    final URI bURI = bAbs.toURI();

    return !bURI.equals(aURI.relativize(bURI));
  }

  @Override
  protected void executeRScript(final File rScriptFile, final boolean sweave,
      final String sweaveOuput, final String... scriptArguments)
      throws IOException {

    final DockerProcess process =
        new DockerProcess(this.dockerImage, getTemporaryDirectory());

    final List<String> commandLine =
        createCommand(rScriptFile, sweave, sweaveOuput, scriptArguments);

    final File stdoutFile = changeFileExtension(rScriptFile, ".out");

    try {

      final int exitValue = process.execute(commandLine, getOutputDirectory(),
          Collections.singletonMap(LANG_ENVIRONMENT_VARIABLE, DEFAULT_R_LANG),
          getTemporaryDirectory(), stdoutFile, stdoutFile, true);

      ProcessUtils.throwExitCodeException(exitValue,
          Joiner.on(' ').join(commandLine));

    } catch (DockerException | InterruptedException e) {
      throw new IOException(e);
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param outputDirectory the output directory
   * @param temporaryDirectory the temporary directory
   * @param dockerImage docker image to use
   * @throws IOException if an error occurs while creating the object
   */
  public DockerRExecutor(final File outputDirectory,
      final File temporaryDirectory, final String dockerImage)
      throws IOException {
    super(outputDirectory, temporaryDirectory);

    if (dockerImage == null) {
      throw new NullPointerException("dockerImage argument cannot be null");
    }

    this.dockerImage = dockerImage;
  }

}
