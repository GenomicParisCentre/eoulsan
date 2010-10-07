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

package fr.ens.transcriptome.eoulsan.io;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.io.compress.BZip2Codec;

/**
 * This class define an output stream for Bzip2 files in hadoop mode.
 * @author Laurent Jourdren
 */
public class HadoopBZip2OutputStream extends OutputStream {

  private OutputStream os;

  @Override
  public void close() throws IOException {

    this.os.close();
  }

  @Override
  public void flush() throws IOException {

    this.os.flush();
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {

    this.os.write(b, off, len);
  }

  @Override
  public void write(byte[] b) throws IOException {

    this.os.write(b);
  }

  @Override
  public void write(int b) throws IOException {

    this.os.write(b);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param os OutputStream
   */
  public HadoopBZip2OutputStream(final OutputStream os) throws IOException {

    BZip2Codec codec = new BZip2Codec();
    this.os = codec.createOutputStream(os);
  }

}
