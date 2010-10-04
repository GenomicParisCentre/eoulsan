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

package fr.ens.transcriptome.eoulsan.steps.mgmt.upload;

import java.io.IOException;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.datasources.DataSource;

/**
 * This interface define the methods used to upload data on HDF or S3.
 * @author Laurent Jourdren
 */
public interface FileUploader {

  void init(DataSource dataSource, String dest);
  void prepare() throws IOException;
  void upload() throws IOException;
  OutputStream createUploadOutputStream() throws IOException;
  String getSrc();
  String getDest();
  
}
