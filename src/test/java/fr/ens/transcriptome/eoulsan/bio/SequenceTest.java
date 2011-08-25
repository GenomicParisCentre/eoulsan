package fr.ens.transcriptome.eoulsan.bio;

import static org.junit.Assert.*;

import org.junit.Test;

public class SequenceTest {

  @Test
  public void testGetSetId() {

    Sequence s = new Sequence();
    assertEquals(0, s.getId());

    s.setId(1);
    assertEquals(1, s.getId());

    s.setId(30);
    assertEquals(30, s.getId());
  }

  @Test
  public void testGetSetName() {

    Sequence s = new Sequence();
    assertEquals(null, s.getName());

    s.setName("toto");
    assertEquals("toto", s.getName());

    s.setName("");
    assertEquals("", s.getName());

    s.setName("titi");
    assertEquals("titi", s.getName());

    s.setName(null);
    assertEquals(null, s.getName());

    assertTrue(s.setNameWithValidation("toto"));
    assertEquals("toto", s.getName());

    assertFalse(s.setNameWithValidation(""));
    assertEquals("", s.getName());

    assertTrue(s.setNameWithValidation("titi"));
    assertEquals("titi", s.getName());

    assertFalse(s.setNameWithValidation(null));
    assertEquals(null, s.getName());
  }

  @Test
  public void testGetSetDescription() {

    Sequence s = new Sequence();
    assertNull(s.getDescription());

    s.setDescription("My description");
    assertEquals("My description", s.getDescription());

    s.setDescription("");
    assertEquals("", s.getDescription());

    s.setDescription("My description2");
    assertEquals("My description2", s.getDescription());

    s.setDescription(null);
    assertNull(s.getDescription());
  }

  @Test
  public void testGetSetAlphabet() {

    Sequence s = new Sequence();
    assertNotNull(s.getAlphabet());
    assertEquals(Alphabets.AMBIGUOUS_DNA_ALPHABET, s.getAlphabet());

    s.setAlphabet(Alphabets.UNAMBIGUOUS_DNA_ALPHABET);
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s.getAlphabet());

    try {
      s.setAlphabet(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s.getAlphabet());
  }

  @Test
  public void testGetSequence() {

    Sequence s = new Sequence();
    assertNull(s.getSequence());

    s.setSequence("ATGC");
    assertEquals("ATGC", s.getSequence());

    s.setSequence("");
    assertEquals("", s.getSequence());

    s.setSequence("===");
    assertEquals("===", s.getSequence());

    s.setSequence("ATGCTT");
    assertEquals("ATGCTT", s.getSequence());

    s.setSequence(null);
    assertNull(s.getSequence());

    assertTrue(s.setSequenceWithValidation("ATGC"));
    assertEquals("ATGC", s.getSequence());

    assertFalse(s.setSequenceWithValidation(""));
    assertEquals("", s.getSequence());

    assertFalse(s.setSequenceWithValidation("==="));
    assertEquals("===", s.getSequence());

    assertTrue(s.setSequenceWithValidation("ATGCTT"));
    assertEquals("ATGCTT", s.getSequence());

    assertFalse(s.setSequenceWithValidation(null));
    assertNull(s.getSequence());
  }

  @Test
  public void testSet() {

    Sequence s1 = new Sequence();
    assertEquals(0, s1.getId());
    assertNull(s1.getName());
    assertNull(s1.getDescription());
    assertEquals(Alphabets.AMBIGUOUS_DNA_ALPHABET, s1.getAlphabet());
    assertNull(s1.getSequence());

    Sequence s2 = new Sequence(1, "toto", "ATGC", "test sequence");
    s2.setAlphabet(Alphabets.UNAMBIGUOUS_DNA_ALPHABET);
    assertEquals(1, s2.getId());
    assertEquals("toto", s2.getName());
    assertEquals("test sequence", s2.getDescription());
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s2.getAlphabet());
    assertEquals("ATGC", s2.getSequence());

