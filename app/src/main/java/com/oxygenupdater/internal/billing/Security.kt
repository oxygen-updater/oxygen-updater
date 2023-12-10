package com.oxygenupdater.internal.billing

import android.util.Base64
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.X509EncodedKeySpec

/**
 * Security-related methods. For a secure implementation, all of this code should be implemented on
 * a server that communicates with the application on the device. For the sake of simplicity and
 * clarity of this example, this code is included here and is executed on the device. If you must
 * verify the purchases on the phone, you should obfuscate this code to make it harder for an
 * attacker to replace the code with stubs that treat all purchases as verified.
 */
object Security {

    private const val KeyFactoryAlgorithm = "RSA"
    private const val SignatureAlgorithm = "SHA1withRSA"

    /**
     * Generates a PublicKey instance from a string containing the Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     *
     * @throws NoSuchAlgorithmException if [KeyFactoryAlgorithm] is not supported
     * @throws IllegalArgumentException if [encodedPublicKey] is invalid
     */
    @Throws(
        NoSuchAlgorithmException::class,
        IllegalArgumentException::class,
    )
    fun generatePublicKey(encodedPublicKey: String): PublicKey = KeyFactory.getInstance(KeyFactoryAlgorithm).generatePublic(
        X509EncodedKeySpec(Base64.decode(encodedPublicKey, Base64.DEFAULT))
    )

    /**
     * Verifies that the signature from the server matches the computed signature on the data.
     * Returns true if the data is correctly signed.
     *
     * @param publicKey  public key associated with the developer account
     * @param signedData signed data from server
     * @param signature  server signature
     *
     * @throws NoSuchAlgorithmException if [SignatureAlgorithm] is not supported
     * @throws InvalidKeyException if [publicKey] is invalid
     * @throws SignatureException if [signedData] or [signature] is invalid
     * @throws IllegalArgumentException if [signature] is invalid
     *
     * @return true if the data and signature match
     */
    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        SignatureException::class,
        IllegalArgumentException::class,
    )
    fun verify(
        publicKey: PublicKey,
        signedData: String,
        signature: String,
    ) = with(Signature.getInstance(SignatureAlgorithm)) {
        initVerify(publicKey)
        update(signedData.toByteArray())
        verify(Base64.decode(signature, Base64.DEFAULT))
    }
}
