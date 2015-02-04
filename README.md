# FTPFolderWatcher
A simple java console program that watches a folder and uploads changes to an FTP mirror.

usage: `java -jar FTPFolderWatcher.jar -addr <remoteAddress> -user <ftpUserName>`

options:

    -addr    The address of the remote FTP site.
    -user    The ftp user name to login with.
    -port    The port number of the FTP site.
    -local   The path of the local folder to mirror. Default is the folder the jar is in. Use ~ for relative.
    -remote  The path of the remote folder to upload changes to. Default is root folder.
    -pass    The password to login. Omit to ask.
