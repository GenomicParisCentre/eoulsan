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

package fr.ens.transcriptome.eoulsan.galaxytools;

import static fr.ens.transcriptome.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractInputs;
import static fr.ens.transcriptome.eoulsan.galaxytools.GalaxyToolXMLParserUtils.extractOutputs;
import static org.python.google.common.base.Preconditions.checkNotNull;
import static org.python.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.workflow.FileNaming;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.galaxytools.elements.ToolElement;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class create an interpreter to tool xml file from Galaxy.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class GalaxyToolInterpreter {

  /** The tool xm lis. */
  private final InputStream toolXMLis;

  // Throw an exception if tag exist in tool file
  /** The Constant TAG_FORBIDDEN. */
  private final static Set<String> TAG_FORBIDDEN = Sets.newHashSet("repeat");

  // Set DOM related to the tool XML file
  /** The doc. */
  private final Document doc;

  /** Data from tool XML. */
  private Map<String, ToolElement> inputs;

  /** The outputs. */
  private Map<String, ToolElement> outputs;

  private ElementPorts inputPorts;
  private ElementPorts outputPorts;

  /** The step parameters. */
  private final Map<String, Parameter> stepParameters;

  /** The tool. */
  private final ToolData tool;

  private boolean isConfigured = false;
  private boolean isExecuted = false;

  //
  // Inner classes
  //

  /**
   * This inner class define the link between an Element an Eoulsan step port.
   */
  private static final class ElementPort {

    private final ToolElement element;

    private final String portName;

    private final int fileIndex;

    /**
     * Get the DataFile linked to the Element.
     * @param context Step context
     * @return a DataFile object
     */
    public DataFile getInputDataFile(final StepContext context) {

      final Data data = context.getInputData(this.portName);

      return this.fileIndex == -1
          ? data.getDataFile() : data.getDataFile(this.fileIndex);
    }

    /**
     * Get the DataFile linked to the Element.
     * @param context Step context
     * @return a DataFile object
     */
    public DataFile getOutputDataFile(final StepContext context,
        final Data inData) {

      final Data data = context.getOutputData(this.portName, inData);

      return this.fileIndex == -1
          ? data.getDataFile() : data.getDataFile(this.fileIndex);
    }

    /**
     * Constructor.
     * @param element Tool element
     * @param portName Eoulsan port name
     * @param fileIndex file index
     */
    public ElementPort(final ToolElement element, final String portName,
        final int fileIndex) {

      this.element = element;
      this.portName = portName;
      this.fileIndex = fileIndex;
    }
  }

  /**
   * This inner class define a collection of ElementPorts.
   */
  private static final class ElementPorts {

    private Map<String, ElementPort> ports = new HashMap<>();

    /**
     * Get an ElementPort from its name.
     * @param elementName the name of the element port
     * @return an ElementPort or null if the element name does not exists
     */
    public ElementPort getPortElements(final String elementName) {

      return this.ports.get(elementName);
    }

    /**
     * Get the ToolElement objects that will be used to create the Eoulsan step
     * ports. Only one of the port of multi-files DataFormat are kept.
     * @return a set of ToolElement
     */
    public Set<ToolElement> getStepElements() {

      final Set<ToolElement> result = new HashSet<>();

      for (ElementPort e : ports.values()) {

        if (e.fileIndex < 1) {
          result.add(e.element);
        }
      }

      return Collections.unmodifiableSet(result);
    }

    /**
     * Sort ToolElements.
     * @param elements element to sort
     * @return a sorted list of ToolElement
     */
    private static List<ToolElement> sortedElements(
        final Collection<ToolElement> elements) {

      final List<ToolElement> elementsSorted = new ArrayList<>(elements);
      Collections.sort(elementsSorted, new Comparator<ToolElement>() {

        @Override
        public int compare(ToolElement o1, ToolElement o2) {

          return o1.getName().compareTo(o2.getName());
        }
      });

      return Collections.unmodifiableList(elementsSorted);
    }

    /**
     * Constructor.
     * @param elements the element
     */
    public ElementPorts(final Map<String, ToolElement> elements) {

      final Multiset<DataFormat> formatCount = HashMultiset.create();
      final Map<DataFormat, String> formatPortNames = new HashMap<>();

      for (ToolElement e : sortedElements(elements.values())) {

        // Discard parameters
        if (!e.isFile()) {
          continue;
        }

        final DataFormat format = e.getDataFormat();

        if (format.getMaxFilesCount() == 1) {
          this.ports.put(e.getName(),
              new ElementPort(e, e.getValidatedName(), -1));
        } else {

          // If the DataFormat of the element is multi-file, only keep one
          // element for Eoulsan step ports

          final String portName;

          if (formatPortNames.containsKey(format)) {

            portName = formatPortNames.get(format);
          } else {

            portName = e.getValidatedName();
            formatPortNames.put(format, portName);
          }

          this.ports.put(e.getName(),
              new ElementPort(e, portName, formatCount.count(format)));
          formatCount.add(format);
        }
      }
    }
  }

  /**
   * Parse tool file to extract useful data to run tool.
   * @param setStepParameters the set step parameters
   * @throws EoulsanException if an data missing
   */
  public void configure(final Set<Parameter> setStepParameters)
      throws EoulsanException {

    checkState(!isConfigured,
        "GalaxyToolStep, this instance has been already configured");

    this.initStepParameters(setStepParameters);

    final Document localDoc = this.doc;

    // Extract variable settings
    this.inputs = extractInputs(localDoc, this.stepParameters);
    this.outputs = extractOutputs(localDoc);

    this.inputPorts = new ElementPorts(this.inputs);
    this.outputPorts = new ElementPorts(this.outputs);

    isConfigured = true;
  }

  /**
   * Convert command tag from tool file in string, variable are replace by
   * value.
   * @param context the context
   * @return the string
   * @throws EoulsanException the Eoulsan exception
   */
  public ToolExecutorResult execute(final StepContext context)
      throws EoulsanException {

    checkState(!isExecuted,
        "GalaxyToolStep, this instance has been already executed");

    context.getLogger().info("Parsing xml file successfully.");
    context.getLogger().info("Tool description " + this.tool);

    final int variablesCount = this.inputs.size() + this.outputs.size();
    final Map<String, String> variables = new HashMap<>(variablesCount);

    Data inData = null;

    // Extract from inputs variable command
    for (final ToolElement ptg : this.inputs.values()) {

      if (ptg.isFile()) {

        final ElementPort inPort =
            this.inputPorts.getPortElements(ptg.getName());

        // Extract value from context from DataFormat
        final Data data = context.getInputData(inPort.portName);

        if (inData == null || isDataNameInDesign(inData, context)) {
          inData = data;
        }

        final DataFile inFile = inPort.getInputDataFile(context);
        variables.put(ptg.getName(), inFile.toFile().getAbsolutePath());

      } else {
        // Variables setting with parameters file
        variables.put(ptg.getName(), ptg.getValue());
      }
    }

    // Extract from outputs variable command
    for (final ToolElement ptg : this.outputs.values()) {

      if (ptg.isFile()) {

        final ElementPort outPort =
            this.outputPorts.getPortElements(ptg.getName());

        // Extract value from context from DataFormat
        final DataFile outFile = outPort.getOutputDataFile(context, inData);
        variables.put(ptg.getName(), outFile.toFile().getAbsolutePath());
      } else {
        // Variables setting with parameters file
        variables.put(ptg.getName(), ptg.getValue());
      }
    }

    if (variables.isEmpty()) {
      throw new EoulsanException("No parameter settings.");
    }

    context.getLogger().info("Tool variable settings  "
        + Joiner.on("\t").withKeyValueSeparator("=").join(variables));

    // Create the Cheetah interpreter
    final CheetahInterpreter cheetahInterpreter =
        new CheetahInterpreter(this.tool.getCommandScript(), variables);

    final String commandLine = cheetahInterpreter.execute();

    // Create the executor and interpret the command tag
    final ToolExecutor executor =
        new ToolExecutor(context, this.tool, commandLine);

    // Execute the command
    final ToolExecutorResult result = executor.execute();

    isExecuted = true;

    // TODO
    return result;
  }

  public String getDescription() {

    return "Launch tool galaxy "
        + this.tool.getToolName() + ", version " + this.tool.getToolVersion()
        + " with interpreter " + this.tool.getInterpreter();
  }

  //
  // Private methods
  //

  /**
   * Convert set parameters in map with name parameter related parameter.
   * @param setStepParameters the set step parameters
   */
  private void initStepParameters(final Set<Parameter> setStepParameters) {

    // Convert Set in Map
    for (final Parameter p : setStepParameters) {
      this.stepParameters.put(p.getName(), p);
    }
  }

  /**
   * Create DOM instance from tool xml file.
   * @return DOM instance
   * @throws EoulsanException if an error occurs during creation instance
   */
  private Document buildDOM() throws EoulsanException {

    try (InputStream in = this.toolXMLis) {
      // Read the XML file
      final DocumentBuilder dBuilder =
          DocumentBuilderFactory.newInstance().newDocumentBuilder();
      final Document doc = dBuilder.parse(in);
      doc.getDocumentElement().normalize();
      return doc;

    } catch (final IOException | SAXException
        | ParserConfigurationException e) {
      throw new EoulsanException(e);
    }
  }

  /**
   * Check DOM validity.
   * @throws EoulsanException the Eoulsan exception
   */
  private void checkDomValidity() throws EoulsanException {

    final Document localDoc = this.doc;

    for (final String tag : TAG_FORBIDDEN) {

      // Check tag exists in tool file
      if (!XMLUtils.getElementsByTagName(localDoc, tag).isEmpty()) {
        // Throw exception
        throw new EoulsanException("Parsing tool xml: unsupported tag " + tag);
      }
    }
  }

  /**
   * Test if a data name is a sample name.
   * @param data the data to test
   * @param context the step context
   * @return true the data name is a sample name
   */
  private boolean isDataNameInDesign(final Data data,
      final StepContext context) {

    final String dataName = data.getName();

    for (Sample sample : context.getWorkflow().getDesign().getSamples()) {

      // TODO Change sample.getName() to sample.getId() with the new Design API
      if (FileNaming.toValidName(sample.getName()).equals(dataName)) {
        return true;
      }
    }

    return false;
  }

  //
  // Getters
  //

  /**
   * Gets the inputs.
   * @return the inputs
   */
  public Map<String, ToolElement> getInputs() {
    return this.inputs;
  }

  /**
   * Gets the outputs.
   * @return the outputs
   */
  public Map<String, ToolElement> getOutputs() {
    return this.outputs;
  }

  /**
   * Gets the in data format expected associated with variable found in command
   * line.
   * @return the in data format expected
   */
  public Set<ToolElement> getInputDataElements() {
    return this.inputPorts.getStepElements();
  }

  /**
   * Gets the out data format expected associated with variable found in command
   * line.
   * @return the out data format expected
   */
  public Set<ToolElement> getOutputDataElements() {
    return this.outputPorts.getStepElements();
  }

  /**
   * Gets the tool data.
   * @return the tool data
   */
  public ToolData getToolData() {
    return this.tool;
  }

  @Override
  public String toString() {
    return "InterpreterToolGalaxy \n[inputs="
        + Joiner.on("\n").withKeyValueSeparator("=").join(this.inputs)
        + ", \noutputs="
        + Joiner.on("\n").withKeyValueSeparator("=").join(this.outputs)
        + ", \ntool=" + this.tool + "]";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param is the input stream
   * @throws EoulsanException the Eoulsan exception
   */
  public GalaxyToolInterpreter(final InputStream is) throws EoulsanException {

    checkNotNull(is, "input stream on XML file");

    this.toolXMLis = is;
    this.doc = buildDOM();
    this.stepParameters = new HashMap<>();

    this.tool = new ToolData(this.doc);

    checkDomValidity();

  }
}
