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

package fr.ens.transcriptome.eoulsan.design2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExperimentMetadataTest {

  @Test
  public void test() {
    Design d = new Design();
    d.addExperiment("1");
    ExperimentMetadata em = d.getExperiment("1").getMetadata();

    // test getSkip
    assertNull(em.getSkip());
    // test setSkip
    em.setSkip("toto");
    assertEquals("toto", em.getSkip());
    // test containsSkip
    assertTrue(em.containsSkip());

    // test getReference
    assertNull(em.getReference());
    // test setReference
    em.setReference("toto");
    assertEquals("toto", em.getReference());
    // test containsReference
    assertTrue(em.containsReference());

    // test getModel
    assertNull(em.getModel());
    // test setModel
    em.setModel("toto");
    assertEquals("toto", em.getModel());
    // test containsModel
    assertTrue(em.containsModel());

    // test getContrast
    assertNull(em.getContrast());
    // test setContrast
    em.setContrast("toto");
    assertEquals("toto", em.getContrast());
    // test containsContrast
    assertTrue(em.containsContrast());

    // test getBuildContrast
    assertNull(em.getBuildContrast());
    // test setBuildContrast
    em.setBuildContrast("toto");
    assertEquals("toto", em.getBuildContrast());
    // test containsBuildContrast
    assertTrue(em.containsBuildContrast());

    // test getDesignFile
    assertNull(em.getDesignFile());
    // test setDesignFile
    em.setDesignFile("toto");
    assertEquals("toto", em.getDesignFile());
    // test containsDesignFile
    assertTrue(em.containsDesignFile());

    // test getComparisonFile
    assertNull(em.getComparisonFile());
    // test setComparisonFile
    em.setComparisonFile("toto");
    assertEquals("toto", em.getComparisonFile());
    // test containsComparisonFile
    assertTrue(em.containsComparisonFile());

    // test getContrastFile
    assertNull(em.getContrastFile());
    // test setContrastFile
    em.setContrastFile("toto");
    assertEquals("toto", em.getContrastFile());
    // test containsContrastFile
    assertTrue(em.containsContrastFile());

  }

}