    // Test if there is no change in s1
    assertEquals(0, s1.getId());
    assertNull(s1.getName());
    assertNull(s1.getDescription());
    assertEquals(Alphabets.AMBIGUOUS_DNA_ALPHABET, s1.getAlphabet());
    assertNull(s1.getSequence());

    s1.set(s2);
    assertEquals(1, s1.getId());
    assertEquals("toto", s1.getName());
    assertEquals("test sequence", s1.getDescription());
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s1.getAlphabet());
    assertEquals("ATGC", s1.getSequence());

    Sequence s3 = new Sequence(s2);
    assertEquals(1, s3.getId());
    assertEquals("toto", s3.getName());
    assertEquals("test sequence", s3.getDescription());
    assertEquals(Alphabets.UNAMBIGUOUS_DNA_ALPHABET, s3.getAlphabet());
    assertEquals("ATGC", s3.getSequence());

    try {
      s1.set(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }

    try {
      new Sequence(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }
  }

  @Test
  public void testLength() {

    Sequence s = new Sequence();
    assertEquals(0, s.length());

    s.setSequence("ATGC");
    assertEquals(4, s.length());
    s.setSequence("ATGCATGC");
    assertEquals(8, s.length());
  }

  @Test
  public void testSubSequence() {

    Sequence s1 = new Sequence(1, "toto", "ATGC");

    try {
      s1.subSequence(-1, 2);
      assertTrue(false);
    } catch (StringIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    try {
      s1.subSequence(0, 5);
      assertTrue(false);
    } catch (StringIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    try {
      s1.subSequence(2, 1);
      assertTrue(false);
    } catch (StringIndexOutOfBoundsException e) {
      assertTrue(true);
    }

    Sequence s2 = s1.subSequence(0, 4);
    assertEquals("ATGC", s2.getSequence());

    s2 = s1.subSequence(1, 4);
    assertEquals("TGC", s2.getSequence());
    assertEquals(-1, s2.getId());
    assertEquals("toto[part]", s2.getName());

    s1.setName(null);
    s2 = s1.subSequence(1, 4);
    assertNull(s2.getName());

    s1.setSequence(null);
    assertNull(s1.subSequence(1, 4));

  }

  @Test
  public void testConcat() {

    Sequence s1 = new Sequence(1, "toto", "AATT");
    Sequence s2 = new Sequence(2, "titi", "GGCC");

    Sequence s3 = s1.concat(s2);
    assertEquals("AATTGGCC", s3.getSequence());
    assertEquals("toto[merged]", s3.getName());

    s1.setSequence(null);
    s3 = s1.concat(s2);
    assertEquals("GGCC", s3.getSequence());

    s1.setSequence("AATT");
    s2.setSequence(null);
    s3 = s1.concat(s2);
    assertEquals("AATT", s3.getSequence());

    s3 = s1.concat(null);
    assertEquals(s1.getId(), s3.getId());
    assertEquals(s1.getName(), s3.getName());
    assertEquals(s1.getDescription(), s3.getDescription());
    assertEquals(s1.getAlphabet(), s3.getAlphabet());
    assertEquals(s1.getSequence(), s3.getSequence());
    assertFalse(s1 == s3);
  }

  @Test
  public void testCountSequenceSequence() {

    Sequence s1 = new Sequence(1, "toto", "AATTGGTT");
    Sequence s2 = new Sequence(1, "titi", "TT");

    assertEquals(2, s1.countSequence(s2));
    s2 = new Sequence(1, "titi", "AA");
    assertEquals(1, s1.countSequence(s2));
    s2 = new Sequence(1, "titi", "GG");
    assertEquals(1, s1.countSequence(s2));
    s2 = new Sequence(1, "titi", "CC");
    assertEquals(0, s1.countSequence(s2));

    assertEquals(0, s1.countSequence((Sequence) null));
  }

  @Test
  public void testCountSequenceString() {

    Sequence s = new Sequence(1, "toto", "AATTGGTT");
    assertEquals(2, s.countSequence("A"));
    assertEquals(1, s.countSequence("AA"));
    assertEquals(4, s.countSequence("T"));
    assertEquals(2, s.countSequence("TT"));
    assertEquals(0, s.countSequence("C"));
    assertEquals(0, s.countSequence(""));
    assertEquals(0, s.countSequence((String) null));

    s = new Sequence(1, "toto", null);
    assertEquals(0, s.countSequence("TT"));

    s = new Sequence(1, "toto", "");
    assertEquals(0, s.countSequence("TT"));

    s = new Sequence(1, "toto", "AATTTGGTT");
    assertEquals(2, s.countSequence("TT"));

    s = new Sequence(1, "toto", "AATTTTGGTT");
    assertEquals(3, s.countSequence("TT"));

  }

  @Test
  public void testGettm() {

    Sequence s = new Sequence(1, "toto", "AATTGGTT");
    assertEquals(s.getTm(50.0f, 50.0f), s.getTm(), 0.1);
  }

  @Test
  public void testGetGCPercent() {

    Sequence s = new Sequence(1, "toto", "AATTGGTT");
    assertEquals(2.0 / 8.0, s.getGCPercent(), 0.1);

    s = new Sequence(1, "toto", "");
    assertEquals(Double.NaN, s.getGCPercent(), 0.1);

    s = new Sequence(1, "toto", null);
    assertEquals(Double.NaN, s.getGCPercent(), 0.1);

    s = new Sequence(1, "toto", "AATTAATT");
    assertEquals(0.0 / 8.0, s.getGCPercent(), 0.1);

    s = new Sequence(1, "toto", "AATTGGCC");
    assertEquals(4.0 / 8.0, s.getGCPercent(), 0.1);
  }

  @Test
  public void testReverseComplement() {

    Sequence s = new Sequence(0, "toto", "ATGC");
    assertEquals("ATGC", s.getSequence());
    s.reverseComplement();
    assertEquals("GCAT", s.getSequence());

    s = new Sequence(0, "toto", null);
    assertNull(s.getSequence());
  }

  @Test
  public void testReverseComplementString() {

    assertNull(Sequence.reverseComplement(null,
        Alphabets.AMBIGUOUS_DNA_ALPHABET));
    assertNull(Sequence.reverseComplement("ATGC", null));

    assertEquals("GCAT",
        Sequence.reverseComplement("ATGC", Alphabets.AMBIGUOUS_DNA_ALPHABET));
  }

  @Test
  public void testToFasta() {

    Sequence s = new Sequence(0, "toto", "ATGC");
    assertEquals(">toto\nATGC\n", s.toFasta());
    s = new Sequence(0, null, "ATGC");
    assertEquals(">\nATGC\n", s.toFasta());
    s = new Sequence(0, "toto", null);
    assertEquals(">toto\n", s.toFasta());

  }

  @Test
  public void testToFastaInt() {

    Sequence s = new Sequence(0, "toto", "ATGC");
    assertEquals(">toto\nATGC\n", s.toFasta(0));
    assertEquals(">toto\nATGC\n", s.toFasta(60));

    s = new Sequence(0, "toto", "ATGCATGCAT");
    assertEquals(">toto\nATGCA\nTGCAT\n", s.toFasta(5));
    assertEquals(">toto\nATGCAT\nGCAT\n", s.toFasta(6));
  }

  @Test
  public void testParseFasta() {

    Sequence s = new Sequence();
    s.parseFasta(">toto\nATGCA\nTGCAT\n");
    assertEquals("toto", s.getName());
    assertEquals("ATGCATGCAT", s.getSequence());
    s.parseFasta(null);
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta("toto\nATGCA\nTGCAT\n");
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta(">toto");
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta(">toto\n");
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta(">toto\n ATGCA \n  \n TGCAT \n");
    assertEquals("toto", s.getName());
    assertEquals("ATGCATGCAT", s.getSequence());

    s.parseFasta("toto\nATGCA\n>TGCAT\n");
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta("");
    assertNull(s.getName());
    assertNull(s.getSequence());

    s.parseFasta(">toto\n>ATGCA\n>TGCAT\n");
    assertNull(s.getName());
    assertNull(s.getSequence());
  }

  @Test
  public void testValidate() {

    Sequence s = new Sequence(0, "toto", "ATGC");
    assertTrue(s.validate());

    s = new Sequence(-1, "toto", "ATGC");
    assertTrue(s.validate());

    s = new Sequence(0, "toto", "A#GC");
    assertFalse(s.validate());
    s = new Sequence(0, "toto", null);
    assertFalse(s.validate());
    s = new Sequence(0, null, "ATGC");
    assertFalse(s.validate());
    s = new Sequence(0, "toto", "");
    assertFalse(s.validate());
  }

  @Test
  public void testEqualsObject() {

    Sequence s1 = new Sequence(0, "toto", "ATGC", "desc");
    Sequence s2 = new Sequence(0, "toto", "ATGC", "desc");
    Sequence s3 = new Sequence(0, "titi", "ATGC", "desc");

    assertTrue(s1.equals(s1));
    assertFalse(s1.equals(null));
    assertFalse(s1.equals("titit"));

    assertTrue(s1.equals(s1));
    assertEquals(s1, s2);
    assertFalse(s1 == s2);
    assertNotSame(s1, s3);

    s3.setName("toto");
    assertTrue(s1.equals(s3));
    s3.setName("titi");
    assertFalse(s1.equals(s3));
    s3.setName("toto");
    assertTrue(s1.equals(s3));

    s3.setDescription("other desc");
    assertFalse(s1.equals(s3));
    assertTrue(s1.equals(s2));
    s3.setDescription("desc");
    assertTrue(s1.equals(s3));

    s2.setAlphabet(Alphabets.UNAMBIGUOUS_DNA_ALPHABET);
    assertFalse(s1.equals(s2));

    s2.setAlphabet(Alphabets.AMBIGUOUS_DNA_ALPHABET);
    assertTrue(s1.equals(s2));

    s2.setSequence("AAAA");
    assertFalse(s1.equals(s2));
    s2.setSequence("ATGC");
    assertTrue(s1.equals(s2));

    s2.setId(5);
    assertFalse(s1.equals(s2));
  }

  @Test
  public void testHashCode() {

    Sequence s1 = new Sequence(0, "toto", "ATGC", "desc");
    Sequence s2 = new Sequence(0, "toto", "ATGC", "desc");
    Sequence s3 = new Sequence(0, "titi", "ATGC", "desc");

    assertEquals(s1.hashCode(), s2.hashCode());
    assertNotSame(s1.hashCode(), s3.hashCode());

    s3.setName("toto");
    assertEquals(s1.hashCode(), s3.hashCode());

    s3.setDescription("other desc");
    assertNotSame(s1.hashCode(), s3.hashCode());
    assertEquals(s1.hashCode(), s2.hashCode());

    s2.setAlphabet(Alphabets.UNAMBIGUOUS_DNA_ALPHABET);
    assertNotSame(s1.hashCode(), s2.hashCode());

    s2.setAlphabet(Alphabets.AMBIGUOUS_DNA_ALPHABET);
    assertEquals(s1.hashCode(), s2.hashCode());

    s2.setId(5);
    assertNotSame(s1.hashCode(), s2.hashCode());
  }

  @Test
  public void testToString() {

    Sequence s = new Sequence(1, "toto", "ATGC", "desc");
    assertEquals(
        "Sequence{id=1, name=toto, description=desc, alphabet=AmbiguousDNA, sequence=ATGC}",
        s.toString());

  }

}
