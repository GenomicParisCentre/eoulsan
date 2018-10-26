package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.Matrix;

/**
 * This class define a writer to save matrix saved at Market Matrix format.
 * @author Laurent Jourdren
 * @since 2.4
 */
public class MarketMatrixExpressionMatrixWriter
    implements ExpressionMatrixWriter {

  private final OutputStream os;

  @Override
  public void write(final ExpressionMatrix matrix) throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    write(matrix, matrix.getRowNames());
  }

  @Override
  public void write(final ExpressionMatrix matrix,
      final Collection<String> rowNamesToWrite) throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");
    Objects.requireNonNull(rowNamesToWrite,
        "rowNamesToWrite argument cannot be null");

    Set<String> rowNames = rowNamesToWrite instanceof Set
        ? (Set<String>) rowNamesToWrite : new HashSet<String>(rowNamesToWrite);

    try (Writer writer = new OutputStreamWriter(this.os)) {

      // Write header
      writer.write(MarketMatrixExpressionMatrixReader.MAGIC_KEY);
      writer.write("matrix coordinate real general\n");

      // Write the size of the matrix
      writer.write(""
          + matrix.getRowCount() + ' ' + matrix.getColumnCount() + ' '
          + entryCount(matrix, rowNames) + '\n');

      Map<String, Integer> rowPositions = keyPositions(matrix.getRowNames());
      Map<String, Integer> columnPositions =
          keyPositions(matrix.getColumnNames());

      for (Matrix.Entry<Double> e : matrix.nonZeroValues()) {
        if (rowNames.contains(e.getRowName())) {
          writer.write(""
              + rowPositions.get(e.getRowName()) + ' '
              + columnPositions.get(e.getColumnName()) + ' ' + e.getValue()
              + '\n');
        }
      }
    }
  }

  /**
   * Get the indexes of the rows and columns.
   * @param entryNames entry names
   * @return a map with the indexes of the entries
   */
  private static Map<String, Integer> keyPositions(
      final List<String> entryNames) {

    final Map<String, Integer> result = new HashMap<>();
    int count = 1;

    for (String e : entryNames) {
      result.put(e, count++);
    }

    return result;
  }

  /**
   * Count the number of entries in the matrix.
   * @param matrix the matrix
   * @param rowNames the row names to write
   * @return the number of the entries in the matrix
   */
  private static int entryCount(ExpressionMatrix matrix, Set<String> rowNames) {

    int entryCount = 0;

    for (Matrix.Entry<Double> e : matrix.nonZeroValues()) {

      if (rowNames.contains(e.getRowName())) {
        entryCount++;
      }
    }

    return entryCount;
  }

  @Override
  public void close() throws IOException {

    this.os.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param os InputStream to use
   */
  public MarketMatrixExpressionMatrixWriter(final OutputStream os) {

    Objects.requireNonNull(os, "os argument cannot be null");

    this.os = os;
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public MarketMatrixExpressionMatrixWriter(final File file)
      throws FileNotFoundException {

    Objects.requireNonNull(file, "file argument cannot be null");

    this.os = new FileOutputStream(file);
  }

  /**
   * Public constructor.
   * @param filename File to use
   */
  public MarketMatrixExpressionMatrixWriter(final String filename)
      throws FileNotFoundException {

    Objects.requireNonNull(filename, "filename argument cannot be null");

    this.os = new FileOutputStream(filename);

  }

}
