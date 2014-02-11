/*******************************************************************************
 * Copyright (c) 2012 VMware, Inc.
 *
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *
 * Contributors:
 *  Lyor Goldstein
 *******************************************************************************/

package org.aspectj.weaver.tools.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import org.aspectj.weaver.tools.cache.AbstractIndexedFileCacheBacking.IndexEntry;

/**
 * @author Lyor Goldstein
 */
public class FlatFileCacheBackingTest extends AsynchronousFileCacheBackingTestSupport {
  public FlatFileCacheBackingTest() {
    super();
  }

  @Override
  protected FlatFileCacheBacking createFileBacking(File dir) {
    return new FlatFileCacheBacking(dir);
  }

  public void testReadIndex() throws IOException {
    final IndexEntry[] entries = {
        createIgnoredEntry("ignored"),
        createIndexEntry("weaved", false, false, bytes, bytes),
        createIndexEntry("generated", true, false, bytes, bytes)
    };
    final File indexFile = getIndexFile();
    writeIndex(indexFile, entries);
    final Map<String, File> dataFiles = createDataFiles(entries);

    final File cacheDir = getCacheDir();
    final AsynchronousFileCacheBacking cache = createFileBacking(cacheDir);
    final Map<String, IndexEntry> indexMap = cache.getIndexMap();
    assertEquals("Mismatched index size", entries.length, indexMap.size());

    final Map<String, byte[]> bytesMap = cache.getBytesMap();
    assertEquals("Mismatched bytes size", dataFiles.size() /* the ignored one has no file */, bytesMap.size());

    for (IndexEntry entry : entries) {
      final String key = entry.key;
      assertNotNull("Missing entry for key=" + key, indexMap.get(key));

      if (entry.ignored) {
        assertNull("Unexpected bytes for ignored key=" + key, bytesMap.get(key));
      } else {
        assertArrayEquals("Mismatched contents for key=" + key, bytes, bytesMap.get(key));
      }
    }
  }

  public void testIgnoredBadCrcDataFiles() throws Exception {
    final IndexEntry[] entries = {
        createIndexEntry("weaved-goodData", false, false, bytes, bytes),
        createIndexEntry("badData-weaved", false, false, bytes, bytes),
        createIndexEntry("generated-goodData", true, false, bytes, bytes),
        createIndexEntry("badData-generated", true, false, bytes, bytes)
    };
    final File indexFile = getIndexFile();
    writeIndex(indexFile, entries);

    final Map<String, File> dataFiles = createDataFiles(entries);
    final long newCrc = generateNewBytes();
    assertTrue("Bad new CRC", newCrc != (-1L));

    final Map<String, File> badFiles = new TreeMap<String, File>();
    for (IndexEntry entry : entries) {
      final String key = entry.key;
      if (key.startsWith("badData")) {
        final File file = dataFiles.get(key);
        final OutputStream out = new FileOutputStream(file);
        try {
          out.write(bytes);
        } finally {
          out.close();
        }
        dataFiles.remove(key);
        badFiles.put(key, file);
      }
    }

    final File cacheDir = getCacheDir();
    final FlatFileCacheBacking cache = createFileBacking(cacheDir);
    final Map<String, IndexEntry> indexMap = cache.getIndexMap();
    assertEquals("Mismatched index size", dataFiles.size(), indexMap.size());

    final Map<String, byte[]> bytesMap = cache.getBytesMap();
    assertEquals("Mismatched bytes size", dataFiles.size(), bytesMap.size());

    for (Map.Entry<String, File> badEntry : badFiles.entrySet()) {
      final String key = badEntry.getKey();
      assertFalse("Unexpectedly indexed: " + key, indexMap.containsKey(key));
      assertFalse("Unexpectedly loaded: " + key, bytesMap.containsKey(key));

      final File file = badEntry.getValue();
      assertFalse("Unexpectedly still readable: " + key, file.canRead());
    }
  }

  public void testSkipMissingDataFileOnReadIndex() throws IOException {
    final IndexEntry[] entries = {
        createIndexEntry("weaved-noData", false, false, null, null),
        createIndexEntry("withData-weaved", false, false, bytes, bytes),
        createIndexEntry("generated-noData", true, false, null, null),
        createIndexEntry("withData-generated", true, false, bytes, bytes)
    };
    final File indexFile = getIndexFile();
    writeIndex(indexFile, entries);

    final Map<String, File> dataFiles = new TreeMap<String, File>();
    for (IndexEntry entry : entries) {
      final String key = entry.key;
      if (key.startsWith("withData")) {
        dataFiles.put(key, createDataFile(entry, bytes));
      }
    }

    final File cacheDir = getCacheDir();
    final FlatFileCacheBacking cache = createFileBacking(cacheDir);
    final Map<String, IndexEntry> indexMap = cache.getIndexMap();
    assertEquals("Mismatched index size", dataFiles.size(), indexMap.size());

    final Map<String, byte[]> bytesMap = cache.getBytesMap();
    assertEquals("Mismatched bytes size", dataFiles.size(), bytesMap.size());

    for (IndexEntry entry : entries) {
      final String key = entry.key;
      if (key.startsWith("withData")) {
        assertTrue("Not indexed: " + key, indexMap.containsKey(key));
        assertTrue("Not loaded: " + key, bytesMap.containsKey(key));
      } else {
        assertFalse("Unexpectedly indexed: " + key, indexMap.containsKey(key));
        assertFalse("Unexpectedly loaded: " + key, bytesMap.containsKey(key));
      }
    }
  }

}
