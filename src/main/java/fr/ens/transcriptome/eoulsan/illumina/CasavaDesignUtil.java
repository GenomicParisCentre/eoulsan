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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.illumina.io.AbstractCasavaDesignTextReader;

/**
 * This class contains utilty methods for Casava design.
 * @since 1.1
 * @author Laurent Jourdren
 */
public final class CasavaDesignUtil {

  /**
   * Check a Casava design object.
   * @param design Casava design object to check
   * @return true if the design is valid
   * @throws EoulsanException if the design is not valid
   */
  public static boolean checkCasavaDesign(final CasavaDesign design)
      throws EoulsanException {

    return checkCasavaDesign(design, null);
  }

  /**
   * Check a Casava design object.
   * @param design Casava design object to check
   * @param flowCellId flow cell id
   * @return true if the design is valid
   * @throws EoulsanException if the design is not valid
   */
  public static boolean checkCasavaDesign(final CasavaDesign design,
      final String flowCellId) throws EoulsanException {

    if (design == null)
      return false;

    if (design.size() == 0)
      throw new EoulsanException("No samples found in the design.");

    String fcid = null;
    boolean first = true;

    final Map<Integer, Set<String>> indexes =
        new HashMap<Integer, Set<String>>();
    final Set<String> sampleIds = new HashSet<String>();
    final Set<Integer> laneWithIndexes = new HashSet<Integer>();
    final Set<Integer> laneWithoutIndexes = new HashSet<Integer>();

    for (CasavaSample sample : design) {

      // Check if all the fields are not empty
      checkFCID(sample.getFlowCellId());

      // Check if the flowcell id is the flow cell expected
      if (flowCellId != null
          && !flowCellId.trim().equals(sample.getFlowCellId()))
        throw new EoulsanException("Bad flowcell name found: "
            + sample.getFlowCellId() + " (" + flowCellId + " expected).");

      // Check if all the samples had the same flow cell id
      if (first) {
        fcid = sample.getFlowCellId();
        first = false;
      } else {

        if (!fcid.equals(sample.getFlowCellId()))
          throw new EoulsanException("Two differents flow cell id found: "
              + fcid + " and " + sample.getFlowCellId() + ".");
      }

      // Check the lane number
      if (sample.getLane() < 1 || sample.getLane() > 8)
        throw new EoulsanException("Invalid lane number found: "
            + sample.getLane() + ".");

      // Check if the sample is null or empty
      checkSampleId(sample.getSampleId(), sampleIds);

      // Check sample reference
      if (isNullOrEmpty(sample.getSampleRef()))
        throw new EoulsanException(
            "Found a null or empty sample reference for sample: "
                + sample.getSampleId() + ".");

      // Check index
      checkIndex(sample.getIndex());

      // Check the description
      if (isNullOrEmpty(sample.getDescription()))
        throw new EoulsanException(
            "Found a null or empty description for sample: "
                + sample.getSampleId() + ".");

      // Check recipe
      if (isNullOrEmpty(sample.getRecipe()))
        throw new EoulsanException("Found a null or empty recipe for sample: "
            + sample.getSampleId() + ".");

      // Check operator
      if (isNullOrEmpty(sample.getOperator()))
        throw new EoulsanException(
            "Found a null or empty operator for sample: "
                + sample.getSampleId() + ".");

      // Check sample project
      checkSampleProject(sample.getSampleProject());

      final String index = sample.getIndex();
      final int lane = sample.getLane();

      // Check if mixing lane with index and lanes without index
      if (index == null || "".equals(index.trim())) {

        if (laneWithoutIndexes.contains(lane))
          throw new EoulsanException(
              "Found two samples without index for the same lane: "
                  + lane + ".");

        if (laneWithIndexes.contains(lane))
          throw new EoulsanException(
              "Found a lane with indexed and non indexed samples: "
                  + lane + ".");

        laneWithoutIndexes.add(lane);
      } else {

        if (laneWithoutIndexes.contains(lane))
          throw new EoulsanException(
              "Found a lane with indexed and non indexed samples: "
                  + lane + ".");
        laneWithIndexes.add(lane);
      }

      // check if a lane has not two or more same indexes
      if (indexes.containsKey(lane)) {

        if (indexes.get(lane).contains(index))
          throw new EoulsanException(
              "Found a lane with two time the same index: "
                  + lane + " (" + index + ").");

      } else
        indexes.put(lane, new HashSet<String>());

      indexes.get(lane).add(index);
    }

    return true;
  }

  private static void checkFCID(final String fcid) throws EoulsanException {

    if (isNullOrEmpty(fcid))
      throw new EoulsanException("Flow cell id is null or empty.");

    for (int i = 0; i < fcid.length(); i++) {

      final char c = fcid.charAt(i);
      if (!(Character.isLetterOrDigit(c)))
        throw new EoulsanException(
            "Invalid flow cell id, only letters or digits are allowed : "
                + fcid + ".");
    }

  }

