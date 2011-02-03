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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.mapreduce.Counter;

/**
 * This class implements an OutputStream that inform Hadoop of the progress of
 * task using counters.
 * @author Laurent Jourdren
 */
public final class ProgressCounterOutputstream extends OutputStream {

  private static final int MAX = 100 * 1024 * 1024;

  private final OutputStream out;
  private final Counter counter;
  private int sum;

  //
  // Override methods
  //

  @Override
  public final void write(final byte[] b, final int off, final int len)
      throws IOException {

    out.write(b, off, len);
    incrementCounter(len);
  }

  @Override
  public final void write(final byte[] b) throws IOException {

    out.write(b);
    incrementCounter(b.length);
  }

  @Override
  public final void write(final int b) throws IOException {

    out.write(b);
    incrementCounter(1);
  }

  @Override
  public void flush() throws IOException {
    out.flush();
  }

  @Override
  public final void close() throws IOException {

    out.flush();
    out.close();
    counter.increment(this.sum);
  }

  //
  // Other methods
  //

  private final void incrementCounter(final int bytes) {

    this.sum += bytes;

    if (this.sum > MAX) {
      counter.increment(this.sum);
      this.sum = 0;
    }

  }

  //
  // Constructor
  //

  public ProgressCounterOutputstream(final OutputStream os,
      final Counter counter) {

    checkNotNull(os, "OutputStream is null");
    checkNotNull(counter, "The counter to use is null.");

    this.out = os;
    this.counter = counter;
  }

}
