package fr.ens.transcriptome.eoulsan.toolgalaxy;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class ToolInterpreterSimpleTest {

  private File dir = new File(new File(".").getAbsolutePath(),
      "src/main/java/files/toolshed/");

  private static boolean VERBOSE = true;

  // @Test
  public void parseSamtoolsRmdup() throws FileNotFoundException,
      EoulsanException {
    final File toolFile =
        new File(dir, "samtools_rmdup/1.0.1/samtools_rmdup.xml");
    String name = "rmdup";
    String version = "1.0.1";
    String desc = "remove PCR duplicates";
    String interpreter = "";

    // type \t name \t value
    String paramTabular_SR = "";
    paramTabular_SR += "input\tinput1\tinput_value\n";
    paramTabular_SR += "output\toutput1\toutput_value\n";
    paramTabular_SR +=
        "param\tbam_paired_end_type.bam_paired_end_type_selector\tSE\n";

    String cmd_SR =
        "samtools rmdup -s input_value output_value "
            + "2&gt;&amp;1 || echo \"Error running samtools rmdup.\" &gt;&amp;2";

    final MockEoulsan mock =
        new MockEoulsan(name, version, desc, interpreter, cmd_SR,
            paramTabular_SR);

    checkInterperter(toolFile, mock);

    // // type \t name \t value
    // String paramTabular_PE = "";
    // paramTabular_PE += "input\tinput1\tinput_value\n";
    // paramTabular_PE += "output\toutput1\toutput_value\n";
    // paramTabular_PE +=
    // "param\tbam_paired_end_type.bam_paired_end_type_selector\tPE\n";
    //
    // String cmd_PE =
    // "samtools rmdup -S input_value output_value "
    // + "2&gt;&amp;1 || echo \"Error running samtools rmdup.\" &gt;&amp;2";
    // mock.setCommand(cmd_PE, paramTabular_PE);
    //
    // checkInterperter(toolFile, mock);
  }

  @Test
  public void parseSimpleToolFileTest() throws FileNotFoundException,
      EoulsanException {
    final File toolFile = new File(dir, "ToolGalaxyBasic.xml");

    // type \t name \t value
    String paramTabular = "";
    paramTabular += "input\tinput\tinput_value\n";
    paramTabular += "output\toutput\toutput_value\n";

    final MockEoulsan mock =
        new MockEoulsan("Compute GC content", "",
            "for each sequence in a file", "perl",
            "toolExample.pl input_value output_value", paramTabular);

    checkInterperter(toolFile, mock);
  }

  private void checkInterperter(final File toolFile, final MockEoulsan mock)
      throws FileNotFoundException, EoulsanException {

    // Create input stream
    final InputStream is = FileUtils.createInputStream(toolFile);

    // Init interpreter tool galaxy
    final ToolInterpreter itg = new ToolInterpreter(is);

    // Configure
    itg.configure(mock.getParameters());

    if (VERBOSE)
      System.out.println(itg.toString());

    assertEquals("Tool name equals ? ", itg.getToolName(), mock.getName());
    assertEquals("Tool version equals ? ", itg.getToolVersion(),
        mock.getVersion());
    assertEquals("Tool description equals ? ", itg.getDescription(),
        mock.getDescription());
    assertEquals("Tool interpreter equals ? ", itg.getInterpreter(),
        mock.getInterpreter());

    // Set ports
    itg.setPortInput(mock.getInputsPort());
    itg.setPortOutput(mock.getOutputsPort());

    // Create command line
    final String cmd = itg.createCommandLine();

    if (VERBOSE)
      System.out.println("command create\n\t " + cmd);

    assertEquals("Command line equals ", cmd, mock.getCommand());
  }

  //
  //
  //

  //
  // Internal class
  //

  final class MockEoulsan {
    private String name;
    private String version;
    private String description;
    private String interpreter;
    private String command;

    private Map<String, String> parameters;
    private Map<String, String> inputs;
    private Map<String, String> outputs;

    public String getDescription() {
      return description;
    }

    public String getInterpreter() {
      return interpreter;
    }

    public String getName() {
      return name;
    }

    public String getVersion() {
      return version;
    }

    public Map<String, String> getParameters() {
      return this.parameters;
    }

    public String getCommand() {
      return command;
    }

    public Map<String, String> getInputsPort() {
      return this.inputs;
    }

    public Map<String, String> getOutputsPort() {
      return this.outputs;
    }

    void initMapPortsAndParameters(final String paramTabular)
        throws EoulsanException {
      Splitter splitLine = Splitter.on('\n').trimResults().omitEmptyStrings();

      for (String line : splitLine.splitToList(paramTabular)) {
        String[] tokens = line.split("\t");

        switch (tokens[0]) {
        case "param":
          this.parameters.put(tokens[1], tokens[2]);
          break;

        case "input":
          this.inputs.put(tokens[1], tokens[2]);
          break;

        case "output":
          this.outputs.put(tokens[1], tokens[2]);
          break;

        default:
          throw new EoulsanException("Entry unknown: " + tokens[0]);
        }
      }
    }

    public void setCommand(final String cmd, final String paramTabular)
        throws EoulsanException {
      this.command = cmd;

      // Re initialize map value
      this.parameters.clear();
      this.inputs.clear();
      this.outputs.clear();

      initMapPortsAndParameters(paramTabular);
    }

    public MockEoulsan(final String name, final String version,
        final String description, final String interpreter,
        final String command, final String paramTabular)
        throws EoulsanException {
      this.name = name;
      this.version = version;
      this.description = description;
      this.interpreter = interpreter;
      this.command = command;

      this.parameters = Maps.newHashMap();
      this.inputs = Maps.newHashMap();
      this.outputs = Maps.newHashMap();

      initMapPortsAndParameters(paramTabular);
    }
  }
}
