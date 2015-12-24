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

package fr.ens.transcriptome.eoulsan.design;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignMetadata;

public class DesignMetadataTest {

  @Test
  public void test() {

    Design d = new Design();

    DesignMetadata dm = d.getMetadata();

    // test getGenomeFile
    assertNull(dm.getGenomeFile());
    // test setGenomeFile
    dm.setGenomeFile("/home/toto/titi.fasta");
    assertEquals("/home/toto/titi.fasta", dm.getGenomeFile());
    // test containsGenomeFile
    assertTrue(dm.containsGenomeFile());

    // test getGffFile
    assertNull(dm.getGffFile());
    // test setGffFile
    dm.setGffFile("/home/toto/titi.gff");
    assertEquals("/home/toto/titi.gff", dm.getGffFile());
    // test containsGffFile
    assertTrue(dm.containsGffFile());

    // test getAdditionnalAnnotationFile
    assertNull(dm.getAdditionnalAnnotationFile());
    // test setAdditionnalAnnotationFile
    dm.setAdditionnalAnnotationFile("/home/toto/titi.txt");
    assertEquals("/home/toto/titi.txt", dm.getAdditionnalAnnotationFile());
    // test containsAdditionnalAnnotationFile
    assertTrue(dm.containsAdditionnalAnnotationFile());

  }

}
