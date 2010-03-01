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

package fr.ens.transcriptome.eoulsan.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.IOUtils;

import fr.ens.transcriptome.eoulsan.Globals;

public final class PathUtils {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  /**
   * Simple PathFilter to filter Paths with their suffix
   * @author Laurent Jourdren
   */
  public static final class SuffixPathFilter implements PathFilter {

    private String suffix;
    private boolean allowCompressedFile;

    @Override
    public boolean accept(final Path path) {

      if (path == null)
        return false;

      final String myName;

      if (this.allowCompressedFile)
        myName =
            StringUtils.removeCompressedExtensionFromFilename(path.getName());
      else
        myName = path.getName();

      if (myName.endsWith(this.suffix))
        return true;

      return false;
    }

    //
    // Constructor
    //

    /**
     * Public constructor.
     * @parm extension extension to use by ExtensionPathFilter
     * @param allowCompressedFile allow files with a compressed extension
     */
    public SuffixPathFilter(final String suffix) {

      this(suffix, false);
    }

    /**
     * Public constructor.
     * @parm extension extension to use by ExtensionPathFilter
     * @param allowCompressedFile allow files with a compressed extension
     */
    public SuffixPathFilter(final String suffix,
        final boolean allowCompressedFile) {

      if (suffix == null)
        throw new NullPointerException("The suffix is null");

      this.suffix = suffix;
      this.allowCompressedFile = allowCompressedFile;
    }

  };

  /**
   * Simple PathFilter to filter Paths with their beginning
   * @author Laurent Jourdren
   */
  public static final class PrefixPathFilter implements PathFilter {

    private String prefix;
    private boolean allowCompressedFile;

    @Override
    public boolean accept(final Path path) {

      if (path == null)
        return false;

      final String myName;

      if (this.allowCompressedFile)
        myName =
            StringUtils.removeCompressedExtensionFromFilename(path.getName());
      else
        myName = path.getName();

      if (myName.startsWith(this.prefix))
        return true;

      return false;
    }

    //
    // Constructor
    //

    /**
     * Public constructor.
     * @param prefix extension to use by ExtensionPathFilter
     */
    public PrefixPathFilter(final String prefix) {

      this(prefix, false);
    }

    /**
     * Public constructor.
     * @param prefix extension to use by ExtensionPathFilter
     * @param allowCompressedFile allow files with a compressed extension
     */
    public PrefixPathFilter(final String prefix,
        final boolean allowCompressedFile) {

      if (prefix == null)
        throw new NullPointerException("The prefix is null");

      this.prefix = prefix;
      this.allowCompressedFile = allowCompressedFile;
    }

  };

  /**
   * Copy a file from a path to a local file. Don't remove original file.
   * @param srcPath Path of the file to copy
   * @param destFile Destination file
   * @param conf Configuration object * @return true if the copy is successful
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyFromPathToLocalFile(final Path srcPath,
      final File destFile, final Configuration conf) throws IOException {

    return copyFromPathToLocalFile(srcPath, destFile, false, conf);
  }

  /**
   * Copy a file from a path to a local file
   * @param srcPath Path of the file to copy
   * @param destFile Destination file
   * @param removeOriginalFile true if the original file must be deleted
   * @param conf Configuration object
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyFromPathToLocalFile(final Path srcPath,
      final File destFile, final boolean removeOriginalFile,
      final Configuration conf) throws IOException {

    if (srcPath == null)
      throw new NullPointerException("The source path is null");
    if (destFile == null)
      throw new NullPointerException("The destination file is null");
    if (conf == null)
      throw new NullPointerException("The configuration object is null");

    final FileSystem fs = FileSystem.get(srcPath.toUri(), conf);
    return FileUtil.copy(fs, srcPath, destFile, removeOriginalFile, conf);
  }

  /**
   * Copy a local file to a path
   * @param srcFile source file
   * @param destPath destination path
   * @param removeSrcFile true if the source file must be removed
   * @param conf Configuration object
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyLocalFileToPath(final File srcFile,
      final Path destPath, final Configuration conf) throws IOException {

    return copyLocalFileToPath(srcFile, destPath, false, conf);
  }

  /**
   * Copy a local file to a path
   * @param srcFile source file
   * @param destPath destination path
   * @param removeSrcFile true if the source file must be removed
   * @param conf Configuration object
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyLocalFileToPath(final File srcFile,
      final Path destPath, final boolean removeSrcFile, final Configuration conf)
      throws IOException {

    if (srcFile == null)
      throw new NullPointerException("The source file is null");
    if (destPath == null)
      throw new NullPointerException("The destination path is null");
    if (conf == null)
      throw new NullPointerException("The configuration object is null");

    return FileUtil.copy(srcFile, FileSystem.get(destPath.toUri(), conf),
        destPath, removeSrcFile, conf);
  }

  /**
   * Unzip a zip file on local file system. Don't remove original zip file.
   * @param path Path of the zip file
   * @param outputDir Output directory of the content of the zip file
   * @param removeOriginalZipFile true if the original zip file must be removed
   * @param conf Configuration object
   * @throws IOException if an error occurs while unzipping the file
   */
  public static void unZipPathToLocalFile(final Path path,
      final File outputDir, final Configuration conf) throws IOException {

    unZipPathToLocalFile(path, outputDir, false, conf);
  }

