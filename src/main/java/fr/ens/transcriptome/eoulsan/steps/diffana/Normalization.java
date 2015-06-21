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

package fr.ens.transcriptome.eoulsan.steps.diffana;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.r.RSConnectionNewImpl;

/**
 * This class create and launch an R script to compute normalisation of
 * expression data
 * @since 1.2
 * @author Vivien Deshaies
 */

public class Normalization {

  protected static final String TARGET_CREATION = "/targetCreation.Rnw";
  protected static final String NORMALIZATION_FUNCTIONS =
      "/normalization_anaDiff_RNAseq_Functions.R";

  private static final String NORMALISATION_PART1_WHITH_TECHREP =
      "/normalisationPart1WithTechRep.Rnw";
  private static final String NORMALIZATION_PART1_WHITHOUT_TECHREP =
      "/normalisationPart1WithoutTechRep.Rnw";
  private static final String CLUSTERING_PCA_RAW = "/clusteringAndPCARaw.Rnw";
  private static final String CLUSTERING_PCA_NORM = "/clusteringAndPCANorm.Rnw";
  private static final String NORMALIZATION_PART2 = "/normalizationPart2.Rnw";

  protected final Design design;
  protected final File expressionFilesDirectory;
  protected final File outPath;
  protected final String expressionFilesPrefix;
  protected final String expressionFilesSuffix;
  protected RSConnectionNewImpl rConnection = null;

  //
  // Public methods
  //

  /**
   * Run normalisation step
   * @throws EoulsanException
   */
  public void run(final StepContext context, final Data data)
      throws EoulsanException {

    // Check if there more than one file to normalize
    if (data.size() < 2) {
      throw new EoulsanException("Cannot normalize less than 2 input files");
    }

    if (context.getSettings().isRServeServerEnabled()) {
      getLogger().info("Normalization : Rserve mode");
      runRserveRnwScript(context, data);
    } else {
      getLogger().info("Normalization : local mode");
      runLocalRnwScript(context, data);
    }
  }

  // Getters

  /**
   * Test if there is Technical replicates into rRepTechGroup field.
   * @param rRepTechGroup list of the technical replicate group
   */
  protected boolean isTechnicalReplicates(final List<String> rRepTechGroup) {

    Map<String, String> rtgMap = new HashMap<>();

    for (String repTechGroup : rRepTechGroup) {

      if (rtgMap.containsKey(repTechGroup)) {
        return true;
      }
      rtgMap.put(repTechGroup, "");
    }

    return false;
  }

  /**
   * Run Rnw script on Rserve server.
   * @param context Step context
   * @param data data to process
   * @throws EoulsanException if an error occurs while executing the script
   */
  protected void runRserveRnwScript(final StepContext context, final Data data)
      throws EoulsanException {

    try {

      // print log info
      getLogger().info(
          "Rserve server name : " + this.rConnection.getServerName());

      // create an experiment map
      Map<String, List<Sample>> experiments = experimentsSplitter();

      // create an iterator on the map values
      for (List<Sample> experimentSampleList : experiments.values()) {

        getLogger().info(
            "Experiment : "
                + experimentSampleList.get(0).getMetadata().getExperiment());

        putExpressionFiles(experimentSampleList, data);

        String rScript = generateScript(experimentSampleList, context);
        runRnwScript(rScript, true);

        if (!context.getSettings().isKeepRServeFiles()) {
          removeExpressionFiles(experimentSampleList);
        }

        if (!context.getSettings().isSaveRscripts()) {
          getLogger().info("Remove R script on RServe: " + rScript);
          this.rConnection.removeFile(rScript);
        }

        this.rConnection.getAllFiles(this.outPath.toString() + "/");
      }

    } catch (REngineException e) {
      throw new EoulsanException("Error while running differential analysis: "
          + e.getMessage(), e);
    } catch (REXPMismatchException e) {
      throw new EoulsanException(
          "Error while getting file : " + e.getMessage(), e);

    } finally {

      try {

        if (!context.getSettings().isKeepRServeFiles()) {
          this.rConnection.removeAllFiles();
        }

        this.rConnection.disConnect();

      } catch (Exception e) {
        throw new EoulsanException("Error while removing files on server : "
            + e.getMessage(), e);
      }
    }
  }

