package com.jcwhatever.ftpfolderwatch;

import com.jcwhatever.ftpfolderwatch.ftp.FtpConnection;
import com.jcwhatever.ftpfolderwatch.ftp.FtpMirror;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Main class
 */
public class Main {

    static Options _options = new Options();

    static {
        _options.addOption("addr", true, "Required. The address of the remote FTP site.");
        _options.addOption("user", true, "Required. The user name to login with.");
        _options.addOption("port", true, "Set the port number of the remote site. Default is 21.");
        _options.addOption("local", true, "The path of the local folder to mirror. Default is folder jar file is in.");
        _options.addOption("remote", true, "The path of the remote folder to mirror. Default is root folder.");
        _options.addOption("pass", true, "The password to login with. Omit to ask.");
    }

    public static void main(String[] args) throws URISyntaxException, ParseException {

        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(_options, args);
        Console console = System.console();

        // remote address and user name are required
        if (!cmd.hasOption("addr") || !cmd.hasOption("user")) {
            showHelp();
            return;
        }

        // password is required when there is no console to type it.
        if (console == null && !cmd.hasOption("pass")) {
            showHelp();
            System.err.println("Password is required.");
            return;
        }

        String address = cmd.getOptionValue("addr");
        int port = 21;
        String localFolder;
        String remoteFolder = cmd.hasOption("remote") ? cmd.getOptionValue("remote") : "";
        String username = cmd.getOptionValue("user");
        String password;

        // get port
        if (cmd.hasOption("port")) {
            String rawPort = cmd.getOptionValue("port");

            try {
                port = Integer.parseInt(rawPort);
            }
            catch (NumberFormatException e) {
                System.err.println("port is expected to be a number.");
                return;
            }
        }

        // get local folder
        if (cmd.hasOption("local")) {

            localFolder = cmd.getOptionValue("local");

            if (localFolder.startsWith("~")) {
                localFolder = getJarFolder().toString() + '/' + localFolder.substring(1);
            }
        }
        else {
            localFolder =  getJarFolder().toString();
        }

        if (cmd.hasOption("pass")) {
            password = cmd.getOptionValue("pass");
        }
        else {
            assert console != null;
            char passwordArray[] = console.readPassword("Remote FTP password: ");
            password = new String(passwordArray);
        }

        start(new FtpConnection(address, port, username, password), localFolder, remoteFolder);
    }

    /**
     * Start watching local folder for changes.
     *
     * @param connection    The {@code FtpConnection} to the mirror site.
     * @param localFolder   The local folder to watch.
     * @param remoteFolder  The remote folder mirror.
     */
    private static void start(FtpConnection connection, String localFolder, String remoteFolder) {

        validateConnection(connection);

        FtpMirror mirror = new FtpMirror(connection, remoteFolder);

        FolderWatcher watcher;

        try {
            watcher = new FolderWatcher(new File(localFolder), mirror);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
            return;
        }

        Console console = System.console();
        if (console != null) {
            String input = null;

            while (!"exit".equals(input)) {
                System.out.println("Type 'exit' to stop.");
                input = console.readLine();
            }
        }
        else {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        watcher.dispose();
        System.exit(0);
    }

    /**
     * Validate an FTP connection by establishing a connection. Ends the
     * program if connection fails.
     *
     * @param connection  The connection to validate.
     */
    private static void validateConnection(FtpConnection connection) {
        System.out.println("Validating connection...");

        if (!connection.validate()) {
            System.err.println("Connection invalid.");
            System.exit(-1);
        }

        System.out.println("Connection valid.");
    }

    /**
     * Show help.
     */
    private static void showHelp() {

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar " + getJar().getName(), _options);
    }

    /**
     * Get the FTPFolderWatcher jar file.
     */
    private static File getJar() {

        try {
            return new File(
                    Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    /**
     * Get the folder the FTPFolderWatcher jar is in.
     */
    private static File getJarFolder() {
        File baseFile = getJar();
        return baseFile.getParentFile();
    }
}
