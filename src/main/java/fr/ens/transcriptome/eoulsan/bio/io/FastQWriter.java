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

package fr.ens.transcriptome.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * this class implements a FastQ writer.
 * @author Laurent Jourdren
 */
public class FastQWriter extends ReadSequenceWriter {

  private Writer writer;

  /**
   * Write the current entry.
   * @throws IOException if an error occurs while writing data
   */
  @Override
  public void write() throws IOException {

    this.writer.write(toFastQ());
  }

  /**
   * Close the writer.
   * @throws IOException if an error occurs while closing the writer
   */
  @Override
  public void close() throws IOException {

    this.writer.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public FastQWriter(final Writer writer) {

    if (writer == null)
      throw new NullPointerException("The writer is null.");

    this.writer = writer;
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public FastQWriter(final OutputStream os) throws FileNotFoundException {

    this.writer = FileUtils.createBufferedWriter(os);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public FastQWriter(final File outputFile) throws IOException {

    this.writer = FileUtils.createBufferedWriter(outputFile);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public FastQWriter(final String outputFilename) throws IOException {

    this.writer = FileUtils.createBufferedWriter(outputFilename);
  }

}
