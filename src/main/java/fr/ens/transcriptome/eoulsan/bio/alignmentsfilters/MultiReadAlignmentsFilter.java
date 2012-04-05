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

import static fr.ens.transcriptome.eoulsan.util.Utils.newArrayList;

import java.util.List;

import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define an alignments filter that calls successively a list of
 * alignments filters.
 * @since 1.1
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
public class MultiReadAlignmentsFilter implements ReadAlignmentsFilter {

  private final List<ReadAlignmentsFilter> list = newArrayList();
  private final ReporterIncrementer incrementer;
  private final String counterGroup;
  
  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    boolean pairedEnd = false;
    
    if (records == null)
      return;
    
    if (records.get(0).getReadPairedFlag())
      pairedEnd = true;

    for (ReadAlignmentsFilter af : this.list) {

      final int sizeBefore = records.size();
      af.filterReadAlignments(records);

      final int sizeAfter = records.size();
      final int diff = sizeBefore - sizeAfter;

      if (diff > 0 && incrementer != null)
        
        // paired-end mode
        if (pairedEnd)
          this.incrementer.incrCounter(counterGroup, "alignments rejected by "
              + af.getName() + " filter", diff/2);

      // single-end mode
        else
          this.incrementer.incrCounter(counterGroup, "alignments rejected by "
              + af.getName() + " filter", diff);

      if (sizeAfter == 0)
        return;
    }

    return;
  }

  /**
   * Add a filter to the multi filter.
   * @param filter filter to add
   */
  public void addFilter(final ReadAlignmentsFilter filter) {

    if (filter != null) {
      this.list.add(filter);
    }

  }

  @Override
  public String getName() {

    return "MultiAlignmentsFilter";
  }

  @Override
  public String getDescription() {

    return "Multi alignments filter";
  }

  @Override
  public void setParameter(String key, String value) {
    // This filter has no parameter
  }

  @Override
  public void init() {
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   */
  public MultiReadAlignmentsFilter() {

    this((ReporterIncrementer) null, null);
  }

  /**
   * Public constructor.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   */
  public MultiReadAlignmentsFilter(final ReporterIncrementer incrementer,
      final String counterGroup) {

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;
  }

  /**
   * Public constructor.
   * @param filters filters to add
   */
  public MultiReadAlignmentsFilter(final List<ReadAlignmentsFilter> filters) {

    this(null, null, filters);
  }

  /**
   * Public constructor.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @param filters filters to add
   */
  public MultiReadAlignmentsFilter(final ReporterIncrementer incrementer,
      final String counterGroup, final List<ReadAlignmentsFilter> filters) {

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;

    if (filters != null) {

      for (ReadAlignmentsFilter filter : filters) {
        addFilter(filter);
      }
    }
  }

}
