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
package fr.ens.transcriptome.eoulsan.it;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;

/**
 * This internal class allow to save Process outputs.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ITCommandResult {

  private final File directory;
  private final String commandLine;
  private final String desc;

  private final StringBuilder message;

  private Throwable exception;
  private int exitValue = -1;
  private long duration = -1;

  @SuppressWarnings("unused")
  private String exceptionMessage;

  public boolean isEmpty() {
    return this.message.toString().isEmpty();
  }

  /**
   * Gets the report on execution command line.
   * @return the report
   */
  public String getReport() {

    this.message.append("\nExecute script for ");
    this.message.append(this.desc);
    this.message.append("\n\tcommand line: ");
    this.message.append(this.commandLine);
    this.message.append("\n\tin directory: ");
    this.message.append(this.directory.getAbsolutePath());

    this.message.append("\n\tduration: ");
    this.message.append(this.duration == -1
        ? "none" : toTimeHumanReadable(this.duration));

    this.message.append("\n\texit value: ");
    this.message.append(this.exitValue);

    this.message.append("\n");

    return this.message.toString();
  }

  /**
   * Checks if is catched exception.
   * @return true, if is catched exception
   */
  public boolean isCatchedException() {
    return this.exception != null;
  }

  //
  // Getter & setter
  //

  /**
   * Set the exit value.
   * @param exitValue the new exit value
   */
  public void setExitValue(final int exitValue) {
    this.exitValue = exitValue;
  }

  /**
   * Set the duration.
   * @param duration the new duration
   */
  public void setDuration(final long duration) {
    this.duration = duration;
  }

  /**
   * Get the exception.
   * @return the exception
   */
  public Throwable getException() {
    return this.exception;
  }

  /**
   * Set the exception.
   * @param exception the new exception
   */
  public void setException(final Exception exception) {
    setException(exception, "");
  }

  /**
   * Set the exception.
   * @param exception the exception
   * @param message the message
   */
  public void setException(final Exception exception, final String message) {
    this.exception = exception;
    this.exceptionMessage = message;
  }

  //
  // Constructor
  //
  /**
   * Constructor.
   * @param commandLine the command line
   * @param directory the directory
   * @param desc the description on command line
   */
  ITCommandResult(final String commandLine, final File directory,
      final String desc) {

    this.commandLine = commandLine;
    this.directory = directory;
    this.desc = desc;

    this.message = new StringBuilder();

  }

}
