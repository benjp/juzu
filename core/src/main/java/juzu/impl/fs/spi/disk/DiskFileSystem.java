/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package juzu.impl.fs.spi.disk;

import juzu.impl.common.QN;
import juzu.impl.common.Timestamped;
import juzu.impl.fs.spi.PathType;
import juzu.impl.fs.spi.ReadWriteFileSystem;
import juzu.impl.common.Content;
import juzu.impl.common.Tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class DiskFileSystem extends ReadWriteFileSystem<File> {

  /** . */
  private final File root;

  /** . */
  private FilenameFilter filter;

  /** . */
  private Charset encoding;

  public DiskFileSystem(File root) {
    this(root, new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return true;
      }
    });
  }

  public DiskFileSystem(File root, FilenameFilter filter) {
    this.root = root;
    this.filter = filter;
    this.encoding = Charset.defaultCharset();
  }

  public DiskFileSystem(final File root, final QN path) {
    this(root, new FilterImpl(root, path));
  }

  public DiskFileSystem(final File root, final String path) {
    this(root, new FilterImpl(root, QN.parse(path)));
  }

  public void applyFilter(String path) {
    filter = new FilterImpl(root, QN.parse(path));
  }

  @Override
  public Class<File> getType() {
    return File.class;
  }

  @Override
  public String getDescription() {
    return "disk[" + root.getAbsolutePath() + "]";
  }

  @Override
  public boolean equals(File left, File right) {
    return left.equals(right);
  }

  @Override
  public File getRoot() throws IOException {
    return root;
  }

  @Override
  public PathType typeOf(File path) throws IOException {
    if (path.isDirectory()) {
      return PathType.DIR;
    } else if (path.isFile()) {
      return PathType.FILE;
    } else {
      return null;
    }
  }

  @Override
  public String getName(File path) throws IOException {
    if (path.equals(root)) {
      return "";
    }
    else {
      return path.getName();
    }
  }

  @Override
  public Iterator<File> getChildren(File dir) throws IOException {
    File[] children = dir.listFiles(filter);
    return children == null ? Tools.<File>emptyIterator() : Arrays.asList(children).iterator();
  }

  @Override
  public File getChild(File dir, String name) throws IOException {
    if (filter.accept(dir, name)) {
      File child = new File(dir, name);
      if (child.exists()) {
        return child;
      }
    }
    return null;
  }

  @Override
  public Timestamped<Content> getContent(File file) throws IOException {
    FileInputStream in = new FileInputStream(file);
    try {
      ByteArrayOutputStream content = new ByteArrayOutputStream();
      byte[] buffer = new byte[256];
      for (int l = in.read(buffer);l != -1;l = in.read(buffer)) {
        content.write(buffer, 0, l);
      }
      return new Timestamped<Content>(file.lastModified(), new Content(content.toByteArray(), encoding));
    }
    finally {
      Tools.safeClose(in);
    }
  }

  @Override
  public long getLastModified(File path) throws IOException {
    return path.lastModified();
  }

  @Override
  public URL getURL(File path) throws IOException {
    return path.toURI().toURL();
  }

  @Override
  public File getFile(File path) {
    return path;
  }

  @Override
  public File makePath(File parent, String name) throws IOException {
    return new File(parent, name);
  }

  @Override
  public void createDir(File dir) throws IOException {
    if (dir.exists()) {
      if (dir.isDirectory()) {
        // Ok
      } else {
        throw new IOException("Dir already exists and is a file " + dir.getCanonicalPath());
      }
    } else {
      if (dir.mkdirs()) {
        // Ok
      } else {
        throw new IOException("Could not create dir " + dir.getCanonicalPath());
      }
    }
  }

  @Override
  public long setContent(File file, Content content) throws IOException {
    File parent = file.getParentFile();
    if (parent != null) {
      if (!parent.exists()) {
        if (!parent.mkdirs()) {
          throw new IOException("Could not create parent directory " + parent.getCanonicalPath());
        }
      }
    }
    InputStream in = content.getInputStream();
    FileOutputStream out = new FileOutputStream(file);
    try {
      Tools.copy(in, out);
    }
    finally {
      Tools.safeClose(out);
    }
    return file.lastModified();
  }

  @Override
  public void removePath(File path) throws IOException {
    path.delete();
  }

  @Override
  public String toString() {
    return "DiskFileSystem[root=" + root.getAbsolutePath() + "]";
  }
}
