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

package fr.ens.transcriptome.eoulsan.core;

import static fr.ens.transcriptome.eoulsan.Globals.APP_BUILD_DATE;
import static fr.ens.transcriptome.eoulsan.Globals.APP_BUILD_NUMBER;
import static fr.ens.transcriptome.eoulsan.Globals.APP_NAME_LOWER_CASE;
import static fr.ens.transcriptome.eoulsan.Globals.APP_VERSION_STRING;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

/**
 * This class allow parse the parameter file.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ParamParser {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  /** Version of the format of the parameter file. */
  private static final String FORMAT_VERSION = "1.0";

  private InputStream is;

  private Map<String, String> constants = initConstants();

  /**
   * Parse the parameter file.
   * @throws EoulsanException if an error occurs while parsing file
   */
  public Command parse() throws EoulsanException {

    return parse(new Command());
  }

  /**
   * Parse the parameter file.
   * @throws EoulsanException if an error occurs while parsing file
   */
  public Command parse(final Command command) throws EoulsanException {

    if (command == null)
      throw new NullPointerException("The command is null");

    final Document doc;

    //
    // Init parser
    //

    try {

      final DocumentBuilderFactory dbFactory =
          DocumentBuilderFactory.newInstance();
      final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(this.is);
      doc.getDocumentElement().normalize();

    } catch (ParserConfigurationException e) {
      throw new EoulsanException("Error while parsing param file: "
          + e.getMessage());
    } catch (SAXException e) {
      throw new EoulsanException("Error while parsing param file: "
          + e.getMessage());
    } catch (IOException e) {
      throw new EoulsanException("Error while reading param file. "
          + e.getMessage());
    }

    final NodeList nAnalysisList = doc.getElementsByTagName("analysis");

    for (int i = 0; i < nAnalysisList.getLength(); i++) {

      Node nNode = nAnalysisList.item(i);
      if (nNode.getNodeType() == Node.ELEMENT_NODE) {

        Element eElement = (Element) nNode;

        //
        // Parse description elements
        //

        final String formatVersion = getTagValue("formatversion", eElement);
        if (formatVersion == null || !FORMAT_VERSION.equals(formatVersion))
          throw new EoulsanException(
              "Invalid version of the format of the parameter file.");

        final String name = getTagValue("name", eElement);
        command.setName(name);

        final String description = getTagValue("description", eElement);
        command.setDescription(description);

        final String author = getTagValue("author", eElement);
        command.setAuthor(author);

        //
        // Parse constants
        //

        setConstants(parseParameters(eElement, "constants", null, false));

        //
        // Parse steps
        //

        final NodeList nStepsList = eElement.getElementsByTagName("steps");

        for (int j = 0; j < nStepsList.getLength(); j++) {

          final Node nodeSteps = nStepsList.item(j);
          if (nodeSteps.getNodeType() == Node.ELEMENT_NODE) {

            final Element stepsElement = (Element) nodeSteps;
            final NodeList nStepList =
                stepsElement.getElementsByTagName("step");

            for (int k = 0; k < nStepList.getLength(); k++) {

              final Node nStepNode = nStepList.item(k);
              if (nStepNode.getNodeType() == Node.ELEMENT_NODE) {

                final Element eStepElement = (Element) nStepNode;

                String stepName = getTagValue("stepname", eStepElement);
                if (stepName == null)
                  stepName = getTagValue("name", eStepElement);
                if (stepName == null)
                  throw new EoulsanException(
                      "Step name not found in parameter file.");

                final String skip =
                    eStepElement.getAttribute("skip").trim().toLowerCase();

                if (!"true".equals(skip)) {

                  final Set<Parameter> parameters =
                      parseParameters(eStepElement, "parameters", stepName,
                          true);

                  LOGGER.info("In parameter file found "
                      + stepName + " step (parameters: " + parameters + ").");
                  command.addStep(stepName, parameters);
                }

              }
            }
          }
        }

        //
        // Parse globals parameters
        //

        command.setGlobalParameters(parseParameters(eElement, "globals", null,
            true));

      }
    }

    return command;
  }

  /**
   * Parse parameter sections
   * @param root root element to parse
   * @param elementName name of the element
   * @param evaluateValues evaluate parameters values
   * @return a set of Parameter object
   * @throws EoulsanException if the tags of the parameter are not found
   */
  private Set<Parameter> parseParameters(final Element root,
      String elementName, final String stepName, final boolean evaluateValues)
      throws EoulsanException {

    final Set<Parameter> result = new LinkedHashSet<Parameter>();

    final NodeList nList = root.getElementsByTagName(elementName);

    for (int i = 0; i < nList.getLength(); i++) {

      final Node node = nList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {

        Element element = (Element) node;
        final NodeList nParameterList =
            element.getElementsByTagName("parameter");

        for (int j = 0; j < nParameterList.getLength(); j++) {

          final Node nParameterNode = nParameterList.item(j);

          if (nParameterNode.getNodeType() == Node.ELEMENT_NODE) {

            Element eStepElement = (Element) nParameterNode;

            final String paramName = getTagValue("name", eStepElement);
            final String paramValue = getTagValue("value", eStepElement);

            if (paramName == null)
              throw new EoulsanException(
                  "<name> Tag not found in parameter section of "
                      + (stepName == null ? "global parameters" : stepName
                          + " step") + " in parameter file.");
            if (paramValue == null)
              throw new EoulsanException(
                  "<value> Tag not found in parameter section of "
                      + (stepName == null ? "global parameters" : stepName
                          + " step") + " in parameter file.");

            result.add(new Parameter(paramName, evaluateValues
                ? evaluateExpressions(paramValue, true) : paramValue));
          }
        }

      }
    }

    return result;
  }

  /**
   * Get the value of a tag
   * @param tag name of the tag
   * @param element root element
   * @return the value of the tag
   */
  private static String getTagValue(final String tag, final Element element) {

    final NodeList nl = element.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {

      final Node n = nl.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE && tag.equals(n.getNodeName()))
        return n.getTextContent();
    }

    return null;
  }

  //
  // Constants handling
  //

  /**
   * Set the constants values
   * @param parameters a set with the parameters
   * @throws EoulsanException if an error occurs while evaluating the parameters
   */
  private void setConstants(final Set<Parameter> parameters)
      throws EoulsanException {

    if (parameters == null)
      return;

    for (Parameter p : parameters)
      if (!"".equals(p.getName()))
        this.constants.put(p.getName().trim(),
            evaluateExpressions(p.getValue(), true));
  }

  /**
   * Initialize the constants values
   * @return
   */
  private static final Map<String, String> initConstants() {

    final Map<String, String> constants = new HashMap<String, String>();

    constants.put(APP_NAME_LOWER_CASE + ".version", APP_VERSION_STRING);
    constants.put(APP_NAME_LOWER_CASE + ".build.number", APP_BUILD_NUMBER);
    constants.put(APP_NAME_LOWER_CASE + ".build.date", APP_BUILD_DATE);

    constants.put("available.processors", ""
        + Runtime.getRuntime().availableProcessors());

    // Add java properties
    for (Map.Entry<Object, Object> e : System.getProperties().entrySet())
      constants.put((String) e.getKey(), (String) e.getValue());

    // Add environment properties
    for (Map.Entry<String, String> e : System.getenv().entrySet())
      constants.put(e.getKey(), e.getValue());

    return constants;
  }

  /**
   * Evaluate expression in a string.
   * @param s string in witch expression must be replaced
   * @param allowExec allow execution of code
   * @return a string with expression evaluated
   * @throws EoulsanException if an error occurs while parsing the string or
   *           executing an expression
   */
  private String evaluateExpressions(final String s, boolean allowExec)
      throws EoulsanException {

    if (s == null)
      return null;

    final StringBuilder result = new StringBuilder();

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      final int c0 = s.codePointAt(i);

      // Variable substitution
      if (c0 == '$' && i + 1 < len) {

        final int c1 = s.codePointAt(i + 1);
        if (c1 == '{') {

          final String expr = subStr(s, i + 2, '}');

          final String trimmedExpr = expr.trim();
          if (this.constants.containsKey(trimmedExpr))
            result.append(this.constants.get(trimmedExpr));

          i += expr.length() + 2;
          continue;
        }
      }

      // Command substitution
      if (c0 == '`') {
        final String expr = subStr(s, i + 1, '`');
        try {
          final String r =
              ProcessUtils.execToString(evaluateExpressions(expr, false));

          // remove last '\n' in the result
          if (r.charAt(r.length() - 1) == '\n')
            result.append(r.substring(0, r.length() - 1));
          else
            result.append(r);

        } catch (IOException e) {
          throw new EoulsanException("Error while evaluating expression \""
              + expr + "\"");
        }
        i += expr.length() + 1;
        continue;
      }

      result.appendCodePoint(c0);
    }

    return result.toString();
  }

  private String subStr(final String s, final int beginIndex,
      final int charPoint) throws EoulsanException {

    final int endIndex = s.indexOf(charPoint, beginIndex);

    if (endIndex == -1)
      throw new EoulsanException("Unexpected end of expression in \""
          + s + "\"");

    return s.substring(beginIndex, endIndex);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param file the parameter file
   * @throws FileNotFoundException if the file is not found
   */
  public ParamParser(final File file) throws FileNotFoundException {

    this(FileUtils.createInputStream(file));
  }

  /**
   * Public constructor.
   * @param file the parameter file
   * @throws IOException if an error occurs while opening the file
   */
  public ParamParser(final DataFile file) throws IOException {

    this(file.open());
  }

  /**
   * Public constructor.
   * @param is Input stream
   */
  public ParamParser(final InputStream is) {

    this.is = is;
  }

}