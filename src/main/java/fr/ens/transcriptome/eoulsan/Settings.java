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

package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class define a settings class.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class Settings {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String MAIN_PREFIX_KEY = "main.";
  private final Properties properties = new Properties();

  private static final String DEBUG_KEY = MAIN_PREFIX_KEY + "debug";
  private static final String AWS_ACCESS_KEY = "aws.access.key";
  private static final String AWS_SECRET_KEY = "aws.secret.key";
  private static final String AWS_UPLOAD_MULTIPART = "aws.upload.multipart";

  private static final String PRINT_STACK_TRACE_KEY = MAIN_PREFIX_KEY
      + "printstacktrace";

  private static final String BYPASS_PLATFORM_CHECKING_KEY = MAIN_PREFIX_KEY
      + "bypass.platform.checking";

  private static final String TMP_DIR_KEY = MAIN_PREFIX_KEY + "tmp.dir";

  private static final String LOCAL_THREADS_NUMBER = MAIN_PREFIX_KEY
      + "local.threads";

  private static final String HADOOP_AWS_ACCESS_KEY =
      "hadoop.conf.fs.s3n.awsAccessKeyId";
  private static final String HADOOP_AWS_SECRET_KEY =
      "hadoop.conf.fs.s3n.awsSecretAccessKey";

  private static final String RSERVE_ENABLED_KEY = MAIN_PREFIX_KEY
      + "rserve.enable";
  private static final String RSERVE_SERVER_NAME_KEY = MAIN_PREFIX_KEY
      + "rserve.servername";

  private static final String SAVE_RSCRIPTS_KEY = MAIN_PREFIX_KEY
      + "save.r.scripts";

  private static final String OBFUSCATE_DESIGN_KEY = MAIN_PREFIX_KEY
      + "design.obfuscate";
  private static final String OBFUSCATE_DESIGN_REMOVE_REPLICATE_INFO_KEY =
      MAIN_PREFIX_KEY + "design.remove.replicate.info";

  private static final String DEFAULT_FASTQ_FORMAT_KEY = MAIN_PREFIX_KEY
      + "default.fastq.format";

  private static final String GENOME_MAPPER_INDEX_STORAGE_KEY = MAIN_PREFIX_KEY
      + "genome.mapper.index.storage.path";

  private static final String GENOME_DESC_STORAGE_KEY = MAIN_PREFIX_KEY
      + "genome.desc.storage.path";

  private static final String GENOME_STORAGE_KEY = MAIN_PREFIX_KEY
      + "genome.storage.path";

  private static final String ANNOTATION_STORAGE_KEY = MAIN_PREFIX_KEY
      + "annotation.storage.path";

  private static final String SEND_RESULT_MAIL_KEY = MAIN_PREFIX_KEY
      + ".mail.send.result.mail";

  private static final String RESULT_MAIL_KEY = MAIN_PREFIX_KEY
      + ".mail.send.result.mail.to";

  private static final String SMTP_HOST_KEY = MAIN_PREFIX_KEY
      + ".mail.smtp.host";

  private static final Set<String> FORBIDDEN_KEYS = Utils
      .unmodifiableSet(new String[] {HADOOP_AWS_ACCESS_KEY,
          HADOOP_AWS_SECRET_KEY});

  //
  // Getters
  //

  /**
   * Test if a setting key exists.
   * @return true if the setting exist
   */
  public boolean isSetting(final String key) {

    return this.properties.containsKey(key);
  }

  /**
   * Test is the debug mode is enabled.
   * @return true if the debug mode is enable
   */
  public boolean isDebug() {

    final String value =
        this.properties.getProperty(DEBUG_KEY, Boolean.toString(Globals.DEBUG));

    return Boolean.valueOf(value);
  }

  /**
   * Test is the debug mode is enabled.
   * @return true if the debug mode is enable
   */
  public boolean isPrintStackTrace() {

    final String value =
        this.properties.getProperty(PRINT_STACK_TRACE_KEY,
            Boolean.toString(Globals.PRINT_STACK_TRACE_DEFAULT));

    return Boolean.valueOf(value);
  }

  /**
   * Get the AWS access key.
   * @return the AWS access key
   */
  public String getAWSAccessKey() {

    return this.properties.getProperty(AWS_ACCESS_KEY);
  }

  /**
   * Get the AWS secret key.
   * @return the AWS secret key
   */
  public String getAWSSecretKey() {

    return this.properties.getProperty(AWS_SECRET_KEY);
  }

  /**
   * Test if AWS multipart upload is enabled.
   * @return true if the multipart upload is enabled
   */
  public boolean isAWSMultipartUpload() {

    final String value =
        this.properties.getProperty(AWS_UPLOAD_MULTIPART,
            Boolean.toString(Globals.AWS_UPLOAD_MULTIPART_DEFAULT));

    return Boolean.valueOf(value);
  }

  /**
   * Test if RServe is enabled.
   * @return true if the RServe server is enabled
   */
  public boolean isRServeServerEnabled() {

    return Boolean
        .parseBoolean(this.properties.getProperty(RSERVE_ENABLED_KEY));
  }

  /**
   * Test if save.r.script is true
   * @return boolean with keep Rscripts values
   */
  public boolean isSaveRscripts() {

    return Boolean.parseBoolean(this.properties.getProperty(SAVE_RSCRIPTS_KEY));
  }

  /**
   * Get the RServe server name.
   * @return The name of the RServe to use
   */
  public String getRServeServername() {

    return this.properties.getProperty(RSERVE_SERVER_NAME_KEY);
  }

  /**
   * Get the temporary directory.
   * @return The temporary directory
   */
  public String getTempDirectory() {

    return this.properties.getProperty(TMP_DIR_KEY,
        System.getProperty("java.io.tmpdir"));
  }

  /**
   * Get the temporary directory File.
   * @return The temporary directory as a File object
   */
  public File getTempDirectoryFile() {

    return new File(getTempDirectory());
  }

  /**
   * Test if design must be obfuscated
   * @return true if design must be obfuscated
   */
  public boolean isObfuscateDesign() {

    return Boolean.parseBoolean(this.properties.getProperty(
        OBFUSCATE_DESIGN_KEY,
        Boolean.toString(Globals.OBFUSCATE_DESIGN_DEFAULT)));
  }

  /**
   * Test if replicate information must be removed from design.
   * @return true if replicate information must be removed
   */
  public boolean isObfuscateDesignRemoveReplicateInfo() {

    return Boolean.parseBoolean(this.properties.getProperty(
        OBFUSCATE_DESIGN_REMOVE_REPLICATE_INFO_KEY, Boolean
            .toString(Globals.OBFUSCATE_DESIGN_REMOVE_REPLICATE_INFO_DEFAULT)));
  }

  /**
   * Get the number of threads to use in Steps computation in local mode.
   * @return the number of threads to use
   */
  public int getLocalThreadsNumber() {

    return Integer.parseInt(this.properties.getProperty(LOCAL_THREADS_NUMBER,
        "" + Runtime.getRuntime().availableProcessors()));
  }

  /**
   * Get the default fastq format.
   * @return the default fastq format
   */
  public FastqFormat getDefaultFastqFormat() {

    return FastqFormat.getFormatFromName(this.properties.getProperty(
        DEFAULT_FASTQ_FORMAT_KEY, Globals.FASTQ_FORMAT_DEFAULT.getName()));
  }

  /**
   * Test if the platform checking must be avoided at Eoulsan startup.
   * @return true if the platform checking must be avoided
   */
  public boolean isBypassPlatformChecking() {

    return Boolean.parseBoolean(this.properties
        .getProperty(BYPASS_PLATFORM_CHECKING_KEY));
  }

  /**
   * Get the genome mapper index storage path.
   * @return the path to genome mapper index storage path
   */
  public String getGenomeMapperIndexStoragePath() {

    return this.properties.getProperty(GENOME_MAPPER_INDEX_STORAGE_KEY);
  }

  /**
   * Get the genome description storage path.
   * @return the path to genome description storage path
   */
  public String getGenomeDescStoragePath() {

    return this.properties.getProperty(GENOME_DESC_STORAGE_KEY);
  }

  /**
   * Get the genome storage path.
   * @return the path to genome storage path
   */
  public String getGenomeStoragePath() {

    return this.properties.getProperty(GENOME_STORAGE_KEY);
  }

  /**
   * Get the annotation storage path.
   * @return the path to annotation storage path
   */
  public String getAnnotationStoragePath() {

    return this.properties.getProperty(ANNOTATION_STORAGE_KEY);
  }

  /**
   * Test if an email must be sent at the end of the analysis.
   * @return true if an email must be sent at the end of the analysis
   */
  public boolean isSendResultMail() {

    return Boolean.parseBoolean(this.properties
        .getProperty(SEND_RESULT_MAIL_KEY));
  }

  /**
   * Get the mail address for eoulsan results.
   * @return the mail address as a string
   */
  public String getResultMail() {

    return this.properties.getProperty(RESULT_MAIL_KEY);
  }

  /**
   * Get the name of the SMTP server host.
   * @return the name of the SMTP server host
   */
  public String getSMTPHost() {

    return this.properties.getProperty(SMTP_HOST_KEY);
  }

  /**
   * Get a setting value.
   * @return settingName value as a String
   */
  public String getSetting(final String settingName) {

    if (settingName == null) {
      return null;
    }

    if (settingName.startsWith(MAIN_PREFIX_KEY)) {
      return null;
    }

    return this.properties.getProperty(settingName);
  }

  /**
   * Get the value of the setting as a integer value
   * @return the value of the setting as an integer
   * @throws EoulsanException if the value is not an integer
   */
  public int getIntSetting(final String settingName) throws EoulsanException {

    if (settingName == null)
      throw new EoulsanException("The setting name is null");

    final String value = getSetting(settingName);
    if (value == null)
      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + settingName + " parameter: null");

    try {

      return Integer.parseInt(value);
    } catch (NumberFormatException e) {

      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + settingName + " parameter: " + value);
    }

  }

  /**
   * Get the value of the setting as a double value
   * @return the value of the setting as an double
   * @throws EoulsanException if the value is not an double
   */
  public double getDoubleSetting(final String settingName)
      throws EoulsanException {

    if (settingName == null)
      throw new EoulsanException("The setting name is null");

    final String value = getSetting(settingName);
    if (value == null)
      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + settingName + " parameter: null");

    try {

      return Double.parseDouble(value);
    } catch (NumberFormatException e) {

      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + settingName + " parameter: " + value);
    }

  }

  /**
   * Get the value of the setting as a boolean value
   * @return the value of the setting as an integer
   * @throws EoulsanException if the value is not an integer
   */
  public boolean getBooleanSetting(final String settingName) {

    return Boolean.parseBoolean(getSetting(settingName));
  }

  /**
   * Get a set of settings names.
   * @return a set with all the name of the settings
   */
  public Set<String> getSettingsNames() {

    final Set<String> result = new HashSet<String>();

    for (String key : this.properties.stringPropertyNames()) {
      if (!key.startsWith(MAIN_PREFIX_KEY)) {
        result.add(key);
      }
    }

    return result;
  }

  //
  // Setters
  //

  /**
   * Set the debug setting.
   * @param debug value of the debug setting
   */
  public void setDebug(final boolean debug) {

    this.properties.setProperty(DEBUG_KEY, Boolean.toString(debug));
  }

  /**
   * Set the print stack trace setting.
   * @param printStackTrace value of the print stack trace setting
   */
  public void setPrintStackTrace(final boolean printStackTrace) {

    this.properties.setProperty(PRINT_STACK_TRACE_KEY,
        Boolean.toString(printStackTrace));
  }

  /**
   * Set the AWS access key.
   * @param value the AWS access key
   */
  public void setAWSAccessKey(final String value) {

    if (value == null) {
      return;
    }

    this.properties.setProperty(AWS_ACCESS_KEY, value);
    this.properties.setProperty(HADOOP_AWS_ACCESS_KEY, value);
  }

  /**
   * Set the AWS secret key.
   * @param value the AWS secret key
   */
  public void setAWSSecretKey(final String value) {

    if (value == null) {
      return;
    }

    this.properties.setProperty(AWS_SECRET_KEY, value);
    this.properties.setProperty(HADOOP_AWS_SECRET_KEY, value);
  }

  /**
   * Set if the AWS multipart upload mode is enabled.
   * @param multipartUpload true if the multipart upload is enabled
   */
  public void setAWSMultipartUpload(final boolean multipartUpload) {

    this.properties.setProperty(AWS_UPLOAD_MULTIPART,
        Boolean.toString(multipartUpload));
  }

  /**
   * Set if RServe is enabled.
   * @param enable true if the RServe server is enable
   */
  public void setRServeServerEnabled(final boolean enable) {

    this.properties.setProperty(RSERVE_ENABLED_KEY, Boolean.toString(enable));
  }

  /**
   * Set if save Rscripts is true
   * @param krs boolean
   */
  public void setSaveRscript(final boolean krs) {

    this.properties.setProperty(SAVE_RSCRIPTS_KEY, Boolean.toString(krs));
  }

  /**
   * Set the RServe server name.
   * @param serverName The name of the RServe to use
   */
  public void setRServeServername(final String serverName) {

    this.properties.setProperty(RSERVE_SERVER_NAME_KEY, serverName);
  }

  /**
   * Set the temporary directory.
   * @param tempDirectory The path to the temporary directory
   */
  public void setTempDirectory(final String tempDirectory) {

    if (tempDirectory != null) {
      this.properties.setProperty(TMP_DIR_KEY, tempDirectory);
    }
  }

  /**
   * Set if the design must be obfuscated
   * @param obfuscate true if the design must be obfuscated
   */
  public void setObfuscateDesign(final boolean obfuscate) {

    this.properties.setProperty(OBFUSCATE_DESIGN_KEY,
        Boolean.toString(obfuscate));
  }

  /**
   * Set if the replicate information must be removed from the design.
   * @param remove true if the replicate information must be remove
   */
  public void setObfuscateRemoveDesignInfo(final boolean remove) {

    this.properties.setProperty(OBFUSCATE_DESIGN_REMOVE_REPLICATE_INFO_KEY,
        Boolean.toString(remove));
  }

  /**
   * Set the number of threads to use in local mode.
   * @param threadsNumber the number of threads to use in local mode
   */
  public void setLocalThreadsNumber(final int threadsNumber) {

    if (threadsNumber < 0)
      return;

    this.properties.setProperty(LOCAL_THREADS_NUMBER,
        Integer.toString(threadsNumber));
  }

  /**
   * Set the Fastq format default value.
   * @param format the value to set
   */
  public void setDefaultFastqFormat(final FastqFormat format) {

    if (format == null)
      throw new NullPointerException("The FastqFormat is null");

    this.properties.setProperty(DEFAULT_FASTQ_FORMAT_KEY, format.getName());
  }

  /**
   * Set if the platform checking must be avoided.
   * @param bypass true to bypass the platform checking
   */
  public void setBypassPlatformChecking(final boolean bypass) {

    this.properties.setProperty(BYPASS_PLATFORM_CHECKING_KEY,
        Boolean.toString(bypass));
  }

  /**
   * Set the genome index storage path.
   * @param genomeMapperIndexStoragePath the path to genome index storage path
   */
  public void setGenomeMapperIndexStoragePath(
      final String genomeMapperIndexStoragePath) {

    this.properties.setProperty(GENOME_MAPPER_INDEX_STORAGE_KEY,
        genomeMapperIndexStoragePath);
  }

  /**
   * Set the genome description storage path.
   * @param genomeDescStoragePath the path to genome index storage path
   */
  public void setGenomeDescStoragePath(final String genomeDescStoragePath) {

    this.properties.setProperty(GENOME_DESC_STORAGE_KEY, genomeDescStoragePath);
  }

  /**
   * Set the genome storage path.
   * @param genomeStoragePath the path to genome index storage path
   */
  public void setGenomeStoragePath(final String genomeStoragePath) {

    this.properties.setProperty(GENOME_STORAGE_KEY, genomeStoragePath);
  }

  /**
   * Set the annotation storage path.
   * @param annotationStoragePath the path to annotation index storage path
   */
  public void setAnnotationStoragePath(final String annotationStoragePath) {

    this.properties.setProperty(ANNOTATION_STORAGE_KEY, annotationStoragePath);
  }

  /**
   * Set if an email must be sent at the end of the analysis.
   * @param enableSendResultMail true if an email must be sent at the end of the
   *          analysis
   */
  public void setSendResultMail(final boolean enableSendResultMail) {

    this.properties.setProperty(SEND_RESULT_MAIL_KEY,
        Boolean.toString(enableSendResultMail));
  }

  /**
   * Set the mail address for eoulsan results.
   * @param the mail address as a string
   */
  public void setResultMail(final String mail) {

    this.properties.setProperty(RESULT_MAIL_KEY, mail);
  }

  /**
   * Set the SMTP server host.
   * @param smtpHost the name of the SMTP server host
   */
  public void setSMTPHost(final String smtpHost) {

    this.properties.setProperty(SMTP_HOST_KEY, smtpHost);
  }

  /**
   * Set a setting value.
   * @param settingName name of the setting to set
   * @param settingValue value of the setting to set
   */
  public void setSetting(final String settingName, final String settingValue) {

    if (settingName == null || settingValue == null) {
      return;
    }

    final String key = settingName.toLowerCase();

    if (FORBIDDEN_KEYS.contains(key)) {
      return;
    }

    if ("main.accesskey".equals(key)) {
      setSetting(AWS_ACCESS_KEY, settingValue);
      return;
    }

    if ("main.awssecretkey".equals(key)) {
      setSetting(AWS_SECRET_KEY, settingValue);
      return;
    }

    this.properties.setProperty(key, settingValue);
  }

  //
  // Other methods
  //

  /**
   * Get the configuration file path.
   * @return the configuration file path
   */
  public static String getConfigurationFilePath() {

    final String os = System.getProperty("os.name");
    final String home = System.getProperty("user.home");

    if (os.toLowerCase(Globals.DEFAULT_LOCALE).startsWith("windows")) {
      return home
          + File.separator + "Application Data" + File.separator
          + Globals.APP_NAME_LOWER_CASE + ".conf";
    }

    return home + File.separator + "." + Globals.APP_NAME_LOWER_CASE;
  }

  /**
   * Create a property object for javamail smtp configuration from the settings.
   * @return a property object
   */
  public Properties getJavaMailSMTPProperties() {

    final Properties result = new Properties();

    for (Map.Entry<Object, Object> e : this.properties.entrySet()) {

      final String key = (String) e.getKey();
      final String value = (String) e.getValue();

      final String prefix = MAIN_PREFIX_KEY + "mail.smtp.";
      final int keyPosStart = MAIN_PREFIX_KEY.length();

      if (key != null && key.startsWith(prefix)) {
        result.setProperty(key.substring(keyPosStart), value);
      }

    }

    return result;
  }

  /**
   * Save application options.
   * @throws IOException if an error occurs while writing results
   */
  public void saveSettings() throws IOException {

    saveSettings(new File(getConfigurationFilePath()));
  }

  /**
   * Save application options.
   * @param file File to save
   * @throws IOException if an error occurs while writing settings
   */
  public void saveSettings(final File file) throws IOException {

    final OutputStream os = FileUtils.createOutputStream(file);

    this.properties.store(os, " "
        + Globals.APP_NAME + " version " + Globals.APP_VERSION_STRING
        + " configuration file");
    os.close();
  }

  /**
   * Load application options.
   * @throws IOException if an error occurs while reading settings
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  public void loadSettings() throws IOException, EoulsanException {

    final File confFile = new File(getConfigurationFilePath());
    if (confFile.exists()) {
      loadSettings(confFile);
    } else {
      LOGGER.config("No configuration file found.");
    }
  }

  /**
   * Load application options.
   * @param file file to save
   * @throws IOException if an error occurs while reading the file
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  public void loadSettings(final File file) throws IOException,
      EoulsanException {

    LOGGER.info("Load configuration file: " + file.getAbsolutePath());
    final InputStream is = FileUtils.createInputStream(file);

    this.properties.load(FileUtils.createInputStream(file));
    is.close();

    for (String key : this.properties.stringPropertyNames()) {
      if (FORBIDDEN_KEYS.contains(key)) {
        throw new EoulsanException("Forbiden key found in configuration file: "
            + key);
      }

      if ("main.accesskey".equals(key.toLowerCase()))
        throw new EoulsanException("main.accesskey key in now invalid. Use "
            + AWS_ACCESS_KEY + " key instead.");

      if ("main.awssecretkey".equals(key.toLowerCase()))
        throw new EoulsanException("main.awssecretkey key in now invalid. Use "
            + AWS_SECRET_KEY + " key instead.");

    }

  }

  private void init() {

    LOGGER.info("System temp directory: "
        + System.getProperty("java.io.tmpdir"));
    setTempDirectory(System.getProperty("java.io.tmpdir"));
  }

  /**
   * Add all the settings to the log.
   */
  public void logSettings() {

    for (Object key : properties.keySet()) {

      final String sKey = (String) key;
      final String sValue = properties.getProperty(sKey);

      if (sKey.equals(HADOOP_AWS_ACCESS_KEY)
          || sKey.endsWith((HADOOP_AWS_SECRET_KEY)))
        LOGGER.info("Setting: " + sKey + "=xxxx value not shown xxxx");
      else
        LOGGER.info("Setting: " + sKey + "=" + sValue);
    }

  }

  //
  // Constructor
  //

  /**
   * Public constructor. Load application options.
   * @throws IOException if an error occurs while reading settings
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  Settings() throws IOException, EoulsanException {

    this(false);

  }

  /**
   * Public constructor. Load application options.
   * @param loadDefaultConfigurationFile true if default configuration file must
   *          be read
   * @throws IOException if an error occurs while reading settings
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  Settings(final boolean loadDefaultConfigurationFile) throws IOException,
      EoulsanException {

    init();

    if (!loadDefaultConfigurationFile) {
      loadSettings();
    }
  }

  /**
   * Public constructor. Load application options.
   * @param file file to save
   * @throws IOException if an error occurs while reading the file
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  Settings(final File file) throws IOException, EoulsanException {

    init();
    loadSettings(file);
  }

  @Override
  public String toString() {
    return this.properties.toString();
  }

}
