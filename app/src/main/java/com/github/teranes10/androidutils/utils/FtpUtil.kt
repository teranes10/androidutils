package com.github.teranes10.androidutils.utils

import android.os.Environment
import android.util.Log
import com.github.teranes10.androidutils.models.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.*
import org.apache.commons.net.io.CopyStreamAdapter
import java.io.*

object FtpUtil {
    private const val TAG = "FtpUtil"

    suspend fun downloadDir(config: FtpDirConfig, toDir: File, listener: FtpDirTransferListener? = null): Outcome<*> =
        withContext(Dispatchers.IO) {
            val ftp = FTPClient()
            try {
                if (!isExternalStorageWritable()) {
                    Log.e(TAG, "downloadDir: Write access not granted.")
                    return@withContext Outcome.fail(false, "Write access not granted.")
                }

                ftp.connect(config.server, config.port)
                Log.d(TAG, "Connected.")

                ftp.login(config.user, config.password)
                Log.d(TAG, "Logged in.")

                ftp.enterLocalPassiveMode()
                ftp.changeWorkingDirectory(config.contentRoot)
                showServerReply(ftp)

                if (!toDir.exists() && toDir.mkdirs()) {
                    Log.i(TAG, "downloadDir: Directory Created.")
                }

                val files = ftp.listFiles(config.dir)
                if (files.isNullOrEmpty()) {
                    Log.e(TAG, "downloadDir: No Files.")
                    return@withContext Outcome.fail(false, "No files.")
                }

                ftp.changeWorkingDirectory("${config.contentRoot}/${config.dir}")

                val totalFiles = files.size
                var downloadedFiles = 0
                var fileSize = 0L

                listener?.let {
                    ftp.copyStreamListener = object : CopyStreamAdapter() {
                        override fun bytesTransferred(totalBytesTransferred: Long, bytesTransferred: Int, streamSize: Long) {
                            if (fileSize > 0) {
                                val percent = (totalBytesTransferred * 100 / fileSize).toInt()
                                listener.onChanged(percent, downloadedFiles, totalFiles)
                            }
                        }
                    }
                }

                for (file in files) {
                    fileSize = file.size
                    val localFile = File(toDir, file.name)
                    BufferedOutputStream(FileOutputStream(localFile)).use { outputStream ->
                        if (ftp.retrieveFile(file.name, outputStream)) {
                            downloadedFiles++
                            listener?.onChanged(100, downloadedFiles, totalFiles)
                        }
                    }
                }

                if (downloadedFiles == totalFiles) {
                    Log.i(TAG, "downloadDir: Downloaded successfully.")
                    return@withContext Outcome.ok(true, "Downloaded successfully.")
                }

                Outcome.fail(false, "Some files failed to download.")
            } catch (e: IOException) {
                Log.e(TAG, "downloadDir: ", e)
                Outcome.fail(false, e.localizedMessage ?: e.message ?: "Something went wrong.")
            } finally {
                ftp.logout()
                ftp.disconnect()
            }
        }

    suspend fun downloadFile(config: FtpFileConfig, localFile: File, listener: FtpFileTransferListener? = null): Outcome<*> =
        withContext(Dispatchers.IO) {
            val ftp = FTPClient()
            try {
                if (!isExternalStorageWritable()) {
                    Log.e(TAG, "downloadFile: Write access not granted.")
                    return@withContext Outcome.fail(false, "Write access not granted.")
                }

                ftp.connect(config.server, config.port)
                Log.d(TAG, "Connected.")

                ftp.login(config.user, config.password)
                Log.d(TAG, "Logged in.")

                ftp.setFileType(FTP.BINARY_FILE_TYPE)
                ftp.enterLocalPassiveMode()
                ftp.changeWorkingDirectory(config.contentRoot)
                showServerReply(ftp)

                localFile.parentFile?.takeIf { !it.exists() }?.mkdirs()

                val files = ftp.listFiles(config.fileName)
                if (files.isNullOrEmpty()) {
                    Log.e(TAG, "downloadFile: File not found.")
                    return@withContext Outcome.fail(false, "File not found.")
                }

                val fileSize = files[0].size

                ftp.copyStreamListener = object : CopyStreamAdapter() {
                    override fun bytesTransferred(totalBytesTransferred: Long, bytesTransferred: Int, streamSize: Long) {
                        val percent = (totalBytesTransferred * 100 / fileSize).toInt()
                        listener?.onChanged(percent)
                    }
                }

                BufferedOutputStream(FileOutputStream(localFile)).use { outputStream ->
                    if (ftp.retrieveFile(config.fileName, outputStream)) {
                        Log.i(TAG, "downloadFile: Downloaded successfully.")
                        return@withContext Outcome.ok(true, "Downloaded successfully.")
                    }
                }

                Outcome.fail(false, "Download failed.")
            } catch (e: IOException) {
                Log.e(TAG, "downloadFile: ", e)
                Outcome.fail(false, e.localizedMessage ?: e.message ?: "Something went wrong.")
            } finally {
                ftp.logout()
                ftp.disconnect()
            }
        }

    private fun showServerReply(ftpClient: FTPClient) {
        ftpClient.replyStrings?.forEach { reply ->
            Log.i(TAG, "showServerReply: $reply")
        }
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    data class FtpDirConfig(
        val server: String,
        val port: Int,
        val user: String,
        val password: String,
        val contentRoot: String,
        val dir: String
    )

    data class FtpFileConfig(
        val server: String,
        val port: Int,
        val user: String,
        val password: String,
        val contentRoot: String,
        val fileName: String
    )

    fun interface FtpFileTransferListener {
        fun onChanged(percentage: Int)
    }

    fun interface FtpDirTransferListener {
        fun onChanged(percentage: Int, filesDownloaded: Int, totalFiles: Int)
    }
}
