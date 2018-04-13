package fr.ens.biologie.genomique.eoulsan.bio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;

/**
 * This class define a BedEntry. <b>Warning<b>: the coordinates stored in the
 * class are 1-based to be coherent with the other classes of the bio packages.
 * However the toBEDXX() methods generate output in 1-based coordinates.
 * @author Laurent Jourdren
 * @since 2.2
 */
public class BEDEntry {

  private final Map<String, List<String>> metaData = new LinkedHashMap<>();
  private String chromosomeName;
  private int chromosomeStart;
  private int chromosomeEnd;
  private String name;
  private String score;
  private char strand;
  private int thickStart;
  private int thickEnd;
  private String rgbItem;
  private final List<GenomicInterval> blocks = new ArrayList<>();

  //
  // Getters
  //

  /**
   * Get metadata keys names.
   * @return the metadata keys names
   */
  public final Set<String> getMetadataKeyNames() {

    if (this.metaData == null) {
      return Collections.emptySet();
    }

    return Collections.unmodifiableSet(this.metaData.keySet());
  }

  /**
   * test if a metadata key exists.
   * @param key key name of the metadata
   * @return true if the entry in the meta data exists
   */
  public final boolean isMetaDataEntry(final String key) {

    if (key == null) {
      return false;
    }

    return this.metaData.containsKey(key);
  }

  /**
   * Get the metadata values for a key.
   * @param key name of the metadata entry
   * @return the values of the attribute or null if the metadata name does not
   *         exists
   */
  public final List<String> getMetadataEntryValues(final String key) {

    if (key == null) {
      return null;
    }

    final List<String> list = this.metaData.get(key);

    if (list == null) {
      return null;
    }

    return Collections.unmodifiableList(list);
  }

  /**
   * Get chromosome name.
   * @return the chromosome name
   */
  public String getChromosomeName() {
    return chromosomeName;
  }

  /**
   * Get the starting position of the feature in the chromosome (zero based).
   * @return the starts of the feature
   */
  public int getChromosomeStart() {
    return chromosomeStart;
  }

  /**
   * Get the ending position of the feature in the chromosome (one based).
   * @return the ends of the feature
   */
  public int getChromosomeEnd() {
    return chromosomeEnd;
  }

  /**
   * Get the name of the BED feature.
   * @return the name of the BED feature
   */
  public String getName() {
    return name;
  }

  /**
   * Get the score of the feature.
   * @return the score of the feature
   */
  public String getScore() {
    return score;
  }

  /**
   * Get the strand of the feature
   * @return the strand of the feature
   */
  public char getStrand() {
    return strand;
  }

  /**
   * Get the starting position at which the feature is drawn thickly.
   * @return the starting position at which the feature is drawn thickly
   */
  public int getThickStart() {
    return thickStart;
  }

  /**
   * Get the ending position at which the feature is drawn thickly.
   * @return the ending position at which the feature is drawn thickly
   */
  public int getThickEnd() {
    return thickEnd;
  }

  /**
   * Get the RGB value of the item.
   * @return the RGB value of the item
   */
  public String getRgbItem() {

    return this.rgbItem;
  }

  /**
   * Get the block count.
   * @return the block count
   */
  public int getBlockCount() {
    return this.blocks.size();
  }

  /**
   * Get the block sizes.
   * @return the block sizes
   */
  public List<Integer> getBlockSizes() {

    final List<Integer> result = new ArrayList<>(this.blocks.size());

    for (GenomicInterval b : this.blocks) {
      result.add(b.getLength());
    }

    return result;
  }

  /**
   * Get the block starts.
   * @return the block starts
   */
  public List<Integer> getBlockStarts() {

    final List<Integer> result = new ArrayList<>(this.blocks.size());

    for (GenomicInterval b : this.blocks) {
      result.add(b.getStart());
    }

    return result;
  }

  /**
   * Get the block starts.
   * @return the block starts
   */
  public List<Integer> getBlockEnds() {

    final List<Integer> result = new ArrayList<>(this.blocks.size());

    for (GenomicInterval b : this.blocks) {
      result.add(b.getEnd());
    }

    return result;
  }

  /**
   * Get the block.
   * @return the block starts
   */
  public List<GenomicInterval> getBlocks() {

    return Collections.unmodifiableList(this.blocks);
  }

  //
  // Setters
  //

