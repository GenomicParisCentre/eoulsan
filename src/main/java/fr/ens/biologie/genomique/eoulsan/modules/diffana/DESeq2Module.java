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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.DEFAULT_SINGLE_INPUT_PORT_NAME;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.modules.diffana.DESeq2.FitType.PARAMETRIC;
import static fr.ens.biologie.genomique.eoulsan.modules.diffana.DESeq2.SizeFactorsType.RATIO;
import static fr.ens.biologie.genomique.eoulsan.modules.diffana.DESeq2.StatisticTest.WALD;
import static java.util.Collections.unmodifiableSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.bio.io.TSVCountsReader;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.modules.diffana.DESeq2.FitType;
import fr.ens.biologie.genomique.eoulsan.modules.diffana.DESeq2.SizeFactorsType;
import fr.ens.biologie.genomique.eoulsan.modules.diffana.DESeq2.StatisticTest;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;

/**
 * Class to run the differential analysis with DEseq2
 * @author Xavier Bauquet
 * @since 2.0
 */

@LocalOnly
public class DESeq2Module extends AbstractModule {

  // Module name
  public static final String MODULE_NAME = "deseq2";

  static final String DESEQ2_DOCKER_IMAGE =
      "bioconductor/release_sequencing:3.1";

  // DEseq2 options names
  private static final String NORMALIZATION_FIGURES = "norm.fig";
  private static final String DIFFANA_FIGURES = "diffana.fig";
  private static final String NORM_DIFFANA = "norm.diffana";
  private static final String DIFFANA = "diffana";
  private static final String SIZE_FACTORS_TYPE = "size.factors.type";
  private static final String FIT_TYPE = "fit.type";
  private static final String STATISTIC_TEST = "statistical.test";

  // Default value for DEseq options
  private boolean normFig = true;
  private boolean diffanaFig = true;
  private boolean normDiffana = true;
  private boolean diffana = true;
  private SizeFactorsType sizeFactorsType = RATIO;
  private FitType fitType = PARAMETRIC;
  private StatisticTest statisticTest = WALD;

  private final Set<Requirement> requirements = new HashSet<>();
  private RExecutor executor;

  //
  // Module methods
  //

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    return new InputPortsBuilder()
        .addPort(DEFAULT_SINGLE_INPUT_PORT_NAME, true, EXPRESSION_RESULTS_TSV)
        .create();
  }

  @Override
  public Set<Requirement> getRequirements() {

    return unmodifiableSet(this.requirements);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    // Parse R executor parameters
    final Set<Parameter> parameters = new HashSet<>(stepParameters);
    this.executor = RModuleCommonConfiguration.parseRExecutorParameter(context,
        parameters, this.requirements, DESEQ2_DOCKER_IMAGE);

    for (Parameter p : parameters) {

      switch (p.getName()) {

      case NORMALIZATION_FIGURES:
        this.normFig = parseBoolean(p);
        break;

      case DIFFANA_FIGURES:
        this.diffanaFig = parseBoolean(p);
        break;

      case NORM_DIFFANA:
        this.normDiffana = parseBoolean(p);
        break;

      case DIFFANA:
        this.diffana = parseBoolean(p);
        break;

      case SIZE_FACTORS_TYPE:
        this.sizeFactorsType = DESeq2.SizeFactorsType.get(p);
        break;

      case FIT_TYPE:
        this.fitType = DESeq2.FitType.get(p.getStringValue());
        break;

      case STATISTIC_TEST:
        this.statisticTest = DESeq2.StatisticTest.get(p.getStringValue());
        break;

      default:
        throw new EoulsanException(
            "Unkown parameter for step " + getName() + " : " + p.getName());
      }
    }
  }

  /**
   * Parse a boolean parameter.
   * @param p the parameter
   * @return a boolean
   * @throws EoulsanException if the parameter is not a boolean
   */
  private static boolean parseBoolean(final Parameter p)
      throws EoulsanException {

    if (p == null) {
      throw new NullPointerException("p parameter cannot be null");
    }

    String value = p.getLowerStringValue().trim();

    if (!("true".equals(value) || "false".equals(value))) {
      throw new EoulsanException("Invalid boolean value for parameter "
          + p.getName() + ": " + p.getValue());
    }

    return p.getBooleanValue();
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Get the design
    final Design design = context.getWorkflow().getDesign();

    // Get the experiment data
    final Data expressionData = context.getInputData(EXPRESSION_RESULTS_TSV);

    // Get the name of expression file by sample
    final Map<String, File> sampleFiles = new HashMap<>();
    for (Data d : expressionData.getListElements()) {
      sampleFiles.put(d.getName(), d.getDataFile().toFile());
    }

    String stepId = context.getCurrentStep().getId();

    try {

      // Check if all the counts of the expression files are not null
      if (!checkIfAllCountsAreNotNull(sampleFiles.values())) {
        throw new EoulsanException(
            "All the counts in the expression files are null");
      }

      for (Experiment e : design.getExperiments()) {

        // Do nothing if the experiment is skipped
        if (DesignUtils.isSkipped(e)) {
          continue;
        }

        // run DEseq2
        new DESeq2(this.executor, stepId, design, e, sampleFiles, this.normFig,
            this.diffanaFig, this.normDiffana, this.diffana,
            this.sizeFactorsType, this.fitType, this.statisticTest,
            context.getSettings().isSaveRscripts())
                .runDEseq2(context.getOutputDirectory());

      }
    } catch (IOException | EoulsanException e) {
      return status.createTaskResult(e,
          "Error while analysis data: " + e.getMessage());
    }

    // Write log file
    return status.createTaskResult();
  }

  /**
   * Check if all the counts of the expression are not null.
   * @param files the files to test
   * @return true if the count are not null
   * @throws IOException if an expression file cannot be read
   * @throws FileNotFoundException if an expression file cannot be found
   */
  private static boolean checkIfAllCountsAreNotNull(Collection<File> files)
      throws FileNotFoundException, IOException {

    for (File f : files) {

      try (TSVCountsReader reader = new TSVCountsReader(f)) {

        // Get the counts
        Map<String, Integer> counts = reader.read();

        for (int value : counts.values()) {

          // End of the check for the first non null value
          if (value > 0) {
            return true;
          }
        }
      }

    }

    return false;
  }

}
