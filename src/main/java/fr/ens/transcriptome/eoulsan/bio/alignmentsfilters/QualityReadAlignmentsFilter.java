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

package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class define a filter based on the quality of an alignment (SAM format).
 * @since 1.2
 * @author Claire Wallon
 */
public class QualityReadAlignmentsFilter extends AbstractReadAlignmentsFilter {

  public static final String FILTER_NAME = "mappingquality";
  private int qualityThreshold = -1;

  private final List<SAMRecord> result = new ArrayList<SAMRecord>();

  @Override
  public String getName() {
    return "mappingquality";
  }

  @Override
  public String getDescription() {
    return "With this filter, the alignments are filtered by their quality score.";
  }

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    if (records == null)
      return;

    // single-end mode
    if (!records.get(0).getReadPairedFlag()) {
      for (SAMRecord r : records) {

        // storage in 'result' of records that do not pass the quality filter
        if (r.getMappingQuality() > this.qualityThreshold
            && r.getMappingQuality() != 255) {
          this.result.add(r);
        }
      }
    }

    // paired-end mode
    else {
      for (int counterRecord = 0; counterRecord < records.size() - 1; counterRecord +=
          2) {

        // storage in 'result' of records that do not pass the quality filter
        if ((records.get(counterRecord).getMappingQuality() > this.qualityThreshold && records
            .get(counterRecord).getMappingQuality() != 255)
            || (records.get(counterRecord + 1).getMappingQuality() > this.qualityThreshold && records
                .get(counterRecord + 1).getMappingQuality() != 255)) {

          // records are stored 2 by 2 because of the paired-end mode
          this.result.add(records.get(counterRecord));
          this.result.add(records.get(counterRecord + 1));
        }
      }
    }

    // all records that do not pass the quality filter are removed
    records.removeAll(result);
    result.clear();
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null)
      return;

    if ("threshold".equals(key.trim())) {

      try {
        this.qualityThreshold = Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        return;
      }

      if (this.qualityThreshold < 0.0)
        throw new EoulsanException("Invalid qualityThreshold: "
            + qualityThreshold);
    } else

      throw new EoulsanException("Unknown parameter for "
          + getName() + " read filter: " + key);
  }

  @Override
  public void init() {

    if (this.qualityThreshold < 0.0)
      throw new IllegalArgumentException("Quality threshold is not set for "
          + getName() + " read alignments filter.");
  }

}