  /**
   * run Rnw script on local mode.
   * @param context Step context
   * @param data data to process
   * @throws EoulsanException if an error occurs while executing the script
   */
  protected void runLocalRnwScript(final StepContext context, final Data data)
      throws EoulsanException {

    try {

      // create an experiment map
      Map<String, List<Sample>> experiments = experimentsSplitter();

      // create an iterator on the map values
      for (List<Sample> experimentSampleList : experiments.values()) {

        getLogger().info(
            "Experiment : "
                + experimentSampleList.get(0).getMetadata().getExperiment());

        createLinkExpressionFiles(experimentSampleList, data);

        String rScript = generateScript(experimentSampleList, context);
        runRnwScript(rScript, false);

        // Remove R script if keep.rscript parameter is false
        if (!context.getSettings().isSaveRscripts()) {
          new File(rScript).delete();
        }
      }

    } catch (Exception e) {
      throw new EoulsanException("Error while running differential analysis: "
          + e.getMessage(), e);
    }
  }

  /**
   * Split design into multiple experiments Samples list.
   * @return experiementMap a map of experiments
   */
  protected Map<String, List<Sample>> experimentsSplitter() {

    List<Sample> samples = this.design.getSamples();
    // Create design HashMap
    Map<String, List<Sample>> experimentMap = new HashMap<>();

    for (Sample s : samples) {
      String expName = s.getMetadata().getExperiment();

      if (experimentMap.containsKey(expName)) {
        experimentMap.get(expName).add(s);
      } else {
        experimentMap.put(expName, Lists.newArrayList(s));
      }
    }

    return experimentMap;
  }

  /**
   * Execute the analysis.
   * @param rnwScript script to execute
   * @throws REngineException if an error occurs while executing the script on
   *           Rserve
   * @throws EoulsanException if an error occurs while executing the script in
   *           local mode
   */
  protected void runRnwScript(final String rnwScript,
      final boolean isRserveEnable) throws REngineException, EoulsanException {

    if (isRserveEnable) {
      getLogger().info("Execute RNW script: " + rnwScript);
      this.rConnection.executeRnwCode(rnwScript);

    } else {

      try {

        final ProcessBuilder pb =
            new ProcessBuilder("/usr/bin/R", "CMD", "Sweave", rnwScript);

        // Set the temporary directory for R
        pb.environment().put("TMPDIR", this.outPath.getAbsolutePath());

        ProcessUtils.logEndTime(pb.start(), Joiner.on(' ').join(pb.command()),
            System.currentTimeMillis());

      } catch (IOException e) {

        throw new EoulsanException(
            "Error while executing R script in anadiff: " + e.getMessage(), e);
      }

    }

  }

  /**
   * Read a static part of the generated script.
   * @param staticFile the name of a file containing a part of the script
   * @return A String with the static part of the script
   * @throws EoulsanException if an error occurs while reading the script
   */
  protected String readStaticScript(final String staticFile)
      throws EoulsanException {

    final StringBuilder sb = new StringBuilder();

    final InputStream is = DiffAna.class.getResourceAsStream(staticFile);

    try {
      final BufferedReader br = FileUtils.createBufferedReader(is);

      String line;

      while ((line = br.readLine()) != null) {

        sb.append(line);
        sb.append('\n');
      }
    } catch (IOException e) {
      throw new EoulsanException("Error while reading a file" + e.getMessage());
    }

    return sb.toString();
  }

