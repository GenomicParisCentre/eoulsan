package fr.ens.transcriptome.eoulsan.core.workflow;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toLetter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

class DataElement extends AbstractData {



  private final Map<String, String> metadata = Maps.newHashMap();
  protected final List<DataFile> files;

  // Field required for multi-files Data creation
  private final DataFile stepWorkingPathname;
  private final String stepId;
  private final String portName;
  private final CompressionType compression;

  private boolean canRename = true;
  private boolean modifiable = false;

  @Override
  public void setName(final String name) {

    if (!this.canRename)
      throw new EoulsanRuntimeException(
          "Data cannot be renamed once it has been used");

    super.setName(name);

    // If DataFile object(s) has not been set in the constructor
    if (this.stepId != null) {

      // Update the DataFile filename
      for (int i = 0; i < this.files.size(); i++) {
        this.files.set(i, createDataFile(i));
      }
    }
  }

  @Override
  public boolean isList() {
    return false;
  }

  @Override
  public List<Data> getListElements() {
    return Collections.singletonList((Data) this);
  }

  @Override
  public Map<String, String> getMetadata() {
    return Collections.unmodifiableMap(this.metadata);
  }

  /**
   * Set a metadata.
   * @param key key of the metadata
   * @param value value of the metadata
   */
  protected void setMetadata(final String key, final String value) {

    if (key == null || value == null)
      return;

    this.metadata.put(key, value);
  }

  @Override
  public Data addDataToList(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDataFilename() {
    return getDataFile().getName();
  }

  @Override
  public String getDataFilename(final int fileIndex) {

    if (getFormat().getMaxFilesCount() < 2)
      throw new EoulsanRuntimeException(
          "Only multifiles DataFormat are handled by this method.");

    return this.files.get(fileIndex).getName();
  }

  @Override
  public DataFile getDataFile() {

    this.canRename = false;
    return this.files.get(0);
  }

  void setDataFile(final DataFile dataFile) {

    Preconditions.checkNotNull(dataFile, "DataFile to set cannot be null");

    if (this.files.size()==0)
      throw new IllegalStateException("Cannot set a DataFile if not already exists");

    this.files.set(0, dataFile);
  }

  void setDataFile(final int fileIndex, final DataFile dataFile) {

    Preconditions.checkArgument(fileIndex>=0, "fileIndex argument must be >=0");
    Preconditions.checkNotNull(dataFile, "DataFile to set cannot be null");

    if (fileIndex>=this.files.size())
      throw new IllegalStateException("Cannot set a DataFile if not already exists");

    this.files.set(fileIndex, dataFile);
  }

  List<DataFile> getDataFiles() {

    return Collections.unmodifiableList(Lists.newArrayList(this.files));
  }

  @Override
  public DataFile getDataFile(int fileIndex) {

    if (getFormat().getMaxFilesCount() < 2)
      throw new EoulsanRuntimeException(
          "Only multi-files DataFormat are handled by this method.");

    if (fileIndex < 0)
      throw new EoulsanRuntimeException(
          "File index parameter cannot be lower than 0");

    if (fileIndex > this.files.size())
      throw new EoulsanRuntimeException("Cannot create file index "
          + fileIndex + " as file index " + this.files.size()
          + " is not created");

    if (fileIndex >= getFormat().getMaxFilesCount())
      throw new EoulsanRuntimeException("The format "
          + getFormat().getName() + " does not support more than "
          + getFormat().getMaxFilesCount() + " multi-files");

    // Create DataFile is required
    if (fileIndex == this.files.size()) {
      this.files.add(createDataFile(fileIndex));
    }

    return this.files.get(fileIndex);
  }

  @Override
  public int getDataFileCount() {
    return this.files.size();
  }

  @Override
  public int getDataFileCount(boolean existingFiles) {
    return this.files.size();
  }

  private DataFile createDataFile(final int fileIndex) {

    final StringBuilder sb = new StringBuilder();

    // Set the name of the step that generated the file
    sb.append(this.stepId);
    sb.append('_');

    // Set the port of the step that generated the file
    sb.append(this.portName);
    sb.append('_');

    // Set the name of the format
    sb.append(getFormat().getName());
    sb.append('_');

    // Set the name of the date
    sb.append(getName());

    // Set the file index if needed
    if (fileIndex >= 0)
      sb.append(toLetter(fileIndex));

    // Set the extension
    sb.append(getFormat().getDefaultExtention());

    // Set the compression extension
    sb.append(this.compression.getExtension());

    return new DataFile(this.stepWorkingPathname, sb.toString());
  }

  public void setUnmodifiable() {

    this.modifiable = true;
  }

  //
  // Constructor
  //

  DataElement(final DataFormat format, final List<DataFile> files) {

    super(format);

    Preconditions.checkNotNull(format, "format argument cannot be null");
    Preconditions.checkNotNull(files, "files argument cannot be null");

    for (DataFile f : files)
      if (f == null)
        throw new IllegalArgumentException(
            "The files list argument cannot contains null elements");

    this.files = Lists.newArrayList(files);

    this.stepWorkingPathname = null;
    this.stepId = null;
    this.portName = null;
    this.compression = null;
  }

  DataElement(final DataFormat format, final DataFile file) {
    this(format, Collections.singletonList(file));
  }

  DataElement(final DataFile stepWorkingPathname,
              final WorkflowOutputPort port) {

    super(port.getFormat());

    Preconditions.checkNotNull(port,
        "stepWorkingPathname argument cannot be null");
    Preconditions.checkNotNull(port, "port argument cannot be null");

    this.stepWorkingPathname = stepWorkingPathname;
    this.stepId = port.getStep().getId();
    this.portName = port.getName();
    this.compression = port.getCompression();

    if (getFormat().getMaxFilesCount() == 1) {
      this.files = Lists.newArrayList(createDataFile(-1));
    } else {
      this.files = Lists.newArrayList(createDataFile(0));
      this.modifiable = true;
    }
  }

}
