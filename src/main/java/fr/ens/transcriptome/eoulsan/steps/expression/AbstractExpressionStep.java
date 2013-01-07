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

package fr.ens.transcriptome.eoulsan.steps.expression;

import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.ExpressionCounter;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.ExpressionCounterService;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;

/**
 * This abstract class define and parse arguments for the expression step.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 * @author Claire Wallon
 */
public abstract class AbstractExpressionStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final String GENOMIC_TYPE_PARAMETER_NAME = "genomictype";
  public static final String ATTRIBUTE_ID_PARAMETER_NAME = "attributeid";

  protected static final String COUNTER_GROUP = "expression";

  private static final String STEP_NAME = "expression";
  private static final String DEFAULT_GENOMIC_TYPE = "exon";
  private static final String DEFAULT_ATTRIBUTE_ID = "PARENT";

  private String genomicType = DEFAULT_GENOMIC_TYPE;
  private String attributeId = DEFAULT_ATTRIBUTE_ID;
  private String tmpDir;

  private ExpressionCounter counter;
  private String stranded = "no";
  private String overlapmode = "union";
  private boolean removeAmbiguousCases = true;

  //
  // Getters
  //

  /**
   * Get the genomic type.
   * @return Returns the genomicType
   */
  protected String getGenomicType() {
    return this.genomicType;
  }

  /**
   * Get the attribute id.
   * @return Returns the attribute id
   */
  protected String getAttributeId() {
    return this.attributeId;
  }

  /**
   * Get the name of the counter to use.
   * @return Returns the counterName
   */
  protected String getCounterName() {
    return this.counter.getCounterName();
  }

  /**
   * Get the stranded mode
   * @return the stranded mode as a String
   */
  protected String getStranded() {
    return this.stranded;
  }

  /**
   * Get the overlap mode
   * @return the overlap mode as a String
   */
  protected String getOverlapMode() {
    return this.overlapmode;
  }

  /**
   * Get the ambiguous case mode
   * @return the ambiguous case mode
   */
  protected boolean isRemoveAmbiguousCases() {
    return this.removeAmbiguousCases;
  }

  /**
   * Get the counter object.
   * @return the counter object
   */
  protected ExpressionCounter getCounter() {

    return this.counter;
  }

  /**
   * Get the temporary directory
   * @return Returns the tmpDir
   */
  protected String getTmpDir() {
    return tmpDir;
  }

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This step compute the expression.";
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {DataFormats.FILTERED_MAPPER_RESULTS_SAM,
        DataFormats.ANNOTATION_GFF, DataFormats.GENOME_DESC_TXT};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {DataFormats.EXPRESSION_RESULTS_TXT};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    String counterName = null;

    for (Parameter p : stepParameters) {

      if (GENOMIC_TYPE_PARAMETER_NAME.equals(p.getName()))
        this.genomicType = p.getStringValue();
      else if (ATTRIBUTE_ID_PARAMETER_NAME.equals(p.getName()))
        this.attributeId = p.getStringValue();
      else if ("counter".equals(p.getName()))
        counterName = p.getStringValue();
      else if ("stranded".equals(p.getName()))
        this.stranded = p.getStringValue();
      else if ("overlapmode".equals(p.getName()))
        this.overlapmode = p.getStringValue();
      else if ("removeambiguouscases".equals(p.getName()))
        this.removeAmbiguousCases = p.getBooleanValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    if (this.genomicType == null)
      throw new EoulsanException("Parent type not set for "
          + getName() + " step.");

    if (this.attributeId == null)
      throw new EoulsanException("Attribute id not set for "
          + getName() + " step.");

    if (counterName == null)
      counterName = "eoulsanCounter";

    this.counter =
        ExpressionCounterService.getInstance().getCounter(counterName);

    if (this.counter == null) {
      throw new EoulsanException("Unknown counter: " + counterName);
    }

    // Set temporary directory
    this.tmpDir = EoulsanRuntime.getRuntime().getSettings().getTempDirectory();

    // Log Step parameters
    LOGGER.info("In "
        + getName() + ", counter=" + this.counter.getCounterName());
    LOGGER.info("In "
        + getName() + ", stranded=" + this.stranded + ", overlapmode="
        + this.overlapmode);
  }
}
