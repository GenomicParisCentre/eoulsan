/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.programs.expression.local;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.MainCLI;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.programs.expression.FinalExpressionTranscriptsCreator;

public class ExpressionLocalMain {

  public static String PROGRAM_NAME = "expression";

  public static String tmpdir;

  private static void expression(final String designFilename,
      final String tmpdir) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      final DesignReader dr = new SimpleDesignReader(designFilename);
      final Design design = dr.read();

      ExpressionPseudoMapReduce epmr = null;
      String lastAnnotationKey = null;

      for (Sample s : design.getSamples()) {

        final String annotationFilename =
            s.getMetadata().getAnnotation().trim();

        final String annotationKey =
            annotationFilename + " " + s.getMetadata().getGenomicType();

        if (!annotationKey.equals(lastAnnotationKey)) {

          epmr =
              new ExpressionPseudoMapReduce(new File(annotationFilename), s
                  .getMetadata().getGenomicType());

          lastAnnotationKey = annotationKey;
        }

        final File alignmentFile =
            new File(Common.SAMPLE_SOAP_ALIGNMENT_PREFIX
                + s.getId() + Common.SOAP_RESULT_EXTENSION);

        final File expressionTmpFile =
            new File(Common.SAMPLE_EXPRESSION_FILE_PREFIX
                + s.getId() + Common.SOAP_RESULT_EXTENSION + ".tmp");

        final File expressionFile =
            new File(Common.SAMPLE_EXPRESSION_FILE_PREFIX
                + s.getId() + Common.SAMPLE_EXPRESSION_FILE_SUFFIX);

        if (tmpdir != null)
          epmr.setMapReduceTemporaryDirectory(new File(tmpdir));
        epmr.doMap(alignmentFile);
        epmr.doReduce(expressionTmpFile);

        final FinalExpressionTranscriptsCreator fetc =
            new FinalExpressionTranscriptsCreator(epmr
                .getTranscriptAndExonFinder());

        fetc.initializeExpressionResults();
        fetc.loadPreResults(expressionTmpFile, epmr.getReporter()
            .getCounterValue(ExpressionPseudoMapReduce.COUNTER_GROUP,
                "reads used"));
        fetc.saveFinalResults(expressionFile);

        // Remove expression Temp file
        expressionTmpFile.delete();

        // Add counters for this sample to log file
        log.append(epmr.getReporter().countersValuesToString(
            ExpressionPseudoMapReduce.COUNTER_GROUP,
            "Expression computation ("
                + s.getName() + ", " + alignmentFile.getName() + ")"));

      }

      // Write log file
      Common.writeLog(new File("expression.log"), startTime, log.toString());

    } catch (EoulsanIOException e) {
      System.err.println("Error while reading design file: " + e.getMessage());
      System.exit(1);
    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("error while filtering: " + e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + " [options] design", options);

    System.exit(0);
  }

  /**
   * Create options for command line
   * @return an Options object
   */
  private static Options makeOptions() {

    // create Options object
    final Options options = new Options();

    options.addOption("version", false, "show version of the software");
    options
        .addOption("about", false, "display information about this software");
    options.addOption("h", "help", false, "display this help");
    options.addOption("license", false,
        "display information about the license of this software");

    options.addOption(OptionBuilder.withArgName("path").hasArg()
        .withDescription("temporary directory").create("tmpdir"));

    return options;
  }

  /**
   * Parse the options of the command line
   * @param args command line arguments
   * @return the number of optional arguments
   */
  private static int parseCommandLine(final String args[]) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      CommandLine line = parser.parse(options, args);

      if (line.hasOption("help"))
        help(options);

      if (line.hasOption("about"))
        MainCLI.about();

      if (line.hasOption("version"))
        MainCLI.version();

      if (line.hasOption("license"))
        MainCLI.license();

      if (line.hasOption("tmpdir")) {
        tmpdir = line.getOptionValue("tmpdir");
        argsOptions += 2;
      }

    } catch (ParseException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }

    return argsOptions;
  }

  /**
   * Main method
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    // Parse the command line
    final int argsOptions = parseCommandLine(args);

    if (args == null || args.length != argsOptions + 1) {

      System.err
          .println("This program needs one argument. Use the -h option to get more information.");
      System.err.println("usage:"
          + Globals.APP_NAME_LOWER_CASE + " " + PROGRAM_NAME
          + " [options] design");
      System.exit(1);
    }
    
    final String designFilename = args[argsOptions];

    expression(designFilename, tmpdir);
  }

}
