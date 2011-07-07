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

package fr.ens.transcriptome.eoulsan.design.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignFactory;
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;

/**
 * This class define a design reader for limma design files.
 * @author Laurent Jourdren
 */
public class SimpleDesignReader extends InputStreamDesignReader {

  private static final String SLIDENUMBER_FIELD = "SampleNumber";
  private static final String NAME_FIELD = "Name";
  private static final String FILENAME_FIELD = "FileName";

  @Override
  public Design read() throws EoulsanIOException {

    Map<String, List<String>> data = new HashMap<String, List<String>>();
    List<String> fieldnames = new ArrayList<String>();

    try {

      setBufferedReader(new BufferedReader(new InputStreamReader(
          getInputStream(), Globals.DEFAULT_FILE_ENCODING)));

      BufferedReader br = getBufferedReader();
      final String separator = getSeparatorField();
      String line = null;

      boolean firstLine = true;
      // String ref = null;

      while ((line = br.readLine()) != null) {

        final String empty = line.trim();
        if ("".equals(empty) || empty.startsWith("#"))
          continue;

        final String[] fields = line.split(separator);

        if (firstLine) {

          for (int i = 0; i < fields.length; i++) {

            final String field = fields[i].trim();
            data.put(field, new ArrayList<String>());

            // Compatibility with old design files
            if (field.equals(FILENAME_FIELD))
              fieldnames.add(SampleMetadata.READS_FIELD);
            else
              fieldnames.add(field);
          }

          firstLine = false;
        } else {

          for (int i = 0; i < fields.length; i++) {

            final String field = fields[i].trim();

            final String fieldName = fieldnames.get(i);

            List<String> l = data.get(fieldName);

            if ((SLIDENUMBER_FIELD.equals(fieldName) || NAME_FIELD
                .equals(fieldName)) && l.contains(field))
              throw new EoulsanIOException(
                  "Invalid file format: "
                      + "SlideNumber or Name fields can't contains duplicate values");

            l.add(field);

          }

        }

      }
    } catch (IOException e) {

      throw new EoulsanIOException("Error while reading the file");
    }

    try {
      getBufferedReader().close();
    } catch (IOException e) {
      throw new EoulsanIOException("Error while closing the file"
          + e.getMessage());
    }

    if (!data.containsKey(SLIDENUMBER_FIELD))
      throw new EoulsanIOException("Invalid file format: No SlideNumber field");

    if (!fieldnames.contains(SampleMetadata.READS_FIELD))
      throw new EoulsanIOException("Invalid file format: No FileName field");

    Design design = DesignFactory.createEmptyDesign();

    List<String> names = data.get(NAME_FIELD);
    List<String> ids = data.get(SLIDENUMBER_FIELD);

    for (int i = 0; i < names.size(); i++) {

      final String name = names.get(i);
      design.addSample(name);
      design.getSample(name).setId(Integer.parseInt(ids.get(i)));
    }

    for (String fd : fieldnames) {

      if (SLIDENUMBER_FIELD.equals(fd) || NAME_FIELD.equals(fd))
        continue;

      design.addMetadataField(fd);
      List<String> descriptions = data.get(fd);

      int k = 0;
      for (String desc : descriptions)
        design.setMetadata(names.get(k++), fd, desc);

    }

    return design;
  }

  /**
   * Identify the type of the DataFile from the source.
   * @param baseDir baseDir of the source if this a file
   * @param source source to identify
   * @return a new DataFile object
   * @throws IOException
   */
  public static DataFile createDataFile(final String baseDir,
      final String source) throws IOException {

    if (source == null)
      throw new IOException("The source is null.");

    final DataFile df = new DataFile(source);
    if (!df.getProtocol().equals("file"))
      return df;

    final String src = df.getSource();
    if (src.startsWith("file:/") || src.startsWith("/"))
      return df;

    return new DataFile(new File(baseDir, source).getPath());

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws EoulsanIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public SimpleDesignReader(final File file) throws EoulsanIOException {

    super(file);
    if (file == null)
      throw new NullPointerException("The design file to read is null.");
  }

  /**
   * Public constructor
   * @param is Input stream to read
   * @throws EoulsanIOException if the stream is null
   */
  public SimpleDesignReader(final InputStream is) throws EoulsanIOException {

    super(is);
  }

  /**
   * Public constructor
   * @param filename File to read
   * @throws EoulsanIOException if the stream is null
   * @throws FileNotFoundException if the file doesn't exist
   */
  public SimpleDesignReader(final String filename) throws EoulsanIOException,
      FileNotFoundException {

    this(new FileInputStream(filename));
  }

}
