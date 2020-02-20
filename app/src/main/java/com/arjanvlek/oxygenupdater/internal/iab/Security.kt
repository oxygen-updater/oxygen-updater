package com.arjanvlek.oxygenupdater.internal.iab

import android.util.Base64
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.exceptions.GooglePlayBillingException
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Security-related methods. For a secure implementation, all of this code should be implemented on
 * a server that communicates with the application on the device. For the sake of simplicity and
 * clarity of this example, this code is included here and is executed on the device. If you must
 * verify the purchases on the phone, you should obfuscate this code to make it harder for an
 * attacker to replace the code with stubs that treat all purchases as verified.
 */
object Security {

    private const val TAG = "IABUtil/Security"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    /**
     * Verifies that the data was signed with the given signature, and returns the verified
     * purchase. The data is in JSON format and signed with a private key. The data also contains
     * the [PurchaseState] and product ID of the purchase.
     *
     * @param base64PublicKey the base64-encoded public key to use for verifying.
     * @param signedData      the signed JSON string (signed, not encrypted)
     * @param signature       the signature for the data, signed with the private key
     */
    fun verifyPurchase(base64PublicKey: String?, signedData: String?, signature: String?): Boolean {
        if (signedData.isNullOrBlank() || base64PublicKey.isNullOrBlank() || signature.isNullOrBlank()) {
            logError(TAG, GooglePlayBillingException("Purchase verification failed: missing data."))
            return BuildConfig.DEBUG // Line modified (https://stackoverflow.com/questions/14600664/android-in-app-purchase-signature-verification-failed). Was: return false.
        }

        val key = generatePublicKey(base64PublicKey)

        return verify(key, signedData, signature)
    }

    /**
     * Generates a PublicKey instance from a string containing the Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     *
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     */
    fun generatePublicKey(encodedPublicKey: String?): PublicKey {
        return try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)

            keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            logError(TAG, GooglePlayBillingException("Invalid key specification."))
            throw IllegalArgumentException(e)
        }
    }

    /**
     * Verifies that the signature from the server matches the computed signature on the data.
     * Returns true if the data is correctly signed.
     *
     * @param publicKey  public key associated with the developer account
     * @param signedData signed data from server
     * @param signature  server signature
     *
     * @return true if the data and signature match
     */
    fun verify(publicKey: PublicKey?, signedData: String, signature: String?): Boolean {
        val signatureBytes: ByteArray
        signatureBytes = try {
            Base64.decode(signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            logError(TAG, "Base64 decoding failed", e)
            return false
        }
        try {
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(signedData.toByteArray())
            if (!sig.verify(signatureBytes)) {
                logError(TAG, GooglePlayBillingException("Signature verification failed."))
                return false
            }
            return true
        } catch (e: NoSuchAlgorithmException) {
            logError(TAG, "No Base64 algorithm loaded", e)
        } catch (e: InvalidKeyException) {
            logError(TAG, "Invalid key", e)
        } catch (e: SignatureException) {
            logError(TAG, "Invalid key signature type", e)
        }
        return false
    }
}
