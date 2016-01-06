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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.steps.mgmt.hadoop;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.IOException;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopOnly;
import fr.ens.biologie.genomique.eoulsan.core.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.StepContext;
import fr.ens.biologie.genomique.eoulsan.core.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.StepStatus;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan1DesignWriter;
import fr.ens.biologie.genomique.eoulsan.steps.AbstractStep;
import fr.ens.biologie.genomique.eoulsan.util.Version;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.PathUtils;

/**
 * This step copy design and workflow file to output directory.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class CopyDesignAndWorkflowFilesToOutputStep extends AbstractStep {

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

    return "Copy design and workflow file to output path.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    this.conf = CommonHadoop.createConfiguration(EoulsanRuntime.getSettings());
  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    final Configuration conf = this.conf;

    final Path designPath = new Path(context.getDesignFile().getSource());
    final Path workflowPath = new Path(context.getWorkflowFile().getSource());
    final Path outputPath = new Path(context.getOutputDirectory().getSource());

    final Path outputDesignPath = new Path(outputPath, designPath.getName());
    final Path outputWorkflowPath =
        new Path(outputPath, workflowPath.getName());

    // Copy design file
    try {
      if (!PathUtils.exists(outputDesignPath, conf)) {

        final FileSystem outputDesignFs = outputDesignPath.getFileSystem(conf);

        final Design design = context.getWorkflow().getDesign();
        new Eoulsan1DesignWriter(outputDesignFs.create(outputDesignPath))
            .write(design);
      }
    } catch (IOException e) {
      getLogger().severe("Unable to copy design file to output path.");
    }

    // Copy workflow file
    try {
      if (!PathUtils.exists(outputWorkflowPath, conf)) {
        PathUtils.copy(designPath, outputWorkflowPath, conf);
      }
    } catch (IOException e) {
      getLogger().severe("Unable to copy design file to output path.");
    }

    return status.createStepResult();
  }

}