  /**
   * Unzip a zip file on local file system.
   * @param srcPath Path of the zip file
   * @param outputDir Output directory of the content of the zip file
   * @param removeOriginalZipFile true if the original zip file must be removed
   * @param conf Configuration object
   * @throws IOException if an error occurs while unzipping the file
   */
  public static void unZipPathToLocalFile(final Path srcPath,
      final File outputDir, final boolean removeOriginalZipFile,
      final Configuration conf) throws IOException {

    if (srcPath == null)
      throw new NullPointerException("The source path is null");
    if (outputDir == null)
      throw new NullPointerException("The destination directory file is null");
    if (conf == null)
      throw new NullPointerException("The configuration object is null");

    final File tmpZipFile = FileUtils.createTempFile("", ".zip");
    System.out.println("srcPath: " + srcPath);
    System.out.println("tmpZipFile: " + tmpZipFile.getAbsolutePath());
    System.out.println("removeOriginalZipFile: " + removeOriginalZipFile);

    copyFromPathToLocalFile(srcPath, tmpZipFile, removeOriginalZipFile, conf);

    System.out.println("outputDir: " + outputDir);
    System.out.println("outputDir exists, dir: "
        + outputDir.exists() + ", " + outputDir.isDirectory());
    // if (!outputDir.mkdirs())
    // throw new IOException("Unable to create output directory: "
    // + outputDir.getAbsolutePath());
    // FileUtil.unZip(tmpZipFile, outputDir);
    FileUtils.unzip(tmpZipFile, outputDir);

    if (!tmpZipFile.delete())
      throw new IOException("Can't remove temporary zip file: "
          + tmpZipFile.getAbsolutePath());
  }

  /**
   * Get the FileSystem of a Path
   * @param path Path to get FileSystem
   * @param conf Configuration Object
   * @return a FileSystem Object
   * @throws IOException if an error occurs while getting the FileSystem object
   */
  public static FileSystem getFileSystem(final Path path,
      final Configuration conf) throws IOException {

    if (path == null)
      throw new NullPointerException("Path is null");
    if (conf == null)
      throw new NullPointerException("The configuration object is null");

    return FileSystem.get(path.toUri(), conf);
  }

  /**
   * Fully delete a file of the content of a directory
   * @param path Path of the file
   * @param conf Configuration Object
   * @return true if the Path is succelly
   * @throws IOException
   */
  public static boolean fullyDelete(final Path path, final Configuration conf)
      throws IOException {

    if (path == null)
      throw new NullPointerException("Path to delete is null");
    if (conf == null)
      throw new NullPointerException("The configuration object is null");

    final FileSystem fs = getFileSystem(path, conf);

    if (fs == null)
      throw new IOException("Unable to delete path, The FileSystem is null");

    return fs.delete(path, true);
  }

  /**
   * Merge several file of a directory into one file.
   * @param srcPath source directory path
   * @param destPath destination path
   * @param deleteSource delete source files
   * @param conf Configuration object
   * @throws IOException if an error occurs while merging files
   */
  public static void copyMerge(final Path srcPath, final Path destPath,
      final Configuration conf) throws IOException {

    copyMerge(srcPath, destPath, false, conf, null);
  }

