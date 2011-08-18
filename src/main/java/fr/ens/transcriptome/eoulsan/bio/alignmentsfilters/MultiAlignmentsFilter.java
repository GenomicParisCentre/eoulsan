package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.List;

import net.sf.samtools.SAMRecord;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define an alignments filter that calls successively a list of
 * alignments filters.
 * @author Laurent Jourdren
 */
public class MultiAlignmentsFilter implements AlignmentsFilter {

  private final List<AlignmentsFilter> list = Lists.newArrayList();
  private final ReporterIncrementer incrementer;
  private final String counterGroup;

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    if (records == null)
      return;

    for (AlignmentsFilter af : this.list) {

      final int sizeBefore = records.size();
      af.filterReadAlignments(records);

      final int sizeAfter = list.size();
      final int diff = sizeBefore - sizeAfter;

      if (diff > 0 && incrementer != null)
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
  public void addFilter(final AlignmentsFilter filter) {

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
  public MultiAlignmentsFilter() {

    this((ReporterIncrementer) null, null);
  }

  /**
   * Public constructor.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   */
  public MultiAlignmentsFilter(final ReporterIncrementer incrementer,
      final String counterGroup) {

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;
  }

  /**
   * Public constructor.
   * @param filters filters to add
   */
  public MultiAlignmentsFilter(final List<AlignmentsFilter> filters) {

    this(null, null, filters);
  }

  /**
   * Public constructor.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @param filters filters to add
   */
  public MultiAlignmentsFilter(final ReporterIncrementer incrementer,
      final String counterGroup, final List<AlignmentsFilter> filters) {

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;

    if (filters != null) {

      for (AlignmentsFilter filter : filters) {
        addFilter(filter);
      }
    }
  }

}
