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

package fr.ens.transcriptome.eoulsan.bio;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.trim;
import static fr.ens.transcriptome.eoulsan.util.Utils.equal;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class define a Sequence.
 * @author Laurent Jourdren
 */
public class Sequence {

  protected int id;
  protected String name;
  protected String description;
  protected Alphabet alphabet = Alphabets.AMBIGUOUS_DNA_ALPHABET;
  protected String sequence;

  //
  // Getters
  //

  /**
   * Get the id of the sequence.
   * @return the id of the sequence
   */
  public final int getId() {
    return this.id;
  }

  /**
   * Set the name of the sequence.
   * @return the name of the sequence
   */
  public final String getName() {
    return this.name;
  }

  /**
   * Get the description of the sequence.
   * @return a string with the description
   */
  public final String getDescription() {
    return this.description;
  }

  /**
   * Get the alphabet used for the sequence.
   * @return the alphabet of the sequence
   */
  public final Alphabet getAlphabet() {
    return this.alphabet;
  }

  /**
   * Get the sequence of the sequence.
   * @return a string with the sequence
   */
  public final String getSequence() {
    return this.sequence;
  }

  //
  // Setters
  //

  /**
   * Set the id of the sequence.
   * @param id id to set
   */
  public final void setId(final int id) {
    this.id = id;
  }

  /**
   * Set the name of the sequence.
   * @param name the name to set
   */
  public final void setName(final String name) {
    this.name = trim(name);
  }

  /**
   * Set the name of the sequence and validate this name. Even if the name is
   * not validated, the name parameter will be the name of the object after
   * execution of this method.
   * @param name the name to set
   * @return true if the name is valid.
   */
  public final boolean setNameWithValidation(final String name) {
    this.name = trim(name);
    return validateName();
  }

  /**
   * Set the description of the sequence.
   * @param description the description to set
   */
  public final void setDescription(final String description) {
    this.description = trim(description);
  }

  /**
   * Set the alphabet of the sequence.
   * @param alphabet the alphabet to set
   */
  public final void setAlphabet(final Alphabet alphabet) {

    if (alphabet == null)
      throw new NullPointerException("The alphabet is null");

    this.alphabet = alphabet;
  }

  /**
   * Set the sequence.
   * @param sequence Sequence to set
   */
  public final void setSequence(final String sequence) {
    this.sequence = trim(sequence);
  }

  /**
   * Set the sequence and validate this sequence. Even if the sequence is not
   * validated, the sequence parameter will be the name of the object after
   * execution of this method.
   * @param sequence Sequence to set
   * @return true if the name is valid.
   */
  public final boolean setSequenceWithValidation(final String sequence) {

    this.sequence = trim(sequence);
    return validateSequence();
  }

  /**
   * Set sequence values with the values of another sequence
   * @param sequence sequence object with values to use to fill current object
   */
  public void set(final Sequence sequence) {

    if (sequence == null)
      throw new NullPointerException("Sequence is null");

    this.id = sequence.id;
    this.name = sequence.name;
    this.description = sequence.description;
    this.sequence = sequence.sequence;
  }

  //
  // Sequence string management
  //

  /**
   * Get the length of the read.
   * @return the length of the read
   */
  public int length() {

    if (this.sequence == null) {
      return 0;
    }

    return this.sequence.length();
  }

  /**
   * Create a subsequence from the current sequence. Note that index start at 0.
   * @param beginIndex begin index of the subsequence
   * @param endIndex end index of the subsequence
   * @return a new sequence object with a subsequence of the current object
   */
  public Sequence subSequence(final int beginIndex, final int endIndex) {

    if (this.sequence == null)
      return null;

    if (beginIndex < 0)
      throw new StringIndexOutOfBoundsException(beginIndex);

    if (endIndex > length())
      throw new StringIndexOutOfBoundsException(endIndex);

    if (beginIndex > endIndex)
      throw new StringIndexOutOfBoundsException(endIndex - beginIndex);

    return new Sequence(-1, this.name == null ? "" : this.name + "[part]",
        getSequence().substring(beginIndex, endIndex));
  }

