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

import org.apache.hadoop.conf.Configuration;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;

/**
 * This class define common constants and other methods specific to Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class CommonHadoop {

  public static final String AWS_S3_SECRET_ACCESS_KEY_PARAM_NAME =
      "fs.s3n.awsSecretAccessKey";
  public static final String AWS_S3_ACCESS_KEY_ID_PARAM_KEY =
      "fs.s3n.awsAccessKeyId";

  public static final String HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME =
      "hadoop.reducer.task.count";

  public static final int CHECK_COMPLETION_TIME = 5000;
  public static final String HADOOP_PARAMETER_PREFIX = "hadoop.conf.";

  public static final String COUNTER_GROUP_KEY =
      Globals.PARAMETER_PREFIX + ".counter.group";

  /**
   * Create a new Configuration object from Eoulsan runtime settings.
   * @return a new Configuration object
   */
  public static final Configuration createConfiguration() {

    return createConfiguration(EoulsanRuntime.getSettings());
  }

  /**
   * Create a new Configuration object from settings.
   * @param settings Settings of the application
   * @return a new Configuration object
   */
  public static final Configuration createConfiguration(
      final Settings settings) {

    if (settings == null) {
      return null;
    }

    final Configuration conf = new Configuration();

    for (String keyName : settings.getSettingsNames()) {

      if (keyName.startsWith(HADOOP_PARAMETER_PREFIX)) {

        final String hadoopKey =
            keyName.substring(HADOOP_PARAMETER_PREFIX.length());

        conf.set(hadoopKey, settings.getSetting(keyName));
      }
    }

    return conf;
  }

}
