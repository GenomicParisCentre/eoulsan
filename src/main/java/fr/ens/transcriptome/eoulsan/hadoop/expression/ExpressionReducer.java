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

package fr.ens.transcriptome.eoulsan.hadoop.expression;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import fr.ens.transcriptome.eoulsan.hadoop.Parameter;
import fr.ens.transcriptome.eoulsan.hadoop.expression.ExonFinder.ExonsParentRange;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

@SuppressWarnings("deprecation")
public class ExpressionReducer implements Reducer<Text, Text, Text, Text> {

  private final ExonFinder ef = new ExonFinder();
  private final GeneExpression gene = new GeneExpression();
  private final String[] fields = new String[9];
  private final Text outputValue = new Text();
  private String parentType;

  @Override
  public void reduce(final Text key, Iterator<Text> values,
      final OutputCollector<Text, Text> collector, final Reporter reporter)
      throws IOException {

    gene.clear();
    

    
    reporter.incrCounter("Expression", "parent", 1);
    final String parentId = key.toString();

    boolean first = true;
    String chr = null;

    int count = 0;

    while (values.hasNext()) {

      count++;
      StringUtils.fastSplit(values.next().toString(), this.fields);

      final String exonChr = this.fields[0];
      final int exonStart = Integer.parseInt(this.fields[1]);
      final int exonEnd = Integer.parseInt(this.fields[2]);
      // codingStrand = Boolean.parseBoolean(this.fields[3]);

      final String alignementChr = this.fields[6];
      final int alignmentStart = Integer.parseInt(this.fields[7]);
      final int alignementEnd = Integer.parseInt(this.fields[8]);

      if (first) {
        chr = exonChr;
        first = false;
      }

      if (!exonChr.equals(alignementChr) || !chr.equals(alignementChr)) {
        reporter.incrCounter("expression", "invalid chromosome", 1);
        continue;
      }

      gene.addAlignement(exonStart, exonEnd, alignmentStart, alignementEnd);
    }

    if (count == 0)
      return;

    final ExonsParentRange range = ef.getExonsParentRange(parentId);

    final String result =
        this.parentType
            + "\t" + chr + "\t" + range.getStart() + "\t" + range.getStop()
            + "\t" + range.getStrand() + "\t" + gene.getLength() + "\t"
            + gene.isCompletlyCovered() + "\t" + gene.getNotCovered() + "\t"
            + gene.getAlignementCount();

    this.outputValue.set(result);

    collector.collect(key, this.outputValue);
  }

  @Override
  public void configure(final JobConf conf) {

    try {

      final Path indexPath =
          new Path(Parameter.getStringParameter(conf,
              ".expression.exonsindex.path", ""));
      File indexFile = FileUtils.createFileInTempDir(indexPath.getName());
      PathUtils.copyFromPathToLocalFile(indexPath, indexFile, conf);
      ef.load(indexFile);
      indexFile.delete();

      this.parentType =
          Parameter.getStringParameter(conf, ".expression.parent.type", "");

    } catch (IOException e) {
      System.out.println(e);
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Override
  public void close() throws IOException {
    // TODO Auto-generated method stub

  }

}
