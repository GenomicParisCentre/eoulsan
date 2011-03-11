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

import java.security.InvalidParameterException;

/**
 * Discribe a version of a software.
 * @author Laurent Jourdren
 */
public final class Version implements Comparable<Version> {

  private int major;
  private int minor;
  private int revision;

  //
  // Getters
  //

  /**
   * Get the number of major version of the Version.
   * @return The major version
   */
  public int getMajor() {
    return major;
  }

  /**
   * Get the number of minor version of the Version.
   * @return The minor version
   */
  public int getMinor() {
    return minor;
  }

  /**
   * Get the number of revision of the Version
   * @return the number of revsion of the version
   */
  public int getRevision() {
    return revision;
  }

  //
  // Setters
  //

  /**
   * Set the number of major version of the Version.
   * @param major The major version of the version
   */
  public void setMajor(final int major) {
    if (major >= 0)
      this.major = major;
    else
      this.major = 0;
  }

  /**
   * Set the number of minor version of the Version.
   * @param minor The minor version of the version
   */
  public void setMinor(final int minor) {
    if (minor >= 0)
      this.minor = minor;
    else
      this.minor = 0;
  }

  /**
   * Set the number of revision of the Version
   * @param revision The number of revision of the version
   */
  public void setRevision(final int revision) {
    if (revision >= 0)
      this.revision = revision;
    else
      this.revision = 0;
  }

  //
  // Other methods
  //

  /**
   * Get the version in a string format.
   * @return The version in a strinf format
   */
  public String toString() {
    return getMajor() + "." + getMinor() + "." + getRevision();
  }

  /**
   * Set the version.
   * @param major The major version of the version
   * @param minor The minor version of the version
   * @param revision The number of revision of the version
   */
  public void setVersion(final int major, final int minor, final int revision) {
    setMajor(major);
    setMinor(minor);
    setRevision(revision);
  }

  /**
   * Set the version.
   * @param version The version to set
   */
  public void setVersion(final String version) {

    if (version == null)
      return;

    String v = version.trim();
    if (version.endsWith("-SNAPSHOT"))
      v = version.replaceAll("-SNAPSHOT", "");
    else if ("UNKNOWN_VERSION".equals(v))
      v = "0.0.0";

    final String[] fields = v.split("\\.");
    int major = 0;
    int minor = 0;
    int revision = 0;

    try {
      if (fields.length > 0)
        major = Integer.parseInt(fields[0].trim());
      if (fields.length > 1)
        minor = Integer.parseInt(fields[1].trim());
      if (fields.length > 2)
        revision = Integer.parseInt(fields[2].trim());
    } catch (NumberFormatException e) {
      throw new InvalidParameterException("Invalid version format in string: "
          + version);
    }

    setVersion(major, minor, revision);
  }

  /**
   * Compare 2 Version object.
   * @param version Version to compare.
   * @return a negative integer, zero, or a positive integer as this object is
   *         less than, equal to, or greater than the specified object.
   */
  @Override
  public int compareTo(final Version version) {

    if (version == null)
      return 1;

    final int compMajor =
        Integer.valueOf(getMajor()).compareTo(version.getMajor());
    if (compMajor != 0)
      return compMajor;

    final int compMinor =
        Integer.valueOf(getMinor()).compareTo(version.getMinor());
    if (compMinor != 0)
      return compMinor;

    return Integer.valueOf(getRevision()).compareTo(version.getRevision());
  }

  public boolean equals(final Object o) {

    if (o == null)
      return false;

    if (!(o instanceof Version))
      return false;

    final Version v = (Version) o;

    return v.major == this.major
        && v.minor == this.minor && v.revision == this.revision;
  }

  /**
   * Get the minimal version from an array of versions.
   * @param versions The array of versions
   * @return The minimal version
   */
  public static Version getMinimalVersion(final Version[] versions) {

    if (versions == null || versions.length == 0)
      return null;

    Version min = versions[0];

    for (int i = 1; i < versions.length; i++)
      if (min.compareTo(versions[i]) > 0)
        min = versions[i];

    return min;
  }

  /**
   * Get the maximal version from an array of versions.
   * @param versions The array of versions
   * @return The maximal version
   */
  public static Version getMaximalVersion(final Version[] versions) {

    if (versions == null || versions.length == 0)
      return null;

    Version max = versions[0];

    for (int i = 1; i < versions.length; i++)
      if (max.compareTo(versions[i]) < 0)
        max = versions[i];

    return max;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public Version() {
    
    this(null);
  }

  /**
   * Public constructor.
   * @param major The major version of the version
   * @param minor The minor version of the version
   * @param revision The number of revision of the version
   */
  public Version(final int major, final int minor, final int revision) {
    
    setVersion(major, minor, revision);
  }

  /**
   * Public constructor.
   * @param version The version to set
   */
  public Version(final String version) {
    
    setVersion(version);
  }

}
