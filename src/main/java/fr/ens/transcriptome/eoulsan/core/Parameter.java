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

import java.io.Serializable;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class define a parameter. The parameter name is always in lower case.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class Parameter implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = -3788321419921821433L;

  private final String name;
  private final String value;

  /**
   * Get the name of the parameter.
   * @return Returns the name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Get the value of the parameter.
   * @return Returns the value
   */
  public String getValue() {
    return this.value;
  }

  /**
   * Get the value of the parameter as a String value.
   * @return the value as a String
   */
  public String getStringValue() {

    return this.value;
  }

  /**
   * Get the value of the parameter as a lower case String value.
   * @return the value as a String
   */
  public String getLowerStringValue() {

    return this.value.toLowerCase();
  }

  /**
   * Get the value of the parameter as a upper case String value.
   * @return the value as a String
   */
  public String getUpperStringValue() {

    return this.value.toUpperCase();
  }

  /**
   * Get the value of the parameter as a integer value.
   * @return the value as an integer
   */
  public int getIntValue() throws EoulsanException {

    try {

      return Integer.parseInt(this.value);
    } catch (NumberFormatException e) {

      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + this.name + " parameter: " + this.value, e);
    }
  }

  /**
   * * Get the value of the parameter as a integer value and check if this value
   * is greater or equals to the min parameter value.
   * @param min minimal value (included)
   * @return the value as an integer
   */
  public int getIntValueGreaterOrEqualsTo(final int min)
      throws EoulsanException {

    return getIntValueInRange(min, Integer.MAX_VALUE);
  }

  /**
   * Get the value of the parameter as a integer value and check if this value
   * is in the correct range.
   * @param min minimal value (included)
   * @param max maximal value (included)
   * @return the value as an integer
   */
  public int getIntValueInRange(final int min, final int max)
      throws EoulsanException {

    final int result = getIntValue();

    final int minValue = Math.min(min, max);
    final int maxValue = Math.max(min, max);

    if (result < minValue) {
      throw new EoulsanException("Invalid "
          + this.name + "parameter (The value must be greater than " + minValue
          + "): " + result);
    }

    if (result > maxValue) {
      throw new EoulsanException("Invalid "
          + this.name + "parameter (The value must be lower than " + maxValue
          + "): " + result);
    }

    return result;
  }

  /**
   * Get the value of the parameter as a double value.
   * @return the value as an integer
   */
  public double getDoubleValue() throws EoulsanException {

    try {

      return Double.parseDouble(this.value);
    } catch (NumberFormatException e) {

      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + this.name + " parameter: " + this.value, e);
    }
  }

  /**
   * Get the value of the parameter as a boolean value.
   * @return the value as a boolean
   */
  public boolean getBooleanValue() {

    return Boolean.parseBoolean(this.value);
  }

  @Override
  public String toString() {

    return this.name + "=" + this.value;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param name Name of the parameter
   * @param value value of the parameter
   */
  public Parameter(final String name, final String value) {

    if (name == null) {
      throw new NullPointerException("Parameter name can't be null");
    }

    final String nameLower = name.toLowerCase().trim();

    if (value == null) {
      throw new NullPointerException("Parameter value can't be null");
    }

    if ("".equals(nameLower)) {
      throw new IllegalArgumentException("Parameter name can't be empty");
    }

    this.name = nameLower;
    this.value = value;
  }

}