  private static void checkSampleId(final String sampleId,
      final Set<String> sampleIds) throws EoulsanException {

    // Check if null of empty
    if (isNullOrEmpty(sampleId))
      throw new EoulsanException("Found a null or empty sample id.");

    // Check for forbidden characters
    for (int i = 0; i < sampleId.length(); i++) {

      final char c = sampleId.charAt(i);
      if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-'))
        throw new EoulsanException(
            "Invalid sample id, only letters, digits, '-' or '_' characters are allowed : "
                + sampleId + ".");
    }

    // Check if the sample has been already defined
    if (sampleIds.contains(sampleId))
      throw new EoulsanException("The sample id \""
          + sampleId + "\" has been define more than one time.");
    sampleIds.add(sampleId);
  }

  private static void checkIndex(final String index) throws EoulsanException {

    if (index == null)
      return;

    for (int i = 0; i < index.length(); i++)
      switch (index.codePointAt(i)) {

      case 'A':
      case 'a':
      case 'T':
      case 't':
      case 'G':
      case 'g':
      case 'C':
      case 'c':
        break;

      default:
        throw new EoulsanException("Invalid index found: " + index + ".");
      }
  }

  private static void checkSampleProject(final String sampleProject)
      throws EoulsanException {

    if (isNullOrEmpty(sampleProject))
      throw new EoulsanException("Found a null or sample project.");

    for (int i = 0; i < sampleProject.length(); i++) {
      final char c = sampleProject.charAt(i);
      if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_'))
        throw new EoulsanException(
            "Invalid sample project, only letters, digits, '-' or '_' characters are allowed: "
                + sampleProject + ".");
    }

  }

  /**
   * Replace index shortcuts in a design object by index sequences.
   * @param design Casava design object
   * @param sequences map for the sequences
   * @throws EoulsanException if the shortcut is unknown
   */
  public static void replaceIndexShortcutsBySequences(
      final CasavaDesign design, final Map<String, String> sequences)
      throws EoulsanException {

    if (design == null || sequences == null)
      return;

    for (final CasavaSample sample : design) {

      if (sample.getIndex() == null)
        throw new NullPointerException("Sample index is null for sample: "
            + sample);

      final String index = sample.getIndex().trim();

      try {
        checkIndex(index);
      } catch (EoulsanException e) {

        if (!sequences.containsKey(index.toLowerCase()))
          throw new EoulsanException("Unknown index sequence shortcut ("
              + index + ") for sample: " + sample);

        sample.setIndex(sequences.get(index.toLowerCase()));
      }

    }

  }

  /**
   * Convert a Casava design to CSV.
   * @param design Casava design object to convert
   * @return a String with the converted design
   */
  public static final String toCSV(final CasavaDesign design) {

    final StringBuilder sb = new StringBuilder();

    sb.append("\"FCID\",\"Lane\",\"SampleID\",\"SampleRef\",\"Index\",\"Description\","
        + "\"Control\",\"Recipe\",\"Operator\",\"SampleProject\"\n");

    if (design == null)
      return sb.toString();

    for (CasavaSample s : design) {

      sb.append(s.getFlowCellId().trim().toUpperCase());
      sb.append(',');
      sb.append(s.getLane());
      sb.append(',');
      sb.append(quote(s.getSampleId().trim()));
      sb.append(',');
      sb.append(quote(s.getSampleRef().trim()));
      sb.append(',');
      sb.append(quote(s.getIndex().toUpperCase()));
      sb.append(',');
      sb.append(quote(s.getDescription().trim()));
      sb.append(',');
      sb.append(s.isControl() ? 'Y' : 'N');
      sb.append(',');
      sb.append(quote(s.getRecipe().trim()));
      sb.append(',');
      sb.append(quote(s.getOperator().trim()));
      sb.append(',');
      sb.append(quote(s.getSampleProject()));

      sb.append('\n');

    }

    return sb.toString();
  }

  public static CasavaDesign parseTabulatedDesign(final String s)
      throws IOException {

    if (s == null)
      return null;

    return new AbstractCasavaDesignTextReader() {

      @Override
      public CasavaDesign read() throws IOException {

        final String[] lines = s.split("\n");

        for (final String line : lines) {

          final String[] fields = line.split("\t");
          parseLine(Arrays.asList(fields));

        }

        return getDesign();
      }
    }.read();
  }

  //
  // Private utility methods
  //

  /**
   * Test if a string is null or empty
   * @param s string to test
   * @return true if the input string is null or empty
   */
  private static boolean isNullOrEmpty(final String s) {

    return s == null || s.isEmpty();
  }

  private static String quote(final String s) {

    if (s == null)
      return "";

    final String trimmed = s.trim();

    if (s.indexOf(' ') != -1 || s.indexOf(',') != -1)
      return '\"' + trimmed + '\"';
    return trimmed;
  }

  //
  // Constructor
  //

  private CasavaDesignUtil() {
  }

}
