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

package fr.ens.transcriptome.eoulsan.core;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.util.Map;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.annotations.EoulsanMode;
import fr.ens.transcriptome.eoulsan.util.ServiceNameLoader;

/**
 * This class allow to get a Step object from a class in the classpath.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class StepService extends ServiceNameLoader<Step> {

  //
  // Protected methods
  //

  @Override
  protected boolean accept(final Class<?> clazz) {

    if (EoulsanRuntime.getRuntime().isHadoopMode()) {
      return true;
    }

    return EoulsanMode.accept(clazz, false);
  }

  @Override
  protected String getMethodName() {

    return "getName";
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */

  StepService() {

    super(Step.class);

    for (Map.Entry<String, String> e : getServiceClasses().entries()) {

      getLogger().config(
          "Found step: " + e.getKey() + " (" + e.getValue() + ")");
    }
  }

}
