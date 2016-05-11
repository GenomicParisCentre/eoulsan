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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.design;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ADDITIONAL_ANNOTATION_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GTF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_FASTA;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;
import fr.ens.biologie.genomique.aozan.illumina.samplesheet.io.SampleSheetCSVReader;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.bio.IlluminaReadId;
import fr.ens.biologie.genomique.eoulsan.bio.io.FastqReader;
import fr.ens.biologie.genomique.eoulsan.core.Naming;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class allow to easily build Design object from files paths.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DesignBuilder {

  private static final int MAX_FASTQ_ENTRIES_TO_READ = 10000;

  private final DataFormatRegistry dfr = DataFormatRegistry.getInstance();
  private final Map<String, List<FastqEntry>> fastqMap = new LinkedHashMap<>();
  private final Map<String, String> prefixMap = new HashMap<>();
  private DataFile genomeFile;
  private DataFile gffFile;
  private DataFile gtfFile;
  private DataFile additionalAnnotationFile;

  /**
   * This class define a exception thrown when a fastq file is empty.
   * @author Laurent Jourdren
   */
  private static class EmptyFastqException extends EoulsanException {

    private static final long serialVersionUID = 5672764893232380662L;

    /**
     * Public constructor
     * @param msg exception message
     */
    public EmptyFastqException(final String msg) {

      super(msg);
    }

  }

  /**
   * This inner class define a fastq entry.
   * @author Laurent Jourdren
   */
  private static class FastqEntry {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final DataFile path;
    private final String sampleName;
    private final String sampleDesc;
    private final String sampleOperator;
    private final String sampleDate;
    private final String firstReadId;
    private final String prefix;
    private final int pairMember;

    private static String getDate(final DataFile file) {

      try {
        long last = file.getMetaData().getLastModified();

        return new SimpleDateFormat(DATE_FORMAT).format(new Date(last));

      } catch (IOException e) {
        return null;
      }

    }

    //
    // static methods
    //

    /**
     * Get the identifier of the first read of a fastq file.
     * @param f the input file
     * @return the identifier of the first read of a fastq file as a string
     * @throws EoulsanException if an error occurs while reading the file or if
     *           the read format is invalid
     */
    private static String getFirstReadSeqId(final DataFile f)
        throws EoulsanException {

      final FastqReader reader;
      try {
        reader = new FastqReader(f.open());

        if (!reader.hasNext()) {
          reader.close();
          reader.throwException();
          throw new EmptyFastqException(
              "Fastq file is empty: " + f.getSource());
        }

        reader.close();
        reader.throwException();

        return reader.next().getName();
      } catch (IOException | BadBioEntryException e) {
        throw new EoulsanException(e);
      }

    }

    private Object[] initPairedEnd() {

      String prefix = this.firstReadId;
      int pairMember = -1;

      try {
        IlluminaReadId irid = new IlluminaReadId(this.firstReadId);
        prefix = irid.getInstrumentId()
            + "\t" + irid.getFlowCellLane() + "\t"
            + irid.getTileNumberInFlowCellLane() + "\t"
            + irid.getXClusterCoordinateInTile() + "\t"
            + irid.getYClusterCoordinateInTile();

        pairMember = irid.getPairMember();

      } catch (EoulsanException e) {

        if (this.firstReadId.endsWith("/1")) {
          prefix = this.firstReadId.substring(0, this.firstReadId.length() - 3);
          pairMember = 1;
        } else if (this.firstReadId.endsWith("/2")) {
          prefix = this.firstReadId.substring(0, this.firstReadId.length() - 3);
          pairMember = 2;
        } else {
          pairMember = 1;
        }
      }

      return new Object[] {prefix, pairMember};
    }

    //
    // Object methods
    //

    @Override
    public boolean equals(final Object obj) {

      if (obj == this) {
        return true;
      }

      if (!(obj instanceof FastqEntry)) {
        return false;
      }

      final FastqEntry that = (FastqEntry) obj;

      return this.path.equals(that.path);
    }

    @Override
    public int hashCode() {

      return this.path.hashCode();
    }

    @Override
    public String toString() {

      final StringBuilder sb = new StringBuilder();

      sb.append("FastqEntry(Sample: ");
      sb.append(this.sampleName);

      if (this.sampleDesc != null) {
        sb.append(", Description: ");
        sb.append(this.sampleDesc);
      }

      if (this.sampleOperator != null) {
        sb.append(", Operator: ");
        sb.append(this.sampleOperator);
      }

      sb.append(", Path: ");
      sb.append(this.path);
      sb.append(")");

      return sb.toString();
    }

    //
    // Constructors
    //

    public FastqEntry(final DataFile path) throws EoulsanException {

      this.path = path;
      this.sampleName = StringUtils.basename(path.getName());
      this.sampleDesc = null;
      this.sampleOperator = null;
      this.sampleDate = getDate(path);
      this.firstReadId = getFirstReadSeqId(path);
      final Object[] array = initPairedEnd();
      this.prefix = (String) array[0];
      this.pairMember = (Integer) array[1];
    }

    public FastqEntry(final DataFile path, final String sampleName,
        final String sampleDesc, final String sampleOperator)
        throws EoulsanException {

      this.path = path;
      this.sampleName = sampleName;
      this.sampleDesc = sampleDesc;
      this.sampleOperator = sampleOperator;
      this.sampleDate = getDate(path);
      this.firstReadId = getFirstReadSeqId(path);
      final Object[] array = initPairedEnd();
      this.prefix = (String) array[0];
      this.pairMember = (Integer) array[1];
    }

  }

  /**
   * Add a file to the design builder
   * @param file file to add
   * @throws EoulsanException if the file does not exist
   */
  public void addFile(final DataFile file) throws EoulsanException {

    if (file == null) {
      return;
    }

    if (!file.exists()) {
      throw new EoulsanException(
          "File " + file + " does not exist or is not a regular file.");
    }

    final String extension =
        StringUtils.extensionWithoutCompressionExtension(file.getName());

    DataFileMetadata md = null;

    try {
      md = file.getMetaData();
    } catch (IOException e) {
    }

    if (isDataFormatExtension(DataFormats.READS_FASTQ, extension, md)) {

      final FastqEntry entry;

      try {
        entry = new FastqEntry(file);
      } catch (EmptyFastqException e) {
        getLogger().warning(e.getMessage());
        return;
      }

      final String sampleId;

      if (this.prefixMap.containsKey(entry.prefix)) {
        sampleId = this.prefixMap.get(entry.prefix);
      } else {
        sampleId = entry.sampleName;
        this.prefixMap.put(entry.prefix, sampleId);
      }

      final List<FastqEntry> sampleEntries;

      if (!this.fastqMap.containsKey(sampleId)) {
        sampleEntries = new ArrayList<>();
        this.fastqMap.put(sampleId, sampleEntries);
      } else {
        sampleEntries = this.fastqMap.get(sampleId);
      }

      // Don't add previously added file
      if (!sampleEntries.contains(entry)) {
        sampleEntries.add(entry);
      }

    } else if (isDataFormatExtension(GENOME_FASTA, extension, md)) {
      this.genomeFile = file;
    } else if (isDataFormatExtension(ANNOTATION_GFF, extension, md)) {
      this.gffFile = file;
    } else if (isDataFormatExtension(ANNOTATION_GTF, extension, md)) {
      this.gtfFile = file;
    } else if (isDataFormatExtension(ADDITIONAL_ANNOTATION_TSV, extension,
        md)) {
      this.additionalAnnotationFile = file;
    } else {
      throw new EoulsanException("Unknown file type: " + file);
    }

  }

  /**
   * Add a filename to the design builder
   * @param filename filename of the file to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFile(final String filename) throws EoulsanException {

    if (filename == null) {
      return;
    }

    getLogger().info("Add file " + filename + " to design.");
    addFile(new DataFile(filename));
  }

  /**
   * Add filenames to the design builder
   * @param filenames array with the filenames to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFiles(final String[] filenames) throws EoulsanException {

    if (filenames == null) {
      return;
    }

    for (String filename : filenames) {
      addFile(filename);
    }
  }

  /**
   * Add filenames to the design builder
   * @param filenames array with the filenames to add
   * @throws EoulsanException if the file does not exists
   */
  public void addFiles(final List<String> filenames) throws EoulsanException {

    if (filenames == null) {
      return;
    }

    for (String filename : filenames) {
      addFile(filename);
    }
  }

  /**
   * Add all the sample from a Bclfastq samplesheet.
   * @param samplesheet The Bcl2fastq samplesheet object
   * @param projectName name of the project
   * @param bcl2fastqOutputDir the output directory of Bcl2fastq demultiplexing
   * @throws EoulsanException if an error occurs while adding the Bcl2fastq
   *           samplesheet
   */
  public void addBcl2FastqSamplesheetProject(final SampleSheet samplesheet,
      final String projectName, final File bcl2fastqOutputDir)
      throws EoulsanException {

    if (samplesheet == null || bcl2fastqOutputDir == null) {
      return;
    }

    if (!bcl2fastqOutputDir.exists() || !bcl2fastqOutputDir.isDirectory()) {
      throw new EoulsanException(
          "The Bcl2fastq output directory does not exists: "
              + bcl2fastqOutputDir);
    }

    final boolean Bcl2Fastq1 =
        new File(bcl2fastqOutputDir.getPath() + "/Project_" + projectName)
            .isDirectory();

    for (fr.ens.biologie.genomique.aozan.illumina.samplesheet.Sample sample : samplesheet) {

      final String sampleProject = sample.getSampleProject();
      final String sampleId = sample.getSampleId();
      final String sampleDesc = sample.getDescription();
      final String sampleOperator = sample.get("Operator");
      final int sampleLane = sample.getLane();

      // Check if sample id field exist for sample
      if (sampleId == null) {
        throw new EoulsanException(
            "No sample Id field found for sample: " + sample);
      }

      // Select only project samples
      if (projectName != null && !projectName.equals(sampleProject)) {
        continue;
      }
      File dataDir;
      if (Bcl2Fastq1) {
        dataDir = new File(bcl2fastqOutputDir.getPath()
            + "/Project_" + sampleProject + "/Sample_" + sampleId);
      } else {
        dataDir = new File(bcl2fastqOutputDir.getPath() + "/" + sampleProject);
      }
      // Test if the directory with fastq files exists
      if (!dataDir.exists() || !dataDir.isDirectory()) {
        continue;
      }

      final String laneKey =
          sampleLane == -1 ? "_L" : String.format("_L%03d_", sampleLane);

      // List the input FASTQ files
      final File[] files = dataDir.listFiles(new FileFilter() {

        @Override
        public boolean accept(final File f) {

          final String filename =
              StringUtils.filenameWithoutCompressionExtension(f.getName());

          if (filename.startsWith(sampleId)
              && filename.contains(laneKey)
              && (filename.endsWith(".fastq") || filename.endsWith(".fq"))) {
            return true;
          }

          return false;
        }
      });

      // Sort the list of input FASTQ files
      Arrays.sort(files);

      for (File fastqFile : files) {

        final List<FastqEntry> list;
        final String normalizedSampleId = Naming.toValidName(sampleId);

        if (this.fastqMap.containsKey(normalizedSampleId)) {
          list = this.fastqMap.get(normalizedSampleId);
        } else {
          list = new ArrayList<>();
          this.fastqMap.put(normalizedSampleId, list);
        }

        try {
          list.add(new FastqEntry(new DataFile(fastqFile), sampleId, sampleDesc,
              sampleOperator));
        } catch (EmptyFastqException e) {
          getLogger().warning(e.getMessage());
        }
      }
    }

  }

  /**
   * Add all the samples from a Bcl2Fastq samplesheet.
   * @param samplesheetFile the path to the Casava design
   * @param projectName the name of the project
   * @throws EoulsanException if an error occurs while reading the Casava design
   */
  public void addBcl2FastqSamplesheetProject(final File samplesheetFile,
      final String projectName) throws EoulsanException {

    if (samplesheetFile == null) {
      return;
    }

    getLogger().info("Add Bcl2fastq samplesheet file "
        + samplesheetFile + " to design with " + (projectName == null
            ? "no project filter." : projectName + " project filter."));

    final File baseDir;
    final File file;

    if (!samplesheetFile.exists()) {
      throw new EoulsanException(
          "The Bcl2fastq samplesheet file does not exists: " + samplesheetFile);
    }

    if (samplesheetFile.isDirectory()) {
      baseDir = samplesheetFile;

      final File[] files = baseDir.listFiles(new FilenameFilter() {

        @Override
        public boolean accept(final File dir, final String filename) {
          if (filename.endsWith(".csv")) {
            return true;
          }
          return false;
        }
      });

      if (files == null || files.length == 0) {
        throw new EoulsanException(
            "No Bcl2fastq samplesheet file found in directory: " + baseDir);
      }

      if (files.length > 1) {
        throw new EoulsanException(
            "More than one Bcl2fastq samplesheet found in directory: "
                + baseDir);
      }

      file = files[0];
    } else {
      baseDir = samplesheetFile.getParentFile();
      file = samplesheetFile;
    }

    try {
      SampleSheetCSVReader reader = new SampleSheetCSVReader(file);
      addBcl2FastqSamplesheetProject(reader.read(), projectName, baseDir);
    } catch (IOException e) {
      throw new EoulsanException(e);
    }

  }

  /**
   * Create design object.
   * @param pairedEndMode true if the paired end mode is enabled
   * @return a new Design object
   * @throws EoulsanException if an error occurs while analyzing input files
   */
  public Design getDesign(final boolean pairedEndMode) throws EoulsanException {

    final Design result = DesignFactory.createEmptyDesign();
    result.addExperiment("exp1");

    final FastqFormat defaultFastqFormat =
        EoulsanRuntime.getSettings().getDefaultFastqFormat();

    for (Map.Entry<String, List<FastqEntry>> e : this.fastqMap.entrySet()) {

      final String sampleId = e.getKey();
      final List<List<FastqEntry>> files = findPairedEndFiles(e.getValue());
      int count = 0;

      for (List<FastqEntry> fes : files) {

        final String desc = fes.get(0).sampleDesc;
        final String date = fes.get(0).sampleDate;
        final String operator = fes.get(0).sampleOperator;
        final String condition = fes.get(0).sampleName;

        if (pairedEndMode) {

          final String finalSampleId = files.size() == 1
              ? sampleId : sampleId + StringUtils.toLetter(count);

          // Convert the list of DataFiles to a list of filenames
          final List<String> filenames = new ArrayList<>();
          for (FastqEntry fe : fes) {
            filenames.add(fe.path.getSource());
          }

          addSample(result, finalSampleId, desc, condition, date, operator,
              defaultFastqFormat, filenames, fes.get(0).path);
          count++;

        } else {

          for (FastqEntry fe : fes) {

            final String finalSampleId = e.getValue().size() == 1
                ? sampleId : sampleId + StringUtils.toLetter(count);

            addSample(result, finalSampleId, desc, condition, date, operator,
                defaultFastqFormat,
                Collections.singletonList(fe.path.getSource()), fe.path);
            count++;
          }

        }
      }

    }

    return result;
  }

  /**
   * Add a Sample to the Design object
   * @param design Design object
   * @param sampleId the id of the sample
   * @param desc description of the sample
   * @param condition condition
   * @param date date of the sample
   * @param operator operator for the sample
   * @param defaultFastqFormat default fastq format
   * @param filenames list of the fastq files for the sample
   * @param fileToCheck DataFile of the file to use to check fastq format
   * @throws EoulsanException if an error occurs while adding the sample
   */
  private void addSample(final Design design, final String sampleId,
      final String desc, final String condition, final String date,
      final String operator, final FastqFormat defaultFastqFormat,
      final List<String> filenames, final DataFile fileToCheck)
      throws EoulsanException {

    if (design == null) {
      return;
    }

    // Create the sample
    design.addSample(sampleId);
    final Sample s = design.getSample(sampleId);
    final SampleMetadata smd = s.getMetadata();

    // Set the fastq file of the sample
    smd.setReads(filenames);

    // Set the description of the sample if exists
    if (desc != null) {
      smd.setDescription(desc);
    } else if (s.getMetadata().containsDescription()) {
      smd.setDescription("no description");
    }

    // Set the date of the sample if exists
    if (date != null) {
      smd.setDate(date);
    }

    // Set the operator of the sample if exists
    if (operator != null) {
      smd.setOperator(operator);
    } else if (s.getMetadata().containsOperator()) {
      smd.setOperator("unknown operator");
    }

    // Set the genome file if exists
    if (this.genomeFile != null) {
      design.getMetadata().setGenomeFile(this.genomeFile.toString());
    }

    // Set the GFF Annotation file
    if (this.gffFile != null) {
      design.getMetadata().setGffFile(this.gffFile.toString());
    }

    // Set the GTF Annotation file
    if (this.gtfFile != null) {
      design.getMetadata().setGtfFile(this.gtfFile.toString());
    }

    // Set additional annotation file
    if (this.additionalAnnotationFile != null) {
      design.getMetadata().setAdditionalAnnotationFile(
          this.additionalAnnotationFile.toString());
    }

    // Identify Fastq format
    FastqFormat format = null;

    try {
      getLogger().info("Check fastq format for " + fileToCheck);
      format = FastqFormat.identifyFormat(fileToCheck.open(),
          MAX_FASTQ_ENTRIES_TO_READ);
    } catch (IOException | BadBioEntryException e) {
      throw new EoulsanException(e);
    }
    smd.setFastqFormat(format == null ? defaultFastqFormat : format);

    // Set replicate technical group
    smd.setRepTechGroup(condition);

    // Set UUID for the sample
    smd.setUUID(UUID.randomUUID().toString());

    // Get the experiement sample of the unique experiment of the design
    final Experiment exp = design.getExperiments().get(0);
    final ExperimentSample es = exp.addSample(s);

    // Set the condition
    es.getMetadata().setCondition(condition);

    // Set the default reference
    es.getMetadata().setReference(false);
  }

  private boolean isDataFormatExtension(final DataFormat dataFormat,
      final String extension, final DataFileMetadata md) {

    if (md != null && md.getDataFormat() != null) {
      return dataFormat.equals(md.getDataFormat());
    }

    for (DataFormat df : this.dfr.getDataFormatsFromExtension(extension)) {

      if (df == dataFormat) {
        return true;
      }
    }

    return false;
  }

  /**
   * Group paired end files.
   * @return a list of 1-2 paired end files
   * @throws EoulsanException if an error occurs while getting the id of first
   *           read of the fastq files
   */
  private List<List<FastqEntry>> findPairedEndFiles(
      final List<FastqEntry> files) throws EoulsanException {

    final Map<String, List<FastqEntry>> mapPrefix = new HashMap<>();
    final Map<FastqEntry, Integer> mapPaired = new HashMap<>();
    final List<List<FastqEntry>> result = new ArrayList<>();

    for (FastqEntry fe : files) {

      mapPaired.put(fe, fe.pairMember);

      final List<FastqEntry> list;

      if (mapPrefix.containsKey(fe.prefix)) {
        list = mapPrefix.get(fe.prefix);
      } else {
        list = new ArrayList<>();
        mapPrefix.put(fe.prefix, list);
        result.add(list);
      }

      list.add(fe);
    }

    // Order the paired end files
    for (List<FastqEntry> list : result) {

      // Check invalid number of files
      if (list.size() > 2) {
        throw new EoulsanException(
            "Found more than 2 files for a sample in paired-end mode: " + list);
      }

      if (list.size() == 2) {

        final int member1 = mapPaired.get(list.get(0));
        final int member2 = mapPaired.get(list.get(1));

        if (member1 == member2) {
          throw new EoulsanException(
              "Found two files with the same pair member: " + list);
        }

        if (member1 < 1 || member1 > 2) {
          throw new EoulsanException(
              "Invalid pair member for file: " + list.get(0));
        }

        if (member2 < 1 || member2 > 2) {
          throw new EoulsanException(
              "Invalid pair member for file: " + list.get(1));
        }

        // Change the order of the file if necessary
        if (member1 == 2 && member2 == 1) {

          final FastqEntry tmp = list.get(0);
          list.set(0, list.get(1));
          list.set(1, tmp);
        }
      }

    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public DesignBuilder() {
  }

  /**
   * Public constructor.
   * @param filenames filenames to add
   * @throws EoulsanException if a file to add to the design does not exist or
   *           is not handled
   */
  public DesignBuilder(final String[] filenames) throws EoulsanException {

    addFiles(filenames);
  }

}
