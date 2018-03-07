package fr.ens.biologie.genomique.eoulsan.bio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public abstract class AbstractExpressionMatrixTest {

  protected abstract ExpressionMatrix createMatrix();
  protected abstract ExpressionMatrix createMatrix(double defaultValue);

  @Test
  public void testAddRow() {

    ExpressionMatrix matrix = createMatrix();

    assertEquals(0, matrix.getRowCount());

    matrix.addRow("row1");
    assertEquals(1, matrix.getRowCount());
    assertTrue(matrix.containsRow("row1"));
    assertFalse(matrix.containsRow("row2"));
    assertEquals(Arrays.asList("row1"), matrix.getRowNames());

    matrix.addRow("row2");
    assertEquals(2, matrix.getRowCount());
    assertTrue(matrix.containsRow("row1"));
    assertTrue(matrix.containsRow("row2"));
    assertFalse(matrix.containsRow("row3"));
    assertEquals(Arrays.asList("row1", "row2"), matrix.getRowNames());

    matrix.addColumn("col1");
    assertEquals(0.0, matrix.getValue("row1", "col1"), 0.0);
    assertEquals(0.0, matrix.getValue("row2", "col1"), 0.0);
  }

  @Test
  public void testAddColumn() {

    ExpressionMatrix matrix = createMatrix();

    assertEquals(0, matrix.getRowCount());

    matrix.addColumn("col1");
    assertEquals(1, matrix.getColumnCount());
    assertTrue(matrix.containsColumn("col1"));
    assertFalse(matrix.containsColumn("col2"));
    assertEquals(Arrays.asList("col1"), matrix.getColumnNames());

    matrix.addColumn("col2");
    assertEquals(2, matrix.getColumnCount());
    assertTrue(matrix.containsColumn("col1"));
    assertTrue(matrix.containsColumn("col2"));
    assertFalse(matrix.containsColumn("col3"));
    assertEquals(Arrays.asList("col1", "col2"), matrix.getColumnNames());

    matrix.addRow("row1");
    assertEquals(0.0, matrix.getValue("row1", "col1"), 0.0);
    assertEquals(0.0, matrix.getValue("row1", "col2"), 0.0);
  }

  @Test
  public void testGetRowValues() {

    ExpressionMatrix matrix = createMatrix();
    matrix.addColumns("col1", "col2");
    matrix.addRows("row1", "row2", "row3");

    matrix.setValue("row1", "col1", 1);
    matrix.setValue("row2", "col1", 2);
    matrix.setValue("row3", "col1", 3);
    matrix.setValue("row1", "col2", 4);
    matrix.setValue("row2", "col2", 5);
    matrix.setValue("row3", "col2", 6);

    assertEquals(Arrays.asList(1.0, 4.0), matrix.getRowValues("row1"));
    assertEquals(Arrays.asList(2.0, 5.0), matrix.getRowValues("row2"));
    assertEquals(Arrays.asList(3.0, 6.0), matrix.getRowValues("row3"));
  }

  @Test
  public void testGetColumnValues() {

    ExpressionMatrix matrix = createMatrix();
    matrix.addColumns("col1", "col2");
    matrix.addRows("row1", "row2", "row3");

    matrix.setValue("row1", "col1", 1);
    matrix.setValue("row2", "col1", 2);
    matrix.setValue("row3", "col1", 3);
    matrix.setValue("row1", "col2", 4);
    matrix.setValue("row2", "col2", 5);
    matrix.setValue("row3", "col2", 6);

    assertEquals(Arrays.asList(1.0, 2.0, 3.0), matrix.getColumnValues("col1"));
    assertEquals(Arrays.asList(4.0, 5.0, 6.0), matrix.getColumnValues("col2"));
  }

  @Test
  public void testGetValue() {

    ExpressionMatrix matrix = createMatrix();
    matrix.addColumns("col1", "col2");
    matrix.addRows("row1", "row2", "row3");

    matrix.setValue("row1", "col1", 1);
    matrix.setValue("row2", "col1", 2);
    matrix.setValue("row3", "col1", 3);
    matrix.setValue("row1", "col2", 4);
    matrix.setValue("row2", "col2", 5);
    matrix.setValue("row3", "col2", 6);

    assertEquals(1.0, matrix.getValue("row1", "col1"), 0.0);
    assertEquals(2.0, matrix.getValue("row2", "col1"), 0.0);
    assertEquals(3.0, matrix.getValue("row3", "col1"), 0.0);
    assertEquals(4.0, matrix.getValue("row1", "col2"), 0.0);
    assertEquals(5.0, matrix.getValue("row2", "col2"), 0.0);
    assertEquals(6.0, matrix.getValue("row3", "col2"), 0.0);

    try {
      matrix.getValue("row4", "col1");
      assertTrue(false);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }

    try {
      matrix.getValue("row1", "col4");
      assertTrue(false);
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }

  }

  @Test
  public void testAdd() {

    ExpressionMatrix matrix1 = createMatrix();
    matrix1.addColumns("col1", "col2");
    matrix1.addRows("row1", "row2", "row3");

    matrix1.setValue("row1", "col1", 1);
    matrix1.setValue("row2", "col1", 2);
    matrix1.setValue("row3", "col1", 3);
    matrix1.setValue("row1", "col2", 4);
    matrix1.setValue("row2", "col2", 5);
    matrix1.setValue("row3", "col2", 6);

    ExpressionMatrix matrix2 = createMatrix();
    matrix2.addColumns("col2", "col3");
    matrix2.addRows("row2", "row3", "row4");

    matrix2.setValue("row2", "col2", 7);
    matrix2.setValue("row3", "col2", 8);
    matrix2.setValue("row4", "col2", 9);
    matrix2.setValue("row2", "col3", 10);
    matrix2.setValue("row3", "col3", 11);
    matrix2.setValue("row4", "col3", 12);

    matrix1.add(matrix2);

    assertEquals(1.0, matrix1.getValue("row1", "col1"), 0.0);
    assertEquals(2.0, matrix1.getValue("row2", "col1"), 0.0);
    assertEquals(3.0, matrix1.getValue("row3", "col1"), 0.0);
    assertEquals(0.0, matrix1.getValue("row4", "col1"), 0.0);
    assertEquals(4.0, matrix1.getValue("row1", "col2"), 0.0);
    assertEquals(7.0, matrix1.getValue("row2", "col2"), 0.0);
    assertEquals(8.0, matrix1.getValue("row3", "col2"), 0.0);
    assertEquals(9.0, matrix1.getValue("row4", "col2"), 0.0);
    assertEquals(0.0, matrix1.getValue("row1", "col3"), 0.0);
    assertEquals(10.0, matrix1.getValue("row2", "col3"), 0.0);
    assertEquals(11.0, matrix1.getValue("row3", "col3"), 0.0);
    assertEquals(12.0, matrix1.getValue("row4", "col3"), 0.0);
  }

  @Test
  public void testRenameColumn() {

    ExpressionMatrix matrix = createMatrix();

    assertEquals(0, matrix.getRowCount());

    matrix.addColumns("col1", "col2", "col3");
    assertEquals(3, matrix.getColumnCount());
    assertTrue(matrix.containsColumn("col2"));

    matrix.renameColumn("col2", "newcol2");
    assertEquals(3, matrix.getColumnCount());
    assertTrue(matrix.containsColumn("newcol2"));
    assertFalse(matrix.containsColumn("col2"));

    matrix.addRows("row1", "row2");

    matrix.setValue("row1", "col1", 1);
    matrix.setValue("row2", "col1", 2);
    matrix.setValue("row1", "newcol2", 3);
    matrix.setValue("row2", "newcol2", 4);
    matrix.setValue("row1", "col3", 5);
    matrix.setValue("row2", "col3", 6);

    assertEquals(3.0, matrix.getValue("row1", "newcol2"), 0.0);
    assertEquals(4.0, matrix.getValue("row2", "newcol2"), 0.0);
    assertEquals(5.0, matrix.getValue("row1", "col3"), 0.0);
    assertEquals(6.0, matrix.getValue("row2", "col3"), 0.0);

    matrix.renameColumn("col3", "newcol3");
    assertEquals(3, matrix.getColumnCount());
    assertFalse(matrix.containsColumn("col3"));
    assertTrue(matrix.containsColumn("newcol3"));

    assertEquals(3.0, matrix.getValue("row1", "newcol2"), 0.0);
    assertEquals(4.0, matrix.getValue("row2", "newcol2"), 0.0);
    assertEquals(5.0, matrix.getValue("row1", "newcol3"), 0.0);
    assertEquals(6.0, matrix.getValue("row2", "newcol3"), 0.0);
  }

  @Test
  public void testSetDefaultValue() {

    ExpressionMatrix matrix = createMatrix();

    assertEquals(0.0, matrix.getDefaultValue(), 0.0);

    matrix.addColumns("col1", "col2");
    matrix.addRows("row1", "row2");

    assertEquals(0.0, matrix.getValue("row1", "col1"), 0.0);
    assertEquals(0.0, matrix.getValue("row2", "col1"), 0.0);
    assertEquals(0.0, matrix.getValue("row1", "col2"), 0.0);
    assertEquals(0.0, matrix.getValue("row2", "col2"), 0.0);

    matrix = createMatrix(1.0);

    assertEquals(0.0, matrix.getDefaultValue(), 1.0);

    matrix.addColumns("col1", "col2");
    matrix.addRows("row1", "row2");

    assertEquals(1.0, matrix.getValue("row1", "col1"), 0.0);
    assertEquals(1.0, matrix.getValue("row2", "col1"), 0.0);
    assertEquals(1.0, matrix.getValue("row1", "col2"), 0.0);
    assertEquals(1.0, matrix.getValue("row2", "col2"), 0.0);

    matrix = createMatrix(Double.NaN);

    assertEquals(Double.NaN, matrix.getDefaultValue(), 0.0);

    matrix.addColumns("col1", "col2");
    matrix.addRows("row1", "row2");

    assertEquals(Double.NaN, matrix.getValue("row1", "col1"), 0.0);
    assertEquals(Double.NaN, matrix.getValue("row2", "col1"), 0.0);
    assertEquals(Double.NaN, matrix.getValue("row1", "col2"), 0.0);
    assertEquals(Double.NaN, matrix.getValue("row2", "col2"), 0.0);
  }

  @Test
  public void testRemoveRow() {

    ExpressionMatrix matrix = createMatrix();
    matrix.addColumns("col1", "col2");
    matrix.addRows("row1", "row2", "row3");

    matrix.setValue("row1", "col1", 1);
    matrix.setValue("row2", "col1", 2);
    matrix.setValue("row3", "col1", 3);
    matrix.setValue("row1", "col2", 4);
    matrix.setValue("row2", "col2", 5);
    matrix.setValue("row3", "col2", 6);

    assertEquals(3, matrix.getRowCount());
    assertEquals(2, matrix.getColumnCount());

    matrix.removeRow("row2");

    assertEquals(2, matrix.getRowCount());
    assertEquals(2, matrix.getColumnCount());

    assertEquals(1.0, matrix.getValue("row1", "col1"), 0.0);
    assertEquals(3.0, matrix.getValue("row3", "col1"), 0.0);
    assertEquals(4.0, matrix.getValue("row1", "col2"), 0.0);
    assertEquals(6.0, matrix.getValue("row3", "col2"), 0.0);
  }

  @Test
  public void testRemoveColumn() {

    ExpressionMatrix matrix = createMatrix();
    matrix.addColumns("col1", "col2");
    matrix.addRows("row1", "row2", "row3");

    matrix.setValue("row1", "col1", 1);
    matrix.setValue("row2", "col1", 2);
    matrix.setValue("row3", "col1", 3);
    matrix.setValue("row1", "col2", 4);
    matrix.setValue("row2", "col2", 5);
    matrix.setValue("row3", "col2", 6);

    assertEquals(3, matrix.getRowCount());
    assertEquals(2, matrix.getColumnCount());

    matrix.removeColumn("col2");

    assertEquals(3, matrix.getRowCount());
    assertEquals(1, matrix.getColumnCount());

    assertEquals(1.0, matrix.getValue("row1", "col1"), 0.0);
    assertEquals(2.0, matrix.getValue("row2", "col1"), 0.0);
    assertEquals(3.0, matrix.getValue("row3", "col1"), 0.0);
  }

}
