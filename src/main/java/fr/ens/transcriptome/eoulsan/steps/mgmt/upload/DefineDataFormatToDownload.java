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

package fr.ens.transcriptome.eoulsan.steps.mgmt.upload;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.steps.mgmt.upload.HDFSDataDownloadStep.DATAFORMATS_TO_DOWNLOAD_SETTING;

import java.util.Set;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This Step allow to define the list of the formats of the files to download at
 * the end of a Hadoop execution.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class DefineDataFormatToDownload extends AbstractStep {

  protected static final String STEP_NAME = "defineformatstodownload";

  private DataFormat[] inFormats;

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "define the list of the formats of the files to download "
        + "at the end of a Hadoop execution";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    String formatNames = null;

    for (Parameter p : stepParameters) {

      if ("formats".equals(p.getName()))
        formatNames = p.getStringValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());
    }

    if (formatNames == null) {
      throw new EoulsanException("No format to download set.");
    }

    final String[] fields = formatNames.split(",");
    final Set<DataFormat> formats = Sets.newHashSet();
    final DataFormatRegistry registery = DataFormatRegistry.getInstance();

    for (String format : fields) {

      if ("".equals(format.trim())) {
        continue;
      }

      final DataFormat df = registery.getDataFormatFromName(format.trim());
      if (df == null) {
        throw new EoulsanException("Format not found : " + format.trim());
      }

      formats.add(df);
    }

    this.inFormats = formats.toArray(new DataFormat[formats.size()]);
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    final StringBuilder sb = new StringBuilder();
    boolean first = true;

    for (DataFormat df : this.inFormats) {

      if (first) {
        first = false;
      } else {
        sb.append(',');
      }

      sb.append(df.getName());
    }

    final String formats = sb.toString();

    getLogger().info("Format to download: " + formats);

    // Save the list of the DataFormat to download in the settings
    final Settings settings = context.getRuntime().getSettings();

    settings.setSetting(DATAFORMATS_TO_DOWNLOAD_SETTING, formats, false);

    status.setMessage("Formats to download: " + formats);
    return status.createStepResult();
  }

}
