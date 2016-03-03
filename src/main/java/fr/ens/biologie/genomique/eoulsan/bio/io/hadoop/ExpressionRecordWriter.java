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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.bio.io.hadoop;

import static fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.Counters.ENTRIES_WRITTEN;
import static fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.Counters.INPUT_ENTRIES;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * This class define a RecordWriter for expression files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionRecordWriter extends RecordWriter<Text, LongWritable> {

  private static final String COUNTERS_GROUP =
      "Expression Output Format Counters";

  private static final String utf8 = "UTF-8";
  private static final byte[] newline;
  private static final byte[] separator;

  static {
    try {
      newline = "\n".getBytes(utf8);
      separator = "\t".getBytes(utf8);
    } catch (UnsupportedEncodingException uee) {
      throw new IllegalArgumentException("can't find " + utf8 + " encoding");
    }
  }

  private final DataOutputStream out;
  private final TaskAttemptContext context;

  @Override
  public synchronized void write(final Text key, final LongWritable value)
      throws IOException, InterruptedException {

    this.context.getCounter(COUNTERS_GROUP, INPUT_ENTRIES).increment(1);

    if (value == null) {
      return;
    }

    this.out.write(key.getBytes(), 0, key.getLength());
    this.out.write(separator);
    this.out.write(value.toString().getBytes(utf8));
    this.out.write(newline);

    this.context.getCounter(COUNTERS_GROUP, ENTRIES_WRITTEN).increment(1);
  }

  @Override
  public synchronized void close(final TaskAttemptContext context)
      throws IOException {

    this.out.close();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param context the context
   * @param out data output stream
   */
  public ExpressionRecordWriter(final TaskAttemptContext context,
      final DataOutputStream out) {

    this.context = context;
    this.out = out;
  }

}
