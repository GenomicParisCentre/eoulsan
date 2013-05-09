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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingStandardFile;
import static fr.ens.transcriptome.eoulsan.util.Utils.checkNotNull;
import static fr.ens.transcriptome.eoulsan.util.Utils.checkState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class abstract implements a generic Mapper.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public abstract class AbstractSequenceReadsMapper implements
    SequenceReadsMapper {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String SYNC = AbstractSequenceReadsMapper.class
      .getName();

  @Override
  public boolean isIndexGeneratorOnly() {
    return false;
  }

  protected abstract String getIndexerExecutable();

  protected String[] getIndexerExecutables() {
    return new String[] {getIndexerExecutable()};
  }

  protected abstract List<String> getIndexerCommand(
      final String indexerPathname, final String genomePathname);

  private File archiveIndexFile;
  private File archiveIndexDir;

  private boolean pairedEnd = false;
  private FastqFormat fastqFormat;

  private int threadsNumber;
  private String mapperArguments = null;
  private File tempDir = EoulsanRuntime.getSettings().getTempDirectoryFile();

  private ReporterIncrementer incrementer;
  private String counterGroup;

  //
  // Getters
  //

  @Override
  public int getThreadsNumber() {

    return this.threadsNumber;
  }

  @Override
  public String getMapperArguments() {

    return this.mapperArguments;
  }

  @Override
  public List<String> getListMapperArguments() {
    if (getMapperArguments() == null)
      return Collections.emptyList();

    String[] tabMapperArguments = getMapperArguments().trim().split(" ");
    return Lists.newArrayList(tabMapperArguments);
  }

  /**
   * Test if the mapper is in pair end mode.
   * @return true if the mapper is in pair end mode
   */
  public boolean isPairEnd() {

    return this.pairedEnd;
  }

  /**
   * Get Fastq format.
   * @return the fastq format
   */
  public FastqFormat getFastqFormat() {

    return this.fastqFormat;
  }

  @Override
  public File getTempDirectory() {

    return this.tempDir;
  }

  /**
   * Convenient method to directly get the absolute path for the temporary
   * directory.
   * @return the absolute path to the tempory directory as a string
   */
  protected String getTempDirectoryPath() {

    return getTempDirectory().getAbsolutePath();
  }

  //
  // Setters
  //

  @Override
  public void setThreadsNumber(final int threadsNumber) {

    this.threadsNumber = threadsNumber;
  }

  @Override
  public void setMapperArguments(final String arguments) {

    if (arguments == null) {
      this.mapperArguments = "";
    } else {

      this.mapperArguments = arguments;
    }
  }

  @Override
  public void setTempDirectory(final File tempDirectory) {

    this.tempDir = tempDirectory;
  }

  //
  // Index creation
  //

  private static File uncompressGenomeIfNecessary(final File genomeFile,
      final File outputDir) throws FileNotFoundException, IOException {

    final CompressionType ct =
        CompressionType.getCompressionTypeByFilename(genomeFile.getName());

    if (ct == CompressionType.NONE)
      return genomeFile;

    // Define the output filename
    final File uncompressFile =
        new File(outputDir,
            StringUtils.filenameWithoutCompressionExtension(genomeFile
                .getName()));

    LOGGER.fine("Uncompress genome " + genomeFile + " to " + uncompressFile);

    // Create input stream
    final InputStream in =
        ct.createInputStream(FileUtils.createInputStream(genomeFile));

    // Create output stream
    final OutputStream out = FileUtils.createOutputStream(uncompressFile);

    // Uncompress
    FileUtils.copy(in, out);

    // Return the uncompress file
    return uncompressFile;
  }

  private void makeIndex(final File genomeFile, final File outputDir)
      throws IOException {

    checkNotNull(genomeFile, "genome file is null");
    checkNotNull(outputDir, "output directory is null");

    final File unCompressGenomeFile =
        uncompressGenomeIfNecessary(genomeFile, outputDir);

    LOGGER.fine("Start computing "
        + getMapperName() + " index for " + unCompressGenomeFile);
    final long startTime = System.currentTimeMillis();

    final String indexerPath;

    synchronized (SYNC) {
      indexerPath = install(getIndexerExecutables());
    }

    if (!outputDir.exists() && !outputDir.mkdir()) {
      throw new IOException("Unable to create directory for genome index");
    }

    final File tmpGenomeFile =
        new File(outputDir, unCompressGenomeFile.getName());

    // Create temporary symbolic link for genome
    if (!unCompressGenomeFile.equals(tmpGenomeFile)) {
      if (!FileUtils.createSymbolicLink(unCompressGenomeFile, tmpGenomeFile))
        throw new IOException("Unable to create the symbolic link in "
            + tmpGenomeFile + " directory for " + unCompressGenomeFile);
    }

    // Build the command line and compute the index
    final List<String> cmd = new ArrayList<String>();
    cmd.addAll(getIndexerCommand(indexerPath, tmpGenomeFile.getAbsolutePath()));

    LOGGER.fine(cmd.toString());

    final int exitValue = ProcessUtils.sh(cmd, tmpGenomeFile.getParentFile());

    if (exitValue != 0) {
      throw new IOException("Bad error result for index creation execution: "
          + exitValue);
    }

    // Remove symbolic link
    if (!tmpGenomeFile.delete()) {
      LOGGER.warning("Cannot remove symbolic link while after creating "
          + getMapperName() + " index");
    }

    final long endTime = System.currentTimeMillis();

    LOGGER.fine("Create the "
        + getMapperName() + " index in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));

  }

  @Override
  public void makeArchiveIndex(final File genomeFile,
      final File archiveOutputFile) throws IOException {

    LOGGER.fine("Start index computation");

    final String indexTmpDirPrefix =
        Globals.APP_NAME_LOWER_CASE
            + "-" + getMapperName().toLowerCase() + "-genomeindexdir-";

    LOGGER.fine("Want to create a temporary directory with prefix: "
        + indexTmpDirPrefix + " in " + getTempDirectory());
    System.out.println("makeArchiveIndex  tempo directory with prefix: "
        + indexTmpDirPrefix + " in " + getTempDirectory());

    final File indexTmpDir =
        File.createTempFile(indexTmpDirPrefix, "", getTempDirectory());

    if (!(indexTmpDir.delete())) {
      throw new IOException("Could not delete temp file ("
          + indexTmpDir.getAbsolutePath() + ")");
    }

    if (!indexTmpDir.mkdir()) {
      throw new IOException("Unable to create directory for genome index");
    }

    makeIndex(genomeFile, indexTmpDir);

    // Zip index files
    FileUtils.createZip(indexTmpDir, archiveOutputFile);

    // Remove temporary directory
    FileUtils.removeDirectory(indexTmpDir);

    LOGGER.fine("End index computation");
  }

  /**
   * Create the soap index in a zip archive.
   * @param is InputStream to use for the genome file
   * @throws IOException if an error occurs while creating the index
   */
  @Override
  public void makeArchiveIndex(final InputStream is,
      final File archiveOutputFile) throws IOException {

    checkNotNull(is, "Input steam is null");
    checkNotNull(archiveOutputFile, "Archive output file is null");

    LOGGER.fine("Copy genome to local disk before computating index");

    final File genomeTmpFile =
        File.createTempFile(Globals.APP_NAME_LOWER_CASE + "-genome", ".fasta",
            getTempDirectory());
    FileUtils.copy(is, FileUtils.createOutputStream(genomeTmpFile));

    makeArchiveIndex(genomeTmpFile, archiveOutputFile);

    if (!genomeTmpFile.delete()) {
      LOGGER.warning("Cannot delete temporary index zip file");
    }

  }

  protected String getIndexPath(final File archiveIndexDir,
      final String extension, final int extensionLength) throws IOException {

    final File[] indexFiles =
        FileUtils.listFilesByExtension(archiveIndexDir, extension);

    if (indexFiles == null || indexFiles.length != 1) {
      throw new IOException("Unable to get index file for " + getMapperName());
    }

    // Get the path to the index
    final String bwtFile = indexFiles[0].getAbsolutePath();

    return bwtFile.substring(0, bwtFile.length() - extensionLength);
  }

  private void unzipArchiveIndexFile(final File archiveIndexFile,
      final File archiveIndexDir) throws IOException {

    if (!archiveIndexFile.exists())
      throw new IOException("No index for the mapper found: "
          + archiveIndexFile);

    // Uncompress archive if necessary
    if (!archiveIndexDir.exists()) {

      if (!archiveIndexDir.mkdir())
        throw new IOException("Can't create directory for "
            + getMapperName() + " index: " + archiveIndexDir);

      LOGGER.fine("Unzip archiveIndexFile "
          + archiveIndexFile + " in " + archiveIndexDir);
      FileUtils.unzip(archiveIndexFile, archiveIndexDir);
    }

    FileUtils.checkExistingDirectoryFile(archiveIndexDir, getMapperName()
        + " index directory");

  }

  //
  // Mapping with File
  //

  @Override
  public final void mapPE(final File readsFile1, final File readsFile2,
      final GenomeDescription gd, final File samFile) throws IOException {

    FileUtils.copy(mapPE(readsFile1, readsFile2, gd), new FileOutputStream(
        samFile));
  }

  @Override
  public final InputStream mapPE(final File readsFile1, final File readsFile2,
      final GenomeDescription gd) throws IOException {

    LOGGER.fine("Mapping with " + getMapperName() + " in pair-end mode");

    checkState(isPairEnd(), "Cannot map a single reads file in pair-end mode.");
    checkNotNull(readsFile1, "readsFile1 is null");
    checkNotNull(readsFile2, "readsFile2 is null");

    checkExistingStandardFile(readsFile1,
        "readsFile1 not exits or is not a standard file.");
    checkExistingStandardFile(readsFile2,
        "readsFile2 not exits or is not a standard file.");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(archiveIndexFile, archiveIndexDir);

    // Process to mapping
    return internalMapPE(readsFile1, readsFile2, archiveIndexDir, gd);
  }

  @Override
  public final void mapSE(final File readsFile, final GenomeDescription gd,
      final File samFile) throws IOException {

    FileUtils.copy(mapSE(readsFile, gd), new FileOutputStream(samFile));
  }

  @Override
  public final InputStream mapSE(final File readsFile,
      final GenomeDescription gd) throws IOException {

    LOGGER.fine("Mapping with " + getMapperName() + " in single-end mode");

    checkState(!isPairEnd(), "Cannot map a single reads file in pair-end mode.");
    checkNotNull(readsFile, "readsFile1 is null");
    checkExistingStandardFile(readsFile,
        "readsFile1 not exits or is not a standard file.");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(archiveIndexFile, archiveIndexDir);

    // Process to mapping
    return internalMapSE(readsFile, archiveIndexDir, gd);
  }

  protected abstract InputStream internalMapPE(final File readsFile1,
      final File readsFile2, final File archiveIndex, final GenomeDescription gd)
      throws IOException;

  protected abstract InputStream internalMapSE(final File readsFile,
      final File archiveIndex, final GenomeDescription gd) throws IOException;

  //
  // Mapping with streams
  //

  @Override
  public final MapperProcess mapPE(final GenomeDescription gd)
      throws IOException {

    LOGGER.fine("Mapping with " + getMapperName() + " in pair-end mode");

    checkState(isPairEnd(), "Cannot map a single reads file in pair-end mode.");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(archiveIndexFile, archiveIndexDir);

    // Process to mapping
    final MapperProcess result = internalMapPE(archiveIndexDir, gd);

    // Set counter
    result.setIncrementer(this.incrementer, this.counterGroup);

    return result;
  }

  @Override
  public final MapperProcess mapSE(final GenomeDescription gd)
      throws IOException {

    LOGGER.fine("Mapping with " + getMapperName() + " in single-end mode");

    checkState(!isPairEnd(), "Cannot map a single reads file in pair-end mode.");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(archiveIndexFile, archiveIndexDir);

    // Process to mapping
    final MapperProcess result = internalMapSE(archiveIndexDir, gd);

    // Set counter
    result.setIncrementer(this.incrementer, this.counterGroup);

    return result;
  }

  protected abstract MapperProcess internalMapPE(final File archiveIndex,
      final GenomeDescription gd) throws IOException;

  protected abstract MapperProcess internalMapSE(final File archiveIndex,
      final GenomeDescription gd) throws IOException;

  //
  // Init
  //

  /**
   * Initialize mapper.
   * @param pairEnd true if the mapper is in pair end mode.
   * @param fastqFormat Fastq format
   * @param archiveIndexFile genome index for the mapper as a ZIP file
   * @param archiveIndexDir uncompressed directory for the genome index for the
   * @param incrementer Objet to use to increment counters
   * @param counterGroup counter name group
   */
  @Override
  public void init(final boolean pairEnd, final FastqFormat fastqFormat,
      final File archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    checkNotNull(incrementer, "incrementer is null");
    checkNotNull(counterGroup, "counterGroup is null");

    checkNotNull(archiveIndexFile, "archiveIndex is null");
    checkNotNull(archiveIndexDir, "archiveIndexDir is null");
    checkExistingStandardFile(archiveIndexFile,
        "The archive index file not exits or is not a standard file.");

    this.pairedEnd = pairEnd;
    this.fastqFormat = fastqFormat;
    this.archiveIndexFile = archiveIndexFile;
    this.archiveIndexDir = archiveIndexDir;
    this.incrementer = incrementer;
    this.counterGroup = counterGroup;
  }

  //
  // Utilities methods
  //

  /**
   * Install a list of binaries bundled in the jar in a temporary directory.
   * This method automatically use the temporary directory defined in the object
   * for the path where to install the binary.
   * @param binaryFilenames programs to install
   * @return a string with the path of the last installed binary
   * @throws IOException if an error occurs while installing binary
   */
  protected String install(final String... binaryFilenames) throws IOException {

    String result = null;

    if (binaryFilenames != null)
      for (String binaryFilename : binaryFilenames)
        result = install(binaryFilename);

    return result;
  }

  /**
   * Install a binary bundled in the jar in a temporary directory. This method
   * automatically use the temporary directory defined in the object for the
   * path where to install the binary.
   * @param binaryFilename program to install
   * @return a string with the path of the installed binary
   * @throws IOException if an error occurs while installing binary
   */
  protected String install(final String binaryFilename) throws IOException {

    return BinariesInstaller.install(binaryFilename, getTempDirectoryPath());
  }

}
