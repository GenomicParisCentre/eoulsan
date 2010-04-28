/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.design.impl;

import fr.ens.transcriptome.eoulsan.NividicRuntimeException;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;
import fr.ens.transcriptome.eoulsan.io.BioAssayFormat;

public class SampleImpl implements Sample {

  private DesignImpl design;
  private int sampleId;

  //
  // Getters
  //

  @Override
  public String getName() {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new NividicRuntimeException("The sample doesn't exists");

    return sampleName;
  }

  @Override
  public SampleMetadata getMetadata() {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new NividicRuntimeException("The sample doesn't exists");

    return this.design.getSampleMetadata(sampleName);
  }

  @Override
  public DataSource getSource() {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new NividicRuntimeException("The sample doesn't exists");

    return this.design.getSource(sampleName);
  }

  @Override
  public String getSourceInfo() {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new NividicRuntimeException("The sample doesn't exists");

    return this.design.getSourceInfo(sampleName);
  }

  @Override
  public BioAssayFormat getSourceFormat() {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new NividicRuntimeException("The sample doesn't exists");

    return this.design.getSourceFormat(sampleName);
  }

  //
  // Setters
  //

  @Override
  public void setName(final String newName) {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new NividicRuntimeException("The sample doesn't exists");

    this.design.renameSample(sampleName, newName);
  }

  @Override
  public void setSource(final DataSource source) {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new NividicRuntimeException("The sample doesn't exists");

    this.design.setSource(sampleName, source);
  }

  @Override
  public void setSource(final String filename) {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new NividicRuntimeException("The sample doesn't exists");

    this.design.setSource(sampleName, filename);
  }

  @Override
  public void setSourceFormat(final String format) {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new NividicRuntimeException("The sample doesn't exists");

    this.design.setSourceFormat(sampleName, format);
  }

  @Override
  public void setSourceFormat(final BioAssayFormat format) {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new NividicRuntimeException("The sample doesn't exists");

    this.design.setSourceFormat(sampleName, format);
  }

  //
  // Other methods
  //

  SampleImpl(final DesignImpl design, final int slideId) {

    this.design = design;
    this.sampleId = slideId;
  }

}
