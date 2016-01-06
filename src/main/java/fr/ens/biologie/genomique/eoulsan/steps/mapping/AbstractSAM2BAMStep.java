package fr.ens.biologie.genomique.eoulsan.steps.mapping;

import static fr.ens.biologie.genomique.eoulsan.core.CommonHadoop.HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_BAM;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_INDEX_BAI;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.steps.AbstractStep;
import fr.ens.biologie.genomique.eoulsan.steps.Steps;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * This class define a step for converting SAM files into BAM.
 * @since 2.0
 * @author Laurent Jourdren
 */
public abstract class AbstractSAM2BAMStep extends AbstractStep {

  private static final String STEP_NAME = "sam2bam";
  private static final int DEFAULT_COMPRESSION_LEVEL = 5;

  protected static final String COUNTER_GROUP = "sam2bam";

  private int compressionLevel = DEFAULT_COMPRESSION_LEVEL;
  private int reducerTaskCount = -1;

  //
  // Getters
  //

  /**
   * Get the compression level to use.
   * @return the compression level to use
   */
  protected int getCompressionLevel() {
    return this.compressionLevel;
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

    return "This step sam convert SAM files to BAM files.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    return singleInputPort(MAPPER_RESULTS_SAM);
  }

  @Override
  public OutputPorts getOutputPorts() {

    return new OutputPortsBuilder().addPort("bam", MAPPER_RESULTS_BAM)
        .addPort("bai", MAPPER_RESULTS_INDEX_BAI).create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "compression.level":
        this.compressionLevel = p.getIntValueInRange(0, 9);
        break;

      case "input.format":
        Steps.deprecatedParameter(context, p, true);
        break;

      case HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME:
        this.reducerTaskCount = p.getIntValueGreaterOrEqualsTo(1);
        break;

      default:
        Steps.unknownParameter(context, p);
      }
    }
  }

}
