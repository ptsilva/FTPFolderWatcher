package com.jcwhatever.ftpfolderwatch.ftp;

import java.io.File;

/**
 * Represents an FTP site that mirrors a local folder.
 */
public interface IFtpMirror {

    /**
     * Upload a file or create a new folder at the mirror.
     *
     * <p>If {@code file} is a file, the file is uploaded. If {@code file} is a folder,
     * a new folder is created at the remote site but inside the folder are not uploaded.</p>
     *
     * <p>When uploading a file or creating a folder, if the specified remote path does not exist,
     * it is created first.</p>
     *
     * @param file        The file/folder to upload or create.
     * @param remotePath  The remote path to upload the file to or create folder at.
     */
    public void upload(File file, String remotePath);

    /**
     * Delete a file or folder from the mirror.
     *
     * <p>If deleting a folder, all of its files and sub folders at the remote site are
     * deleted before deleting the folder.</p>
     *
     * @param name        The name of the file or folder to delete.
     * @param remotePath  The remote path of the file or folder to delete.
     */
    public void delete(String name, String remotePath);
}
