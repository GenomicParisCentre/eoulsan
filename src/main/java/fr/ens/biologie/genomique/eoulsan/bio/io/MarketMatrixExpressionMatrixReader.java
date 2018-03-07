package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.bio.io.BioCharsets.GFF_CHARSET;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.bio.DenseExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.bio.ExpressionMatrix;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class define a reader for matrix saved at Market Matrix format.
 * @author Laurent Jourdren
 * @since 2.2
 */
public class MarketMatrixExpressionMatrixReader
    implements ExpressionMatrixReader {

  private String MAGIC_KEY = "%%MatrixMarket ";

  private final BufferedReader reader;

  /**
   * Get the row name of a row number
   * @param rowNumber
   * @return the row name
   */
  protected String getRowName(final int rowNumber) {
    return "row" + rowNumber;
  }

  /**
   * Get the column name of a column number
   * @param columnNumber
   * @return the column name
   */
  protected String getColumnName(final int columnNumber) {
    return "column" + columnNumber;
  }

  @Override
  public ExpressionMatrix read() throws IOException {

    return read(new DenseExpressionMatrix());
  }

  @Override
  public ExpressionMatrix read(final ExpressionMatrix matrix)
      throws IOException {

    Objects.requireNonNull(matrix, "matrix argument cannot be null");

    boolean first = true;

    int rowCount = -1;
    int columnCount = -1;
    int nonzero = -1;

    String line;
    int lineCount = 0;

    while ((line = reader.readLine()) != null) {

      lineCount++;
      if (first) {

        if (!line.startsWith(MAGIC_KEY)) {
          throw new IOException("Invalid Market Matrice header: " + line);
        }

        List<String> fields =
            Splitter.on(' ').trimResults().omitEmptyStrings().splitToList(line);

        if (fields.size() < 2) {
          throw new IOException("Invalid Market Matrice header: " + line);
        }

        if (!"matrix".equals(fields.get(1))) {
          throw new IOException("The reader only handle matrix files");
        }

        for (String s : fields.subList(2, fields.size() - 1)) {

          switch (s.toLowerCase()) {
          case "coordinate":
          case "real":
          case "integer":
          case "general":
            break;

          default:
            throw new IOException(
                "The reader does not support qualifier: " + s);
          }
        }

        first = false;
        continue;
      }

      // Skip comments
      if (line.startsWith("%")) {
        continue;
      }

      // Throw an error if the line is too long
      if (line.length() > 1024) {
        throw new IOException(
            "Invalide line length (>1024), line#" + lineCount + ": " + line);
      }

      line = line.trim();

      // Skip empty lines
      if (line.isEmpty()) {
        continue;
      }

      List<String> fields = Splitter.on(' ').splitToList(line);
      if (fields.size() != 3) {
        throw new IOException(
            "3 values are expected line #" + lineCount + ": " + line);
      }

      int i;
      int j;
      double value;

      try {
        i = Integer.parseInt(fields.get(0));
        j = Integer.parseInt(fields.get(1));
        value = Double.parseDouble(fields.get(2));
      } catch (NumberFormatException e) {
        throw new IOException(
            "Invalid number format line #" + lineCount + ": " + line);
      }

      if (nonzero == -1) {
        rowCount = i;
        columnCount = j;
        nonzero = (int) value;

        // Fill row names
        for (int k = 1; k <= rowCount; k++) {
          matrix.addRow(getRowName(k));
        }

        // Fill column names
        for (int k = 1; k <= columnCount; k++) {
          matrix.addColumn(getColumnName(k));
        }

      } else {
        matrix.setValue(getRowName(i), getColumnName(j), value);
      }
    }

    return matrix;
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public MarketMatrixExpressionMatrixReader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }

    this.reader = new BufferedReader(new InputStreamReader(is, GFF_CHARSET));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public MarketMatrixExpressionMatrixReader(final File file)
      throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("File is null");
    }

    this.reader = FileUtils.createBufferedReader(file, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param filename File to use
   */
  public MarketMatrixExpressionMatrixReader(final String filename)
      throws FileNotFoundException {

    this.reader = FileUtils.createBufferedReader(filename, GFF_CHARSET);
  }

}
