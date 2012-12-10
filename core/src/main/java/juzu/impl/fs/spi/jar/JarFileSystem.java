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

package juzu.impl.fs.spi.jar;

import juzu.UndeclaredIOException;
import juzu.impl.common.Timestamped;
import juzu.impl.common.Tools;
import juzu.impl.fs.spi.PathType;
import juzu.impl.fs.spi.ReadFileSystem;
import juzu.impl.common.Content;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class JarFileSystem extends ReadFileSystem<String> {

  /** . */
  private final URL baseURL;

  /** . */
  private final TreeMap<String, ZipEntry> entries;

  public JarFileSystem(JarFile f) throws IOException {
    TreeMap<String, ZipEntry> entries = new TreeMap<String, ZipEntry>();
    for (Enumeration<? extends ZipEntry> en = f.entries();en.hasMoreElements();) {
      ZipEntry entry = en.nextElement();
      String name = entry.getName();
      if (name.length() > 0 && name.charAt(name.length() - 1) != '/') {
        entries.put(name, entry);
        String current = name;
        while (true) {
          int index = current.lastIndexOf('/');
          if (index == -1) {
            break;
          } else {
            current = name.substring(0, index);
            if (entries.containsKey(current)) {
              break;
            } else {
              entries.put(current, null);
            }
          }
        }
      }
    }
    this.baseURL = new File(f.getName()).toURI().toURL();
    this.entries = entries;
  }

  public JarFileSystem(URL baseURL) throws IOException {
    final ZipInputStream in = new ZipInputStream(baseURL.openStream());
    try {
      this.baseURL = baseURL;
      this.entries = entries(new Enumeration<ZipEntry>() {
        ZipEntry next;
        public boolean hasMoreElements() {
          try {
            if (next == null) {
              next = in.getNextEntry();
            }
            return next != null;
          }
          catch (IOException e) {
            throw new UndeclaredIOException(e);
          }
        }
        public ZipEntry nextElement() {
          if (!hasMoreElements()) {
            throw new NoSuchElementException();
          }
          ZipEntry tmp = next;
          next = null;
          return tmp;
        }
      });
    }
    catch (UndeclaredIOException e) {
      throw e.getCause();
    }
    finally {
      Tools.safeClose(in);
    }
  }

  private TreeMap<String, ZipEntry> entries(Enumeration<ZipEntry> e) {
    TreeMap<String, ZipEntry> entries = new TreeMap<String, ZipEntry>();
    while (e.hasMoreElements()) {
      ZipEntry entry = e.nextElement();
      String name = entry.getName();
      if (name.length() > 0 && name.charAt(name.length() - 1) != '/') {
        entries.put(name, entry);
        String current = name;
        while (true) {
          int index = current.lastIndexOf('/');
          if (index == -1) {
            break;
          } else {
            current = name.substring(0, index);
            if (entries.containsKey(current)) {
              break;
            } else {
              entries.put(current, null);
            }
          }
        }
      }
    }
    return entries;
  }

  @Override
  public boolean equals(String left, String right) {
    return left.equals(right);
  }

  @Override
  public String getRoot() throws IOException {
    return "";
  }

  @Override
  public String getChild(String dir, String name) throws IOException {
    String key = dir + name;
    if (entries.containsKey(key)) {
      if (entries.get(key) != null) {
        return key;
      } else {
        return key + "/";
      }
    } else {
      return null;
    }
  }

  @Override
  public long getLastModified(String path) throws IOException {
    if (path.isEmpty()) {
      return 1;
    } else {
      ZipEntry entry = entries.get(path);
      return entry.getTime();
    }
  }

  @Override
  public Class<String> getType() {
    return String.class;
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public String getName(String path) throws IOException {
    if (path.isEmpty()) {
      return "";
    } else {
      int from = path.length();
      if (path.length() > 0 && path.charAt(path.length() - 1) == '/') {
        from--;
      }
      int index = path.lastIndexOf('/', from - 1);
      return index == -1 ? path.substring(0, from) : path.substring(index + 1, from);
    }
  }

  @Override
  public Iterator<String> getChildren(final String dir) throws IOException {
    final Iterator<String> i = entries.navigableKeySet().tailSet(dir, false).iterator();
    return new Iterator<String>() {
      String next;
      public boolean hasNext() {
        if (next == null) {
          while (i.hasNext()) {
            String next = i.next();
            if (next.startsWith(dir)) {
              int pos = next.lastIndexOf('/');
              if (pos == -1) {
                if (dir.isEmpty()) {
                  this.next = next;
                  break;
                }
              } else {
                if (pos == dir.length() - 1) {
                  this.next = next;
                  break;
                }
              }
            } else {
              break;
            }
          }
        }
        return next != null;
      }
      public String next() {
        if (hasNext()) {
          String ret;
          if (entries.get(next) != null) {
            ret = next;
          } else {
            ret = next + "/";
          }
          next = null;
          return ret;
        } else {
          throw new NoSuchElementException();
        }
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public PathType typeOf(String path) throws IOException {
    if (path.isEmpty() || path.length() > 0 && path.charAt(path.length() - 1) == '/') {
      return PathType.DIR;
    } else {
      return PathType.FILE;
    }
  }

  @Override
  public Timestamped<Content> getContent(String file) throws IOException {
    URL url = getURL(file);
    URLConnection conn = url.openConnection();
    long lastModified = conn.getLastModified();
    byte[] bytes = Tools.bytes(conn.getInputStream());
    return new Timestamped<Content>(lastModified, new Content(bytes, Charset.defaultCharset()));
  }

  @Override
  public File getFile(String path) {
    return null;
  }

  @Override
  public URL getURL(String path) throws NullPointerException, IOException {
    return new URL("jar:" + baseURL + "!/" + path);
  }
}
