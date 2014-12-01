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

package fr.ens.transcriptome.eoulsan.steps;

import static fr.ens.transcriptome.eoulsan.io.CompressionType.NONE;
import static fr.ens.transcriptome.eoulsan.io.CompressionType.getCompressionTypeByContentEncoding;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.annotations.ReuseStepInstance;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.splitermergers.Merger;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a generic merger step
 * @author Laurent Jourdren
 * @since 2.0
 */
@HadoopCompatible
@ReuseStepInstance
public class MergerStep extends AbstractStep {

  private Merger merger;
  private CompressionType compression = NONE;

  //
  // Inner class
  //

  /**
   * This inner class allow to create iterator needed by SplitterMerger.merge()
   * method.
   */
  private static final class MergerIterator {

    private final ListMultimap<String, Data> map = ArrayListMultimap.create();
    private int maxFileIndex = 1;

    public Set<String> getDataNames() {

      return this.map.keySet();
    }

    public int getMaxFileIndex() {

      return this.maxFileIndex;
    }

    public Iterator<DataFile> getIterator(final String dataName)
        throws EoulsanException {

      return getIterator(dataName, -1);
    }

    public Iterator<DataFile> getIterator(final String dataName,
        final int fileIndex) throws EoulsanException {

      final List<Data> list = Lists.newArrayList(map.get(dataName));

      // Sort Data by their part number
      Collections.sort(list, new Comparator<Data>() {
        @Override
        public int compare(final Data a, final Data b) {

          return Integer.valueOf(a.getPart()).compareTo(b.getPart());
        }
      });

      // Check if two data has the same part number
      final Set<Integer> partNumbers = new HashSet<>();
      for (Data data : list) {

        if (partNumbers.contains(data.getPart())) {
          throw new EoulsanException(
              "Found two or more data with the same part: " + data.getName());
        }
        partNumbers.add(data.getPart());
      }

      final Iterator<Data> it = list.iterator();

      // Create the iterator itself
      return new Iterator<DataFile>() {

        @Override
        public boolean hasNext() {

          return it.hasNext();
        }

        @Override
        public DataFile next() {

          if (fileIndex == -1) {
            return it.next().getDataFile();
          } else {
            return it.next().getDataFile(fileIndex);
          }
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    /**
     * Constructor.
     * @param data the data
     */
    public MergerIterator(final Data data) {

      for (Data d : data.getListElements()) {
        this.map.put(d.getName(), d);

        if (d.getDataFileCount() > this.maxFileIndex) {
          this.maxFileIndex = d.getDataFileCount();
        }
      }
    }

  }

  //
  // Step methods
  //

  @Override
  public String getName() {

    return "merger";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    return new InputPortsBuilder().addPort("input", true,
        this.merger.getFormat()).create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    return new OutputPortsBuilder().addPort("output", true,
        this.merger.getFormat(), this.compression).create();
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    final Set<Parameter> mergerParameters = new HashSet<>();

    for (Parameter p : stepParameters) {

      if ("format".equals(p.getName())) {

        // Get format
        final DataFormat format =
            DataFormatRegistry.getInstance()
                .getDataFormatFromName(p.getValue());

        // Check if the format exists
        if (format == null) {
          throw new EoulsanException("Unknown format: " + p.getValue());
        }

        // Check if a merger exists for the format
        if (!format.isMerger()) {
          throw new EoulsanException("No splitter exists for format: "
              + format.getName());
        }

        // Set the merger
        this.merger = format.getMerger();

      } else if ("compression".equals(p.getName())) {

        this.compression = getCompressionTypeByContentEncoding(p.getValue());
      } else {
        mergerParameters.add(p);
      }
    }

    // Check if a format has been set
    if (this.merger == null) {
      throw new EoulsanException("No format set for splitter");
    }

    // Configure the merger
    this.merger.configure(mergerParameters);
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    final DataFormat format = this.merger.getFormat();

    // Get input and output data
    final Data inData = context.getInputData(format);
    final Data outData = context.getOutputData(format, inData);

    try {

      final MergerIterator it = new MergerIterator(inData);

      for (String dataName : it.getDataNames()) {

        // If Mono-file format
        if (format.getMaxFilesCount() == 1) {

          // Get output file
          final DataFile outFile =
              outData.addDataToList(dataName).getDataFile();

          // Launch merger
          this.merger.merge(it.getIterator(dataName), outFile);
        } else {

          // For each file of the multi-file format
          for (int fileIndex = 0; fileIndex < it.getMaxFileIndex(); fileIndex++) {

            // Get output file
            final DataFile outFile =
                outData.addDataToList(dataName).getDataFile(fileIndex);

            // Launch splitting
            this.merger.merge(it.getIterator(dataName, fileIndex), outFile);
          }
        }
      }

      // Successful result
      return status.createStepResult();
    } catch (IOException e) {

      // Fail of the step
      return status.createStepResult(e);
    } catch (EoulsanException e) {

      // Fail of the step
      return status.createStepResult(e);
    }
  }
}
