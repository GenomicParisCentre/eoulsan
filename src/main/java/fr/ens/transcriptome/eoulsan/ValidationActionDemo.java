package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.eoulsan.actions.Action;
import fr.ens.transcriptome.eoulsan.actions.ActionService;
import fr.ens.transcriptome.eoulsan.actions.ValidationAction;
import fr.ens.transcriptome.eoulsan.data.DataSetAnalysis;
import fr.ens.transcriptome.eoulsan.io.ComparatorDirectories;
import fr.ens.transcriptome.eoulsan.io.CompareFiles;
import fr.ens.transcriptome.eoulsan.io.LogCompareFiles;

public class ValidationActionDemo {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static void main(String[] args) throws EoulsanException {
    File f = new File("/home/sperrin/param_toto.xml");
    int beginIndex = "param_".length();
    int endIndex = f.getName().length() - ".xml".length();

    String name = f.getName().substring(beginIndex, endIndex);

    System.out.println("name " + name);
    // ValidationActionDemo.mainbis();

    // testLogCompare();
  }

  public static void testLogCompare() {
    // Map<Object, Object> map = System.getProperties();
    // System.out.println("print properties \n"
    // + Joiner.on("\n").withKeyValueSeparator("\t").join(map));
    //
    // System.exit(0);

    CompareFiles comp = new LogCompareFiles();
    // String
    // fileAFperrin/Documents/test_eoulsan/dataset_source/test_expected/eoulsan-20140128-160713/filtersam.log";
    String dir =
        new File(
            "/home/sperrin/Documents/test_eoulsan/dataset_source/test_expected/")
            .listFiles(new FileFilter() {

              @Override
              public boolean accept(final File pathname) {
                return pathname.getName().startsWith("eoulsan-");
              }
            })[0].getAbsolutePath();

    System.out.println("dir log " + dir);

    // String fileA =
    // "/home/sperrin/Documents/test_eoulsan/dataset_source/expected/eoulsan-20140124-112847/mapreads.log";
    // String fileB =
    // "/home/sperrin/Documents/test_eoulsan/dataset_source/test_expected/"
    // + dir + "/mapreads.log";

    String fileA =
        "/home/sperrin/Documents/test_eoulsan/dataset_source/expected/eoulsan-20140124-112847/filtersam.log";
    String fileB = dir + "/filtersam.log";
    try {
      System.out.println("result " + comp.compareFiles(fileA, fileB));

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static void maintri() {

    String s =
        "validation -p /home/sperrin/home-net/eoulsan -d validation -s /home/sperrin/Documents/test_eoulsan/dataset_source/expected -o /home/sperrin/Documents/test_eoulsan/dataset_source ";

    String[] cmd = s.split(" ");

    System.out.println("cmd " + Arrays.asList(cmd));
    System.setProperty(Globals.LAUNCH_MODE_PROPERTY, "local");
    Main.main(cmd);
  }

  public static void mainbis() {

    final String pathEoulsanNewVersion = "/home/sperrin/home-net/eoulsan";
    final String listDatasets =
        "/home/sperrin/Documents/test_eoulsan/dataset_source/expected";
    final String pathOutputDirectory =
        "/home/sperrin/Documents/test_eoulsan/dataset_source";
    final String jobDescription = "validation_test";

    
    final String confpath = "/home/sperrin/Documents/test_eoulsan/dataset_source/test_fonctionnel.conf";

    // Set the default local for all the application
    Globals.setDefaultLocale();

    // Set default log level
    LOGGER.setLevel(Globals.LOG_LEVEL);
    LOGGER.getParent().getHandlers()[0].setFormatter(Globals.LOG_FORMATTER);

    // Select the application execution mode
    final String eoulsanMode = System.getProperty(Globals.LAUNCH_MODE_PROPERTY);

    Main main;
    // if (eoulsanMode != null && eoulsanMode.equals("local")) {
    main = new MainCLI(new String[] {"validation"});
    // } else {
    // main = new MainHadoop(args);
    // }

    // // Get the action to execute
    // final Action action = main.getAction();
    Action action0 = ActionService.getInstance().getAction("validation");

    ValidationAction action = (ValidationAction) action0;

    // Get the Eoulsan settings
    final Settings settings = EoulsanRuntime.getSettings();

    // Test if action can be executed with current platform
    if (!settings.isBypassPlatformChecking()
        && !action.isCurrentArchCompatible()) {
      Common.showErrorMessageAndExit(Globals.WELCOME_MSG
          + "\nThe " + action.getName() + " of " + Globals.APP_NAME
          + " is not available for your platform. Required platforms: " + ".");

    }

    boolean completed = false;

    if (completed) {
      // Run action
      action.run(confpath, jobDescription);

    } else {
      try {
        DataSetAnalysis datasetExpected;
        datasetExpected =
            new DataSetAnalysis(
                "/home/sperrin/Documents/test_eoulsan/dataset_source/expected",
                true);

        // Init data set tested
        final DataSetAnalysis datasetTested =
            new DataSetAnalysis(
                "/home/sperrin/Documents/test_eoulsan/dataset_source/test_expected/",
                false);
        datasetTested.init();
        System.out.println("expected size "
            + datasetExpected.getFilesByName().size());

        System.out.println("tested size "
            + datasetTested.getFilesByName().size());

        // Compare two directory
        ComparatorDirectories comparator =
            new ComparatorDirectories(ValidationAction.USE_SERIALIZATION,
                ValidationAction.CHECKING_SAME_NAME);

        // Launch comparison
        comparator.compareDataSet("testName", datasetExpected, datasetTested);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (EoulsanException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

    }
  }
}
