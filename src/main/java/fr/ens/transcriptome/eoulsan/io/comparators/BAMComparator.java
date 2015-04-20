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

package fr.ens.transcriptome.eoulsan.io.comparators;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.util.BloomFilterUtils;

/**
 * This class allow compare two BAM file with use BloomFilter.
 * @since 2.0
 * @author Sandrine Perrin
 */
public class BAMComparator extends AbstractComparatorWithBloomFilter {

  public static final String COMPARATOR_NAME = "BAMComparator";
  private static final Collection<String> EXTENSIONS = Sets.newHashSet(".bam");

  final Set<String> tagsToNotCompare;

  private int numberElementsCompared;

  @Override
  public boolean compareFiles(final BloomFilterUtils filter,
      final InputStream isBAM) throws IOException {

    String line = null;
    this.numberElementsCompared = 0;

    // Create Bam reader
    final SamReader bamfr =
        SamReaderFactory.makeDefault().open(SamInputResource.of(isBAM));

    // Get iterator on file
    final Iterator<SAMRecord> it = bamfr.iterator();

    // Parse file
    while (it.hasNext()) {
      final SAMRecord r = it.next();

      // Convert in SAM
      line = r.getSAMString();
      this.numberElementsCompared++;

      // Header
      if (line.charAt(0) == '@') {

        // Skip specified tag in header sam file
        if (!this.tagsToNotCompare.contains(getTag(line))) {

          if (!filter.mightContain(line)) {
            // Save line occurs fail comparison
            setCauseFailComparison(line);

            // Close reader
            bamfr.close();

            return false;
          }
        }
      } else {
        // Line
        if (!filter.mightContain(line)) {
          // Save line occurs fail comparison
          setCauseFailComparison(line);

          // Close reader
          bamfr.close();

          return false;
        }
      }

    }

    // Close reader
    bamfr.close();

    // Check count element is the same between two files
    if (this.numberElementsCompared != filter.getAddedNumberOfElements()) {
      setCauseFailComparison("Different count elements "
          + this.numberElementsCompared + " was "
          + filter.getAddedNumberOfElements() + " expected.");
      return false;
    }

    return true;
  }

  @Override
  protected BloomFilterUtils buildBloomFilter(final InputStream is)
      throws IOException {

    // Create filter
    final BloomFilterUtils filter =
        initBloomFilter(getExpectedNumberOfElements());

    // Parse BAM file
    try (final SamReader bamfr =
        SamReaderFactory.makeDefault().open(SamInputResource.of(is))) {

      final Iterator<SAMRecord> it = bamfr.iterator();

      while (it.hasNext()) {
        // Convert in line in SAM and save in filter
        filter.put(it.next().getSAMString());
      }

    } catch (final Exception e) {
      throw new IOException("Fail read BAM file exception: " + e.getMessage());
    }

    return filter;
  }

  //
  // Other methods
  //

  private static String getTag(final String samHeaderLine) {

    if (samHeaderLine.length() == 0) {
      return "";
    }

    final int pos = samHeaderLine.indexOf('\t');

    if (pos == -1) {
      return samHeaderLine.substring(1);
    }

    return samHeaderLine.substring(1, pos);
  }

  @Override
  public String getName() {

    return COMPARATOR_NAME;
  }

  @Override
  public Collection<String> getExtensions() {
    return EXTENSIONS;
  }

  @Override
  public int getNumberElementsCompared() {
    return this.numberElementsCompared;
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param useSerializeFile true if it needed to save BloomFilter in file with
   *          extension '.ser'
   */
  public BAMComparator(final boolean useSerializeFile) {
    super(useSerializeFile);

    this.tagsToNotCompare = new HashSet<>();
  }

  /**
   * Public constructor, specify all headers tags not used to compare.
   * @param useSerializeFile true if it needed to save BloomFilter in file with
   *          extension '.ser'
   * @param headersTags all headers tags
   */
  public BAMComparator(final boolean useSerializeFile,
      final String... headersTags) {
    super(useSerializeFile);

    if (headersTags == null) {
      throw new NullPointerException("headersTags is null");
    }

    this.tagsToNotCompare = Sets.newHashSet(headersTags);
  }

}
