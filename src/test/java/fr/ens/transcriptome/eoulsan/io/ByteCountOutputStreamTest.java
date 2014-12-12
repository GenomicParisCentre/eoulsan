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

package fr.ens.transcriptome.eoulsan.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Random;

import org.junit.Test;

public class ByteCountOutputStreamTest {

  @Test
  public void test() {

    try {

      testString1("Il est beau le soleil.");
      testString2("Il est beau le soleil.");

      final Random rand = new Random(System.currentTimeMillis());
      final StringBuilder sb = new StringBuilder();

      for (int i = 0; i < 100; i++) {

        sb.setLength(0);
        final int count = rand.nextInt(100000);
        for (int y = 0; y < count; y++) {
          sb.append('1');
        }

        final String s = sb.toString();
        testString1(s);
        testString2(s);

      }

    } catch (IOException e) {
      assertTrue(false);
    }

  }

  private void testString1(final String s) throws IOException {

    final byte[] bytes = s.getBytes();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    final ByteCountOutputStream bcos = new ByteCountOutputStream(baos);

    bcos.write(bytes);

    bcos.close();

    assertEquals(s, baos.toString());
    assertEquals(s.getBytes().length, bcos.getBytesNumberWritten());
  }

  private void testString2(final String s) throws IOException {

    final byte[] bytes = s.getBytes();

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    final ByteCountOutputStream bcos =
        new ByteCountOutputStream(baos, bytes.length);

    Writer writer = new OutputStreamWriter(bcos);

    writer.write(s);
    writer.close();
  }

}
