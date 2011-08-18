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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a genome description.
 * @author Laurent Jourdren
 */
public class GenomeDescription {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String PREFIX = "genome.";
  private static final String NAME_PREFIX = PREFIX + "name";
  private static final String LENGTH_PREFIX = PREFIX + "length";
  private static final String MD5_PREFIX = PREFIX + "md5";
  private static final String SEQUENCE_PREFIX = PREFIX + "sequence.";
  private static final String SEQUENCES_COUNT_PREFIX = PREFIX + "sequences";

  private String genomeName;
  private Map<String, Integer> sequences = Maps.newHashMap();
  private List<String> sequencesOrder = Lists.newArrayList();
  private String md5Digest;

  //
  // Setters
  //

  /**
   * Set the genome name.
   * @param genomeName name of the genome
   */
  private void setGenomeName(final String genomeName) {

    this.genomeName = genomeName;
  }

  /**
   * Add a sequence.
   * @param sequenceName name of the sequence
   * @param sequenceLength length of the sequence
   */
  public void addSequence(final String sequenceName, final int sequenceLength) {

    LOGGER.fine("Add sequence: "
        + sequenceName + " with " + sequenceLength + " pb");

    if (!this.sequences.containsKey(sequenceName))
      this.sequencesOrder.add(sequenceName);

    this.sequences.put(sequenceName, sequenceLength);

  }

  /**
   * Set the md5 digest of the genome file
   * @param md5Digest the md5 digest
   */
  public void setMD5Digest(final String md5Digest) {

    this.md5Digest = md5Digest;
  }

  //
  // Getters
  //

  /**
   * Get the genome name.
   * @return the genome name
   */
  public String getGenomeName() {

    return this.genomeName;
  }

  /**
   * Get the length of a sequence
   * @param sequenceName name of the sequence
   * @return the length of the sequence or -1 if the sequence does not exists
   */
  public int getSequenceLength(final String sequenceName) {

    if (this.sequences.containsKey(sequenceName)) {

      return this.sequences.get(sequenceName);
    }

    return -1;
  }

  /**
   * Get the names of the sequences.
   * @return a set with the name of the sequence
   */
  public List<String> getSequencesNames() {

    return Collections.unmodifiableList(this.sequencesOrder);
  }

  /**
   * Get the md5 digest for the genome.
   * @return the md5 digest
   */
  public String getMD5Digest() {

    return this.md5Digest;
  }

  /**
   * Get the number of sequences in the genome.
   * @return the number of sequences in the genome
   */
  public int getSequenceCount() {

    return this.sequences.size();
  }

  /**
   * Get the genome length;
   * @return the genome length
   */
  public int getGenomeLength() {

    int count = 0;

    for (Map.Entry<String, Integer> e : this.sequences.entrySet())
      count += e.getValue();

    return count;
  }

  //
  // Save description
  //

  /**
   * Save genome description.
   * @param os OutputStream to use for genome description writing
   */
  public void save(final OutputStream os) throws IOException {

    Preconditions.checkNotNull(os, "OutputStream is null");

    final Writer writer = FileUtils.createFastBufferedWriter(os);

    if (this.genomeName != null)
      writer.write(NAME_PREFIX + "=" + getGenomeName() + '\n');

    if (this.md5Digest != null)
      writer.write(MD5_PREFIX + "=" + getMD5Digest() + '\n');

    writer.write(SEQUENCES_COUNT_PREFIX + '=' + getSequenceCount() + '\n');

    writer.write(LENGTH_PREFIX + '=' + getGenomeLength() + '\n');

    for (String seqName : getSequencesNames()) {

      writer.write(SEQUENCE_PREFIX
          + seqName + "=" + getSequenceLength(seqName) + "\n");
    }

    writer.close();
  }

  /**
   * Save genome description.
   * @param file output file
   */
  public void save(final File file) throws FileNotFoundException, IOException {

    Preconditions.checkNotNull(file, "File is null");
    save(new FileOutputStream(file));
  }

  //
  // Load description
  //

