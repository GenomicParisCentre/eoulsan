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

package fr.ens.transcriptome.eoulsan.steps.generators;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_FASTA;

import java.io.IOException;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;

/**
 * This class define a step that generate a genome mapper index.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GenomeMapperIndexGeneratorStep extends AbstractStep {

  private final SequenceReadsMapper mapper;

  @Override
  public String getName() {

    return "_genericindexgenerator";
  }

  @Override
  public String getDescription() {

    return "Generate Mapper index";
  }

  @Override
  public DataFormat[] getInputFormats() {

    return new DataFormat[] {GENOME_FASTA, GENOME_DESC_TXT};
  }

  @Override
  public DataFormat[] getOutputFormats() {

    return new DataFormat[] {this.mapper.getArchiveFormat()};
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    final long startTime = System.currentTimeMillis();

    try {

      if (design.getSampleCount() == 0)
        throw new EoulsanException("No sample found in design file.");

      final Sample s1 = design.getSamples().get(0);

      // Get the genome DataFile
      final DataFile genomeDataFile =
          context.getInputDataFile(GENOME_FASTA, s1);

      // Get the genome description DataFile
      final DataFile descDataFile =
          context.getInputDataFile(GENOME_DESC_TXT, s1);
      final GenomeDescription desc =
          GenomeDescription.load(descDataFile.open());

      // Get the output DataFile
      final DataFile mapperIndexDataFile =
          context.getOutputDataFile(this.mapper.getArchiveFormat(), s1);

      // Set mapper temporary directory
      mapper.setTempDirectory(context.getSettings().getTempDirectoryFile());

      // Create indexer
      final GenomeMapperIndexer indexer = new GenomeMapperIndexer(this.mapper);

      // Create index
      indexer.createIndex(genomeDataFile, desc, mapperIndexDataFile);

    } catch (EoulsanException e) {

      return new StepResult(context, e);
    } catch (IOException e) {

      return new StepResult(context, e);
    }

    return new StepResult(context, startTime, this.mapper.getMapperName()
        + " index creation");
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param mapperName name of the mapper
   */
  public GenomeMapperIndexGeneratorStep(final String mapperName) {

    Preconditions.checkNotNull(mapperName, "Mapper name is null");

    this.mapper =
        SequenceReadsMapperService.getInstance().getMapper(mapperName);

    Preconditions.checkNotNull(this.mapper, "Mapper name not found: "
        + mapperName);
  }

}
