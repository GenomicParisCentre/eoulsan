package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

public class HeadCropTrimmerReadFilterTest {

  @Test
  public void HeadCropTirmmertest() throws EoulsanException {

    ReadFilter filter = new HeadCropTrimmerReadFilter();

    filter.setParameter("arguments", "5");
    filter.init();

    ReadSequence read =
        new ReadSequence(0, "read1", "AGGGGGCAAA", "xwxwxxabcd");
    assertTrue(filter.accept(read));
    assertEquals("read1", read.getName());
    assertEquals("GCAAA", read.getSequence());
    assertEquals("xabcd", read.getQuality());
    assertFalse(read.getSequence() == "AGGGG");
    assertFalse(read.getSequence() == "GCAAA");
    
    assertFalse(filter.accept(null));

    assertFalse(filter.accept(new ReadSequence(0, "read2", "AGGGG", "xxxxx")));

    read = new ReadSequence(0, "read3", "AGGGGGCAAA", "xxxxxxxxxx");
    assertTrue(filter.accept(read));
    assertEquals("read3", read.getName());
    assertFalse(read.getSequence() == "AGGGGGCAAA");
    assertFalse(read.getQuality() == "xxxxxxxxxx");
    
    filter = new HeadCropTrimmerReadFilter();
    filter.setParameter("arguments", "11");
    filter.init();
    
    read = new ReadSequence(0, "read4", "AGGGGGCAAA", "xxxxxxxxxx");
    assertFalse(filter.accept(read));
    assertEquals("read4", read.getName());
    assertEquals("AGGGGGCAAA", read.getSequence());
    assertEquals("xxxxxxxxxx", read.getQuality());
    
    
  }

}
