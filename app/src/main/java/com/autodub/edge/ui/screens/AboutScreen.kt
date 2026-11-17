package com.autodub.edge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodub.edge.BuildConfig
import com.autodub.edge.ui.components.AutoDubLogo
import com.autodub.edge.ui.components.CyberCard
import com.autodub.edge.ui.components.StatusLine
import com.autodub.edge.ui.theme.CyberColors

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AutoDubLogo()

        CyberCard {
            Text(
                text = "Informasi Aplikasi",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )
            StatusLine(
                label = "Nama Aplikasi",
                value = "AutoDub Edge",
                valueColor = CyberColors.CyberCyan
            )
            StatusLine(
                label = "Versi",
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                valueColor = CyberColors.TextPrimary
            )
            StatusLine(
                label = "Package ID",
                value = BuildConfig.APPLICATION_ID,
                valueColor = CyberColors.TextSecondary
            )
            StatusLine(
                label = "Minimum Android",
                value = "Android 8.0 (API 26)",
                valueColor = CyberColors.TextSecondary
            )
        }

        CyberCard {
            Text(
                text = "Arsitektur Dual Voice & Pipeline",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )
            Text(
                text = "1. Screen Capture tanpa root menggunakan MediaProjection & ImageReader.\n" +
                    "2. Dual-Box ROI cropping & ML Kit Text Recognition (Nama Karakter + Dialog).\n" +
                    "3. Duplicate detection + Fuzzy Character Matching dengan database Room.\n" +
                    "4. Offline EN → ID Translation melalui Google ML Kit Translation.\n" +
                    "5. Dual Voice Engine: Gemini Expressive AI TTS (Voice Design, Persona Anime, 8 Emosi, Cache Disk) saat online + Fallback otomatis ke Android Native TTS saat offline tanpa menghentikan OCR dan terjemahan.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = CyberColors.TextSecondary
            )
        }

        CyberCard {
            Text(
                text = "Kebutuhan API Key, Kuota & Biaya Provider",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberColors.TextPrimary
            )
            Text(
                text = "• Gemini TTS (gemini-3.8-flash-tts / gemini-3.8-flash-lite-tts): Memerlukan API key resmi dari Google AI Studio atau URL proxy backend Anda yang dikonfigurasi saat runtime (tidak ada API key hardcoded di APK).\n" +
                    "• Kuota & Biaya: Free tier Google AI Studio menyediakan kuota gratis terbatas (RPM/RPD); untuk pemakaian intensif / game panjang tanpa batas kuota rendah, gunakan tier berbayar (Pay-as-you-go sesuai tarif token/audio Gemini API).\n" +
                    "• Android Native TTS & ML Kit Offline: Gratis 100%, berjalan penuh di perangkat tanpa kuota dan tanpa koneksi internet.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = CyberColors.TextSecondary
            )
        }
    }
}