  /**
   * Generate the R script.
   * @param experimentSamplesList list of sample experiments
   * @return String rScript R script to execute
   * @throws EoulsanException if an error occurs while generate the R script
   */
  protected String generateScript(final List<Sample> experimentSamplesList,
      final StepContext context) throws EoulsanException {

    final Map<String, List<Integer>> conditionsMap = new HashMap<>();

    final List<Integer> rSampleIds = new ArrayList<>();
    final List<String> rSampleNames = new ArrayList<>();
    final List<String> rCondNames = new ArrayList<>();
    List<String> rRepTechGroup = new ArrayList<>();
    int i = 0;

    // Get samples ids, conditions names/indexes and repTechGroups
    for (Sample s : experimentSamplesList) {

      if (!s.getMetadata().isConditionField()) {
        throw new EoulsanException("No condition field found in design file.");
      }

      final String condition = s.getMetadata().getCondition().trim();

      if ("".equals(condition)) {
        throw new EoulsanException("No value for condition in sample: "
            + s.getName() + " (" + s.getId() + ")");
      }

      final String repTechGroup = s.getMetadata().getRepTechGroup().trim();

      if (!"".equals(repTechGroup)) {
        rRepTechGroup.add(repTechGroup);
      }

      final List<Integer> index;
      if (!conditionsMap.containsKey(condition)) {
        index = new ArrayList<>();
        conditionsMap.put(condition, index);
      } else {
        index = conditionsMap.get(condition);
      }
      index.add(i);

      rSampleIds.add(s.getId());
      rSampleNames.add(s.getName());
      rCondNames.add(condition);

      i++;
    }

    checkRepTechGroupCoherence(rRepTechGroup, rCondNames);

    // Create Rnw script StringBuilder with preamble
    String pdfTitle =
        escapeUnderScore(experimentSamplesList.get(0).getMetadata()
            .getExperiment())
            + " normalisation";
    final StringBuilder sb =
        generateRnwpreamble(experimentSamplesList, pdfTitle);

    /*
     * Replace "na" values of repTechGroup by unique sample ids to avoid pooling
     * problem while executing R script
     */
    replaceRtgNA(rRepTechGroup, rSampleNames);

    // Add sampleNames vector
    generateSampleNamePart(rSampleNames, sb);

    // Add SampleIds vector
    generateSampleIdsPart(rSampleIds, sb);

    // Add file names vector
    generateExpressionFileNamesPart(sb);

    // Add repTechGroupVector
    generateRepTechGroupPart(rRepTechGroup, sb);

    // Add condition to R script
    generateConditionPart(rCondNames, sb);

    // Add projectPath, outPath and projectName
    sb.append("# projectPath : path of count files directory\n");
    sb.append("projectPath <- \"\"\n");
    sb.append("# outPath path of the outputs\n");
    sb.append("outPath <- \"./\"\n");
    sb.append("projectName <- ");
    sb.append("\""
        + experimentSamplesList.get(0).getMetadata().getExperiment() + "\""
        + "\n");
    sb.append("@\n\n");

    // Add target creation
    sb.append(readStaticScript(TARGET_CREATION));

    sb.append("\\section{Analysis}\n\n");
    sb.append("\t\\subsection{Normalization}\n\n");
    sb.append("\\begin{itemize}\n\n");

    if (experimentSamplesList.size() > 2) {
      sb.append(readStaticScript(CLUSTERING_PCA_RAW));
    }

    // Add normalization part
    if (isTechnicalReplicates(rRepTechGroup)) {
      sb.append(readStaticScript(NORMALISATION_PART1_WHITH_TECHREP));
    } else {
      sb.append(readStaticScript(NORMALIZATION_PART1_WHITHOUT_TECHREP));
    }

    // Add normalise data clustering if it's possible
    if (isEnoughRepTechGroup(rRepTechGroup)) {
      sb.append(readStaticScript(CLUSTERING_PCA_NORM));
    }

    sb.append(readStaticScript(NORMALIZATION_PART2));

    // end document
    sb.append("\\end{document}\n");

    String rScript = null;
    try {
      rScript =
          "normalization_"
              + experimentSamplesList.get(0).getMetadata().getExperiment()
              + ".Rnw";
      if (context.getSettings().isRServeServerEnabled()) {
        getLogger().info("Write script on Rserve: " + rScript);
        this.rConnection.writeStringAsFile(rScript, sb.toString());
      } else {
        Writer writer = FileUtils.createFastBufferedWriter(rScript);
        writer.write(sb.toString());
        writer.close();
      }
    } catch (REngineException | IOException e) {
      e.printStackTrace();
    }

    return rScript;

  }

