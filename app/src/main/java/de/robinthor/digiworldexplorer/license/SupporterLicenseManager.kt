package de.robinthor.digiworldexplorer.license

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

data class SupporterLicense(
    val licenseId: String,
    val edition: String,
    val issuedAt: Long,
)

object SupporterLicenseManager {
    private const val PREFERENCES = "supporter_license"
    private const val ACTIVATION_CODE = "activation_code"
    private const val SCHEME = "DWE1"
    private const val PUBLIC_KEY_BASE64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE0qPAxdeSgc0kDPGrjaoGHu8UgxOan59c6gF84wnigyLcvSsfdVWsuw8k/uAhBYblxjQ0RQttyy+VBDM0diYodQ=="

    fun load(context: Context): SupporterLicense? {
        val code = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(ACTIVATION_CODE, null)
            ?: return null
        return verify(code)
    }

    fun activate(context: Context, enteredCode: String): SupporterLicense? {
        val normalized = enteredCode.filterNot(Char::isWhitespace)
        val license = verify(normalized) ?: return null
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(ACTIVATION_CODE, normalized)
            .apply()
        return license
    }

    fun remove(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove(ACTIVATION_CODE)
            .apply()
    }

    fun verify(code: String): SupporterLicense? = runCatching {
        val parts = code.filterNot(Char::isWhitespace).split('.')
        require(parts.size == 3 && parts[0] == SCHEME)

        val payload = decodeBase64Url(parts[1])
        val signatureBytes = decodeBase64Url(parts[2])
        val publicKeyBytes = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT)
        val publicKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(publicKeyBytes))
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(publicKey)
        verifier.update(payload)
        require(verifier.verify(signatureBytes))

        val json = JSONObject(String(payload, Charsets.UTF_8))
        val licenseId = json.getString("licenseId")
        val edition = json.getString("edition")
        val issuedAt = json.getLong("issuedAt")
        require(licenseId.startsWith("DWE-SUP-") && edition == "supporter" && issuedAt > 0)
        SupporterLicense(licenseId, edition, issuedAt)
    }.getOrNull()

    private fun decodeBase64Url(value: String): ByteArray {
        val padded = value + "=".repeat((4 - value.length % 4) % 4)
        return Base64.decode(padded, Base64.URL_SAFE or Base64.NO_WRAP)
    }
}
