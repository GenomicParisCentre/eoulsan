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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.steps.expression.local;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.steps.expression.ExpressionCounters.INVALID_CHROMOSOME_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.steps.expression.ExpressionCounters.INVALID_SAM_ENTRIES_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.steps.expression.ExpressionCounters.PARENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.steps.expression.ExpressionCounters.PARENT_ID_NOT_FOUND_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.steps.expression.ExpressionCounters.TOTAL_READS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.steps.expression.ExpressionCounters.UNUSED_READS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.steps.expression.ExpressionCounters.USED_READS_COUNTER;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMRecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.bio.SAMUtils;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.steps.expression.ExonsCoverage;
import fr.ens.biologie.genomique.eoulsan.steps.expression.TranscriptAndExonFinder;
import fr.ens.biologie.genomique.eoulsan.steps.expression.TranscriptAndExonFinder.Exon;
import fr.ens.biologie.genomique.eoulsan.steps.expression.TranscriptAndExonFinder.Transcript;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.PseudoMapReduce;
import fr.ens.biologie.genomique.eoulsan.util.Reporter;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class implements the local version of map reduce algorithm for
 * expression computation.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public final class ExpressionPseudoMapReduce extends PseudoMapReduce {

  private TranscriptAndExonFinder tef;
  private final String counterGroup;
  private final SAMLineParser parser;
  private final ExonsCoverage geneExpr = new ExonsCoverage();
  private final String[] fields = new String[9];

  private final Map<String, Exon> oneExonByParentId = new HashMap<>();

  //
  // Mapper and reducer
  //

  /**
   * Mapper.
   * @param value input of the mapper
   * @param output List of output of the mapper
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the mapper
   */
  @Override
  public void map(final String value, final List<String> output,
      final Reporter reporter) throws IOException {

    // Don't read SAM headers
    if (value == null || value.length() == 0 || value.charAt(0) == '@') {
      return;
    }

    final SAMRecord samRecord;

    try {

      samRecord = this.parser.parseLine(value);
    } catch (SAMException e) {

      reporter.incrCounter(this.counterGroup,
          INVALID_SAM_ENTRIES_COUNTER.counterName(), 1);
      getLogger().info("Invalid SAM output entry: "
          + e.getMessage() + " line='" + value + "'");
      return;
    }

    final String chr = samRecord.getReferenceName();
    final int start = samRecord.getAlignmentStart();
    final int end = samRecord.getAlignmentEnd();

    final Set<Exon> exons = this.tef.findExons(chr, start, end);

    reporter.incrCounter(this.counterGroup, TOTAL_READS_COUNTER.counterName(),
        1);
    if (exons == null) {
      reporter.incrCounter(this.counterGroup,
          UNUSED_READS_COUNTER.counterName(), 1);
      return;
    }

    reporter.incrCounter(this.counterGroup, USED_READS_COUNTER.counterName(),
        1);
    int count = 1;
    final int nbExons = exons.size();

    this.oneExonByParentId.clear();

    // Sort the exon
    final List<Exon> exonSorted = Lists.newArrayList(exons);
    Collections.sort(exonSorted);

    for (Exon e : exonSorted) {
      this.oneExonByParentId.put(e.getParentId(), e);
    }

    List<String> keysSorted = new ArrayList<>(this.oneExonByParentId.keySet());
    Collections.sort(keysSorted);

    for (String key : keysSorted) {

      final Exon e = this.oneExonByParentId.get(key);

      output.add(e.getParentId()
          + "\t" + e.getChromosome() + "\t" + e.getStart() + "\t" + e.getEnd()
          + "\t" + e.getStrand() + "\t" + (count++) + "\t" + nbExons + "\t"
          + chr + "\t" + start + "\t" + end);
    }
  }

  /**
   * Reducer
   * @param key input key of the reducer
   * @param values values for the key
   * @param output list of output values of the reducer
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the reducer
   */
  @Override
  public void reduce(final String key, final Iterator<String> values,
      final List<String> output, final Reporter reporter) throws IOException {

    this.geneExpr.clear();

    reporter.incrCounter(this.counterGroup, PARENTS_COUNTER.counterName(), 1);

    final String parentId = key;

    boolean first = true;
    String chr = null;

    int count = 0;

    while (values.hasNext()) {

      count++;
      StringUtils.fastSplit(values.next(), this.fields);

      final String exonChr = this.fields[0];
      final int exonStart = Integer.parseInt(this.fields[1]);
      final int exonEnd = Integer.parseInt(this.fields[2]);

      final String alignmentChr = this.fields[6];
      final int alignmentStart = Integer.parseInt(this.fields[7]);
      final int alignmentEnd = Integer.parseInt(this.fields[8]);

      if (first) {
        chr = exonChr;
        first = false;
      }

      if (!exonChr.equals(alignmentChr) || !chr.equals(alignmentChr)) {
        reporter.incrCounter(this.counterGroup,
            INVALID_CHROMOSOME_COUNTER.counterName(), 1);
        continue;
      }

      this.geneExpr.addAlignment(exonStart, exonEnd, alignmentStart,
          alignmentEnd, true);
    }

    if (count == 0) {
      return;
    }

    final Transcript transcript = this.tef.getTranscript(parentId);

    if (transcript == null) {
      reporter.incrCounter(this.counterGroup,
          PARENT_ID_NOT_FOUND_COUNTER.counterName(), 1);
      return;
    }

    final int geneLength = transcript.getLength();
    final int notCovered = this.geneExpr.getNotCovered(geneLength);

    final String result =
        key + "\t" + notCovered + "\t" + this.geneExpr.getAlignmentCount();

    output.add(result);
  }

  /**
   * Get the annotation object.
   * @return the annotation object
   */
  public TranscriptAndExonFinder getTranscriptAndExonFinder() {

    return this.tef;
  }

  //
  // Annotation file loader
  //

  /**
   * Load annotation information.
   * @param annotationFile annotation file to load
   * @param expressionType expression type to use
   * @param attributeId GFF attribute ID to be used as feature ID
   * @throws IOException if an error occurs while reading annotation file
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  private void loadAnnotationFile(final File annotationFile,
      final String expressionType, final String attributeId)
          throws IOException, BadBioEntryException {

    final CompressionType ct =
        CompressionType.getCompressionTypeByFilename(annotationFile.getName());

    final InputStream is =
        ct.createInputStream(FileUtils.createInputStream(annotationFile));

    this.tef = new TranscriptAndExonFinder(is, expressionType, attributeId);
  }

  /**
   * Load annotation information.
   * @param annotationIs annotation stream to load
   * @param expressionType expression type to use
   * @param attributeId GFF attribute ID to be used as feature ID
   * @throws IOException if an error occurs while reading annotation file
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  private void loadAnnotationFile(final InputStream annotationIs,
      final String expressionType, final String attributeId)
          throws IOException, BadBioEntryException {

    checkNotNull(annotationIs, "Annotation stream is null");
    checkNotNull(expressionType, "Expression type is null");

    this.tef =
        new TranscriptAndExonFinder(annotationIs, expressionType, attributeId);
  }

  //
  // Constructor
  //

  /**
   * Load annotation information.
   * @param annotationFile annotation file to load
   * @param expressionType expression type to use
   * @param attributeId GFF attribute ID to be used as feature ID
   * @param genomeDescFile genome description file\
   * @param counterGroup counter group
   * @throws IOException if an error occurs while reading annotation file
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  public ExpressionPseudoMapReduce(final File annotationFile,
      final String expressionType, final String attributeId,
      final File genomeDescFile, final String counterGroup)
          throws IOException, BadBioEntryException {

    this.counterGroup = counterGroup;

    // Load genome description object
    final GenomeDescription genomeDescription =
        GenomeDescription.load(genomeDescFile);

    // Create parser object
    this.parser =
        new SAMLineParser(SAMUtils.newSAMFileHeader(genomeDescription));

    loadAnnotationFile(annotationFile, expressionType, attributeId);
  }

  /**
   * Load annotation information.
   * @param annotationIs annotation input stream to load
   * @param expressionType expression type to use
   * @param attributeId GFF attribute ID to be used as feature ID
   * @param genomeDescIs genome description input stream
   * @param counterGroup counter group
   * @throws IOException if an error occurs while reading annotation file
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  public ExpressionPseudoMapReduce(final InputStream annotationIs,
      final String expressionType, final String attributeId,
      final InputStream genomeDescIs, final String counterGroup)
          throws IOException, BadBioEntryException {

    this.counterGroup = counterGroup;

    // Load genome description object
    final GenomeDescription genomeDescription =
        GenomeDescription.load(genomeDescIs);

    // Create parser object
    this.parser =
        new SAMLineParser(SAMUtils.newSAMFileHeader(genomeDescription));

    loadAnnotationFile(annotationIs, expressionType, attributeId);
  }

}