  /**
   * Write Rnw preamble.
   * @param experimentSamplesList sample experiment list
   * @param title title of the document
   * @return a StringBuilder with Rnw preamble
   */
  protected StringBuilder generateRnwpreamble(
      final List<Sample> experimentSamplesList, final String title) {

    StringBuilder sb = new StringBuilder();
    // Add packages to the LaTeX StringBuilder
    sb.append("\\documentclass[a4paper,10pt]{article}\n");
    sb.append("\\usepackage[utf8]{inputenc}\n");
    sb.append("\\usepackage{lmodern}\n");
    sb.append("\\usepackage{a4wide}\n");
    sb.append("\\usepackage{marvosym}\n");
    sb.append("\\usepackage{graphicx}\n\n");
    // Set Sweave options
    sb.append("\\SweaveOpts{eps = FALSE, pdf = TRUE}\n");
    sb.append("\\setkeys{Gin}{width=0.95\textwidth}\n\n");
    // Add document title
    sb.append("\\title{" + title + "}\n\n");

    // Begin document...
    sb.append("\\begin{document}\n");
    sb.append("\\maketitle\n\n");

    // Add a begin R code chunck mark
    sb.append("<<functions, echo=FALSE>>=\n");

    // Add the auto generate info
    sb.append("### Auto generated by ");
    sb.append(Globals.APP_NAME);
    sb.append(" ");
    sb.append(Globals.APP_VERSION_STRING);
    sb.append(" on ");
    sb.append(new Date(System.currentTimeMillis()));
    sb.append(" ###\n\n");
    // Add function part to string builder
    try {
      sb.append(readStaticScript(NORMALIZATION_FUNCTIONS));
    } catch (EoulsanException e) {
      e.printStackTrace();
    }

    // Add a end R code chunck mark
    sb.append("@\n\n");

    // Add initialization part
    sb.append("\\section{Initialization}\n");
    sb.append("<<>>=\n");

    return sb;
  }

  /**
   * Add sampleNames vector to R script.
   * @param rSampleNames sample names
   * @param sb StringBuilder where write the part of the script
   */
  protected void generateSampleNamePart(final List<String> rSampleNames,
      final StringBuilder sb) {

    // Add samples names to R script
    sb.append("# create sample names vector\n");
    sb.append("sampleNames <- c(");
    boolean first = true;
    for (String r : rSampleNames) {

      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      sb.append('\"');
      sb.append(r);
      sb.append('\"');
    }
    sb.append(")\n\n");

  }

  /**
   * Add SampleIds vector to R script.
   * @param rSampleIds samples identifiers
   * @param sb StringBuilder where write the part of the script
   */
  protected void generateSampleIdsPart(final List<Integer> rSampleIds,
      final StringBuilder sb) {

    // Put sample ids into R vector
    sb.append("sampleIds <- c(");
    int i = 0;
    for (int id : rSampleIds) {
      i++;
      sb.append("" + id);
      if (i < rSampleIds.size()) {
        sb.append(",");
      }
    }
    sb.append(")\n\n");
  }

  /**
   * Add expression file name vector to R script.
   * @param sb StringBuilder where write the part of the script
   */
  protected void generateExpressionFileNamesPart(final StringBuilder sb) {

    // Add file names vector
    sb.append("#create file names vector\n");
    sb.append("fileNames <- paste(\"" + this.expressionFilesPrefix + '\"' + ',');
    sb.append("sampleIds"
        + ',' + '\"' + this.expressionFilesSuffix + '\"' + ',' + "sep=\"\""
        + ")" + "\n\n");
  }

