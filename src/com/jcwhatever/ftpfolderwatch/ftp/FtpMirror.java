package com.jcwhatever.ftpfolderwatch.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * FTP Mirror Site
 */
public class FtpMirror implements IFtpMirror {

    private final IFtpConnection _connection;
    private final String _rootFolder;
    private String _workingDirectory = null;
    private final Set<String> _createdFolders = new HashSet<>(10);

    /**
     * Constructor.
     *
     * @param connection    The mirror site connection.
     * @param remoteFolder  The remote site folder to act as the mirrors root folder.
     */
    public FtpMirror(IFtpConnection connection, String remoteFolder) {
        _connection = connection;
        _rootFolder = remoteFolder;
    }

    @Override
    public void upload(final File file, final String remotePath) {

        final String path = Paths.get(_rootFolder, remotePath).toString();
        final String filename = file.getName();

        System.out.println("Preparing to upload '" + file.getName() + "' to '" + remotePath + '\'');

        _connection.connect(new IFtpHandler() {
            @Override
            public void handle(FTPClient ftp) throws IOException {

                if (!_createdFolders.contains(path)) {
                    createRemotePath(ftp, path);
                    _createdFolders.add(path);
                }

                if (!changeWorkingDirectory(ftp, path))
                    return;

                if (file.isFile()) {

                    FileInputStream stream = null;

                    try {
                        stream = new FileInputStream(file);
                        if (ftp.storeFile(filename, stream)) {
                            System.out.println("Uploaded: " + filename);
                        } else {
                            System.out.println("Failed to upload: " + filename);
                        }
                    } finally {
                        if (stream != null) {
                            stream.close();
                        }
                    }
                } else if (file.isDirectory()) {

                    if (ftp.makeDirectory(filename)) {
                        System.out.println("Made directory: " + filename);
                    } else {
                        System.out.println("Failed to make directory: " + filename);
                    }

                }
            }
        });
    }

    @Override
    public void delete(final String name, final String remotePath) {

        final Path remoteAbsPath = Paths.get(_rootFolder, remotePath);

        System.out.println("Preparing to delete '" + name + "' from '" + remotePath + '\'');

        _connection.connect(new IFtpHandler() {

            @Override
            public void handle(FTPClient ftp) throws IOException {

                if (!changeWorkingDirectory(ftp, remoteAbsPath.toString()))
                    return;

                FTPFile[] files = ftp.listFiles();
                if (files == null) {
                    System.err.println("Delete failed. No files found.");
                    return;
                }

                for (FTPFile file : files) {

                    if (file.getName().equals(name)) {

                        if (file.isDirectory()) {
                           deleteFolder(ftp, name);
                        }
                        else {
                            deleteFile(ftp, name);
                        }
                        return;
                    }
                }

                System.err.println("Failed to delete file or folder: " + name + ", File not found.");
            }
        });
    }

    /**
     * Change the current working directory.
     *
     * @param ftp         The ftp client.
     * @param remotePath  The remote path to set as the working directory.
     *
     * @return  True if successful.
     *
     * @throws IOException
     */
    private boolean changeWorkingDirectory(FTPClient ftp, String remotePath) throws IOException {

        String workingDirectory = remotePath.replace("\\", "/");

        if (ftp.changeWorkingDirectory(workingDirectory)) {
            System.out.println("Changed working directory to:" + workingDirectory);
            _workingDirectory = workingDirectory;
            return true;
        }
        else {
            System.out.println("Failed to change working directory to:" + workingDirectory);
            System.out.println("Aborting.");
            return false;
        }
    }

    /**
     * Delete a remote file from the current working directory.
     *
     * @param ftp       The ftp client.
     * @param filename  The name of the file to delete.
     *
     * @throws IOException
     */
    private void deleteFile(FTPClient ftp, String filename) throws IOException {
        if (ftp.deleteFile(filename)) {
            System.out.println("Deleted file: " + filename);
        }
        else {
            System.out.println("Failed to delete file: " + filename);
        }
    }

    /**
     * Delete a remote folder from the current working directory.
     *
     * @param ftp         The ftp client.
     * @param foldername  The name of the folder to delete.
     *
     * @throws IOException
     */
    private void deleteFolder(FTPClient ftp, String foldername) throws IOException {

        String currentDirectory = _workingDirectory;
        changeWorkingDirectory(ftp, currentDirectory + '/' + foldername);

        FTPFile[] files = ftp.listFiles();
        if (files != null) {
            for (FTPFile file : files) {

                if (file.getName().equals(".") || file.getName().equals(".."))
                    continue;

                deleteContents(ftp, file);
            }
        }

        changeWorkingDirectory(ftp, currentDirectory);

        if (ftp.removeDirectory(foldername)) {
            System.out.println("Deleted folder: " + foldername);
        } else {
            System.out.println("Failed to delete folder: " + foldername);
        }
    }

    /**
     * Recursively delete all contents from a folder or delete a file from the remote site.
     *
     * @param ftp   The FTP client.
     * @param file  The file or folder to delete.
     *
     * @throws IOException
     */
    private void deleteContents(FTPClient ftp, FTPFile file) throws IOException {

        if (file.isDirectory()) {

            String currentDirectory = _workingDirectory;

            if (changeWorkingDirectory(ftp, _workingDirectory + '/' + file.getName())) {

                FTPFile[] files = ftp.listFiles();
                if (files != null) {
                    for (FTPFile f : files) {
                        deleteContents(ftp, f);
                    }
                }
                ftp.removeDirectory(file.getName());

                changeWorkingDirectory(ftp, currentDirectory);
            }
        }
        else {
            deleteFile(ftp, file.getName());
        }
    }

    /**
     * Ensure a remote path exists and create if it does not.
     *
     * @param ftp         The FTP client.
     * @param remotePath  The remote path.
     *
     * @throws IOException
     */
    private void createRemotePath(FTPClient ftp, String remotePath) throws IOException {

        if (remotePath.isEmpty())
            return;

        changeWorkingDirectory(ftp, "/");

        Path path = Paths.get(remotePath);

        LinkedList<Path> paths = new LinkedList<>();

        paths.addFirst(path);

        while (path.getParent() != null) {
            path = path.getParent();
            if (path.getFileName() == null) {

                if (path.getParent() == null) {
                    break;
                }
                else {
                    System.err.println("Failed to create remote path: " + remotePath);
                    return;
                }
            }

            paths.addFirst(path);
        }

        createRemotePathRecurse(ftp, paths.removeFirst(), paths);
    }

    /**
     * Recursive portion of {@code createRemotePath}.
     */
    private void createRemotePathRecurse(FTPClient ftp, Path current, LinkedList<Path> paths) throws IOException {

        Path currentFolderPath = current.getFileName();
        if (currentFolderPath == null)
            return;

        FTPFile[] folders = ftp.listDirectories();
        String currentFolder = currentFolderPath.toString();

        if (folders == null) {
            ftp.makeDirectory(currentFolder);
        }
        else {
            boolean hasFolder = false;
            for (FTPFile folder : folders) {
                if (folder.getName().equals(currentFolder)) {
                    hasFolder = true;
                    break;
                }
            }
            if (!hasFolder) {
                ftp.makeDirectory(currentFolder);
            }
        }

        if (!paths.isEmpty()) {

            changeWorkingDirectory(ftp, current.toString());

            createRemotePathRecurse(ftp, paths.removeFirst(), paths);
        }
    }
}
