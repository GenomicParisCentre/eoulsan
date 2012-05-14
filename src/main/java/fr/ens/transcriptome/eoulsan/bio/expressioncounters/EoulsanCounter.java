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

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.expression.FinalExpressionTranscriptsCreator;
import fr.ens.transcriptome.eoulsan.steps.expression.local.ExpressionPseudoMapReduce;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * @since 1.2
 * @author Claire Wallon
 */
public class EoulsanCounter extends AbstractExpressionCounter {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String COUNTER_NAME = "eoulsanCounter";

  @Override
  public String getCounterName() {

    return COUNTER_NAME;
  }

  @Override
  protected void internalCount(File alignmentFile, DataFile annotationFile,
      File expressionFile, final DataFile genomeDescFile, Reporter reporter,
      String counterGroup) throws IOException {

    ExpressionPseudoMapReduce epmr = null;
    String lastAnnotationKey = null;
    final String genomicType = getGenomicType();

    final String annotationKey =
        annotationFile.getName() + " " + getGenomicType();

    // Get expression temporary file
    final File expressionTmpFile =
        new File(alignmentFile.getAbsolutePath() + ".tmp");

    try {

      if (!annotationKey.equals(lastAnnotationKey)) {
        epmr =
            new ExpressionPseudoMapReduce(annotationFile.open(), genomicType,
                genomeDescFile.open(), counterGroup);
        lastAnnotationKey = annotationKey;
      }

      if (getTempDirectory() != null)
        epmr.setMapReduceTemporaryDirectory(new File(getTempDirectory()));
      epmr.doMap(alignmentFile);
      epmr.doReduce(expressionTmpFile);

      final FinalExpressionTranscriptsCreator fetc =
          new FinalExpressionTranscriptsCreator(
              epmr.getTranscriptAndExonFinder());

      fetc.initializeExpressionResults();
      fetc.loadPreResults(expressionTmpFile, epmr.getReporter()
          .getCounterValue(counterGroup, "reads used"));
      fetc.saveFinalResults(expressionFile);

      // Remove expression Temp file
      if (!expressionTmpFile.delete())
        LOGGER.warning("Can not delete expression temporary file: "
            + expressionTmpFile.getAbsolutePath());

    } catch (BadBioEntryException e) {
      // exit("Invalid annotation entry: " + e.getEntry());
    }
  }

}
