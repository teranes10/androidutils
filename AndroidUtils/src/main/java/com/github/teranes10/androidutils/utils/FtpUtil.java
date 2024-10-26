package com.github.teranes10.androidutils.utils;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamAdapter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import au.com.softclient.mydevices.models.response.Result;

public class FtpUtil {
    private static final String TAG = "FtpUtil";

    public static CompletableFuture<Result<?>> downloadDir(FtpDirConfig config, File toDir, FtpDirTransferListener listener) {
        return CompletableFuture.supplyAsync(() -> {
            FTPClient ftp = new FTPClient();
            OutputStream outputStream = null;
            try {
                if (!isExternalStorageWritable()) {
                    Log.e(TAG, "downloadDir: Write access not granted.");
                    return Result.fail("Write access not granted.");
                }

                ftp.connect(config.server, config.port);
                Log.d(TAG, "Connected.");

                ftp.login(config.user, config.password);
                Log.d(TAG, "Logged in");

                //    ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.enterLocalPassiveMode();

                ftp.changeWorkingDirectory(config.contentRoot);
                showServerReply(ftp);

                if (!toDir.exists()) {
                    boolean created = toDir.mkdirs();
                    if (created) {
                        Log.i(TAG, "downloadDir: Directory Created.");
                    }
                }


                FTPFile[] files = ftp.listFiles(config.dir);
                if (files == null || files.length == 0) {
                    Log.e(TAG, "downloadDir: No Files.");
                    return Result.fail("No files.");
                }

                ftp.changeWorkingDirectory(config.contentRoot + "/" + config.dir);

                final int filesTotal = files.length;
                int[] downloadedFilesTotal = new int[]{0};
                long[] fileSize = new long[]{0};

                if (listener != null) {
                    ftp.setCopyStreamListener(new CopyStreamAdapter() {

                        @Override
                        public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                            if (fileSize[0] > 0) {
                                int percent = (int) (totalBytesTransferred * 100 / fileSize[0]);
                                listener.onChanged(percent, downloadedFilesTotal[0], filesTotal);
                            }
                        }
                    });
                }

                for (FTPFile file : files) {
                    fileSize[0] = file.getSize();
                    File localFile = new File(toDir.getAbsolutePath() + "/" + file.getName());
                    outputStream = new BufferedOutputStream(new FileOutputStream(localFile));
                    boolean success = ftp.retrieveFile(file.getName(), outputStream);
                    if (success) {
                        ++downloadedFilesTotal[0];
                        if (listener != null) {
                            listener.onChanged(100, downloadedFilesTotal[0], filesTotal);
                        }
                    }
                }

                if (filesTotal == downloadedFilesTotal[0]) {
                    Log.i(TAG, "downloadFile: Downloaded successfully.");
                    return Result.ok("Downloaded successfully.");
                }
            } catch (IOException e) {
                Log.e(TAG, "downloadFile: ", e);
                return Result.fail("Something went wrong. " + e.getLocalizedMessage());
            } finally {
                try {
                    ftp.logout();
                    ftp.disconnect();
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "downloadFile: ", e);
                }
            }

            return Result.fail("Something went wrong.");
        }).exceptionally(e -> Result.fail("Something went wrong."));
    }

    public static CompletableFuture<Result<Object>> downloadFile(FtpFileConfig config, File localFile) {
        return downloadFile(config, localFile);
    }

    public static CompletableFuture<Result<?>> downloadFile(FtpFileConfig config, File localFile, FtpFileTransferListener listener) {
        return CompletableFuture.supplyAsync(() -> {
            FTPClient ftp = new FTPClient();
            OutputStream outputStream = null;
            try {
                if (!isExternalStorageWritable()) {
                    Log.e(TAG, "downloadFile: Write access not granted.");
                    return Result.fail("Write access not granted.");
                }

                ftp.connect(config.server, config.port);
                Log.d(TAG, "Connected.");

                ftp.login(config.user, config.password);
                Log.d(TAG, "Logged in");

                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.enterLocalPassiveMode();

                ftp.changeWorkingDirectory(config.contentRoot);
                showServerReply(ftp);

                String dirPath = localFile.getParent();
                if (dirPath != null) {
                    File dir = new File(dirPath);
                    if (!dir.exists()) {
                        boolean created = dir.mkdirs();
                        if (created) {
                            Log.i(TAG, "downloadFile: Directory Created.");
                        }
                    }
                }

                FTPFile[] files = ftp.listFiles(config.fileName);
                if (files == null || files.length == 0) {
                    Log.e(TAG, "downloadFile: File not found.");
                    return Result.fail("File not found.");
                }

                long fileSize = files[0].getSize();

                CopyStreamAdapter streamListener = new CopyStreamAdapter() {

                    @Override
                    public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                        int percent = (int) (totalBytesTransferred * 100 / fileSize);
                        if (listener != null) {
                            listener.onChanged(percent);
                        }
                    }
                };

                ftp.setCopyStreamListener(streamListener);
                outputStream = new BufferedOutputStream(new FileOutputStream(localFile));
                boolean success = ftp.retrieveFile(config.fileName, outputStream);
                if (success) {
                    Log.i(TAG, "downloadFile: Downloaded successfully.");
                    return Result.ok("Downloaded successfully.");
                }
            } catch (IOException e) {
                Log.e(TAG, "downloadFile: ", e);
                return Result.fail("Something went wrong. " + e.getLocalizedMessage());
            } finally {
                try {
                    ftp.logout();
                    ftp.disconnect();
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "downloadFile: ", e);
                }
            }

            return Result.fail("Something went wrong.");
        }).exceptionally(e -> Result.fail("Something went wrong."));
    }

    private static void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null) {
            for (String aReply : replies) {
                Log.i(TAG, "showServerReply: " + aReply);
            }
        }
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static class FtpDirConfig extends FtpConfig {
        public String dir;

        public FtpDirConfig(String server, int port, String user, String password, String contentRoot, String dir) {
            super(server, port, user, password, contentRoot);
            this.dir = dir;
        }
    }

    public static class FtpFileConfig extends FtpConfig {
        public String fileName;

        public FtpFileConfig(String server, int port, String user, String password, String contentRoot, String fileName) {
            super(server, port, user, password, contentRoot);
            this.fileName = fileName;
        }
    }

    private static class FtpConfig {
        public String server;
        public int port;
        public String user;
        public String password;
        public String contentRoot;

        public FtpConfig(String server, int port, String user, String password, String contentRoot) {
            this.server = server;
            this.port = port;
            this.user = user;
            this.password = password;
            this.contentRoot = contentRoot;
        }
    }

    public interface FtpFileTransferListener {
        void onChanged(int percentage);
    }

    public interface FtpDirTransferListener {
        void onChanged(int percentage, int filesDownloaded, int totalFiles);
    }
}
