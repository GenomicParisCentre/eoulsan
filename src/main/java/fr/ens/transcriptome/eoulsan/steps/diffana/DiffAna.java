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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.r.RSConnectionNewImpl;

/**
 * This class create and launch a R script to compute differential analysis.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Vivien Deshaies
 */
public class DiffAna {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String NORMALISATION_WHITH_TECHREP =
      "/normalisationWithTechRep.Rnw";
  private static final String NORMALISATION_WHITHOUT_TECHREP =
      "/normalisationWithoutTechRep.Rnw";
  private static final String NORMALISATION_FUNCTIONS =
      "/normalizationRNAseqFunctions.R";
  private static final String DISPERSION_ESTIMATION_WITH_REPLICATES =
      "/dispersionEstimationWithReplicates.Rnw";
  private static final String DISPERSION_ESTIMATION_WITHOUT_REPLICATES =
      "/dispersionEstimationWithoutReplicates.Rnw";
  private static final String KINETIC_ANADIFF = "/kineticAnadiff.Rnw";
  private static final String NOT_KINETIC_ANADIFF = "/notKineticAnadiff.Rnw";

  private Design design;
  private File expressionFilesDirectory;
  private File outPath;
  private String expressionFilesPrefix;
  private String expressionFilesSuffix;
  private RSConnectionNewImpl rConnection;

  //
  // Public methods
  //

  public void run() throws EoulsanException {

    try {
      // Create an experiment map
      Map<String, List<Sample>> experiments = experimentsSpliter();
      // Create an iterator on the map
      Set<String> keys = experiments.keySet();
      Iterator<String> itr = keys.iterator();

      while (itr.hasNext()) {
        String key = itr.next();
        List<Sample> experimentSamplesList = experiments.get(key);
        
        if (EoulsanRuntime.getSettings().isRServeServerEnabled())
          putExpressionFiles(experimentSamplesList);

        String rScript = writeScript(experimentSamplesList);
        runRnwScript(rScript);

        if (EoulsanRuntime.getSettings().isRServeServerEnabled()) {
          removeExpressionFiles(experimentSamplesList);
          this.rConnection.removeFile(rScript);
          this.rConnection.getAllFiles(outPath.toString() + "/");
        }
      }

    } catch (REngineException e) {
      throw new EoulsanException("Error while running differential analysis: "
          + e.getMessage());
    } catch (REXPMismatchException e) {
      throw new EoulsanException("Error while getting file : " + e.getMessage());

    } finally {
      try {
        if (EoulsanRuntime.getSettings().isRServeServerEnabled()) {
          this.rConnection.removeAllFiles();
          this.rConnection.disConnect();
        }
      } catch (Exception e) {
        throw new EoulsanException("Error while removing files on server : "
            + e.getMessage());
      }
    }

  }