  /**
   * Contact two sequences.
   * @param sequence sequence to contact
   * @return a new sequence object with the sequence of the current object and
   *         the sequence of the input sequence
   */
  public Sequence concat(final Sequence sequence) {

    final Sequence result = new Sequence();
    result.setName(this.name + " [merged]");

    if (sequence == null || sequence.getSequence() == null)
      return result;

    result.setSequence(getSequence() + sequence.getSequence());
    return result;
  }

  /**
   * Count the number of times of a non overlapping sequence is found in the
   * current sequence.
   * @param sequence query sequence
   * @return the number of time that query sequence was found.
   */
  public int countSequence(final Sequence sequence) {

    return countSequence(sequence == null ? null : sequence.getSequence());
  }

  /**
   * Count the number of times of a a non overlapping string is found in the
   * current sequence.
   * @param s query string
   * @return the number of time that query sequence was found.
   */
  public int countSequence(final String s) {

    if (s == null || this.sequence == null)
      return 0;

    int count = 0;
    int index = 0;

    while ((index = this.sequence.indexOf(s, index)) != -1)
      count++;

    return count;
  }

  //
  // Other methods
  //

  /**
   * Get the tm of the sequence.
   * @return the tm of the sequence
   */
  public final float getTm() {

    return getTm(50, 50);
  }

  /**
   * Get the tm of the sequence.
   * @param dnac DNA concentration [nM]
   * @param saltc salt concentration [mM
   * @return the tm temp for the sequence
   */
  public final float getTm(final float dnac, final float saltc) {

    return MeltingTemp.tmstalucDNA(this.sequence, dnac, saltc);
  }

  /**
   * Get the GC percent for the sequence.
   * @return the GC percent for the sequence
   */
  public final double getGCPercent() {

    if (this.sequence == null) {
      return Double.NaN;
    }

    final int len = this.sequence.length();

    int count = 0;

    for (int i = 0; i < len; i++) {

      if (this.sequence.charAt(i) == 'G' || this.sequence.charAt(i) == 'C') {
        count++;
      }
    }

    return (double) count / (double) len;
  }

  /**
   * Set the sequence as the reverse complement.
   */
  public final void reverseComplement() {

    this.sequence = reverseComplement(this.sequence);
  }

  /**
   * Get the sequence as the reverse complement. This method work only with
   * A,T,G and C bases.
   * @param sequence sequence to reverse complement
   * @return the reverse complement sequence
   */
  public static final String reverseComplement(final String sequence) {

    if (sequence == null) {
      return null;
    }

    final int len = sequence.length();
    final StringBuilder sb = new StringBuilder(len);

    for (int i = len - 1; i >= 0; i--) {

      switch (sequence.charAt(i)) {

      case 'A':
        sb.append('T');
        break;
      case 'T':
        sb.append('A');
        break;
      case 'G':
        sb.append('C');
        break;
      case 'C':
        sb.append('G');
        break;
      case 'a':
        sb.append('t');
        break;
      case 't':
        sb.append('t');
        break;
      case 'g':
        sb.append('c');
        break;
      case 'c':
        sb.append('g');
        break;

      default:
        throw new EoulsanRuntimeException("Invalid character in sequence: '"
            + sequence.charAt(i) + "'");
      }

    }
    return sb.toString();
  }

  //
  // Output methods
  //

  /**
   * Return the sequence object in string in Fasta format.
   * @return the sequence in Fasta format
   */
  public String toFasta() {

    return '>' + this.name + '\n' + this.sequence + '\n';
  }

