package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.Image;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;

/**
 * This class define a Docker client using the Spotify Docker client library.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SpotifyDockerClient implements DockerClient {

  private DefaultDockerClient client;

  @Override
  public void initialize(URI dockerConnectionURI) throws IOException {

    synchronized (this) {

      if (this.client != null) {
        return;
      }

      final URI dockerConnection =
          EoulsanRuntime.getSettings().getDockerConnectionURI();

      if (dockerConnection == null) {
        throw new IOException("Docker connection URI is not set. "
            + "Please set the \"main.docker.uri\" global parameter");
      }

      this.client = new DefaultDockerClient(dockerConnection);

      if (this.client == null) {
        throw new IOException("Unable to connect to Docker deamon: "
            + EoulsanRuntime.getSettings().getDockerConnection());
      }
    }
  }

  @Override
  public DockerImageInstance createConnection(String dockerImage) {

    return new SportifyDockerImageInstance(this.client, dockerImage);
  }

  @Override
  public void close() {

    synchronized (this) {

      if (client != null) {
        client.close();
        this.close();
      }
    }
  }

  @Override
  public Set<String> listImageTags() throws IOException {

    final Set<String> result = new HashSet<>();

    try {
      List<Image> images = this.client.listImages();

      if (images != null) {
        for (Image image : images) {
          if (image != null) {
            for (String tag : image.repoTags()) {
              if (tag != null) {
                result.add(tag);
              }
            }
          }
        }
      }
    } catch (DockerException | InterruptedException e) {
      throw new IOException(e);
    }

    return result;
  }

}
