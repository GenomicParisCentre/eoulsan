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

package fr.ens.transcriptome.eoulsan.design;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.design.io.DesignReader;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignReader;

/**
 * Utils methods for Design.
 * @author Laurent Jourdren
 */
public final class DesignUtils {

  /**
   * Show a design
   * @param design Design to show
   */
  public static void showDesign(final Design design) {

    List<String> metadataFields = design.getMetadataFieldsNames();

    StringBuffer sb = new StringBuffer();

    // Write header
    sb.append("SlideNumber");
    sb.append("\t");

    sb.append("FileName");

    for (String f : metadataFields) {

      sb.append("\t");
      sb.append(f);
    }

    System.out.println(sb.toString());

    // Write data
    List<Sample> slides = design.getSamples();

    for (Sample s : slides) {

      sb.setLength(0);

      sb.append(s.getName());
      sb.append("\t");

      String sourceInfo = s.getSourceInfo();
      if (sourceInfo != null)
        sb.append(sourceInfo);

      for (String f : metadataFields) {

        sb.append("\t");
        sb.append(s.getMetadata().get(f));
      }

      System.out.println(sb.toString());
    }

  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private DesignUtils() {
  }

  /**
   * Check if there are duplicate samples in the design.
   * @param design Design to test
   * @return if there are no duplicate
   */
  public static boolean checkSamples(final Design design) {

    final Set<String> samplesSources = new HashSet<String>();

    for (Sample s : design.getSamples()) {

      if (samplesSources.contains(s.getSource()))
        return false;
      samplesSources.add(s.getSource());
    }

    return true;
  }

  /**
   * Check if there are duplicate samples in the design.
   * @param design Design to test
   * @return if there are no duplicate
   * @throws EoulsanException if a source is a duplicate
   */
  private static boolean checkSamplesWithException(final Design design)
      throws EoulsanException {

    final Set<String> samplesSources = new HashSet<String>();

    for (Sample s : design.getSamples()) {

      if (samplesSources.contains(s.getSource()))
        throw new EoulsanException(
            "Error: The design contains one or more duplicate sample sources: "
                + s.getSource() + " (sample " + s.getId() + ")");
      ;
      samplesSources.add(s.getSource());
    }

    return true;
  }

  /**
   * Check if there is more than one genome in the design
   * @param design Design to test
   * @return true if there is more than one genome in the genome
   */
  public static boolean checkGenomes(final Design design) {

    if (!design.isMetadataField(SampleMetadata.GENOME_FIELD))
      return false;

    final Set<String> genomes = new HashSet<String>();

    for (Sample s : design.getSamples()) {

      String genome = s.getMetadata().getGenome();

      if (genomes.size() == 1 && !genomes.contains(genome))
        return false;

      if (genomes.size() == 0)
        genomes.add(genome);
    }

    return true;
  }

  /**
   * Check if there is more than one annotation in the design
   * @param design Design to test
   * @return true if there is more than one annotation in the genome
   */
  public static boolean checkAnnotations(final Design design) {

    if (!design.isMetadataField(SampleMetadata.GENOME_FIELD))
      return false;

    final Set<String> annotations = new HashSet<String>();

    for (Sample s : design.getSamples()) {

      String annotation = s.getMetadata().getAnnotation();

      if (annotations.size() == 1 && !annotations.contains(annotation))
        return false;

      if (annotations.size() == 0)
        annotations.add(annotation);
    }

    return true;
  }

  /**
   * Read and Check design
   * @param is InputStream for the design
   * @return a Design object
   * @throws EoulsanException if an error occurs while reading the design
   */
  public static Design readAndCheckDesign(final InputStream is)
      throws EoulsanException {

    final DesignReader dr = new SimpleDesignReader(is);
    final Design design = dr.read();

    DesignUtils.checkSamplesWithException(design);

    if (!DesignUtils.checkGenomes(design))
      throw new EoulsanException(
          "Warning: The design contains more than one genome file.");

    if (!DesignUtils.checkAnnotations(design))
      throw new EoulsanException(
          "Warning: The design contains more than one annotation file.");

    return design;
  }

  /**
   * Remove optional description fields and obfuscate condition field.
   * @param design design object to obfuscate
   * @param removeReplicateInformation if replicate information must be removed
   */
  public static void obfuscate(final Design design,
      final boolean removeReplicateInformation) {

    if (design == null) {
      return;
    }

    removeFieldIfExists(design, SampleMetadata.COMMENT_FIELD);
    removeFieldIfExists(design, SampleMetadata.DATE_FIELD);
    removeFieldIfExists(design, SampleMetadata.OPERATOR_FIELD);

    if (removeReplicateInformation) {
      removeFieldIfExists(design, SampleMetadata.CONDITION_FIELD);
      removeFieldIfExists(design, SampleMetadata.REPLICAT_TYPE_FIELD);
    }

    final Map<String, Integer> map = Maps.newHashMap();
    int count = 0;

    for (Sample s : design.getSamples()) {

      s.setName("s" + s.getId());

      if (design.isMetadataField(SampleMetadata.CONDITION_FIELD)) {
        final String cond = s.getMetadata().getCondition();

        if (!map.containsKey(cond)) {
          map.put(cond, ++count);
        }

        s.getMetadata().setCondition("c" + map.get(cond));
      }
    }
  }

  private static final void removeFieldIfExists(final Design design,
      final String fieldName) {

    if (design == null || fieldName == null) {
      return;
    }

    if (design.isMetadataField(fieldName)) {
      design.removeMetadataField(fieldName);
    }

  }

}
