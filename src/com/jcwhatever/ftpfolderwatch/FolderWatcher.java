package com.jcwhatever.ftpfolderwatch;

import com.jcwhatever.ftpfolderwatch.ftp.IFtpMirror;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

/**
 * Asynchronously watches a folder and all of its sub folders (Recursive)
 * for changes and sends changes to an {@link IFtpMirror}.
 */
public class FolderWatcher {

    private final Path _path;
    private final IFtpMirror _mirror;
    private final WatchService _watcher;
    private final Thread _watchThread;
    private final Map<WatchKey, File> _keyMap = new HashMap<>(10);

    private volatile boolean _isRunning;

    /**
     * Constructor.
     *
     * @param folder  The folder to watch for changes.
     * @param mirror  The FTP mirror to send changes to.
     *
     * @throws IOException
     */
    public FolderWatcher(File folder, IFtpMirror mirror) throws IOException {

        _path = folder.toPath();
        _mirror = mirror;
        _watcher = FileSystems.getDefault().newWatchService();

        watch(folder);

        _isRunning = true;
        _watchThread = new Thread(new Watcher());
        _watchThread.start();
    }

    /**
     * Stop watch thread and close watcher.
     */
    public void dispose() {
        _isRunning = false;
        _watchThread.interrupt();
        try {
            _watcher.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Watch a folder and recursively watch all sub folders.
     *
     * @param folder  The folder to watch.
     *
     * @throws IOException
     */
    private void watch(File folder) throws IOException {

        Path path = folder.toPath();

        WatchKey key = path.register(_watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY);
        _keyMap.put(key, folder);

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    watch(file);
                }
            }
        }
    }

    /**
     * File watcher thread.
     */
    private class Watcher implements Runnable {

        @Override
        public void run() {

            while (_isRunning) {

                WatchKey key;
                try {
                    key = _watcher.take();
                } catch (InterruptedException x) {
                    break;
                }

                // get folder associated with watch key.
                File keyFolder = _keyMap.get(key);
                if (keyFolder == null) {
                    System.err.println("Failed to find key folder.");
                    return;
                }

                // sleep to allow file system changes to settle.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }

                Path remotePath = _path.relativize(keyFolder.toPath());

                for (WatchEvent<?> event: key.pollEvents()) {

                    WatchEvent.Kind<?> kind = event.kind();

                    // ignore overflow
                    if (kind == StandardWatchEventKinds.OVERFLOW)
                        continue;

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>)event;
                    Path filePath = pathEvent.context();
                    File file = keyFolder.toPath().resolve(filePath).toFile();

                    // ignore hidden files
                    if (file.isHidden())
                        continue;

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE ||
                            kind == StandardWatchEventKinds.ENTRY_MODIFY) {

                        // watch new directory for changes
                        if (file.isDirectory() && kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            try {
                                WatchKey newKey = file.toPath().register(_watcher,
                                        StandardWatchEventKinds.ENTRY_CREATE,
                                        StandardWatchEventKinds.ENTRY_DELETE,
                                        StandardWatchEventKinds.ENTRY_MODIFY);

                                _keyMap.put(newKey, file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // upload file/folder to mirror
                        _mirror.upload(file, remotePath.toString());
                    }
                    else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {

                        // remove directory from watch
                        if (file.isDirectory()) {
                            key.cancel();
                            _keyMap.remove(key);
                        }

                        // delete file/folder from mirror
                        _mirror.delete(filePath.toString(), remotePath.toString());
                    }
                }

                // reset key to receive more events
                if (_keyMap.containsKey(key))
                    key.reset();
            }
        }
    }

}
