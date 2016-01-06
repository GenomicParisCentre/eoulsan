package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.List;

/**
 * This class define the default executor interpreter. This interpreter use
 * <code>/bin/sh -c</code> to create the command line.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DefaultExecutorInterpreter extends AbstractExecutorInterpreter {

  @Override
  public String getName() {

    return "default";
  }

  @Override
  public List<String> createCommandLine(final String arguments) {

    checkNotNull(arguments, "arguments argument cannot be null");

    return Arrays.asList("/bin/sh", "-c", arguments);
  }

}