  /**
   * Merge several file of a directory into one file.
   * @param srcPath source directory path
   * @param destPath destination path
   * @param deleteSource delete source files
   * @param conf Configuration object
   * @throws IOException if an error occurs while merging files
   */
  public static void copyMerge(final Path srcPath, final Path destPath,
      final boolean deleteSource, final Configuration conf) throws IOException {

    copyMerge(srcPath, destPath, deleteSource, conf, null);
  }

  /**
   * Merge several file of a directory into one file.
   * @param srcPath source directory path
   * @param destPath destination path
   * @param deleteSource delete source files
   * @param conf Configuration object
   * @param addString string to add
   * @throws IOException if an error occurs while merging files
   */
  public static void copyMerge(final Path srcPath, final Path destPath,
      final boolean deleteSource, final Configuration conf,
      final String addString) throws IOException {

    final FileSystem srcFs = getFileSystem(srcPath, conf);
    final FileSystem destFs = getFileSystem(destPath, conf);

    FileUtil.copyMerge(srcFs, srcPath, destFs, destPath, deleteSource, conf,
        addString);

  }

  /**
   * Create a new path with the same parent directory and basename but without
   * another extension.
   * @param path base path to use
   * @param extension extension to add
   * @return a new Path object
   */
  public static Path newPathWithOtherExtension(final Path path,
      final String extension) {

    if (path == null)
      throw new NullPointerException("Path is null");

    if (extension == null)
      throw new NullPointerException("Extension is null");

    return new Path(path.getParent(), StringUtils.basename(path.getName())
        + extension);
  }

  /**
   * Return a list of the file of a path
   * @param dir Path of the directory
   * @param prefix filter on suffix
   * @param conf Configuration
   * @return a list of Path
   * @throws IOException if an error occurs while listing the directory
   */
  public static List<Path> listPathsByPrefix(final Path dir,
      final String prefix, final Configuration conf) throws IOException {

    return listPathsByPrefix(dir, prefix, false, conf);
  }

  /**
   * Return a list of the file of a path
   * @param dir Path of the directory
   * @param prefix filter on suffix
   * @param allowCompressedExtension Allow compressed extensions
   * @param conf Configuration
   * @return a list of Path
   * @throws IOException if an error occurs while listing the directory
   */
  public static List<Path> listPathsByPrefix(final Path dir,
      final String prefix, final boolean allowCompressedExtension,
      final Configuration conf) throws IOException {

    if (dir == null)
      throw new NullPointerException("Directory path is null");

    if (prefix == null)
      throw new NullPointerException("Prefix is null");

    if (conf == null)
      throw new NullPointerException("Configuration is null");

    final FileSystem fs = getFileSystem(dir, conf);
    if (!fs.getFileStatus(dir).isDir())
      throw new IOException("Directory path is not a directory: " + dir);

    final FileStatus[] filesStatus =
        fs.listStatus(dir, new PrefixPathFilter(prefix,
            allowCompressedExtension));

    if (filesStatus == null)
      return Collections.emptyList();

    final List<Path> result = new ArrayList<Path>(filesStatus.length);

    for (FileStatus fst : filesStatus)
      result.add(fst.getPath());

    return result;
  }

  /**
   * Return a list of the file of a path
   * @param dir Path of the directory
   * @param suffix filter on suffix
   * @param conf Configuration
   * @return a list of Path
   * @throws IOException if an error occurs while listing the directory
   */
  public static List<Path> listPathsBySuffix(final Path dir,
      final String suffix, final Configuration conf) throws IOException {

    return listPathsBySuffix(dir, suffix, false, conf);
  }

  /**
   * Return a list of the file of a path
   * @param dir Path of the directory
   * @param suffix filter on suffix
   * @param allowCompressedExtension Allow compressed extensions
   * @param conf Configuration
   * @return a list of Path
   * @throws IOException if an error occurs while listing the directory
   */
  public static List<Path> listPathsBySuffix(final Path dir,
      final String suffix, final boolean allowCompressedExtension,
      final Configuration conf) throws IOException {

    if (dir == null)
      throw new NullPointerException("Directory path is null");

    if (suffix == null)
      throw new NullPointerException("Suffix is null");

    if (conf == null)
      throw new NullPointerException("Configuration is null");

    final FileSystem fs = getFileSystem(dir, conf);
    if (!fs.getFileStatus(dir).isDir())
      throw new IOException("Directory path is not a directory: " + dir);

    final FileStatus[] filesStatus =
        fs.listStatus(dir, new SuffixPathFilter(suffix,
            allowCompressedExtension));

    if (filesStatus == null)
      return Collections.emptyList();

    final List<Path> result = new ArrayList<Path>(filesStatus.length);

    for (FileStatus fst : filesStatus)
      result.add(fst.getPath());

    return result;
  }