  /**
   * Return the sequence object in string in Fasta format with a maximal width
   * for lines.
   * @return the sequence in Fasta format
   */
  public String toFasta(final int width) {

    if (width < 1)
      return toFasta();

    final StringBuilder sb = new StringBuilder();
    sb.append('>');
    sb.append(this.name);
    sb.append('\n');

    final int len = this.sequence.length();
    int pos = 0;

    while (pos < len) {

      final int nextPos = pos + width;
      sb.append(this.sequence.subSequence(pos, nextPos > len ? len : nextPos));
      sb.append('\n');
      pos = nextPos;
    }

    return sb.toString();
  }

  //
  // Parser methods
  //

  /**
   * Parse one fastq sequence
   * @param s string to parse
   */
  public void parseFasta(final String s) {

    if (s == null || s.trim().length() == 0) {
      setName(null);
      setSequence(null);
    }

    final String[] lines = s.split("\n");

    String name = null;
    final StringBuilder seq = new StringBuilder();

    boolean first = true;

    for (String line : lines) {

      final String trimmed = line.trim();

      if (first) {
        first = false;

        if (!trimmed.startsWith(">"))
          break;

        name = trimmed.substring(1).trim();
      }

      if (trimmed.startsWith(">"))
        break;

      seq.append(trimmed);
    }

    if (seq.length() > 0) {
      setName(name);
      setSequence(seq.toString());
    } else {
      setName(null);
      setSequence(null);
    }

  }

  //
  // Validation methods
  //

  /**
   * Validate the name field of the object.
   * @return true if the name field of this object is valid
   */
  protected final boolean validateName() {

    return this.name != null && this.name.length() > 0;
  }

  /**
   * Validate the sequence field of the object. The sequence must be not null,
   * have a length greater than 0 and all the letter of the sequence must be in
   * the current alphabet.
   * @return true if the sequence field of this object is valid
   */
  protected final boolean validateSequence() {

    final String seq = this.sequence;
    final int len = seq == null ? 0 : seq.length();

    if (len == 0)
      return false;

    final Alphabet alphabet = this.alphabet;
    final char[] array = sequence.toCharArray();

    for (int i = 0; i < len; i++)
      if (!alphabet.isLetterValid(array[i]))
        return false;

    return true;
  }

  /**
   * Check if the sequence is valid. To be valid a sequence must get a name and
   * a sequence with a length > 0. Only authorized bases are "ATGCNXatgcnx".
   * @return true if the sequence is validated
   */
  public boolean validate() {

    return validateName() && validateSequence();
  }

  //
  // Object methods
  //

  @Override
  public int hashCode() {

    return Utils.hashCode(this.id, this.name, this.description, this.alphabet,
        this.sequence);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this)
      return true;

    if (o == null)
      return false;

    if (!(o instanceof Sequence))
      return false;

    final Sequence that = (Sequence) o;

    return this.id == that.id
        && equal(this.name, that.name)
        && equal(this.description, that.description)
        && this.alphabet == that.alphabet
        && equal(this.sequence, that.sequence);
  }

  @Override
  public String toString() {

    return this.getName()
        + "{id=" + this.id + ", name=" + this.name + ", description="
        + this.description + ", alphabet=" + this.alphabet + ", sequence="
        + this.sequence + "}";
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public Sequence() {
  }

  /**
   * Public constructor.
   * @param id identifier of the sequence
   * @param name Name of the sequence
   * @param sequence Sequence of the sequence
   */
  public Sequence(final int id, final String name, final String sequence) {

    this.id = id;
    this.name = name;
    this.sequence = sequence;
  }

  /**
   * Public constructor.
   * @param id identifier
   * @param name Name of the sequence
   * @param sequence Sequence of the sequence
   * @param description Description of the sequence
   */
  public Sequence(final int id, final String name, final String sequence,
      final String description) {

    this.id = id;
    this.name = name;
    this.sequence = sequence;
    this.description = description;
  }

  /**
   * Public constructor
   * @param sequence Sequence object which value will be used in the new object
   */
  public Sequence(final Sequence sequence) {

    if (sequence == null)
      return;

    this.id = sequence.id;
    this.name = sequence.name;
    this.sequence = sequence.sequence;
    this.description = sequence.description;
  }

}
