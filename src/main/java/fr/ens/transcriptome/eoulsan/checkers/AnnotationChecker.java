/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.checkers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.expression.ExpressionStep;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder;

public class AnnotationChecker implements Checker {

  private String genomicType;

  @Override
  public String getName() {

    return "annotation_checker";
  }

  @Override
  public void configure(Set<Parameter> stepParameters,
      Set<Parameter> globalParameters) throws EoulsanException {
    for (Parameter p : stepParameters) {

      if (ExpressionStep.GENOMIC_TYPE_PARAMETER_NAME.equals(p.getName()))
        this.genomicType = p.getStringValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

  }

  @Override
  public boolean check(final Design design, final Context context,
      final CheckStore checkInfo) throws EoulsanException {
    if (design == null)
      throw new NullPointerException("The design is null");

    if (context == null)
      throw new NullPointerException("The execution context is null");

    if (checkInfo == null)
      throw new NullPointerException("The check info info is null");

    final List<Sample> samples = design.getSamples();

    if (samples == null)
      throw new NullPointerException("The samples are null");

    if (samples.size() == 0)
      throw new EoulsanException("No samples found in design");

    final Sample s = samples.get(0);

    final InputStream is;

    try {
      is = context.getInputStream(DataFormats.ANNOTATION_GFF, s);

      final TranscriptAndExonFinder ef =
          new TranscriptAndExonFinder(is, this.genomicType);

      if (ef.getTranscriptsIds().size() == 0)
        throw new EoulsanException("No transcripts found for genomic type ("
            + genomicType + ") in annotation.");

      // TODO compare chromosomes names
      final Set<String> genomeChromosomes =
          getGenomeChromosomes(design, context, checkInfo);

      if (genomeChromosomes != null) {

        final Set<String> annotationChromosomes = ef.getChromosomesIds();

        for (String chr : annotationChromosomes)
          if (!genomeChromosomes.contains(chr))
            throw new EoulsanException("Chromosome "
                + chr + " not found in the list of genome chromosomes.");
      }

    } catch (IOException e) {
      throw new EoulsanException(
          "Error while reading annotation file for checking: " + e.getMessage());
    } catch (BadBioEntryException e) {
      throw new EoulsanException(
          "Bad entry in annotation file while checking: " + e.getEntry());
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private Set<String> getGenomeChromosomes(final Design design,
      final Context context, final CheckStore checkInfo)
      throws EoulsanException {

    Object o = checkInfo.get(GenomeChecker.INFO_CHROMOSOME);

    if (o == null) {

      DataFormats.GENOME_FASTA.getChecker().check(design, context, checkInfo);
      o = checkInfo.get(GenomeChecker.INFO_CHROMOSOME);

      if (o == null)
        return null;
    }

    return ((Map<String, Integer>) checkInfo.get(GenomeChecker.INFO_CHROMOSOME))
        .keySet();
  }

}
