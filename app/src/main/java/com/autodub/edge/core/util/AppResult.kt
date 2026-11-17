package com.autodub.edge.core.util

sealed class AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>()
    data class Error(val error: AppError) : AppResult<Nothing>()
    data object Loading : AppResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): AppResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (AppError) -> Unit): AppResult<T> {
        if (this is Error) action(error)
        return this
    }
}

sealed class AppError(val userMessage: String, val causeThrowable: Throwable? = null) {
    class OcrFailed(detail: String, cause: Throwable? = null) :
        AppError("Gagal membaca teks layar: $detail", cause)

    class DialogNotFound :
        AppError("Teks dialog tidak terdeteksi di area ROI dialog.")

    class CharacterNotFound(rawName: String) :
        AppError("Karakter '$rawName' tidak ditemukan, menggunakan Default Narrator.")

    class TranslationModelUnavailable(langPair: String, cause: Throwable? = null) :
        AppError("Model terjemahan offline ($langPair) belum diunduh atau gagal dimuat.", cause)

    class NetworkUnavailable(detail: String, cause: Throwable? = null) :
        AppError("Koneksi internet terputus: $detail", cause)

    class TtsProviderFailed(detail: String, cause: Throwable? = null) :
        AppError("Mesin suara AI gagal memproses audio: $detail", cause)

    class AudioPlaybackFailed(detail: String, cause: Throwable? = null) :
        AppError("Pemutaran audio terhenti: $detail", cause)

    class OverlayPermissionDenied :
        AppError("Izin tampil di atas aplikasi lain (Overlay) belum diberikan.")

    class MediaProjectionStopped(detail: String = "Tangkapan layar dihentikan oleh sistem atau pengguna.") :
        AppError(detail)

    class DatabaseFailed(detail: String, cause: Throwable? = null) :
        AppError("Operasi database lokal gagal: $detail", cause)

    class ServiceTerminated(detail: String = "Layanan latar belakang dihentikan oleh sistem.") :
        AppError(detail)
}