  /**
   * Add metadata entry value.
   * @param key name of key of the metadata entry
   * @param value The value
   * @return true if the value is correctly added to the metadata
   */
  public final boolean addMetaDataEntry(final String key, final String value) {

    if (key == null || value == null) {
      return false;
    }

    final List<String> list;

    if (!this.metaData.containsKey(key)) {
      list = new ArrayList<>();
      this.metaData.put(key, list);
    } else {
      list = this.metaData.get(key);
    }

    list.add(value);

    return true;
  }

  /**
   * Add metadata entries values. Stop at first entry that fail to be added.
   * @param entries the entries to add
   * @return true if all the entries are correctly added to the metadata
   */
  public final boolean addMetaDataEntries(
      final Map<String, List<String>> entries) {

    if (entries == null) {
      return false;
    }

    for (Map.Entry<String, List<String>> e : entries.entrySet()) {
      for (String v : e.getValue()) {

        if (!addMetaDataEntry(e.getKey(), v)) {
          return false;
        }

      }
    }

    return true;
  }

  /**
   * Remove a metadata entry.
   * @param key key of the metadata entry to remove
   * @return true if the entry is removed
   */
  public final boolean removeMetaDataEntry(final String key) {

    if (this.metaData.containsKey(key)) {
      return false;
    }

    return this.metaData.remove(key) != null;
  }

  /**
   * Set chromosome name.
   * @param chromosomeName the chromosome name
   */
  public void setChromosomeName(final String chromosomeName) {

    if (chromosomeName == null) {
      throw new NullPointerException("chromosomeName argument cannot be null");
    }

    this.chromosomeName = chromosomeName;
  }

  /**
   * Set the starting position of the feature in the chromosome (zero based).
   * @param chromosomeStart the start of the feature
   */
  public void setChromosomeStart(final int chromosomeStart) {

    if (chromosomeStart < 0) {
      throw new IllegalArgumentException(
          "chromosomeStart argument cannot be lower than zero: "
              + chromosomeStart);
    }

    this.chromosomeStart = chromosomeStart;
  }

  /**
   * Get the ending position of the feature in the chromosome (one based).
   * @return the ends of the feature
   */
  public void setChromosomeEnd(final int chromosomeEnd) {

    if (chromosomeEnd < 0) {
      throw new IllegalArgumentException(
          "chromosomeEnd argument cannot be lower than zero: " + chromosomeEnd);
    }

    this.chromosomeEnd = chromosomeEnd;
  }

  /**
   * Set the name of the BED feature.
   * @param name the name of the BED feature
   */
  public void setName(final String name) {

    this.name = name;
  }

  /**
   * Get the score of the feature.
   * @return the score of the feature
   */
  public void setScore(final String score) {

    this.score = score;
  }

  /**
   * Get the score of the feature.
   * @return the score of the feature
   */
  public void setScore(final int score) {

    if (score < 0 || score > 1000) {
      throw new IllegalArgumentException(
          "score must be in the range 0 - 1000: " + score);
    }

    this.score = Integer.toString(score);
  }

  /**
   * Get the score of the feature.
   * @return the score of the feature
   */
  public void setScore(final double score) {

    this.score = Double.toString(score);
  }

  /**
   * Get the strand of the feature
   * @return the strand of the feature
   */
  public void setStrand(final char strand) {

    switch (strand) {
    case '-':
    case '+':
    case 0:
      this.strand = strand;
      break;

    default:
      throw new IllegalArgumentException("Invalid strand value: " + strand);
    }
  }

  /**
   * Set the starting position at which the feature is drawn thickly.
   * @param thickStart the starting position at which the feature is drawn
   *          thickly
   */
  public void setThickStart(final int thickStart) {

    if (thickStart < 0) {
      throw new IllegalArgumentException(
          "thickStart argument cannot be lower than zero: " + thickStart);
    }

    this.thickStart = thickStart;
  }

  /**
   * Set the ending position at which the feature is drawn thickly.
   * @param thickEnd the ending position at which the feature is drawn thickly
   */
  public void setThickEnd(final int thickEnd) {

    if (thickEnd < 0) {
      throw new IllegalArgumentException(
          "thickEnd argument cannot be lower than zero: " + thickEnd);
    }

    this.thickEnd = thickEnd;
  }