  /**
   * Write the section of the script that handle technical replicate groups.
   * @param rRepTechGroup list of technical replicate groups
   * @param sb StringBuilder where write the part of the script
   */
  protected void generateRepTechGroupPart(final List<String> rRepTechGroup,
      final StringBuilder sb) {

    if (isTechnicalReplicates(rRepTechGroup)) {

      // Add repTechGroup vector
      sb.append("# create technical replicates groups vector\n");
      sb.append("repTechGroup <- c(");
      boolean first = true;

      for (String r : rRepTechGroup) {

        if (first) {
          first = false;
        } else {
          sb.append(',');
        }

        sb.append('\"');
        sb.append(r);
        sb.append('\"');
      }
      sb.append(")\n\n");

    } else {
      /*
       * Add repTechGroup vector equal to sampleNames to avoid error in R
       * function buildTarget
       */
      sb.append("# create technical replicates groups vector\n");
      sb.append("repTechGroup <- sampleNames\n\n");
    }
  }

  /**
   * Add condition vector to R script.
   * @param rCondNames condition names
   * @param sb StringBuilder where write the part of the script
   */
  protected void generateConditionPart(final List<String> rCondNames,
      final StringBuilder sb) {

    sb.append("# create condition vector\n");
    sb.append("condition <- c(");
    boolean first = true;
    for (String r : rCondNames) {

      if (first) {
        first = false;
      } else {
        sb.append(',');
      }

      sb.append('\"');
      sb.append(r);
      sb.append('\"');

    }
    sb.append(")\n\n");
  }

  /**
   * Check if there is a problem in the repTechGroup coherence.
   * @param rRepTechGroup technical replicate group
   * @param rCondNames condition names
   * @throws EoulsanException if an error if found in the design file
   */
  protected void checkRepTechGroupCoherence(final List<String> rRepTechGroup,
      final List<String> rCondNames) throws EoulsanException {

    // Check repTechGroup field coherence
    Map<String, String> condRepTGMap = new HashMap<>();
    for (int i = 0; i < rRepTechGroup.size(); i++) {

      String repTechGroup = rRepTechGroup.get(i);
      String condition = rCondNames.get(i);

      if (!repTechGroup.toLowerCase().equals("na")) {
        if (!condRepTGMap.containsKey(repTechGroup)) {
          condRepTGMap.put(repTechGroup, condition);
        } else if (!condRepTGMap.get(repTechGroup).equals(condition)) {
          throw new EoulsanException(
              "There is a mistake in RepTechGroup field of design file : "
                  + "two condition have the same repTechGroup value : "
                  + repTechGroup);
        }
      }
    }
  }

  /**
   * Escape underscore for LaTeX title.
   * @param s string to escape
   * @return s with escaped underscore
   */
  protected String escapeUnderScore(final String s) {

    String s2 = s.replace("_", "\\_");

    return s2;
  }

  /**
   * Replace na values in RepTechGroup list to avoid pooling error.
   * @param rRepTechGroup list of technical replicate groups
   * @param rSampleNames sample names
   */
  protected void replaceRtgNA(final List<String> rRepTechGroup,
      final List<String> rSampleNames) {

    for (int j = 0; j < rRepTechGroup.size(); j++) {

      if (rRepTechGroup.get(j).toLowerCase().equals("na")) {
        rRepTechGroup.set(j, rSampleNames.get(j));
      }
    }
  }

  /*
   * Private methods
   */

  /**
   * Test if there is enough distinct repTechGroup (>2) to perform clustering.
   * @param rRepTechGroup list of technical replicate groups
   * @return true if there is enough distinct repTechGroup (>2) to perform
   *         clustering
   */
  private boolean isEnoughRepTechGroup(final List<String> rRepTechGroup) {

    List<String> repTechGroupMap = new ArrayList<>();
    for (String r : rRepTechGroup) {

      if (!repTechGroupMap.contains(r)) {
        repTechGroupMap.add(r);
      }

      if (repTechGroupMap.size() > 2) {
        return true;
      }
    }
    return false;
  }

