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

package fr.ens.transcriptome.eoulsan.illumina;

import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeNames;
import static fr.ens.transcriptome.eoulsan.util.XMLUtils.getAttributeValue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.XMLUtils;

/**
 * This class handle the content of the RTA summary.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class RTAReadSummary implements Iterable<RTALaneSummary> {

  private int id;
  private String type;
  private double densityRatio;
  private final List<RTALaneSummary> lanes = new ArrayList<>();

  //
  // Getters
  //

  /**
   * Get the id of the read
   * @return Returns the id
   */
  public int getId() {
    return this.id;
  }

  /**
   * Return the type of the read
   * @return Returns the type
   */
  public String getType() {
    return this.type;
  }

  /**
   * Get the density ration for the read
   * @return Returns the densityRatio
   */
  public double getDensityRatio() {
    return this.densityRatio;
  }

  //
  // Parser
  //

  public void parse(final File file)
      throws ParserConfigurationException, SAXException, IOException {

    parse(FileUtils.createInputStream(file));
  }

  public void parse(final InputStream is)
      throws ParserConfigurationException, SAXException, IOException {

    final Document doc;

    final DocumentBuilderFactory dbFactory =
        DocumentBuilderFactory.newInstance();
    final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    doc = dBuilder.parse(is);
    doc.getDocumentElement().normalize();

    parse(doc);

    is.close();
  }

  private void parse(final Document document) {

    for (Element e : XMLUtils.getElementsByTagName(document, "Summary")) {

      // Parse Summary tag attributes
      for (String attributeName : getAttributeNames(e)) {

        switch (attributeName) {
        case "Read":
          this.id = Integer.parseInt(getAttributeValue(e, attributeName));
          break;
        case "ReadType":
          this.type = getAttributeValue(e, attributeName);
          break;
        case "densityRatio":
          this.densityRatio =
              Double.parseDouble(getAttributeValue(e, attributeName));
          break;
        }
      }

      // Parse Lane tag
      for (Element laneElement : XMLUtils.getElementsByTagName(e, "Lane")) {

        final RTALaneSummary lane =
            new RTALaneSummary(this.id, this.densityRatio);
        lane.parse(laneElement);
        this.lanes.add(lane);
      }

    }

  }

  //
  // Iterable method
  //

  @Override
  public Iterator<RTALaneSummary> iterator() {

    return this.lanes.iterator();
  }

  //
  // Object method
  //

  @Override
  public String toString() {

    return this.getClass().getSimpleName()
        + "{id=" + this.id + ", type=" + this.type + ", densityRatio="
        + this.densityRatio + ", lane=" + this.lanes + "}";
  }

}
