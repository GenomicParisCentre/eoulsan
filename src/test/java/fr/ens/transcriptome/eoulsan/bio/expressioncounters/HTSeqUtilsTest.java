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

package fr.ens.transcriptome.eoulsan.bio.expressioncounters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;

public class HTSeqUtilsTest {

  @Before
  public void setUp() throws Exception {

    // GenomicArray<String> ga = new GenomicArray<String>();
    //
    // final FastGemomicInterval fgi = new FastGemomicInterval("chr1", '+');
    //
    // GenomicInterval iv1 = fgi.iv(10, 50);
    // ga.addEntry(iv1, "a");
  }

  // @Test
  public void testStoreAnnotation() {

  }

  // @Test
  public void testAddIntervals() {

  }

  // @Test
  public void testParseCigar() {

  }

  @Test
  public void testFeaturesOverlapped() throws EoulsanException, IOException {

    final String chromosome = "chr1";
    final char strand = '+';
    Set<String> results;

    // intervals of a SAM alignment
    List<GenomicInterval> ivSeq = new ArrayList<GenomicInterval>();
    ivSeq.add(new GenomicInterval(chromosome, 5, 15, strand));

    // annotation
    GenomicArray<String> annot = new GenomicArray<String>();
    annot.addEntry(new GenomicInterval(chromosome, 1, 20, strand), "a");
    annot.addEntry(new GenomicInterval(chromosome, 25, 45, strand), "b");

    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.UNION, StrandUsage.YES);
    assertTrue(results.contains("a"));
    assertFalse(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_NONEMPTY, StrandUsage.YES);
    assertTrue(results.contains("a"));
    assertFalse(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_STRICT, StrandUsage.YES);
    assertTrue(results.contains("a"));
    assertFalse(results.contains("b"));
    
    /*************************************************************/

    ivSeq.clear();
    ivSeq.add(new GenomicInterval(chromosome, 23, 40, strand));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_NONEMPTY, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertTrue(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_STRICT, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertFalse(results.contains("b"));
    
    /*************************************************************/

    ivSeq.clear();
    ivSeq.add(new GenomicInterval(chromosome, 5, 23, strand));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_NONEMPTY, StrandUsage.YES);
    assertTrue(results.contains("a"));
    assertFalse(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_STRICT, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertFalse(results.contains("b"));
    
    /*************************************************************/

    ivSeq.clear();
    ivSeq.add(new GenomicInterval(chromosome, 15, 30, strand));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.UNION, StrandUsage.YES);
    assertTrue(results.contains("a"));
    assertTrue(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_NONEMPTY, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertFalse(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_STRICT, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertFalse(results.contains("b"));
    
    /*************************************************************/

    ivSeq.clear();
    ivSeq.add(new GenomicInterval(chromosome, 30, 55, strand));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.UNION, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertTrue(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_NONEMPTY, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertTrue(results.contains("b"));
    
    results = HTSeqUtils.featuresOverlapped(ivSeq, annot,
        OverlapMode.INTERSECTION_STRICT, StrandUsage.YES);
    assertFalse(results.contains("a"));
    assertFalse(results.contains("b"));
  }

}