  /**
   * Write the R script
   * @return rScript a String containing script to run
   * @throws EoulsanException
   */
  public String writeScript(final List<Sample> experimentSamplesList)
      throws EoulsanException {

    final Map<String, List<Integer>> conditionsMap = Maps.newHashMap();

    final List<Integer> rSampleIds = Lists.newArrayList();
    final List<String> rSampleNames = Lists.newArrayList();
    final List<String> rCondNames = Lists.newArrayList();
    final List<String> rRepTechGroup = Lists.newArrayList();
    int i = 0;

    // Get samples ids, conditions names/indexes and replicate types
    for (Sample s : experimentSamplesList) {

      if (!s.getMetadata().isConditionField())
        throw new EoulsanException("No condition field found in design file.");

      final String condition = s.getMetadata().getCondition().trim();

      if ("".equals(condition))
        throw new EoulsanException("No value for condition in sample: "
            + s.getName() + " (" + s.getId() + ")");

      final String repTechGroup = s.getMetadata().getRepTechGroup().trim();

      if (!"".equals(repTechGroup)) {
        rRepTechGroup.add(repTechGroup);
      }

      final List<Integer> index;
      if (!conditionsMap.containsKey(condition)) {
        index = Lists.newArrayList();
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

    // Determine if there is biological replicates
    boolean biologicalReplicate = false;
    for (String condition : rCondNames) {
      List<Integer> condPos = conditionsMap.get(condition);

      for (i = 0; i < condPos.size() - 1; i++) {
        int pos1 = condPos.get(i);
        int pos2 = condPos.get(i + 1);
        if (!rRepTechGroup.get(pos1).equals(rRepTechGroup.get(pos2))) {
          biologicalReplicate = true;
          break;
        }
      }
    }

    // Check repTechGroup field coherence
    Map<String, String> condRepTGMap = Maps.newHashMap();
    for (i = 0; i < rRepTechGroup.size(); i++) {

      String repTechGroup = rRepTechGroup.get(i);
      String condition = rCondNames.get(i);

      if (!condRepTGMap.containsKey(repTechGroup))
        condRepTGMap.put(repTechGroup, condition);
      else if (!condRepTGMap.get(repTechGroup).equals(condition))
        throw new EoulsanException(
            "There is a mistake in RepTechGroup field of design file : "
                + "two condition have the same repTechGroup value : "
                + repTechGroup);
    }

    // Create Rnw script stringbuilder
    final StringBuilder sb = new StringBuilder();

    // Add packages to the LaTeX stringbuilder
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
    sb.append("\\title{"
        + experimentSamplesList.get(0).getMetadata().getExperiment()
        + " analysis}\n\n");

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
    sb.append(readStaticScript(NORMALISATION_FUNCTIONS));

    // Add a end R code chunck mark
    sb.append("@\n\n");

    // Add initialization part
    sb.append("\\section{Initialization}\n");
    sb.append("<<>>=\n");
    // Determine if there is technical replicates
    boolean rep = false;
    for (int j = 0; j < rSampleIds.size(); j++) {
      if (!rRepTechGroup.get(j).toLowerCase().equals("na")) {
        rep = true;
      } else {
        /*
         * Replace "na" values of repTechGroup by unique sample ids to avoid
         * pooling problem while executing R script
         */
        rRepTechGroup.set(j, rSampleIds.get(j).toString());
      }
    }

    // Test if there is a reference field
    if (isReference(experimentSamplesList)) {
      for (Sample s : experimentSamplesList) {
        boolean refval = s.getMetadata().isReference();
        if (refval) {
          // Add reference to R script
          sb.append("ref <- "
              + "\"" + s.getMetadata().getCondition() + "\"\n\n");
          break;
        }
      }
    }

    // Add normalization part
    if (rep)
      writeWithTechnicalReplicate(sb, rSampleIds, rSampleNames, rCondNames,
          rRepTechGroup, experimentSamplesList.get(0).getMetadata()
              .getExperiment());
    else
      writeWithoutTechnicalReplicates(sb, rSampleIds, rSampleNames, rCondNames,
          experimentSamplesList.get(0).getMetadata().getExperiment());

    // Add dispersion estimation part
    if (biologicalReplicate) {
      sb.append(readStaticScript(DISPERSION_ESTIMATION_WITH_REPLICATES));
    } else {
      sb.append(readStaticScript(DISPERSION_ESTIMATION_WITHOUT_REPLICATES));
    }

    if (isReference(experimentSamplesList)) {
      sb.append(readStaticScript(KINETIC_ANADIFF));
    } else {
      sb.append(readStaticScript(NOT_KINETIC_ANADIFF));
    }

    String rScript = null;
    try {
      rScript =
          experimentSamplesList.get(1).getMetadata().getExperiment()
              + "_" + "diffAna" + ".Rnw";
      if (EoulsanRuntime.getSettings().isRServeServerEnabled()) {
        this.rConnection.writeStringAsFile(rScript, sb.toString());
      } else {
        Writer writer = FileUtils.createFastBufferedWriter(rScript);
        writer.write(sb.toString());
        writer.close();
      }
    } catch (REngineException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return rScript;

  }

  /**
   * Put all expression files needed for the analysis on the R server
   * @throws REngineException
   */
  public void putExpressionFiles(List<Sample> experiment)
      throws REngineException {

    int i;

    for (Sample s : experiment) {
      i = s.getId();

      // Put file on rserve server
      this.rConnection.putFile(new File(expressionFilesDirectory
          + "/" + this.expressionFilesPrefix + i + this.expressionFilesSuffix),
          this.expressionFilesPrefix + i + this.expressionFilesSuffix);
    }
  }

  /**
   * Remove all expression files from the R server after analysis
   * @param experiment
   * @throws REngineException
   */
  public void removeExpressionFiles(List<Sample> experiment)
      throws REngineException {

    int i;

    for (Sample s : experiment) {
      i = s.getId();

      // Remove file from rserve server
      this.rConnection.removeFile(expressionFilesDirectory
          + "/" + this.expressionFilesPrefix + i + this.expressionFilesSuffix);
    }
  }

  /*
   * Private methods
   */

  /**
   * Split design into multiple experiments Samples list
   * @return experiementMap a map of experiments
   */
  private Map<String, List<Sample>> experimentsSpliter() {

    String exp = this.design.getSample(0).getMetadata().getExperiment();
    
    LOGGER.info("" + exp);
    
    List<Sample> samples = this.design.getSamples();
    // Create design HashMap
    Map<String, List<Sample>> experimentMap = Maps.newHashMap();

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
   * @param rScript
   * @throws IOException
   * @throws REngineException
   * @throws EoulsanException
   */
  private void runRnwScript(String rnwScript) throws REngineException,
      EoulsanException {

    if (EoulsanRuntime.getSettings().isRServeServerEnabled()) {
      this.rConnection.executeRnwCode(rnwScript);

    } else {

      try {

        final ProcessBuilder pb =
            new ProcessBuilder("/usr/bin/R", "CMD", "Sweave", rnwScript);

        // Set the temporary directory for R
        pb.environment().put("TMPDIR", this.outPath.getAbsolutePath());

        ProcessUtils.logEndTime(pb.start(), Joiner.on(' ').join(pb.command()),
            System.currentTimeMillis());

        if (!new File(rnwScript).delete())
          LOGGER.warning("Unable to remove R script: " + rnwScript);

      } catch (IOException e) {

        throw new EoulsanException(
            "Error while executing R script in anadiff: " + e.getMessage());
      }

    }

  }

  /**
   * Read a static part of the generated script.
   * @param staticFile the name of a file containing a part of the script
   * @return A String with the static part of the script
   * @throws EoulsanException
   */
  private String readStaticScript(String staticFile) throws EoulsanException {

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
   * Write code with technical replicates.
   * @param sb StringBuilder to use
   * @param rSampleIds R samples ids
   * @param rSampleNames R samples names
   * @param rCondIndexes R conditions indexes
   * @param rCondNames R conditions names
   * @param rRepTechGroup R technical replicate group
   */
  private void writeWithTechnicalReplicate(final StringBuilder sb,
      final List<Integer> rSampleIds, final List<String> rSampleNames,
      final List<String> rCondNames, final List<String> rRepTechGroup,
      final String experimentName) {

    // Add samples names to R script
    sb.append("# create sample names vector\n");
    sb.append("sampleNames <- c(");
    boolean first = true;
    for (String r : rSampleNames) {

      if (first)
        first = false;
      else
        sb.append(',');
      sb.append('\"');
      sb.append(r.toLowerCase());
      sb.append('\"');
    }
    sb.append(")\n\n");

    // Put sample ids into R vector
    sb.append("sampleIds <- c(");
    int i = 0;
    for (int id : rSampleIds) {
      i++;
      sb.append("" + id);
      if (i < rSampleIds.size())
        sb.append(",");
    }
    sb.append(")\n\n");

    // Add file names vector
    sb.append("#create file names vector\n");
    sb.append("fileNames <- paste(\"" + expressionFilesPrefix + '\"' + ',');
    sb.append("sampleIds"
        + ',' + '\"' + expressionFilesSuffix + '\"' + ',' + "sep=\"\"" + ")"
        + "\n\n");

    // Add repTechGroup vector
    sb.append("# create technical replicates groups vector\n");
    sb.append("repTechGroup <- c(");
    first = true;
    for (String r : rRepTechGroup) {

      if (first)
        first = false;
      else
        sb.append(',');

      sb.append('\"');
      sb.append(r);
      sb.append('\"');
    }
    sb.append(")\n\n");

    // Add condition to R script
    sb.append("# create condition vector\n");
    sb.append("condition <- c(");
    first = true;
    for (String r : rCondNames) {

      if (first)
        first = false;
      else
        sb.append(',');
      sb.append('\"');
      sb.append(r);
      sb.append('\"');
    }
    sb.append(")\n\n");

    // Add projectPath, outPath and projectName
    sb.append("# projectPath : path of count files directory\n");
    sb.append("projectPath <- \"\"\n");
    sb.append("# outPath path of the outputs\n");
    sb.append("outPath <- \"./\"\n");
    sb.append("projectName <- ");
    sb.append("\"" + experimentName + "\"" + "\n");
    sb.append("@\n\n");

    // Add not variable part of the analysis
    try {
      sb.append(readStaticScript(NORMALISATION_WHITH_TECHREP));
    } catch (EoulsanException e) {
      e.printStackTrace();
    }
  }

  /**
   * Write normalization code without replicates
   * @param sb A StringBuilder
   * @param rSampleIds
   * @param rSampleNames
   * @param rCondNames
   */
  private void writeWithoutTechnicalReplicates(final StringBuilder sb,
      final List<Integer> rSampleIds, final List<String> rSampleNames,
      final List<String> rCondNames, String experimentName) {

    // Add samples names to R script
    sb.append("# create sample names vector\n");
    sb.append("sampleNames <- c(");
    boolean first = true;
    for (String r : rSampleNames) {

      if (first)
        first = false;
      else
        sb.append(',');
      sb.append('\"');
      sb.append(r);
      sb.append('\"');
    }
    sb.append(")\n\n");

    // Put sample ids into R vector
    sb.append("sampleIds <- c(");
    int i = 0;
    for (int id : rSampleIds) {
      i++;
      sb.append("" + id);
      if (i < rSampleIds.size())
        sb.append(",");
    }
    sb.append(")\n\n");

    // Add file names vector
    sb.append("#create file names vector\n");
    sb.append("fileNames <- paste(\"" + expressionFilesPrefix + '\"' + ',');
    sb.append("sampleIds"
        + ',' + '\"' + expressionFilesSuffix + '\"' + ',' + "sep=\"\"" + ")"
        + "\n\n");

    /*
     * Add repTechGroup vector equal to sampleNames to avoid error in R function
     * buildTarget
     */
    sb.append("# create technical replicates groups vector\n");
    sb.append("repTechGroup <- sampleNames\n\n");

    // Add condition to R script
    sb.append("# create condition vector\n");
    sb.append("condition <- c(");
    first = true;
    for (String r : rCondNames) {

      if (first)
        first = false;
      else
        sb.append(',');
      sb.append('\"');
      sb.append(r);
      sb.append('\"');
    }
    sb.append(")\n\n");

    // Add projectPath, outPath and projectName
    sb.append("# projectPath : path of count files directory\n");
    sb.append("projectPath <- \"\"\n");
    sb.append("# outPath path of the outputs\n");
    sb.append("outPath <- \"./\"\n");
    sb.append("projectName <- ");
    sb.append("\"" + experimentName + "\"" + "\n");
    sb.append("@\n\n");

    // Add not variable part of the analysis
    try {
      sb.append(readStaticScript(NORMALISATION_WHITHOUT_TECHREP));
    } catch (EoulsanException e) {
      e.printStackTrace();
    }

  }

  /**
   * Test if there is reference in an experiment
   * @param experiment
   * @return boolean isRef
   */
  private boolean isReference(List<Sample> experiment) {

    if (experiment == null
        || experiment.size() == 0
        || !experiment.get(0).getMetadata().isReferenceField())
      return false;

    for (Sample s : experiment) {
      if (s.getMetadata().isReference()) {
        return true;
      }
    }
    return false;
  }

  /*
   * Constructor
   */

  /**
   * Public constructor.
   * @param design Design to set
   */
  public DiffAna(final Design design, final File expressionFilesDirectory,
      final String expressionFilesPrefix, final String expressionFilesSuffix,
      final File outPath, final String rServerName) {

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
        .exists()))
      throw new NullPointerException(
          "The path of the expression files doesn't exist or is not a directory.");

    this.expressionFilesDirectory = expressionFilesDirectory;

    if (!(outPath.isDirectory() && outPath.exists()))
      throw new NullPointerException(
          "The outpath file doesn't exist or is not a directory.");

    this.outPath = outPath;

    this.rConnection = new RSConnectionNewImpl(rServerName);
  }

}
