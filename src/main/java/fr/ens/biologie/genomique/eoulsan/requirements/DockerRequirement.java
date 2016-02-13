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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.requirements;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.ProgressDetail;
import com.spotify.docker.client.messages.ProgressMessage;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Progress;
import fr.ens.biologie.genomique.eoulsan.util.docker.DockerManager;

/**
 * This class define a Docker requirement.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerRequirement extends AbstractRequirement {

  public static final String REQUIREMENT_NAME = "docker";

  private static final String DOCKER_IMAGE_PARAMETER = "docker.image";

  private String dockerImage;

  @Override
  public String getName() {

    return REQUIREMENT_NAME;
  }

  @Override
  public Set<Parameter> getParameters() {

    final Set<Parameter> result = super.getParameters();

    result.add(new Parameter(DOCKER_IMAGE_PARAMETER, this.dockerImage));

    return Collections.unmodifiableSet(result);
  }

  @Override
  public void configure(final Set<Parameter> parameters)
      throws EoulsanException {

    for (Parameter p : parameters) {

      switch (p.getName()) {

      case DOCKER_IMAGE_PARAMETER:
        this.dockerImage = p.getValue();
        break;

      default:
        super.configure(Collections.singleton(p));
        break;
      }

    }
  }

  @Override
  public boolean isAvailable() {

    final DockerClient dockerClient = DockerManager.getInstance().getClient();

    if (dockerClient == null) {
      return false;
    }

    try {
      List<Image> images = dockerClient.listImages();

      for (Image image : images) {
        for (String tag : image.repoTags()) {
          if (this.dockerImage.equals(tag)) {
            return true;
          }
        }
      }

    } catch (DockerException | InterruptedException e) {

      return false;
    }

    return false;
  }

  @Override
  public void install(final Progress progress) throws EoulsanException {

    final DockerClient dockerClient = DockerManager.getInstance().getClient();

    try {

      dockerClient.pull(this.dockerImage, new ProgressHandler() {

        private Set<String> imageIds = new HashSet<>();

        @Override
        public void progress(final ProgressMessage msg) throws DockerException {

          final ProgressDetail pg = msg.progressDetail();

          if (!this.imageIds.contains(msg.id())) {
            this.imageIds.add(msg.id());
          }

          final int imageCount = this.imageIds.size();

          final double currentImageProgress;
          if (pg == null || pg.total() == 0) {
            currentImageProgress = 0;
          } else {
            currentImageProgress = (double) pg.current() / pg.total();
          }

          final double pullProgress =
              (currentImageProgress + imageCount - 1.0) / (double) imageCount;

          progress.setProgress(pullProgress);

        }
      });
    } catch (DockerException | InterruptedException e) {

      throw new EoulsanException(e);
    }

    if (!isAvailable()) {
      throw new EoulsanException(
          "Unable to to pull Docker image: " + this.dockerImage);
    }

  }

  //
  // Static methods
  //

  /**
   * Create a new docker image as mandatory requirement.
   * @param dockerImage the docker image name.
   * @throws EoulsanException if an error occurs while configuring the
   *           requirement
   */
  public static Requirement newDockerRequirement(final String dockerImage)
      throws EoulsanException {

    return newDockerRequirement(dockerImage, false);
  }

  /**
   * Create a new docker image requirement.
   * @param dockerImage the docker image
   * @param optional true if the docker image is a mandatory requirement
   * @throws EoulsanException if an error occurs while configuring the
   *           requirement
   */
  public static Requirement newDockerRequirement(final String dockerImage,
      final boolean optional) throws EoulsanException {

    checkNotNull(dockerImage, "dockerImage argument cannot be null");
    checkArgument(!dockerImage.trim().isEmpty(),
        "dockerImage argument cannot be empty");

    final Requirement result = new DockerRequirement();

    final Set<Parameter> parameters = new HashSet<Parameter>();

    parameters.add(new Parameter(OPTIONAL_PARAMETER, "" + optional));
    parameters.add(new Parameter(INSTALLABLE_PARAMETER, "" + true));
    parameters.add(new Parameter(DOCKER_IMAGE_PARAMETER, dockerImage.trim()));

    result.configure(parameters);

    return result;
  }

}