  /**
   * Put all expression files needed for the analysis on the R server.
   * @throws REngineException if an error occurs on RServe server
   * @throws EoulsanException if try to overwrite an existing expression file
   */
  private void putExpressionFiles(final List<Sample> experiment, final Data data)
      throws REngineException, EoulsanException {

    final Set<String> outputFilenames = new HashSet<>();

    for (Data d : data.getListElements()) {

      final int sampleId = d.getMetadata().getSampleId();
      final DataFile inputFile = d.getDataFile();

      final String outputFilename =
          this.expressionFilesPrefix + sampleId + this.expressionFilesSuffix;

      // Check if the sample ID exists
      if (sampleId == -1) {
        throw new EoulsanException("No sample Id found for input file: "
            + inputFile);
      }

      // Check if try to overwrite an existing output file
      if (outputFilenames.contains(outputFilename)) {
        throw new EoulsanException(
            "Cannot overwrite expression file on Rserve: " + outputFilename);
      }
      outputFilenames.add(outputFilename);

      // Put file on rserve server
      getLogger().info(
          "Put file on RServe: " + inputFile + " to " + outputFilename);
      try {
        this.rConnection.putFile(inputFile.open(), outputFilename);
      } catch (IOException e) {
        throw new EoulsanException(e);
      }
    }

  }

  /**
   * Put all expression files needed for the analysis on the R server.
   * @throws REngineException if an error occurs on RServe server
   */
  private void createLinkExpressionFiles(final List<Sample> experiment,
      final Data data) throws REngineException {

    for (Data d : data.getListElements()) {

      final int sampleId = d.getMetadata().getSampleId();
      final File inputFile = d.getDataFile().toFile();
      final String linkFilename =
          this.expressionFilesPrefix + sampleId + this.expressionFilesSuffix;
      final File linkFile = new File(inputFile.getParentFile(), linkFilename);

      if (!linkFile.exists()) {
        try {
          Files.createSymbolicLink(linkFile.toPath(), inputFile.toPath());
        } catch (IOException e) {
          // Do nothing
        }
      }

    }

  }

  /**
   * Remove all expression files from the R server after analysis.
   * @param experiment list of samples
   * @throws REngineException if an error occurs on RServe server
   */
  private void removeExpressionFiles(final List<Sample> experiment)
      throws REngineException {

    int i;

    for (Sample s : experiment) {
      i = s.getId();

      // Remove file from rserve server
      this.rConnection.removeFile(this.expressionFilesDirectory
          + "/" + this.expressionFilesPrefix + i + this.expressionFilesSuffix);
    }
  }

  /*
   * Constructor
   */

  /**
   * Public constructor.
   * @param design The design object
   * @param expressionFilesDirectory the directory of expression files
   * @param expressionFilesPrefix the prefix of expression files
   * @param expressionFilesSuffix the suffix of expression file
   * @param outPath the output path
   * @param rServerName the name of the RServe server
   * @throws EoulsanException if an error occurs if connection to RServe server
   *           cannot be established
   */
  public Normalization(final Design design,
      final File expressionFilesDirectory, final String expressionFilesPrefix,
      final String expressionFilesSuffix, final File outPath,
      final String rServerName, final boolean rServeEnable)
      throws EoulsanException {

    checkNotNull(design, "design is null.");
    checkNotNull(expressionFilesDirectory,
        "The path of the expression files is null.");
    checkNotNull(expressionFilesPrefix,
        "The prefix for expression files is null");
    checkNotNull(expressionFilesSuffix,
        "The suffix for expression files is null");

    this.design = design;
    this.expressionFilesPrefix = expressionFilesPrefix;
    this.expressionFilesSuffix = expressionFilesSuffix;

    if (!(expressionFilesDirectory.isDirectory() && expressionFilesDirectory
        .exists())) {
      throw new NullPointerException(
          "The path of the expression files doesn't exist or is not a directory.");
    }

    this.expressionFilesDirectory = expressionFilesDirectory;

    if (!(outPath.isDirectory() && outPath.exists())) {
      throw new NullPointerException(
          "The output path file doesn't exist or is not a directory.");
    }

    this.outPath = outPath;

    if (rServeEnable) {

      if (rServerName != null) {
        this.rConnection = new RSConnectionNewImpl(rServerName);
      } else {
        throw new EoulsanException("Missing Rserve server name");
      }
    }
  }

}
