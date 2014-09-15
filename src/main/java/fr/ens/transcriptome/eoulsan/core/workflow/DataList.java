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

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataMetadata;
import fr.ens.transcriptome.eoulsan.design.Design;

/**
 * This class define a data list.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class DataList extends AbstractData implements Serializable {

  private static final long serialVersionUID = -2933515018143805029L;

  private final List<Data> list = Lists.newArrayList();

  // Required by the metadata
  private final Design design;

  // Field required for multi-files Data creation
  private final WorkflowOutputPort port;

  @Override
  public Data addDataToList(final String name) {
    return addDataToList(name, -1);
  }

  @Override
  public Data addDataToList(final String name, final int part) {

    Preconditions.checkNotNull(name, "name argument cannot be null");

    if (this.port == null)
      throw new UnsupportedOperationException();

    final AbstractData result = new DataElement(this.port, this.design);
    result.setName(name);
    result.setPart(part);
    this.list.add(result);

    return result;
  }

  @Override
  public int getPart() {
    return -1;
  }

  @Override
  public boolean isList() {
    return true;
  }

  @Override
  public List<Data> getListElements() {
    return Collections.unmodifiableList(this.list);
  }

  /**
   * Get the modifiable list of data object.
   * @return a list object
   */
  List<Data> getModifiableList() {
    return this.list;
  }

  @Override
  public DataMetadata getMetadata() {
    return new UnmodifiableDataMetadata(new SimpleDataMetadata(this.design));
  }

  @Override
  public String getDataFilename() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDataFilename(int fileIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataFile getDataFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataFile getDataFile(int fileIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getDataFileCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getDataFileCount(boolean existingFiles) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("name", getName())
        .add("format", getFormat().getName()).add("metadata", getMetadata())
        .add("list", isList()).add("elements", this.list).toString();
  }

  //
  // Constructors
  //

  /**
   * Constructor.
   * @param port input port
   * @param design design
   */
  DataList(final WorkflowInputPort port, final Design design) {

    super(port.getFormat());

    checkNotNull(design, "design argument cannot be null");

    this.port = null;
    this.design = design;
  }

  /**
   * Constructor.
   * @param port output port
   * @param design
   */
  DataList(final WorkflowOutputPort port, final Design design) {

    super(port.getFormat());

    checkNotNull(design, "design argument cannot be null");

    this.port = port;
    this.design = design;
  }

}
