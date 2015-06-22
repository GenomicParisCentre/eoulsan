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
package fr.ens.transcriptome.eoulsan.steps;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.GalaxyToolInterpreter;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.ToolData;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.ToolExecutorResult;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.elements.ToolElement;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * The Class GalaxyToolStep.
 * @author Sandrine Perrin
 * @since 2.1
 */
@LocalOnly
public class GalaxyToolStep extends AbstractStep {

  /** Tool data. */
  private ToolData toolData;

  /** The tool interpreter. */
  private GalaxyToolInterpreter toolInterpreter;

  @Override
  public String getName() {
    return this.toolData.getToolName();
  }

  public String getToolVersionName() {
    return this.toolData.getToolVersion();
  }

  @Override
  public Version getVersion() {
    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    boolean isEmpty = true;

    for (final Map.Entry<DataFormat, ToolElement> entry : this.toolInterpreter
        .getInDataFormatExpected().entrySet()) {
      isEmpty = false;

      builder.addPort(entry.getValue().getValidatedName(), entry.getKey(), true);
    }

    if (isEmpty) {
      return InputPortsBuilder.noInputPort();
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    final OutputPortsBuilder builder = new OutputPortsBuilder();
    boolean isEmpty = true;

    for (final Map.Entry<DataFormat, ToolElement> entry : this.toolInterpreter
        .getOutDataFormatExpected().entrySet()) {
      isEmpty = false;
      builder.addPort(entry.getValue().getValidatedName(), entry.getKey());

      return singleOutputPort(entry.getKey());
    }

    if (isEmpty) {
      return OutputPortsBuilder.noOutputPort();
    }

    return builder.create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    // Configure tool interpreter
    this.toolInterpreter.configure(stepParameters);

    // Extract tool data
    this.toolData = this.toolInterpreter.getToolData();

  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // TODO check in data and out data corresponding to tool.xml
    // Check DataFormat expected corresponding from stepContext

    checkArgument(
        this.toolInterpreter.checkDataFormat(context),
        "GalaxyTool step, dataFormat inval between extract from analysis and setting in xml file.");

    int exitValue = -1;
    ToolExecutorResult result = null;

    try {
      result = this.toolInterpreter.execute(context);
      exitValue = result.getExitValue();

    } catch (EoulsanException e) {
      return status.createStepResult(e,
          "Error execution tool interpreter from building tool command line : "
              + e.getMessage());
    }

    // Set the description of the context
    status.setDescription(this.toolInterpreter.getDescription());

    status.setMessage("Command line generate by python interpreter: "
        + result.getCommandLine() + ".");

    // Execution script fail, create an exception
    if (exitValue != 0) {

      return status.createStepResult(null,
          "Fail execution tool galaxy with command "
              + result.getCommandLine() + ". Exit value: " + exitValue);
    }

    if (result.asThrowedException()) {
      final Throwable e = result.getException();

      return status.createStepResult(e,
          "Error execution interrupted: " + e.getMessage());
    }

    return status.createStepResult();

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param toolXMLis the input stream on tool xml file
   * @throws EoulsanException the Eoulsan exception
   */
  public GalaxyToolStep(final InputStream toolXMLis) throws EoulsanException {

    this.toolInterpreter =
        new GalaxyToolInterpreter(toolXMLis);
  }

}
