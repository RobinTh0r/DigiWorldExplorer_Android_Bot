package de.robinthor.digiworldexplorer.update

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateResult {
    data class Available(val version: String, val url: String) : UpdateResult()
    data object Current : UpdateResult()
    data object Failed : UpdateResult()
}

object UpdateChecker {
    private const val LATEST_RELEASE = "https://api.github.com/repos/RobinTh0r/DigiWorldExplorer_Android_Bot/releases/latest"

    fun check(currentVersion: String, callback: (UpdateResult) -> Unit) {
        Thread {
            val result = try {
                val connection = URL(LATEST_RELEASE).openConnection() as HttpURLConnection
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "DigiWorldExplorer-Android/$currentVersion")
                connection.inputStream.bufferedReader().use { reader ->
                    val json = JSONObject(reader.readText())
                    val latest = json.getString("tag_name")
                    val releaseUrl = json.getString("html_url")
                    if (isNewer(currentVersion, latest)) UpdateResult.Available(latest, releaseUrl) else UpdateResult.Current
                }
            } catch (_: Exception) {
                UpdateResult.Failed
            }
            callback(result)
        }.start()
    }

    fun isNewer(current: String, latest: String): Boolean {
        val currentVersion = parse(current)
        val latestVersion = parse(latest)
        for (index in 0..2) {
            if (latestVersion.numbers[index] != currentVersion.numbers[index]) {
                return latestVersion.numbers[index] > currentVersion.numbers[index]
            }
        }
        return currentVersion.preRelease && !latestVersion.preRelease
    }

    private fun parse(value: String): ParsedVersion {
        val clean = value.trim().removePrefix("v").removePrefix("V")
        val numbers = clean.substringBefore('-').split('.').take(3).map { it.toIntOrNull() ?: 0 }.toMutableList()
        while (numbers.size < 3) numbers += 0
        return ParsedVersion(numbers, '-' in clean)
    }

    private data class ParsedVersion(val numbers: List<Int>, val preRelease: Boolean)
}