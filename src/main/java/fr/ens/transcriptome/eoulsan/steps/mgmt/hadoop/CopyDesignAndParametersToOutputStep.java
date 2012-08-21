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

package fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * This step copy design and parameter file to output directory.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class CopyDesignAndParametersToOutputStep extends AbstractStep {

  /** Logger. */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  /** Step name. */
  public static final String STEP_NAME = "_copy_design_params_to_output";

  private Configuration conf;

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "Copy design and parameters file to output path.";
  }

  @Override
  public String getLogName() {

    return null;
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    this.conf = CommonHadoop.createConfiguration(EoulsanRuntime.getSettings());
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    final Configuration conf = this.conf;

    final Path designPath = new Path(context.getDesignPathname());
    final Path paramPath = new Path(context.getParameterPathname());
    final Path outputPath = new Path(context.getOutputPathname());

    final Path outputDesignPath = new Path(outputPath, designPath.getName());
    final Path outputParamPath = new Path(outputPath, paramPath.getName());

    // Copy design file
    try {
      if (!PathUtils.exists(outputDesignPath, conf)) {

        final FileSystem outputDesignFs = outputDesignPath.getFileSystem(conf);

        new SimpleDesignWriter(outputDesignFs.create(outputDesignPath))
            .write(design);
      }
    } catch (IOException e) {
      logger.severe("Unable to copy design file to output path.");
    } catch (EoulsanIOException e) {
      logger.severe("Unable to copy design file to output path.");
    }

    // Copy parameter file
    try {
      if (!PathUtils.exists(outputParamPath, conf))
        PathUtils.copy(designPath, outputParamPath, conf);
    } catch (IOException e) {
      logger.severe("Unable to copy design file to output path.");
    }

    return new StepResult(context, true, "");
  }

}
