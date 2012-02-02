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

package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define an absract EoulsanRuntime.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractEoulsanRuntime {

  private final Settings settings;

  /**
   * Get Settings.
   * @return a Settings object
   */
  public Settings getSettings() {

    return this.settings;
  }

  /**
   * Get the temporary directory.
   * @return the temporary directory as a File object
   */
  public abstract File getTempDirectory();

  /**
   * Test if Eoulsan runs in Hadoop mode.
   * @return true if Eoulsan runs in Hadoop mode
   */
  public abstract boolean isHadoopMode();

  /**
   * Test if Eoulsan runs in an Amazon MapReduce cluster.
   * @return true if Eoulsan in an Amazon MapReduce cluster
   */
  public abstract boolean isAmazonMode();

  /**
   * Create an InputStream to load data.
   * @param dataSource the source of the data to load
   * @return an InputStream corresponding to the source
   * @throws IOException if an error occurs the InputStream
   */
  public abstract InputStream getInputStream(String dataSource)
      throws IOException;

  /**
   * Create a raw InputStream (without decompression of input data) to load
   * data.
   * @param dataSource the source of the data to load
   * @return an InputStream corresponding to the source
   * @throws IOException if an error occurs the InputStream
   */
  public abstract InputStream getRawInputStream(String dataSource)
      throws IOException;

  /**
   * Create an OutputStream to load data.
   * @param dataSource the source of the data to load
   * @return an OutputStream corresponding to the source
   * @throws IOException if an error occurs the OutputStream
   */
  public abstract OutputStream getOutputStream(String dataSource)
      throws IOException;

  /**
   * Decompress an inputStream if needed.
   * @param is the InputStream
   * @param source source of the inputStream
   * @return a InputStream with decompression integrated or not
   * @throws IOException if an error occurs while creating decompressor
   *           InputStream
   */
  protected InputStream decompressInputStreamIsNeeded(final InputStream is,
      final String source) throws IOException {

    final String extension = StringUtils.compressionExtension(source);

    return CompressionType.getCompressionTypeByExtension(extension)
        .createInputStream(is);
  }

  /**
   * Create a new temporary directory.
   * @return the new directory
   * @throws IOException if there is an error creating the temporary directory
   */
  public File createTempDir() throws IOException {

    return FileUtils.createTempDir(getTempDirectory(), null);
  }

  /**
   * Create a new temporary directory.
   * @param prefix prefix of the temporary directory
   * @return the new directory
   * @throws IOException if there is an error creating the temporary directory
   */
  public File createTempDir(final String prefix) throws IOException {

    return FileUtils
        .createTempDir(getTempDirectory(), prefix);
  }

  /**
   * Create a new temporary file.
   * @param prefix Prefix of the temporary file
   * @param suffix suffix of the temporary file
   * @return the new temporary file
   * @throws IOException if there is an error creating the temporary directory
   */
  public File createTempFile(final String prefix, final String suffix)
      throws IOException {

    return FileUtils.createTempFile(getTempDirectory(),
        prefix, suffix);
  }

  /**
   * Create a file in the temporary directory.
   * @param filename The filename to create
   * @return The new File
   */
  public File createFileInTempDir(final String filename) {

    return new File(getTempDirectory(), filename);
  }

  //
  // Constructor
  //

  protected AbstractEoulsanRuntime(final Settings settings) {

    if (settings == null) {
      throw new EoulsanError("The settings are null");
    }

    this.settings = settings;
  }
}
