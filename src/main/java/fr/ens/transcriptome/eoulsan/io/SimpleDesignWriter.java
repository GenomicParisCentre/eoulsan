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
 * of the �cole Normale Sup�rieure and the individual authors.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;

/**
 * This class implements a writer for limma design files.
 * @author Laurent Jourdren
 */
public class SimpleDesignWriter extends DesignWriter {

  private BufferedWriter bw;
  private static final String SEPARATOR = "\t";
  private static final String NEWLINE = "\r\n"; // System.getProperty("line.separator");
  private boolean writeScanLabelsSettings = true;

  @Override
  public void write(final Design design) throws EoulsanIOException {

    if (design == null)
      throw new NullPointerException("Design is null");

    try {
      bw =
          new BufferedWriter(new OutputStreamWriter(getOutputStream(),
              Globals.DEFAULT_FILE_ENCODING));

      if (this.bw == null)
        throw new EoulsanIOException("No stream to write");

      List<String> metadataFields = design.getMetadataFieldsNames();

      // Write header
      bw.append("SampleNumber");
      bw.append(SEPARATOR);

      bw.append("Name");
      bw.append(SEPARATOR);

      bw.append("FileName");

      for (String f : metadataFields) {

        if (SampleMetadata.SLIDE_NUMBER_FIELD.equals(f))
          continue;

        bw.append(SEPARATOR);
        bw.append(f);
      }

      bw.append(NEWLINE);

      // Write data
      List<Sample> samples = design.getSamples();

      for (Sample s : samples) {

        bw.append(Integer.toString(s.getId()));
        bw.append(SEPARATOR);

        bw.append(s.getName());
        bw.append(SEPARATOR);

        String sourceInfo = s.getSourceInfo();
        if (sourceInfo != null)
          bw.append(sourceInfo);

        for (String f : metadataFields) {

          if (SampleMetadata.SLIDE_NUMBER_FIELD.equals(f))
            continue;

          bw.append(SEPARATOR);
          bw.append(s.getMetadata().get(f));
        }

        bw.append(NEWLINE);
      }

      bw.close();
    } catch (IOException e) {
      throw new EoulsanIOException("Error while writing stream : "
          + e.getMessage());
    }

  }

  /**
   * Test if the scan labels settings must be written.
   * @return Returns true if thq scan labels settings must be written
   */
  boolean isWriteScanLabelsSettings() {
    return writeScanLabelsSettings;
  }

  /**
   * Set if the scan labels settings must be written.
   * @param writeScanLabelsSettings true if he scan labels settings must be
   *          written
   */
  void setWriteScanLabelsSettings(final boolean writeScanLabelsSettings) {
    this.writeScanLabelsSettings = writeScanLabelsSettings;
  }

  //
  // Construtors
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws NividicIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public SimpleDesignWriter(final File file) throws EoulsanIOException {

    super(file);
  }

  /**
   * Public constructor
   * @param os Input stream to read
   * @throws NividicIOException if the stream is null
   */
  public SimpleDesignWriter(final OutputStream os) throws EoulsanIOException {
    super(os);
  }

  /**
   * Public constructor
   * @param filename File to write
   * @throws NividicIOException if the stream is null
   * @throws FileNotFoundException if the file doesn't exist
   */
  public SimpleDesignWriter(final String filename) throws EoulsanIOException,
      FileNotFoundException {

    this(new FileOutputStream(filename));
  }
}