  /**
   * Set the RGB value of the item.
   * @param rgbItem the RGB value of the item
   */
  public void setRgbItem(String rgbItem) {

    this.rgbItem = rgbItem;
  }

  /**
   * Set the RGB value of the item.
   * @param rgbItem the RGB value of the item
   */
  public void setRgbItem(int r, int g, int b) {

    if (r < 0 || r > 255) {
      throw new IllegalArgumentException(
          "red value must be in [0-255] interval: " + r);
    }

    if (g < 0 || g > 255) {
      throw new IllegalArgumentException(
          "green value must be in [0-255] interval: " + g);
    }

    if (b < 0 || b > 255) {
      throw new IllegalArgumentException(
          "blue value must be in [0-255] interval: " + b);
    }

    this.rgbItem = "" + r + ',' + g + ',' + b;
  }

  /**
   * Add a block to the list of block.
   * @param block the block to add
   */
  public void addBlock(final int startBlock, final int endBlock) {

    this.blocks.add(new GenomicInterval(this.chromosomeName, startBlock,
        endBlock, this.strand == 0 ? '.' : this.strand));
  }

  /**
   * Add a block to the list of block.
   * @param block the block to add
   */
  public void removeBlock(final int startBlock, final int endBlock) {

    GenomicInterval block = new GenomicInterval(this.chromosomeName, startBlock,
        endBlock, this.strand == 0 ? '.' : this.strand);

    this.blocks.remove(block);
  }

  //
  // toString methods
  //

  /**
   * Convert the object to a BED 3 columns entry.
   * @return a BED entry
   */
  public String toBED3() {

    return this.chromosomeName
        + '\t' + (this.chromosomeStart - 1) + '\t' + (this.chromosomeEnd + 1);
  }

  /**
   * Convert the object to a BED 4 columns entry.
   * @return a BED entry
   */
  public String toBED4() {

    return this.chromosomeName
        + '\t' + (this.chromosomeStart - 1) + '\t' + (this.chromosomeEnd + 1)
        + '\t' + (this.name == null ? "" : this.name);
  }

  /**
   * Convert the object to a BED 5 columns entry.
   * @return a BED entry
   */
  public String toBED5() {

    return this.chromosomeName
        + '\t' + (this.chromosomeStart - 1) + '\t' + (this.chromosomeEnd + 1)
        + '\t' + (this.name != null ? this.name : "") + '\t'
        + (this.score != null ? this.score : '0');
  }

  /**
   * Convert the object to a BED 6 columns entry.
   * @return a BED entry
   */
  public String toBED6() {

    return this.chromosomeName
        + '\t' + (this.chromosomeStart - 1) + '\t' + (this.chromosomeEnd + 1)
        + '\t' + (this.name != null ? this.name : "") + '\t'
        + (this.score != null ? this.score : '0') + '\t'
        + (this.strand != 0 ? this.strand : "");
  }

