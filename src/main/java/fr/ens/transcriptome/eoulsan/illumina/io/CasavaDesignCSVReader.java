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

package fr.ens.transcriptome.eoulsan.illumina.io;

import static com.google.common.collect.Lists.newArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a reader for Casava design CSV files.
 * @author Laurent Jourdren
 */
public class CasavaDesignCSVReader extends AbstractCasavaDesignTextReader {

  private BufferedReader reader;
  private final Splitter splitter = Splitter.on(',');

  @Override
  public CasavaDesign read() throws IOException {

    String line = null;

    while ((line = this.reader.readLine()) != null) {

      line = line.trim();
      if ("".equals(line))
        continue;

      line = line.replaceAll("\"", "");

      try {

        // Parse the line
        parseLine(newArrayList(splitter.split(line)));
      } catch (IOException e) {

        // If an error occurs while parsing add the line to the exception
        // message
        throw new IOException(e.getMessage() + " in line: " + line);
      }
    }

    reader.close();

    return getDesign();
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public CasavaDesignCSVReader(final InputStream is) {

    if (is == null)
      throw new NullPointerException("InputStream is null");

    this.reader = new BufferedReader(new InputStreamReader(is));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public CasavaDesignCSVReader(final File file) throws FileNotFoundException {

    if (file == null)
      throw new NullPointerException("File is null");

    if (!file.isFile())
      throw new FileNotFoundException("File not found: "
          + file.getAbsolutePath());

    this.reader = FileUtils.createBufferedReader(file);
  }

}
