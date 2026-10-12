package pl.teamping.net;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public final class Transport {

    private Transport() {}

    private static final byte[] X25519_SPKI = hex("302a300506032b656e032100");
    private static final byte[] HKDF_INFO = "teamping-relay-v2".getBytes(StandardCharsets.UTF_8);
    private static final java.security.SecureRandom RNG = new java.security.SecureRandom();

    public static KeyPair x25519() {
        try {
            return KeyPairGenerator.getInstance("X25519").generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] rawPublic(KeyPair kp) {
        byte[] spki = kp.getPublic().getEncoded();
        return Arrays.copyOfRange(spki, spki.length - 32, spki.length);
    }

    public static byte[] sessionKey(KeyPair clientEph, byte[] serverEphRaw, byte[] serverStaticRaw) {
        try {
            byte[] clientPub = rawPublic(clientEph);

            byte[] dhEph = ecdh(clientEph.getPrivate(), serverEphRaw);
            byte[] dhStatic = ecdh(clientEph.getPrivate(), serverStaticRaw);
            byte[] ikm = new byte[dhEph.length + dhStatic.length];
            System.arraycopy(dhEph, 0, ikm, 0, dhEph.length);
            System.arraycopy(dhStatic, 0, ikm, dhEph.length, dhStatic.length);

            byte[] salt = new byte[serverStaticRaw.length + serverEphRaw.length + clientPub.length];
            System.arraycopy(serverStaticRaw, 0, salt, 0, serverStaticRaw.length);
            System.arraycopy(serverEphRaw, 0, salt, serverStaticRaw.length, serverEphRaw.length);
            System.arraycopy(clientPub, 0, salt, serverStaticRaw.length + serverEphRaw.length, clientPub.length);
            return hkdf(ikm, salt, HKDF_INFO, 32);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] ecdh(java.security.PrivateKey mine, byte[] peerRaw) throws Exception {
        KeyAgreement ka = KeyAgreement.getInstance("X25519");
        ka.init(mine);
        ka.doPhase(pub(peerRaw), true);
        return ka.generateSecret();
    }

    public static String seal(byte[] key, String plaintext) {
        try {
            byte[] iv = new byte[12];
            RNG.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] body = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + body.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(body, 0, out, iv.length, body.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("seal", e);
        }
    }

    public static String open(byte[] key, String b64) {
        try {
            byte[] blob = Base64.getDecoder().decode(b64);
            byte[] iv = Arrays.copyOfRange(blob, 0, 12);
            byte[] ctTag = Arrays.copyOfRange(blob, 12, blob.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(c.doFinal(ctTag), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("open", e);
        }
    }

    private static PublicKey pub(byte[] raw) throws Exception {
        byte[] spki = new byte[X25519_SPKI.length + 32];
        System.arraycopy(X25519_SPKI, 0, spki, 0, X25519_SPKI.length);
        System.arraycopy(raw, 0, spki, X25519_SPKI.length, 32);
        return KeyFactory.getInstance("X25519").generatePublic(new X509EncodedKeySpec(spki));
    }

    private static byte[] hkdf(byte[] ikm, byte[] salt, byte[] info, int len) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt.length == 0 ? new byte[32] : salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        byte[] out = new byte[len];
        byte[] t = new byte[0];
        int pos = 0, ctr = 1;
        while (pos < len) {
            mac.update(t);
            mac.update(info);
            mac.update((byte) ctr++);
            t = mac.doFinal();
            int n = Math.min(t.length, len - pos);
            System.arraycopy(t, 0, out, pos, n);
            pos += n;
        }
        return out;
    }

    private static byte[] hex(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return b;
    }
}
