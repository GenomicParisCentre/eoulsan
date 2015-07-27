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

package fr.ens.transcriptome.eoulsan.steps.mapping;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.CommonHadoop.HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;

import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilterBuilder;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define an abstract step for read filtering.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractReadsFilterStep extends AbstractStep {

  protected static final String STEP_NAME = "filterreads";

  protected static final String COUNTER_GROUP = "reads_filtering";

  private Map<String, String> readsFiltersParameters;
  private int reducerTaskCount = -1;

  //
  // Getters
  //

  /**
   * Get the parameters of the read filter.
   * @return a map with all the parameters of the filter
   */
  protected Map<String, String> getReadFilterParameters() {

    return this.readsFiltersParameters;
  }

  /**
   * Get the reducer task count.
   * @return the reducer task count
   */
  protected int getReducerTaskCount() {

    return this.reducerTaskCount;
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

    return "This step filters reads.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    return singleInputPort(READS_FASTQ);
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(READS_FASTQ);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    final MultiReadFilterBuilder filterBuilder = new MultiReadFilterBuilder();

    for (Parameter p : stepParameters) {

      // Check if the parameter is deprecated
      checkDeprecatedParameter(p, context.getCurrentStep().getId());

      switch (p.getName()) {

      case HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME:
        this.reducerTaskCount = p.getIntValueGreaterOrEqualsTo(1);

        break;

      default:
        filterBuilder.addParameter(p.getName(), p.getStringValue());
        break;
      }

    }

    // Force parameter checking
    filterBuilder.getReadFilter();

    this.readsFiltersParameters = filterBuilder.getParameters();
  }

  //
  // Other methods
  //

  /**
   * Check deprecated parameters.
   * @param parameter the parameter to check
   * @param stepId step id
   * @throws EoulsanException if the parameter is no more supported
   */
  static void checkDeprecatedParameter(final Parameter parameter,
      final String stepId) throws EoulsanException {

    if (parameter == null) {
      return;
    }

    final String stepMessage =
        stepId == null ? "" : "In the \"" + stepId + "\" step, ";

    switch (parameter.getName()) {

    case "lengthThreshold":
      throw new EoulsanException(stepMessage
          + "the parameter \"" + parameter.getName()
          + "\" is deprecated, use \"trim.length.threshold\" parameter "
          + "instead");
    case "qualityThreshold":
      throw new EoulsanException(stepMessage
          + "the parameter \"" + parameter.getName()
          + "\" is deprecated, use \"quality.threshold\" parameter instead");

    case "pairend.accept.pairend":
      throw new EoulsanException("The parameter \""
          + parameter.getName()
          + "\" is deprecated, use \"pairedend.accept.paired.end\" parameter instead");

    case "pairend.accept.singlend":
      throw new EoulsanException("The parameter \""
          + parameter.getName()
          + "\" is deprecated, use \"pairedend.accept.single.end\" parameter instead");

    case "trim.length.threshold":
      getLogger().warning(stepMessage
          + "the \"" + parameter.getName()
          + "\" parameter is deprecated and will be soon removed. "
          + "Please use \"trimpolynend\" and \"length\" filters instead");
      break;

    default:
      break;
    }
  }

  /**
   * Get the ReadFilter object.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @return a new ReadFilter object
   * @throws EoulsanException if an error occurs while initialize one of the
   *           filter
   */
  protected MultiReadFilter getReadFilter(final ReporterIncrementer incrementer,
      final String counterGroup) throws EoulsanException {

    // As filters are not thread safe, create a new MultiReadFilterBuilder
    // with a new instance of each filter
    return new MultiReadFilterBuilder(this.readsFiltersParameters)
        .getReadFilter(incrementer, counterGroup);
  }

}