  /**
   * Convert the object to a BED 12 columns entry.
   * @return a BED entry
   */
  public String toBED12() {

    StringBuilder sb = new StringBuilder();

    sb.append(this.chromosomeName);
    sb.append('\t');
    sb.append(this.chromosomeStart - 1);
    sb.append('\t');
    sb.append(this.chromosomeEnd + 1);
    sb.append('\t');
    sb.append(this.name != null ? this.name : "");
    sb.append('\t');
    sb.append(this.score != null ? this.score : '0');
    sb.append('\t');
    sb.append(this.strand != 0 ? this.strand : "");
    sb.append('\t');
    sb.append(this.thickStart - 1);
    sb.append('\t');
    sb.append(this.thickEnd + 1);
    sb.append('\t');
    sb.append(this.rgbItem != null ? this.rgbItem : "");
    sb.append('\t');
    sb.append(getBlockCount());
    sb.append('\t');

    for (int i : getBlockStarts()) {
      sb.append(i - 1);
      sb.append(',');
    }
    sb.append('\t');

    for (int i : getBlockSizes()) {
      sb.append(i - 1);
      sb.append(',');
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    return toBED12();
  }

  //
  // Parse methods
  //

  /**
   * Parse an entry.
   * @param s the entry to parse
   * @throws BadBioEntryException if the entry is malformed
   */
  public void parse(final String s) throws BadBioEntryException {

    if (s == null) {
      throw new NullPointerException("s argument cannot be null");
    }

    // Count the number of fields
    int count = s.length() - s.replace("\t", "").length() + 1;

    parse(s, count);
  }

  /**
   * Parse an entry.
   * @param s the entry to parse
   * @param requiredFieldCount the required field count
   * @throws BadBioEntryException if the entry is malformed
   */
  public void parse(final String s, final int requiredFieldCount)
      throws BadBioEntryException {

    if (s == null) {
      throw new NullPointerException("s argument cannot be null");
    }

    if (requiredFieldCount < 3
        || requiredFieldCount > 12
        || (requiredFieldCount > 6 && requiredFieldCount < 12)) {
      throw new IllegalArgumentException(
          "Invalid required field count: " + requiredFieldCount);
    }

    final Splitter splitter = Splitter.on('\t').trimResults();

    List<String> fields = splitter.splitToList(s);

    this.chromosomeName = fields.get(0);
    if (this.chromosomeName.isEmpty()) {
      throw new BadBioEntryException("chromosome name is empty", s);
    }

    this.chromosomeStart = parseCoordinate(fields.get(1), 1, Integer.MIN_VALUE);
    this.chromosomeEnd = parseCoordinate(fields.get(2), -1, Integer.MAX_VALUE);
    this.blocks.clear();

    if (requiredFieldCount == 3) {
      return;
    }

    this.name = fields.get(3);

    if (requiredFieldCount == 4) {
      return;
    }

    this.score = fields.get(4);

    if (requiredFieldCount == 5) {
      return;
    }

    switch (fields.get(5)) {

    case "+":
    case "-":
      this.strand = fields.get(5).charAt(0);
      break;

    default:
      this.strand = 0;
    }

    if (requiredFieldCount == 6) {
      return;
    }

    // Parse RGB
    setRgbItem(fields.get(8));

    this.thickStart = parseCoordinate(fields.get(6), 1, Integer.MIN_VALUE);
    this.thickEnd = parseCoordinate(fields.get(7), -1, Integer.MAX_VALUE);

    int blockCount = parseInt(fields.get(9), -1);

    if (blockCount == -1) {
      throw new BadBioEntryException("Invalid block count: " + fields.get(9),
          s);
    }

    List<Integer> starts = parseIntList(fields.get(10));
    List<Integer> sizes = parseIntList(fields.get(11));

    if (starts.size() != blockCount) {
      throw new BadBioEntryException("Invalid block starts: "
          + blockCount + "\t" + starts.size() + "\t" + fields.get(10), s);
    }
    if (sizes.size() != blockCount) {
      throw new BadBioEntryException("Invalid block sizes: " + fields.get(11),
          s);
    }

    for (int i = 0; i < blockCount; i++) {
      addBlock(starts.get(i) + 1, starts.get(i) + 1 + sizes.get(i));
    }

  }

  /**
   * Parse an integer in a String.
   * @param s String to parse
   * @param defaultValue the default value if the string is null
   * @return the parsed integer
   */
  private static int parseInt(final String s, final int defaultValue) {

    if (s == null) {
      return defaultValue;
    }

    try {
      return Integer.parseInt(s.trim());
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Parse coordinates in a String.
   * @param s the string to parse
   * @param diff the difference between position in string in internal storage
   * @param defaultValue the default value
   * @return the parsed integer
   */
  private static int parseCoordinate(final String s, final int diff,
      final int defaultValue) {

    if (s == null) {
      return defaultValue;
    }

    try {
      return Integer.parseInt(s.trim()) + diff;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Parse integers in a string.
   * @param s String to parse
   * @return a list with the parsed integers
   */
  private List<Integer> parseIntList(final String s) {

    final Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();

    List<Integer> result = new ArrayList<>();
    for (String v : splitter.split(s)) {

      try {
        result.add(Integer.parseInt(v));
      } catch (NumberFormatException e) {
      }
    }

    return result;
  }

  //
  // Other methods
  //

  /**
   * Clear the entry.
   */
  public void clear() {

    this.chromosomeName = null;
    this.chromosomeStart = 0;
    this.chromosomeEnd = 0;
    this.name = null;
    this.score = null;
    this.strand = 0;
    this.thickStart = 0;
    this.thickEnd = 0;
    this.rgbItem = "0";
    this.blocks.clear();

  }

  /**
   * Clear metadata of the entry.
   */
  public final void clearMetaData() {

    this.metaData.clear();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public BEDEntry() {

    clear();
  }

}