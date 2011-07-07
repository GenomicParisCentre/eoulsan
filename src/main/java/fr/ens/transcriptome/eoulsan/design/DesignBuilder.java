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

package fr.ens.transcriptome.eoulsan.design;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class allow to easyly build Design object from files paths.
 * @author Laurent Jourdren
 */
public class DesignBuilder {

  private List<File> fastqList = new ArrayList<File>();
  private File genomeFile;
  private File gffFile;

  /**
   * Add a file to the design builder
   * @param file file to add
   * @throws EoulsanException if the file does not exist
   */
  public void addFile(final File file) throws EoulsanException {

    if (file == null)
      return;

    if (!file.exists() || !file.isFile())
      throw new EoulsanException("File "
          + file + " does not exist or is not a regular file.");

    final String filename =
        StringUtils.filenameWithoutCompressionExtension(file.getName());

    final CompressionType ct =
        CompressionType.getCompressionTypeByFilename(file.getName());

    if (filename.endsWith(DataFormats.READS_FASTQ.getDefaultExtention())) {

      // Don't add previously added file
      if (!this.fastqList.contains(file))
        this.fastqList.add(file);

    } else if (filename
        .endsWith(DataFormats.GENOME_FASTA.getDefaultExtention())) {

      // Compressed genome is currently not handled
      if (ct != CompressionType.NONE)
        throw new EoulsanException(
            "Compressed genome is not currently handled.");

      this.genomeFile = file;

    } else if (filename.endsWith(DataFormats.ANNOTATION_GFF
        .getDefaultExtention())) {

      // Compressed annotation is currently not handled
      if (ct != CompressionType.NONE)
        throw new EoulsanException(
            "Compressed annotation is not currently handled.");

      this.gffFile = file;

    }
  }

  /**
   * Add a filename to the design builder
   * @param filename filename of the file to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFile(final String filename) throws EoulsanException {

    if (filename == null)
      return;

    addFile(new File(filename));
  }

  /**
   * Create design object.
   * @return a new Design object
   */
  public Design getDesign() {

    final Design result = DesignFactory.createEmptyDesign();
    final int phredOffset =
        EoulsanRuntime.getSettings().getPhredOffsetDefault();

    for (File fq : this.fastqList) {

      final String sampleName = StringUtils.basename(fq.getName());

      // Create the sample
      result.addSample(sampleName);
      final Sample s = result.getSample(sampleName);
      final SampleMetadata smd = s.getMetadata();

      // Set the fastq file of the sample
      smd.setReads(fq.toString());

      // Set the genome file if exists
      if (this.genomeFile != null)
        smd.setGenome(this.genomeFile.toString());

      // Set the Annaotion file
      if (this.gffFile != null)
        smd.setAnnotation(this.gffFile.toString());

      smd.setPhredOffset(phredOffset);
      smd.setCondition(sampleName);
      smd.setReplicatType("T");
      smd.setUUID(UUID.randomUUID().toString());

    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param filenames filenames to add
   * @throws EoulsanException if a file to add to the design does not exist or
   *           is not handled
   */
  public DesignBuilder(final String[] filenames) throws EoulsanException {

    if (filenames == null)
      return;

    for (String filename : filenames)
      addFile(filename);
  }

}
