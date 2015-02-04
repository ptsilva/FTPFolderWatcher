package com.jcwhatever.ftpfolderwatch.ftp;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;

/**
 * Handles FTP operations
 */
public interface IFtpHandler {

    /**
     * Invoked after an FTP connection is established to perform operations
     * on the {@code FTPClient}.
     *
     * @param ftp  The {@code FTPClient} to perform operations on.
     *
     * @throws IOException
     */
    void handle(FTPClient ftp) throws IOException;
}
