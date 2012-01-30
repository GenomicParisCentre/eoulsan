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

package fr.ens.transcriptome.eoulsan.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class is used to install binaries bundled in the jar.
 * @author Laurent Jourdren
 */
public class BinariesInstaller {

  private static Logger logger = Logger.getLogger(Globals.APP_NAME);
  private static final int BUFFER_SIZE = 32 * 1024;

  private static void install(final String inputPath, final String file,
      final String outputPath) throws FileNotFoundException, IOException {

    if (new File(outputPath, file).isFile()) {
      logger.fine(file + " is allready installed.");
      return;
    }

    final File outputDir = new File(outputPath);

    if (!outputDir.isDirectory()) {
      if (!outputDir.mkdirs())
        throw new IOException(
            "Can't create directory for binaries installation: "
                + outputDir.getAbsolutePath());
      FileUtils.setDirectoryWritable(outputDir, true, false);
    }

    final String resourcePath = inputPath.toLowerCase() + "/" + file;
    final InputStream is =
        BinariesInstaller.class.getResourceAsStream(resourcePath);

    if (is == null)
      throw new FileNotFoundException("Unable to find the correct resource ("
          + resourcePath + ")");

    final File outputFile = new File(outputDir, file);
    OutputStream fos = FileUtils.createOutputStream(outputFile);

    byte[] buf = new byte[BUFFER_SIZE];
    int i = 0;

    while ((i = is.read(buf)) != -1)
      fos.write(buf, 0, i);

    is.close();
    fos.close();

    FileUtils.setExecutable(outputFile, true, false);
    FileUtils.setReadable(outputFile, true, false);
  }

  /**
   * Install a binary bundled in the jar in a temporary directory. If no
   * temporary directory is defined, use the "java.io.tmpdir" property.
   * @param binaryFilename program to install
   * @param temporary directory where to install the binary
   * @return a string with the path of the installed binary
   * @throws IOException if an error occurs while installing binary
   */
  public static String install(final String binaryFilename, final String tempDir)
      throws IOException {

    final File tempDirFile =
        new File(tempDir == null
            ? System.getProperty("java.io.tmpdir") : tempDir.trim());

    if (!tempDirFile.exists())
      throw new IOException("Temporary directory does not exits: "
          + tempDirFile);

    if (!tempDirFile.isDirectory())
      throw new IOException("Temporary directory is not a directory: "
          + tempDirFile);

    if (!SystemUtils.isUnix())
      throw new IOException("Can only install binaries on *nix systems.");

    final String os = System.getProperty("os.name").toLowerCase();
    final String arch = System.getProperty("os.arch").toLowerCase();

    logger.fine("Try to install \""
        + binaryFilename + "\" for " + os + " (" + arch + ")");

    String osArchKey = os + "\t" + arch;

    // Bypass platform checking if necessary
    if (!EoulsanRuntime.getSettings().isBypassPlatformChecking()) {

      // Check if platform is allowed
      if (!Globals.AVAILABLE_BINARY_ARCH.contains(osArchKey))
        throw new FileNotFoundException(
            "There is no executable for your plateform ("
                + os + ") included in " + Globals.APP_NAME);

      // Change the os and arch if alias
      if (Globals.AVAILABLE_BINARY_ARCH_ALIAS.containsKey(osArchKey))
        osArchKey = Globals.AVAILABLE_BINARY_ARCH_ALIAS.get(osArchKey);
    }

    final String inputPath =
        "/" + osArchKey.replace(" ", "").replace('\t', '/');

    final String outputPath =
        tempDirFile.getAbsolutePath()
            + "/" + Globals.APP_NAME_LOWER_CASE + "/"
            + Globals.APP_VERSION_STRING;

    // Test if the file is allready installed
    if (new File(outputPath, binaryFilename).isFile()) {
      logger.info(binaryFilename + " is allready installed.");
      return outputPath + "/" + binaryFilename;
    }

    // install the file
    install(inputPath, binaryFilename, outputPath);

    logger.fine("Successful installation of "
        + binaryFilename + " in " + outputPath);
    return outputPath + "/" + binaryFilename;
  }
}
