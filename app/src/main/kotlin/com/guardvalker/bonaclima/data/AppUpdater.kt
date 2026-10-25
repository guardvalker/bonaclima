package com.guardvalker.bonaclima.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

private const val GITHUB_REPO = "guardvalker/bonaclima"
private const val INSTALL_ACTION = "com.guardvalker.bonaclima.INSTALL_ACTION"
private const val CHECK_EVERY_MS = 60 * 60_000L

/**
 * Busca el último release en GitHub y, si es más nuevo, baja el APK y lo instala con [PackageInstaller] pidiéndole
 * a Android que no muestre confirmación. Android solo lo respeta cuando la app ya es su propio instalador, así que
 * la primera actualización tras una instalación manual muestra el aviso del sistema y las siguientes no.
 */
class AppUpdater(context: Context) {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences("updater", Context.MODE_PRIVATE)

    val currentVersion: String =
        this.context.packageManager.getPackageInfo(this.context.packageName, 0).versionName ?: "0"

    sealed interface Result {
        /** No se consultó (revisado hace poco o ya hay una actualización en curso). */
        data object Skipped : Result
        data object UpToDate : Result
        data class Installing(val version: String) : Result
    }

    suspend fun checkAndInstall(force: Boolean = false): Result {
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong("last_check", 0L) < CHECK_EVERY_MS) return Result.Skipped
        if (!busy.compareAndSet(false, true)) return Result.Skipped
        try {
            val release = json.decodeFromString<ReleaseDto>(httpGet("https://api.github.com/repos/$GITHUB_REPO/releases/latest"))
            prefs.edit().putLong("last_check", now).apply()
            val latest = release.tag.removePrefix("v")
            val asset = release.assets.firstOrNull { it.name.endsWith(".apk") }
            if (asset == null || !isNewer(latest, currentVersion)) {
                busy.set(false)
                return Result.UpToDate
            }
            val apk = File(context.cacheDir, "update.apk")
            download(asset.url, apk)
            install(apk)
            return Result.Installing(latest)
        } catch (e: Exception) {
            busy.set(false)
            throw e
        }
    }

    private suspend fun download(url: String, target: File) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url.toHttpUrl()).header("User-Agent", "BonaClima").build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body.byteStream().use { input -> target.outputStream().use { input.copyTo(it) } }
        }
    }

    private fun install(file: File) {
        if (receiverRegistered.compareAndSet(false, true)) {
            ContextCompat.registerReceiver(context, resultReceiver, IntentFilter(INSTALL_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        }
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setSize(file.length())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                file.inputStream().use { input ->
                    session.openWrite("update.apk", 0, file.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val sender = PendingIntent.getBroadcast(
                    context, sessionId, Intent(INSTALL_ACTION).setPackage(context.packageName), PendingIntent.FLAG_MUTABLE,
                ).intentSender
                session.commit(sender)
            }
        } catch (e: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw e
        }
    }

    private val resultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    // Android pide confirmación manual (pasa en la primera actualización tras instalar a mano).
                    @Suppress("DEPRECATION")
                    val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    if (confirm == null) busy.set(false) else context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                else -> busy.set(false)
            }
        }
    }

    private companion object {
        val busy = AtomicBoolean(false)
        val receiverRegistered = AtomicBoolean(false)
    }
}

internal fun isNewer(latest: String, current: String): Boolean {
    val a = latest.split('.').map { it.toIntOrNull() ?: 0 }
    val b = current.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

@Serializable
private data class ReleaseDto(@SerialName("tag_name") val tag: String, val assets: List<AssetDto> = emptyList())

@Serializable
private data class AssetDto(val name: String, @SerialName("browser_download_url") val url: String)
