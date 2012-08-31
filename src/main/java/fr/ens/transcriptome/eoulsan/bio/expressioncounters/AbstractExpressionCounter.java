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

package fr.ens.transcriptome.eoulsan.bio.expressioncounters;

import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingStandardFile;
import static fr.ens.transcriptome.eoulsan.util.Utils.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This abstract class implements a generic Expression Counter.
 * @since 1.2
 * @author Claire Wallon
 */
public abstract class AbstractExpressionCounter implements ExpressionCounter {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private String genomicType;
  private StrandUsage stranded;
  private OverlapMode overlapMode;
  private Reporter reporter;
  private String counterGroup;
  private String tempDir = EoulsanRuntime.getSettings().getTempDirectory();

  //
  // Getters
  //

  @Override
  public StrandUsage getStranded() {
    return this.stranded;
  }

  @Override
  public OverlapMode getOverlapMode() {
    return this.overlapMode;
  }

  @Override
  public String getTempDirectory() {
    return this.tempDir;
  }

  @Override
  public String getGenomicType() {
    return this.genomicType;
  }

  //
  // Setters
  //

  @Override
  public void setStranded(final String stranded) {

    setStranded(StrandUsage.getStrandUsageFromName(stranded));
  }

  @Override
  public void setStranded(final StrandUsage stranded) {

    if (stranded == null)
      this.stranded = StrandUsage.NO;
    else
      this.stranded = stranded;
  }

  @Override
  public void setOverlapMode(final String mode) {

    setOverlapMode(OverlapMode.getOverlapModeFromName(mode));
  }

  @Override
  public void setOverlapMode(final OverlapMode mode) {

    if (mode == null)
      this.overlapMode = OverlapMode.UNION;
    else
      this.overlapMode = mode;
  }

  @Override
  public void setTempDirectory(final String tempDirectory) {

    this.tempDir = tempDirectory;
  }

  @Override
  public void setGenomicType(final String genomicType) {

    this.genomicType = genomicType;
  }

  //
  // Counting
  //

  @Override
  public final void count(final File alignmentFile,
      final DataFile annotationFile, final File expressionFile,
      final DataFile genomeDescFile) throws IOException, EoulsanException,
      BadBioEntryException {

    LOGGER.fine("Counting with " + getCounterName());

    checkNotNull(alignmentFile, "alignmentFile is null");
    checkNotNull(annotationFile, "annotationFile is null");
    checkNotNull(expressionFile, "expressionFile is null");
    checkExistingStandardFile(alignmentFile,
        "alignmentFile not exits or is not a standard file.");

    // Process to counting
    internalCount(alignmentFile, annotationFile, expressionFile,
        genomeDescFile, this.reporter, this.counterGroup);
  }

  /**
   * This method runs the ExpressionCounter.
   * @param alignmentFile : file containing SAM alignments
   * @param annotationFile : file containing the reference genome annotation
   * @param expressionFile : output file for the expression step
   * @param genomeDescFile : file containing the genome description
   * @param reporter : the Reporter object of the Eoulsan run
   * @param counterGroup : string with the counter name group for the expression
   *          step
   * @throws IOException
   */
  protected abstract void internalCount(final File alignmentFile,
      final DataFile annotationFile, final File expressionFile,
      final DataFile genomeDescFile, Reporter reporter, String counterGroup)
      throws IOException, EoulsanException, BadBioEntryException;

  //
  // Init
  //

  @Override
  public void init(final String genomicType, final Reporter reporter,
      final String counterGroup) {

    checkNotNull(reporter, "reporter is null");
    checkNotNull(counterGroup, "counterGroup is null");

    this.genomicType = genomicType;
    this.reporter = reporter;
    this.counterGroup = counterGroup;
  }

}
