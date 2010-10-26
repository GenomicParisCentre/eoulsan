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

package fr.ens.transcriptome.eoulsan.steps.anadiff.local;

import java.io.File;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.datatypes.DataTypes;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.anadiff.AnaDiff;

/**
 * This class define the step of differential analysis in local mode.
 * @author Laurent Jourdren
 */
public class AnaDiffLocalMain implements Step {

  private static final String STEP_NAME = "anadiff";

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This class compute the differential analysis for the experiment.";
  }

  @Override
  public DataType[] getInputTypes() {
    return new DataType[] {DataTypes.EXPRESSION_RESULTS};
  }

  @Override
  public DataType[] getOutputType() {
    return new DataType[] {DataTypes.ANADIF_RESULTS};
  }

  @Override
  public String getLogName() {

    return STEP_NAME;
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) {
  }

  @Override
  public StepResult execute(final Design design, final ExecutorInfo info) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      final AnaDiff ad = new AnaDiff(design, new File("."));

      ad.run();

      // Write log file
      return new StepResult(this, startTime, log.toString());

    } catch (EoulsanException e) {

      return new StepResult(this, e, "Error while analysis data: "
          + e.getMessage());
    }

  }

}