  /**
   * Load genome description.
   * @param is InputStream to use
   */
  public static GenomeDescription load(final InputStream is) throws IOException {

    Preconditions.checkNotNull(is, "InputStream is null");

    final GenomeDescription result = new GenomeDescription();

    final BufferedReader read = FileUtils.createBufferedReader(is);

    String line = null;

    final Splitter splitter = Splitter.on('=').trimResults();

    while ((line = read.readLine()) != null) {

      final List<String> fields =
          Lists.newArrayList(splitter.split(line.toString()));

      if (fields.size() > 1) {

        final String key = fields.get(0);

        if (key.startsWith(NAME_PREFIX))
          result.setGenomeName(fields.get(1));
        if (key.startsWith(MD5_PREFIX))
          result.setMD5Digest(fields.get(1));
        else
          try {
            if (key.startsWith(SEQUENCE_PREFIX))
              result.addSequence(key.substring(SEQUENCE_PREFIX.length()),
                  Integer.parseInt(fields.get(1)));
          } catch (NumberFormatException e) {

          }
      }
    }

    is.close();

    return result;
  }

  /**
   * Load genome description.
   * @param file File to use
   */
  public static GenomeDescription load(final File file) throws IOException {

    checkNotNull(file, "File is null");
    return load(new FileInputStream(file));
  }

  //
  // Static methods
  //

  /**
   * Create a GenomeDescription object from a Fasta file.
   * @param genomeFastaIs InputStream
   */
  public static GenomeDescription createGenomeDescFromFasta(
      final File genomeFastaFile) throws BadBioEntryException, IOException {

    checkNotNull(genomeFastaFile, "The genome file is null");

    return createGenomeDescFromFasta(
        FileUtils.createInputStream(genomeFastaFile), genomeFastaFile.getName());
  }

  /**
   * Create a GenomeDescription object from a Fasta file.
   * @param genomeFastaIs InputStream
   */
  public static GenomeDescription createGenomeDescFromFasta(
      final InputStream genomeFastaIs, final String filename)
      throws BadBioEntryException, IOException {

    Preconditions.checkNotNull(genomeFastaIs,
        "The input stream of the genome is null");

    final GenomeDescription result = new GenomeDescription();
    result.setGenomeName(StringUtils.basename(filename));

    final BufferedReader br = FileUtils.createBufferedReader(genomeFastaIs);
    MessageDigest md5Digest;

    try {
      md5Digest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      md5Digest = null;
    }

    String line = null;

    String currentChr = null;
    int currentSize = 0;

    while ((line = br.readLine()) != null) {

      line = line.trim();
      if ("".equals(line))
        continue;

      if (line.startsWith(">")) {
        if (currentChr != null) {
          result.addSequence(currentChr, currentSize);
        }

        // Get sequence name
        currentChr = parseChromosomeName(line);

        // Update digest with chromosome name
        if (md5Digest != null)
          md5Digest.update(currentChr.getBytes());

        currentSize = 0;
      } else {
        if (currentChr == null)
          throw new BadBioEntryException(
              "No fasta header found at the start of the fasta file.", line);

        final String trimmedLine = line.trim();

        // Update digest with current sequence line
        if (md5Digest != null)
          md5Digest.update(trimmedLine.getBytes());

        // Add the number of bases of the line to currentSize
        currentSize += checkBases(trimmedLine);
      }
    }
    result.addSequence(currentChr, currentSize);
    if (md5Digest != null)
      result.setMD5Digest(digestToString(md5Digest));

    genomeFastaIs.close();

    return result;
  }

  private static String parseChromosomeName(final String fastaHeader) {

    if (fastaHeader == null)
      return null;

    final String s = fastaHeader.substring(1).trim();
    String[] fields = s.split("\\s");

    if (fields == null || fields.length == 0)
      return null;

    return fields[0];
  }

  private static int checkBases(final String s) throws BadBioEntryException {

    // TODO use alphabet here

    final char[] array = s.toCharArray();

    for (int i = 0; i < array.length; i++)
      switch (array[i]) {

      case 'A':
      case 'a':
      case 'C':
      case 'c':
      case 'G':
      case 'g':
      case 'T':
      case 't':
      case 'U':
      case 'R':
      case 'Y':
      case 'K':
      case 'M':
      case 'S':
      case 'W':
      case 'B':
      case 'D':
      case 'H':
      case 'V':
      case 'N':
      case 'n':
      case 'X':
      case 'x':

        break;

      default:
        throw new BadBioEntryException("Invalid base in genome: " + array[i], s);
      }

    return array.length;
  }

  private static final String digestToString(final MessageDigest md) {

    if (md == null)
      return null;

    final BigInteger bigInt = new BigInteger(1, md.digest());

    return bigInt.toString(16);
  }

  //
  // Other methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("sequences", this.sequences.size())
        .toString();
  }

}