  /**
   * Create a new temporary path. Nothing is created on the file system.
   * @param directory parent directory of the temporary file to create
   * @param prefix Prefix of the temporary file
   * @param suffix suffix of the temporary file
   * @return the new temporary file
   * @throws IOException if there is an error creating the temporary directory
   */
  public static Path createTempPath(final Path directory, final String prefix,
      final String suffix, final Configuration conf) throws IOException {

    final Path myDir;
    final String myPrefix;
    final String mySuffix;

    if (directory == null)
      throw new NullPointerException("Directory is null");

    myDir = directory;

    if (prefix == null)
      myPrefix = "";
    else
      myPrefix = prefix;

    if (suffix == null)
      mySuffix = "";
    else
      mySuffix = suffix;

    final FileSystem fs = getFileSystem(directory, conf);
    Path tempFile;

    final int maxAttempts = 9;
    int attemptCount = 0;
    do {
      attemptCount++;
      if (attemptCount > maxAttempts)

        throw new IOException("The highly improbable has occurred! Failed to "
            + "create a unique temporary directory after " + maxAttempts
            + " attempts.");

      final String filename =
          myPrefix + UUID.randomUUID().toString() + mySuffix;
      tempFile = new Path(myDir, filename);
    } while (fs.isFile(tempFile));

    return tempFile;
  }

  /**
   * Copy all files in a directory to one output file (merge).
   * @param paths list of path files to concat
   * @param dstPath destination path
   * @param conf Configuration
   */
  public static boolean concat(final List<Path> paths, final Path dstPath,
      final Configuration conf) throws IOException {

    return concat(paths, dstPath, false, true, null);
  }

  /**
   * Copy all files in a directory to one output file (merge).
   * @param paths list of path files to concat
   * @param dstPath destination path
   * @param deleteSource true if the original files must be deleted
   * @param overwrite true if an existing destination file must be deleted
   * @param conf Configuration
   */
  public static boolean concat(final List<Path> paths, final Path dstPath,
      final boolean deleteSource, final boolean overwrite,
      final Configuration conf) throws IOException {

    return concat(paths, dstPath, deleteSource, overwrite, conf, null);
  }

  /**
   * Copy all files in a directory to one output file (merge).
   * @param paths list of path files to concat
   * @param dstPath destination path
   * @param deleteSource true if the original files must be deleted
   * @param overwrite true if an existing destination file must be deleted
   * @param conf Configuration
   * @param addString string to add
   */
  public static boolean concat(final List<Path> paths, final Path dstPath,
      final boolean deleteSource, final boolean overwrite,
      final Configuration conf, final String addString) throws IOException {

    if (paths == null)
      throw new NullPointerException("The list of path to concat is null");

    if (paths.size() == 0)
      return false;

    final FileSystem srcFs = getFileSystem(paths.get(0), conf);
    final FileSystem dstFs = getFileSystem(dstPath, conf);

    if (!overwrite && dstFs.exists(dstPath))
      throw new IOException("The output file already exists: " + dstPath);

    final OutputStream out = dstFs.create(dstPath);

    try {
      // FileStatus contents[] = srcFS.listStatus(srcDir);
      // for (int i = 0; i < contents.length; i++) {
      for (Path p : paths)
        if (!srcFs.getFileStatus(p).isDir()) {
          InputStream in = srcFs.open(p);
          try {
            IOUtils.copyBytes(in, out, conf, false);
            if (addString != null)
              out.write(addString.getBytes("UTF-8"));

          } finally {
            in.close();
          }
        }

    } finally {
      out.close();
    }

    if (deleteSource)
      for (Path p : paths)
        if (!srcFs.delete(p, false))
          return false;

    return true;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private PathUtils() {
  }

}
