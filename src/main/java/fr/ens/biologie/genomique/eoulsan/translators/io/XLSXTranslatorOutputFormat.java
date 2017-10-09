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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.translators.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

/**
 * This class define a TranslatorOutputFormat that generate a Microsoft Excel
 * XLSX file.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class XLSXTranslatorOutputFormat implements TranslatorOutputFormat {

  private static final int MAX_LINES_IN_MEMORY = 10;

  private final OutputStream os;
  private final SXSSFWorkbook wb = new SXSSFWorkbook(MAX_LINES_IN_MEMORY);
  private final Sheet sheet = this.wb.createSheet("new sheet");
  private final CreationHelper createHelper = wb.getCreationHelper();
  private final CellStyle headerStyle;
  private final CellStyle linkStyle;
  private int rowCount;
  private int colCount;
  private Row row = this.sheet.createRow(this.rowCount++);

  @Override
  public void addHeaderField(final String fieldName) throws IOException {

    final Cell cell = this.row.createCell(this.colCount++);
    cell.setCellValue(new XSSFRichTextString(fieldName));
    cell.setCellStyle(this.headerStyle);
  }

  @Override
  public void newLine() throws IOException {

    this.colCount = 0;
    this.row = this.sheet.createRow(this.rowCount++);
  }

  @Override
  public void writeEmpty() throws IOException {

    this.row.createCell(this.colCount++);
  }

  @Override
  public void writeLong(final long l) throws IOException {

    final Cell cell = this.row.createCell(this.colCount++);
    cell.setCellValue(l);
  }

  @Override
  public void writeDouble(final double d) throws IOException {

    final Cell cell = this.row.createCell(this.colCount++);
    cell.setCellValue(d);
  }

  @Override
  public void writeText(final String text) throws IOException {

    final Cell cell = this.row.createCell(this.colCount++);
    if (text != null) {
      cell.setCellValue(new XSSFRichTextString(text));
    }
  }

  @Override
  public void writeLink(final String text, final String link)
      throws IOException {

    final Cell cell = this.row.createCell(this.colCount++);

    if (text != null) {

      if (link != null) {
        Hyperlink hyperlink = createHelper.createHyperlink(Hyperlink.LINK_URL);
        hyperlink.setAddress(link);
        cell.setHyperlink(hyperlink);
        cell.setCellStyle(this.linkStyle);
      }

      cell.setCellValue(new XSSFRichTextString(text));
    }
  }

  @Override
  public void close() throws IOException {

    this.wb.write(this.os);
    this.os.close();

    // Dispose of temporary files backing the workbook on disk
    this.wb.dispose();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param os output stream
   */
  public XLSXTranslatorOutputFormat(final OutputStream os) {

    if (os == null) {
      throw new NullPointerException("The output stream is null");
    }

    this.os = os;

    // Temporary files will be compressed
    this.wb.setCompressTempFiles(true);

    // Create a new header font and alter it.
    Font font = this.wb.createFont();
    font.setItalic(true);
    font.setFontHeightInPoints((short) 10);

    // Fonts are set into a style so create a new one to use.
    this.headerStyle = this.wb.createCellStyle();
    this.headerStyle.setFillForegroundColor(HSSFColor.ORANGE.index);
    this.headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
    this.headerStyle.setFont(font);

    // Create a new link font and alter it.
    Font linkfont = this.wb.createFont();
    linkfont.setUnderline(XSSFFont.U_SINGLE);
    linkfont.setColor(HSSFColor.BLUE.index);

    // Create link style
    this.linkStyle = this.wb.createCellStyle();
    this.linkStyle.setFont(linkfont);
  }

  /**
   * Public constructor.
   * @param file output file
   */
  public XLSXTranslatorOutputFormat(final File file) throws IOException {

    this(new FileOutputStream(file));
  }

}
