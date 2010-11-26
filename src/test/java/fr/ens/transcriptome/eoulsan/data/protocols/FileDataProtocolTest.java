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

package fr.ens.transcriptome.eoulsan.data.protocols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeDebug;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.data.protocols.FileDataProtocol;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class FileDataProtocolTest {

  private static final String fileContent = "Hello, World!";

  private void writeFile(File f, String s) throws IOException {

    Writer writer = new FileWriter(f);

    writer.write(s);
    writer.close();
  }

  @Before
  public void setUp() throws Exception {

    EoulsanRuntimeDebug.initDebugEoulsanRuntime();
  }

  @Test
  public void testGetData() throws IOException {

    File f = File.createTempFile("junit-", ".toto");
    writeFile(f, fileContent);

    DataFile df = new DataFile(f.getAbsolutePath());
    InputStream is = df.open();

    ByteBuffer bb = ByteBuffer.allocate(1024);

    int i = 0;

    while ((i = is.read()) != -1)
      bb.put((byte) i);

    is.close();
    f.delete();

    assertEquals(fileContent, new String(bb.array(), 0, bb.position()));
  }

  @Test
  public void testPutDataDataFile() throws IOException {

    File f1 = File.createTempFile("junit-", ".toto");
    DataFile df = new DataFile(f1.getAbsolutePath());

    OutputStream os = df.create();
    Writer writer = new OutputStreamWriter(os);
    writer.write(fileContent);
    writer.close();

    File f2 = File.createTempFile("junit-", ".toto");
    writeFile(f2, fileContent);

    assertTrue(FileUtils.compareFile(f1, f2));

    f1.delete();
    f2.delete();
  }

  @Test
  public void testGetMetadata() throws IOException, EoulsanException {

    File f = File.createTempFile("junit-", ".toto");
    writeFile(f, fileContent);

    DataFile df = new DataFile(f.getAbsolutePath());
    DataFileMetadata md = df.getMetaData();

    assertEquals("", md.getContentType());
    assertEquals("", md.getContentEncoding());
    assertEquals(fileContent.length(), md.getContentLength());
    assertEquals(null, md.getContentMD5());
    assertEquals(null, md.getDataFormat());
    assertEquals(f.lastModified(), md.getLastModified());
    f.delete();

    f = File.createTempFile("junit-", ".txt");
    writeFile(f, fileContent);
    md = new DataFile(f.getAbsolutePath()).getMetaData();
    assertEquals("text/plain", md.getContentType());
    f.delete();

    f = File.createTempFile("junit-", ".txt.gz");
    writeFile(f, fileContent);
    md = new DataFile(f.getAbsolutePath()).getMetaData();
    assertEquals("text/plain", md.getContentType());
    assertEquals("gzip", md.getContentEncoding());
    f.delete();

    f = File.createTempFile("junit-", ".png.bz2");
    writeFile(f, fileContent);
    md = new DataFile(f.getAbsolutePath()).getMetaData();
    assertEquals("image/png", md.getContentType());
    assertEquals("bzip2", md.getContentEncoding());
    f.delete();

    DataFormatRegistry.getInstance().register(DataFormats.READS_FASTQ);

    f = new File(new File(System.getProperty("java.io.tmpdir")), "reads_1.fq");
    writeFile(f, fileContent);
    md = new DataFile(f.getAbsolutePath()).getMetaData();
    assertEquals(DataFormats.READS_FASTQ, md.getDataFormat());
    assertEquals("text/plain", md.getContentType());
    assertEquals("", md.getContentEncoding());
    f.delete();

    f =
        new File(new File(System.getProperty("java.io.tmpdir")),
            "reads_1.fq.bz2");
    writeFile(f, fileContent);
    md = new DataFile(f.getAbsolutePath()).getMetaData();
    assertEquals(DataFormats.READS_FASTQ, md.getDataFormat());
    assertEquals("text/plain", md.getContentType());
    assertEquals("bzip2", md.getContentEncoding());
    f.delete();

  }

  @Test
  public void testExists() throws IOException {

    File f = new File("/tmp/toto.txt");
    DataFile df = new DataFile(f.getAbsolutePath());

    assertFalse(f.exists());
    assertEquals(f.exists(), df.exists());

    f = File.createTempFile("junit-", ".toto");
    df = new DataFile(f.getAbsolutePath());

    writeFile(f, fileContent);
    assertTrue(f.exists());
    assertEquals(f.exists(), df.exists());

    f.delete();
    assertFalse(f.exists());
    assertEquals(f.exists(), df.exists());
  }

  @Test
  public void testGetFile() throws IOException {

    File f = new File("/tmp/toto.txt");
    DataFile df = new DataFile(f.getAbsolutePath());
    FileDataProtocol p = (FileDataProtocol) df.getProtocol();

    assertEquals(f, p.getFile(df));

  }

  @Test
  public void testIsReadable() {

    FileDataProtocol p = new FileDataProtocol();
    assertTrue(p.isReadable());

  }

  @Test
  public void testIsWritable() {

    FileDataProtocol p = new FileDataProtocol();
    assertTrue(p.isWritable());
  }

}
