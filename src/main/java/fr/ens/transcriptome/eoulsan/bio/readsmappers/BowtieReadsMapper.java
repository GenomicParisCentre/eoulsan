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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a wrapper on the Bowtie mapper. Includes only specific
 * methods of bowtie
 * @since 1.0
 * @author Laurent Jourdren
 */
public class BowtieReadsMapper extends AbstractBowtieReadsMapper {

  public static final String MAPPER_NAME = "Bowtie";
  private static final String DEFAULT_PACKAGE_VERSION = "0.12.9";
  public static final String DEFAULT_ARGUMENTS = "--best -k 2";

  private static final Version FIRST_FLAVORED_VERSION = new Version(1, 1, 0);
  private static final String MAPPER_EXECUTABLE = "bowtie";
  private static final String MAPPER_NEW_EXECUTABLE = "bowtie-align";
  private static final String INDEXER_EXECUTABLE = "bowtie-build";

  private static final String EXTENSION_INDEX_FILE = ".rev.1.ebwt";

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
  }

  @Override
  protected String getDefaultPackageVersion() {

    return DEFAULT_PACKAGE_VERSION;
  }

  @Override
  protected String getExtensionIndexFile() {

    return EXTENSION_INDEX_FILE
        + (isLongIndexFlavor(FIRST_FLAVORED_VERSION) ? "l" : "");
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.BOWTIE_INDEX_ZIP;
  }

  @Override
  protected String getIndexerExecutable() {

    return flavoredBinary(INDEXER_EXECUTABLE, FIRST_FLAVORED_VERSION);
  }

  @Override
  protected String[] getMapperExecutables() {

    return new String[] { flavoredBinary(MAPPER_EXECUTABLE,
        MAPPER_NEW_EXECUTABLE, FIRST_FLAVORED_VERSION) };
  }

  protected static final String getBowtieQualityArgument(
      final FastqFormat format) {

    switch (format) {

    case FASTQ_SOLEXA:
      return "--solexa-quals";

    case FASTQ_ILLUMINA:
    case FASTQ_ILLUMINA_1_5:
      return "--phred64-quals";

    case FASTQ_SANGER:
    default:
      return "--phred33-quals";
    }
  }

  @Override
  public String getDefaultMapperArguments() {
    return DEFAULT_ARGUMENTS;
  }

  @Override
  protected List<String> createCommonArgs(final String bowtiePath,
      final String index, final boolean inputCrossbowFormat,
      final boolean memoryMappedIndex) {

    final List<String> result = new ArrayList<>();

    // Bowtie Executable path
    result.add(bowtiePath);

    // Set the user options
    result.addAll(getListMapperArguments());

    // Set the number of threads to use
    if (!isMultipleInstancesEnabled()) {
      result.add("-p");
      result.add(getThreadsNumber() + "");
    }

    // Enable memory mapped index
    if (memoryMappedIndex) {
      result.add("--mm");
    }

    // Input Format in FASTQ or Crossbow format
    if (inputCrossbowFormat) {
      result.add(("-r"));
    } else {
      result.add(("-q"));
    }

    // Set the quality format
    result.add(bowtieQualityArgument());

    // Output in SAM format
    result.add("-S");

    // Quiet mode
    result.add("--quiet");

    // Genome index name
    result.add(index);

    return result;
  }

}
