package com.securenet.auditor.ui.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.securenet.auditor.domain.model.HostInfo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfReportGenerator(private val context: Context) {
    private val TAG = "PdfReportGenerator"

    data class ReportData(
        val scanDate: Date,
        val networkName: String,
        val networkIp: String,
        val hostsFound: List<HostInfo>,
        val scanDurationMs: Long,
        val openPortsCount: Int,
        val criticalFindings: List<String>,
        val highFindings: List<String>,
        val mediumFindings: List<String>,
        val lowFindings: List<String>,
        val overallRiskScore: Int,
        val generatedBy: String = "Network Auditor"
    )

    fun generateReport(data: ReportData): Uri? {
        val fileName = "NetworkAudit_${
            SimpleDateFormat("yyyyMMdd_HHmmss",
            Locale.getDefault()).format(data.scanDate)
        }.pdf"

        val pdfDocument = PdfDocument()

        // PAGE 1 — EXECUTIVE SUMMARY
        val page1Info = PdfDocument.PageInfo.Builder(
            595, 842, 1).create() // A4 size
        val page1 = pdfDocument.startPage(page1Info)
        val canvas1 = page1.canvas

        drawExecutiveSummary(canvas1, data)
        pdfDocument.finishPage(page1)

        // PAGE 2 — TECHNICAL DETAILS
        val page2Info = PdfDocument.PageInfo.Builder(
            595, 842, 2).create()
        val page2 = pdfDocument.startPage(page2Info)
        val canvas2 = page2.canvas

        drawTechnicalDetails(canvas2, data)
        pdfDocument.finishPage(page2)

        // Save to Internal Storage
        val outputDir = File(context.filesDir, "reports")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, fileName)
        Log.d(TAG, "Saving PDF to: ${outputFile.absolutePath}")

        try {
            val fos = FileOutputStream(outputFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            Log.d(TAG, "PDF saved successfully")

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF: ${e.message}", e)
            pdfDocument.close()
            return null
        }
    }

    private fun drawExecutiveSummary(
        canvas: Canvas, data: ReportData) {

        val paint = Paint()

        // Header background - dark bar
        paint.color = android.graphics.Color.parseColor(
            "#0D1117")
        canvas.drawRect(0f, 0f, 595f, 80f, paint)

        // App name
        paint.color = android.graphics.Color.parseColor(
            "#00BFA5")
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("NETWORK AUDITOR", 30f, 35f, paint)

        // Subtitle
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText(
            "Security Audit Report", 30f, 55f, paint)

        // Date
        paint.color = android.graphics.Color.parseColor(
            "#8B949E")
        paint.textSize = 10f
        val dateStr = SimpleDateFormat(
            "MMMM dd, yyyy  HH:mm:ss",
            Locale.getDefault()).format(data.scanDate)
        canvas.drawText(dateStr, 30f, 70f, paint)

        // EXECUTIVE SUMMARY title
        paint.color = android.graphics.Color.parseColor(
            "#0D1117")
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("EXECUTIVE SUMMARY", 30f, 110f, paint)

        // Divider line
        paint.color = android.graphics.Color.parseColor(
            "#00BFA5")
        paint.strokeWidth = 2f
        canvas.drawLine(30f, 115f, 565f, 115f, paint)

        // Risk score circle
        val riskColor = when {
            data.overallRiskScore >= 80 -> "#4CAF50"
            data.overallRiskScore >= 60 -> "#FFC107"
            data.overallRiskScore >= 40 -> "#FF5722"
            else -> "#F44336"
        }
        paint.color = android.graphics.Color.parseColor(
            riskColor)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(500f, 155f, 40f, paint)

        paint.color = android.graphics.Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText(
            "${data.overallRiskScore}",
            if (data.overallRiskScore < 100) 488f else 480f,
            162f, paint)

        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("RISK SCORE", 470f, 175f, paint)

        // Summary stats boxes
        paint.color = android.graphics.Color.parseColor(
            "#161B22")
        paint.style = Paint.Style.FILL

        // Box 1 - Hosts
        canvas.drawRoundRect(
            30f, 125f, 155f, 185f, 8f, 8f, paint)
        paint.color = android.graphics.Color.parseColor(
            "#00BFA5")
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText(
            "${data.hostsFound.size}", 65f, 163f, paint)
        paint.color = android.graphics.Color.parseColor(
            "#8B949E")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Hosts Found", 45f, 178f, paint)

        // Box 2 - Open Ports
        paint.color = android.graphics.Color.parseColor(
            "#161B22")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            165f, 125f, 290f, 185f, 8f, 8f, paint)
        paint.color = android.graphics.Color.parseColor(
            "#FFC107")
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText(
            "${data.openPortsCount}", 200f, 163f, paint)
        paint.color = android.graphics.Color.parseColor(
            "#8B949E")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Open Ports", 185f, 178f, paint)

        // Box 3 - Critical Findings
        paint.color = android.graphics.Color.parseColor(
            "#161B22")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            300f, 125f, 420f, 185f, 8f, 8f, paint)
        paint.color = android.graphics.Color.parseColor(
            "#F44336")
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText(
            "${data.criticalFindings.size}", 
            330f, 163f, paint)
        paint.color = android.graphics.Color.parseColor(
            "#8B949E")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Critical", 325f, 178f, paint)

        // Network info section
        var y = 210f
        paint.color = android.graphics.Color.parseColor(
            "#0D1117")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("NETWORK INFORMATION", 30f, y, paint)
        y += 8f

        paint.color = android.graphics.Color.parseColor(
            "#00BFA5")
        paint.strokeWidth = 1f
        canvas.drawLine(30f, y, 565f, y, paint)
        y += 20f

        val networkInfo = listOf(
            Pair("Network Name (SSID):", data.networkName),
            Pair("Scan Target:", data.networkIp),
            Pair("Scan Duration:", 
                "${data.scanDurationMs}ms"),
            Pair("Total Hosts Discovered:", 
                "${data.hostsFound.size}"),
            Pair("Total Open Ports:", 
                "${data.openPortsCount}"),
            Pair("Report Generated:", data.generatedBy)
        )

        networkInfo.forEach { (label, value) ->
            paint.color = android.graphics.Color.parseColor(
                "#8B949E")
            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText(label, 30f, y, paint)
            paint.color = android.graphics.Color.parseColor(
                "#E6EDF3")
            paint.isFakeBoldText = true
            canvas.drawText(value, 220f, y, paint)
            y += 18f
        }

        // Findings summary
        y += 10f
        paint.color = android.graphics.Color.parseColor(
            "#0D1117")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("SECURITY FINDINGS SUMMARY", 30f, y, paint)
        y += 8f
        paint.color = android.graphics.Color.parseColor(
            "#00BFA5")
        canvas.drawLine(30f, y, 565f, y, paint)
        y += 20f

        // Findings by severity
        val severities = listOf(
            Triple("CRITICAL", data.criticalFindings.size,
                "#F44336"),
            Triple("HIGH", data.highFindings.size,
                "#FF5722"),
            Triple("MEDIUM", data.mediumFindings.size,
                "#FFC107"),
            Triple("LOW", data.lowFindings.size,
                "#4CAF50")
        )

        severities.forEach { (sev, count, color) ->
            // Severity bar
            paint.color = android.graphics.Color.parseColor(
                color)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(
                30f, y - 12f, 110f, y + 4f,
                4f, 4f, paint)

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText(sev, 38f, y - 1f, paint)

            paint.color = android.graphics.Color.parseColor(
                "#E6EDF3")
            paint.textSize = 11f
            canvas.drawText(
                "$count finding${if(count != 1) "s" else ""}",
                120f, y - 1f, paint)

            // Progress bar
            paint.color = android.graphics.Color.parseColor(
                "#21262D")
            canvas.drawRoundRect(
                220f, y - 10f, 500f, y + 2f,
                3f, 3f, paint)
            val maxCount = maxOf(
                data.criticalFindings.size,
                data.highFindings.size,
                data.mediumFindings.size,
                data.lowFindings.size, 1)
            val barWidth = (count.toFloat() / maxCount) * 280f
            if (barWidth > 0) {
                paint.color = android.graphics.Color
                    .parseColor(color)
                canvas.drawRoundRect(
                    220f, y - 10f,
                    220f + barWidth, y + 2f,
                    3f, 3f, paint)
            }
            y += 25f
        }

        // Recommendations
        y += 10f
        paint.color = android.graphics.Color.parseColor(
            "#0D1117")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText(
            "TOP RECOMMENDATIONS", 30f, y, paint)
        y += 8f
        paint.color = android.graphics.Color.parseColor(
            "#00BFA5")
        canvas.drawLine(30f, y, 565f, y, paint)
        y += 18f

        val recommendations = mutableListOf<String>()
        if (data.criticalFindings.isNotEmpty()) {
            recommendations.add(
                "Immediately address all CRITICAL findings " +
                "before they are exploited")
        }
        data.hostsFound.forEach { host ->
            if (host.openPorts.contains(23)) {
                recommendations.add(
                    "Disable Telnet (port 23) on " +
                    "${host.ipAddress} — use SSH instead")
            }
            if (host.openPorts.contains(3306)) {
                recommendations.add(
                    "Restrict MySQL (port 3306) access on " +
                    "${host.ipAddress} — not for public access")
            }
            if (host.openPorts.contains(445)) {
                recommendations.add(
                    "Review SMB (port 445) on " +
                    "${host.ipAddress} — high ransomware risk")
            }
        }
        if (recommendations.isEmpty()) {
            recommendations.add(
                "Network appears well-secured. " +
                "Continue regular auditing.")
        }

        recommendations.take(5).forEach { rec ->
            paint.color = android.graphics.Color.parseColor(
                "#00BFA5")
            paint.textSize = 10f
            canvas.drawText("→", 30f, y, paint)
            paint.color = android.graphics.Color.parseColor(
                "#E6EDF3")
            canvas.drawText(rec, 45f, y, paint)
            y += 18f
        }

        // Footer
        paint.color = android.graphics.Color.parseColor(
            "#0D1117")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 800f, 595f, 842f, paint)
        paint.color = android.graphics.Color.parseColor(
            "#8B949E")
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText(
            "Generated by Network Auditor  |  " +
            "CONFIDENTIAL — For authorized use only  |  " +
            "Page 1 of 2",
            30f, 825f, paint)
    }

    private fun drawTechnicalDetails(
        canvas: Canvas, data: ReportData) {

        val paint = Paint()

        // Header
        paint.color = android.graphics.Color.parseColor(
            "#0D1117")
        canvas.drawRect(0f, 0f, 595f, 80f, paint)
        paint.color = android.graphics.Color.parseColor(
            "#00BFA5")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(
            "TECHNICAL APPENDIX", 30f, 35f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText(
            "Full Host Discovery & Port Scan Results",
            30f, 52f, paint)

        // Host table
        var y = 100f
        paint.color = android.graphics.Color.parseColor(
            "#0D1117")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("DISCOVERED HOSTS", 30f, y, paint)
        y += 8f
        paint.color = android.graphics.Color.parseColor(
            "#00BFA5")
        paint.strokeWidth = 1.5f
        canvas.drawLine(30f, y, 565f, y, paint)
        y += 5f

        // Table header row
        paint.color = android.graphics.Color.parseColor(
            "#161B22")
        paint.style = Paint.Style.FILL
        canvas.drawRect(30f, y, 565f, y + 20f, paint)

        val headers = listOf(
            Pair("IP Address", 30f),
            Pair("Hostname", 155f),
            Pair("Open Ports", 310f),
            Pair("Risk", 490f)
        )

        headers.forEach { (text, x) ->
            paint.color = android.graphics.Color.parseColor(
                "#8B949E")
            paint.textSize = 9f
            paint.isFakeBoldText = true
            paint.style = Paint.Style.FILL
            canvas.drawText(text, x + 5f, y + 13f, paint)
        }
        y += 22f

        // Host rows
        data.hostsFound.forEachIndexed { index, host ->
            if (y > 760f) return@forEachIndexed // Page overflow protection

            // Alternating row background
            if (index % 2 == 0) {
                paint.color = android.graphics.Color
                    .parseColor("#0D1117")
                canvas.drawRect(
                    30f, y - 3f, 565f, y + 14f, paint)
            }

            // IP Address
            paint.color = android.graphics.Color.parseColor(
                "#00BFA5")
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText(host.ipAddress, 35f, y + 9f, paint)

            // Hostname
            paint.color = android.graphics.Color.parseColor(
                "#E6EDF3")
            paint.isFakeBoldText = false
            canvas.drawText(
                host.hostname?.take(20) ?: "Unresolved",
                160f, y + 9f, paint)

            // Open Ports
            val portsText = if (host.openPorts.isEmpty())
                "None"
            else host.openPorts.take(5).joinToString(", ")
            paint.color = if (host.openPorts.isEmpty())
                android.graphics.Color.parseColor("#4CAF50")
            else
                android.graphics.Color.parseColor("#FFC107")
            canvas.drawText(portsText, 315f, y + 9f, paint)

            // Risk level
            val (riskText, riskColor) = when {
                host.openPorts.any {
                    it in listOf(23, 445, 3306, 5900) } ->
                    Pair("CRITICAL", "#F44336")
                host.openPorts.any {
                    it in listOf(21, 3389) } ->
                    Pair("HIGH", "#FF5722")
                host.openPorts.isNotEmpty() ->
                    Pair("MEDIUM", "#FFC107")
                else -> Pair("LOW", "#4CAF50")
            }

            paint.color = android.graphics.Color.parseColor(
                riskColor)
            paint.isFakeBoldText = true
            canvas.drawText(riskText, 495f, y + 9f, paint)

            y += 18f
        }

        // Port details section
        y += 15f
        if (y < 700f) {
            paint.color = android.graphics.Color.parseColor(
                "#0D1117")
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText(
                "OPEN PORT ANALYSIS", 30f, y, paint)
            y += 8f
            paint.color = android.graphics.Color.parseColor(
                "#00BFA5")
            canvas.drawLine(30f, y, 565f, y, paint)
            y += 15f

            val allOpenPorts = data.hostsFound
                .flatMap { host ->
                    host.openPorts.map { port ->
                        Triple(host.ipAddress, port,
                            host.hostname)
                    }
                }
                .sortedBy { it.second }

            allOpenPorts.forEach { (ip, port, hostname) ->
                if (y > 760f) return@forEach

                val portInfo = mapOf(
                    21 to Pair("FTP", "HIGH"),
                    22 to Pair("SSH", "LOW"),
                    23 to Pair("Telnet", "CRITICAL"),
                    80 to Pair("HTTP", "MEDIUM"),
                    443 to Pair("HTTPS", "INFO"),
                    445 to Pair("SMB", "CRITICAL"),
                    3306 to Pair("MySQL", "CRITICAL"),
                    3389 to Pair("RDP", "HIGH"),
                    5900 to Pair("VNC", "HIGH"),
                    8080 to Pair("HTTP-Alt", "MEDIUM")
                )

                val (service, risk) = portInfo[port]
                    ?: Pair("Unknown", "INFO")

                val riskColor = when (risk) {
                    "CRITICAL" -> "#F44336"
                    "HIGH" -> "#FF5722"
                    "MEDIUM" -> "#FFC107"
                    "LOW" -> "#4CAF50"
                    else -> "#2196F3"
                }

                paint.color = android.graphics.Color
                    .parseColor(riskColor)
                paint.textSize = 9f
                paint.isFakeBoldText = true
                canvas.drawText("[$risk]", 30f, y, paint)

                paint.color = android.graphics.Color
                    .parseColor("#E6EDF3")
                paint.isFakeBoldText = false
                canvas.drawText(
                    "  Port $port ($service)  →  $ip",
                    95f, y, paint)
                y += 16f
            }
        }

        // Footer
        paint.color = android.graphics.Color.parseColor(
            "#0D1117")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 800f, 595f, 842f, paint)
        paint.color = android.graphics.Color.parseColor(
            "#8B949E")
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText(
            "Generated by Network Auditor  |  " +
            "CONFIDENTIAL — For authorized use only  |  " +
            "Page 2 of 2",
            30f, 825f, paint)
    }
}
