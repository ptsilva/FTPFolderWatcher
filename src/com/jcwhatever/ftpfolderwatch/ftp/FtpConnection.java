package com.jcwhatever.ftpfolderwatch.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;

/**
 * Insecure FTP connection.
 */
public class FtpConnection implements IFtpConnection {

    private final String _address;
    private final int _port;
    private final String _username;
    private final String _password;

    private FTPClient _client;
    private long _expires;

    /**
     * Constructor.
     *
     * @param address   The address of the remote FTP site.
     * @param port      The port number of the remote FTP site.
     * @param userName  The user name to connect with.
     * @param password  The password to connect with.
     */
    public FtpConnection(String address, int port, String userName, String password) {
        _address = address;
        _port = port;
        _username = userName;
        _password = password;
    }

    /**
     * Validate address, username and password by connecting to the remote site.
     *
     * @return  True if connection successful, otherwise false.
     */
    public boolean validate() {
        FTPClient ftp = connect();
        if (ftp == null)
            return false;

        if (ftp.isConnected()) {
            disconnect();
            return true;
        }
        return false;
    }

    @Override
    public String getAddress() {
        return _address;
    }

    @Override
    public int getPort() {
        return _port;
    }

    @Override
    public String getUsername() {
        return _username;
    }

    @Override
    public boolean connect(IFtpHandler handler) {

        FTPClient ftp = connect();
        if (ftp == null)
            return false;

        try {
            handler.handle(ftp);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        disconnect();

        return true;
    }

    /**
     * Connect to the FTP site and return an {@code FTPClient} instance.
     *
     * @return  The client instance or null if failed.
     */
    private FTPClient connect() {

        // return current client if available and not expired.
        if (_client != null && _client.isConnected() && System.currentTimeMillis() < _expires) {
            return _client;
        }

        _expires = System.currentTimeMillis() + (10 * 1000);

        FTPClient ftp = new FTPClient();
        FTPClientConfig config = new FTPClientConfig();
        ftp.configure(config);

        try {

            ftp.connect(_address, _port);
            ftp.login(_username, _password);

            System.out.println("Connected to " + _address + '.');
            System.out.print(ftp.getReplyString());

            int reply = ftp.getReplyCode();

            if(!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                return _client = null;
            }

            _client = ftp;

        } catch(IOException e) {
            e.printStackTrace();

            if(ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch(IOException ioe) {
                    // do nothing
                }
            }

            return _client = null;
        }

        return _client;
    }

    /**
     * Disconnect from the current FTP site.
     */
    private void disconnect() {
        if (_client == null)
            return;

        if (!_client.isConnected()) {
            _client = null;
            return;
        }

        if (System.currentTimeMillis() <= _expires) {
            return;
        }

        try {
            _client.logout();
        } catch (IOException e) {
            e.printStackTrace();
        }

        _client = null;
    }
}
