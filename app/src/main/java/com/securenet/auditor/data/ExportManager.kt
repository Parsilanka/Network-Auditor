package com.securenet.auditor.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.securenet.auditor.data.db.ScanResultEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExportManager(private val context: Context) {

    fun exportAsCsv(scans: List<ScanResultEntity>): Uri? {
        val fileName = "network_audit_${
            SimpleDateFormat("yyyyMMdd_HHmmss", 
            Locale.getDefault()).format(Date())}.csv"
        
        val csvContent = buildString {
            appendLine("Timestamp,IP Address,Hostname,MAC,Vendor,Open Ports,Tag")
            scans.forEach { scan ->
                appendLine("${
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(Date(scan.timestamp))
                },${scan.ipAddress},${scan.hostname ?: "N/A"}," +
                "${scan.macAddress ?: "N/A"},${scan.vendor ?: "N/A"}," +
                "${scan.openPorts},${scan.tag ?: ""}")
            }
        }
        
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        
        val file = File(downloadsDir, fileName)
        file.writeText(csvContent)
        
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    fun shareExport(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(
            intent, "Share Scan Results"))
    }
}
