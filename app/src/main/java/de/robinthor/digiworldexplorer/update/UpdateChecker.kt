package de.robinthor.digiworldexplorer.update

import org.json.JSONArray
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
    private const val RELEASES = "https://api.github.com/repos/RobinTh0r/DigiWorldExplorer_Android_Bot/releases?per_page=20"

    fun check(currentVersion: String, includePreReleases: Boolean = false, callback: (UpdateResult) -> Unit) {
        Thread {
            val result = try {
                val endpoint = if (includePreReleases) RELEASES else LATEST_RELEASE
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "DigiWorldExplorer-Android/$currentVersion")
                connection.inputStream.bufferedReader().use { reader ->
                    val body = reader.readText()
                    val release = if (includePreReleases) selectRelease(parseReleases(JSONArray(body)), true) else JSONObject(body).let { ReleaseInfo(it.getString("tag_name"), it.getString("html_url"), it.optBoolean("prerelease", false), it.optBoolean("draft", false)) }
                    if (release == null) UpdateResult.Current else {
                        val latest = release.tag
                        val releaseUrl = release.url
                        if (isNewer(currentVersion, latest)) UpdateResult.Available(latest, releaseUrl) else UpdateResult.Current
                    }
                }
            } catch (_: Exception) {
                UpdateResult.Failed
            }
            callback(result)
        }.start()
    }

    internal data class ReleaseInfo(val tag: String, val url: String, val preRelease: Boolean, val draft: Boolean)

    private fun parseReleases(releases: JSONArray): List<ReleaseInfo> = buildList {
        for (index in 0 until releases.length()) releases.optJSONObject(index)?.let { release ->
            add(ReleaseInfo(release.optString("tag_name"), release.optString("html_url"), release.optBoolean("prerelease", false), release.optBoolean("draft", false)))
        }
    }

    internal fun selectRelease(releases: List<ReleaseInfo>, includePreReleases: Boolean): ReleaseInfo? =
        releases.firstOrNull { !it.draft && (includePreReleases || !it.preRelease) }
    fun isNewer(current: String, latest: String): Boolean {
        val currentVersion = parse(current)
        val latestVersion = parse(latest)
        for (index in 0..2) {
            if (latestVersion.numbers[index] != currentVersion.numbers[index]) {
                return latestVersion.numbers[index] > currentVersion.numbers[index]
            }
        }
        if (currentVersion.qualifier.isEmpty()) return false
        if (latestVersion.qualifier.isEmpty()) return true
        val count = maxOf(currentVersion.qualifier.size, latestVersion.qualifier.size)
        for (index in 0 until count) {
            val currentPart = currentVersion.qualifier.getOrNull(index) ?: return true
            val latestPart = latestVersion.qualifier.getOrNull(index) ?: return false
            if (currentPart == latestPart) continue
            val currentNumber = currentPart.toIntOrNull()
            val latestNumber = latestPart.toIntOrNull()
            return when {
                currentNumber != null && latestNumber != null -> latestNumber > currentNumber
                currentNumber != null -> false
                latestNumber != null -> true
                else -> latestPart > currentPart
            }
        }
        return false
    }

    private fun parse(value: String): ParsedVersion {
        val clean = value.trim().removePrefix("v").removePrefix("V")
        val numbers = clean.substringBefore('-').split('.').take(3).map { it.toIntOrNull() ?: 0 }.toMutableList()
        while (numbers.size < 3) numbers += 0
        val qualifier = clean.substringAfter('-', "").split('.', '-').filter { it.isNotBlank() }
        return ParsedVersion(numbers, qualifier)
    }

    private data class ParsedVersion(val numbers: List<Int>, val qualifier: List<String>)
}