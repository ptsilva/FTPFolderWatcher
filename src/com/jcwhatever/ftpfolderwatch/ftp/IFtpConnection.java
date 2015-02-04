package com.jcwhatever.ftpfolderwatch.ftp;

/**
 * A type responsible for connecting to an FTP site.
 */
public interface IFtpConnection {

    /**
     * Get the address of the FTP connection.
     */
    String getAddress();

    /**
     * Get the port number.
     */
    int getPort();

    /**
     * Get the user login name.
     */
    String getUsername();

    /**
     * Connect to the FTP site.
     *
     * @param handler  The handler that will perform FTP operations.
     *
     * @return  True if successful.
     */
    boolean connect(IFtpHandler handler);
}
