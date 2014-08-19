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

package fr.ens.transcriptome.eoulsan.util;

import java.io.File;

/**
 * Get information about processor on Linux systems.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class LinuxCpuInfo extends LinuxInfo {

  private static final String CPUINFO_FILE = "/proc/cpuinfo";

  @Override
  public File getInfoFile() {

    return new File(CPUINFO_FILE);
  }

  /**
   * Get the model of processor
   * @return the model of processor
   */
  public String getModelName() {

    return get("model name");
  }

  /**
   * Get the processor name.
   * @return the processor name
   */
  public String getProcessor() {

    return get("processor");
  }

  /**
   * Get CPU MHz.
   * @return the frequency of the processor
   */
  public String getCPUMHz() {

    return get("cpu MHz");
  }

  /**
   * Get processor bogomips.
   * @return the processor bogomips
   */
  public String getBogoMips() {

    return get("bogomips");
  }

  /**
   * Get the number of cores of the processor.
   * @return the number of core of the processor
   */
  public String getCores() {

    return get("cpu cores");
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public LinuxCpuInfo() {

    parse();
  }

}
