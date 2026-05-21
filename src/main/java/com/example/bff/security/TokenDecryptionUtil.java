package com.example.bff.security;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.config.TinkConfig;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class TokenDecryptionUtil {

    private static Aead aead;

    static {
        try {
            TinkConfig.register();

            // Embed the key JSON directly in the code
            String keyJson = "{"
                    + "\"primaryKeyId\":339737423,"
                    + "\"key\":["
                    + "  {"
                    + "    \"keyData\":{"
                    + "      \"typeUrl\":\"type.googleapis.com/google.crypto.tink.AesGcmKey\","
                    + "      \"value\":\"GiCdyK5Sr8dC9PfXKRxfyYWR9sl5DgejhdsojzoYJ76PGA==\","
                    + "      \"keyMaterialType\":\"SYMMETRIC\""
                    + "    },"
                    + "    \"status\":\"ENABLED\","
                    + "    \"keyId\":339737423,"
                    + "    \"outputPrefixType\":\"TINK\""
                    + "  }"
                    + "]"
                    + "}";

            // Use JsonKeysetReader with a string instead of a file
            KeysetHandle keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withString(keyJson));
            aead = keysetHandle.getPrimitive(Aead.class);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize TokenDecryptionUtil", e);
        }
    }

    /**
     * Deciphers a HEX-encoded, ciphered token using the embedded keyset.
     *
     * @param jwtInHex the ciphered token in HEX format
     * @return the deciphered (original) JWT
     * @throws Exception if decryption fails
     */
    public static String decryptToken(String jwtInHex) throws Exception {
        if (jwtInHex == null || jwtInHex.isEmpty()) {
            throw new IllegalArgumentException("Token must be specified!");
        }

        // Decode the ciphered token from HEX
        byte[] cipheredToken = DatatypeConverter.parseHexBinary(jwtInHex);

        // Decipher the token using the Aead primitive
        byte[] decipheredToken = aead.decrypt(cipheredToken, null);

        // Return the deciphered token as a String
        return new String(decipheredToken, StandardCharsets.UTF_8);
    }
}