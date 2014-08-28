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

package fr.ens.transcriptome.eoulsan.util.hadoop;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a Hadoop reporter.
 * @since 1.0
 * @author Laurent Jourdren
 */
@SuppressWarnings("rawtypes")
public class HadoopReporter implements Reporter {

  private final TaskInputOutputContext context;
  private final Counters counters;

  @Override
  public void incrCounter(final String counterGroup, final String counterName,
      long amount) {

    if (context != null) {
      // Use in mappers and reducers
      this.context.getCounter(counterGroup, counterName).increment(amount);
    } else {
      // Use in other cases
      this.counters.getGroup(counterGroup).findCounter(counterName)
          .increment(amount);
    }
  }

  @Override
  public long getCounterValue(final String counterGroup,
      final String counterName) {

    // This method does not works in Hadoop mappers and reducers
    if (context != null) {
      throw new NotImplementedException();
    }

    return this.counters.findCounter(counterGroup, counterName).getValue();
  }

  @Override
  public Set<String> getCounterGroups() {

    // This method does not works in Hadoop mappers and reducers
    if (context != null) {
      throw new NotImplementedException();
    }

    return Sets.newHashSet(counters.getGroupNames());
  }

  @Override
  public Set<String> getCounterNames(final String group) {

    // This method does not works in Hadoop mappers and reducers
    if (context != null) {
      throw new NotImplementedException();
    }

    final Set<String> result = Sets.newHashSet();

    for (Counter c : counters.getGroup(group))
      result.add(c.getName());

    return result;
  }

  //
  // Constructors
  //

  /**
   * Constructor. This constructor is used by Hadoop mappers and reducers.
   * @param context context to use for counter incrementation
   */
  public HadoopReporter(final TaskInputOutputContext context) {

    checkNotNull(context, "context is null");

    this.counters = null;
    this.context = context;
  }

  /**
   * Constructor.
   * @param counters counters to use for counter incrementation
   */
  public HadoopReporter(final Counters counters) {

    checkNotNull(counters, "counters is null");

    this.counters = counters;
    this.context = null;
  }

}
