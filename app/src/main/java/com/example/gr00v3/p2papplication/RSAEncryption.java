package com.example.gr00v3.p2papplication;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.Signature;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.spongycastle.asn1.cms.EnvelopedData;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemObjectGenerator;
import org.spongycastle.util.io.pem.PemWriter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static android.R.attr.path;
import static android.provider.CalendarContract.Instances.BEGIN;
import static org.apache.commons.codec.binary.Base64.decodeBase64;

public class RSAEncryption {

	private final String pubKeyFileName = "keys/keypair/pub.der";
	private final String privKeyFileName = "keys/keypair/priv.der";
	private final String certFileName = "keys/cert/cert.cert.pem";
	private final String chainCertFileName = "keys/cert/ca-chain.cert.pem";

	private String certStr;

	private PublicKey pubKey;
	private PrivateKey privKey;
	private Certificate cert;
	private Certificate chainCert;

	private Cipher cipher;

	private final String storageDirectory;
	
	public RSAEncryption() {
		Security.addProvider(new BouncyCastleProvider());

		try {
			this.cipher = Cipher.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}

//		//Read keys from File to String

		storageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/");

		try {
			privKey = getPrivKeyFromFile(privKeyFileName);
			pubKey = getPubKeyFromFile(pubKeyFileName);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Read cert to string from file
		try {
			certStr = FileUtils.readFileToString(new File(storageDirectory + certFileName));
		} catch (IOException e) {
			e.printStackTrace();
		}

		cert = getCertificateFromFile(certFileName);
		chainCert = getCertificateFromFile(chainCertFileName);

////		http://stackoverflow.com/questions/10007147/getting-a-illegalblocksizeexception-data-must-not-be-longer-than-256-bytes-when
	}

    private PrivateKey getPrivKeyFromFile( String fileName ) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		//byte[] keyBytes = Files.readAllBytes(new File(fileName).toPath());
		byte keyBytes[] = FileUtils.readFileToByteArray(new File(storageDirectory + fileName));

	    PKCS8EncodedKeySpec spec =
	      new PKCS8EncodedKeySpec(keyBytes);
	    KeyFactory kf = KeyFactory.getInstance("RSA");
	    return kf.generatePrivate(spec);
	}
	
	private PublicKey getPubKeyFromFile( String fileName ) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		//byte[] keyBytes = Files.readAllBytes(new File(fileName).toPath());
		byte keyBytes[] = FileUtils.readFileToByteArray(new File(storageDirectory + fileName));

	    X509EncodedKeySpec spec =
	      new X509EncodedKeySpec(keyBytes);
	    KeyFactory kf = KeyFactory.getInstance("RSA");
	    return kf.generatePublic(spec);
	}
	
	private Certificate getCertificateFromFile( String fileName ) {
		Certificate cert = null;
		File file = new File(storageDirectory + fileName);
		try{
		    CertificateFactory cf = CertificateFactory.getInstance("X.509");
		    cert = cf.generateCertificate(new FileInputStream(file));
		}catch(Exception ex){
		    ex.printStackTrace();
		}
		return cert;
	}

	private Certificate getCertificateFromString( String certStr ) {
		Certificate cert = null;
		try{
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			InputStream is = new ByteArrayInputStream(certStr.getBytes("UTF-8"));
			cert = cf.generateCertificate(is);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return cert;
	}

	public boolean verifyCert( String certStr ) {
		String certStr1 = certStr.substring(0, 965) + certStr.substring(977);
		Certificate cert = getCertificateFromString(certStr1);
		if(cert == null) {
			String certStr2 = certStr.substring(0, 964) + certStr.substring(976);
			cert = getCertificateFromString(certStr2);
		}


		System.out.println("FECK");
		return true;
	}

	public String convertToBase64PEMString(Certificate x509Cert) throws IOException {
		StringWriter sw = new StringWriter();
		try (PemWriter pw = new PemWriter(sw)) {
			pw.writeObject(new PemObject("CERTIFICATE", x509Cert.getEncoded()));
			pw.flush();
		} catch (CertificateEncodingException e) {
			e.printStackTrace();
		}
		return sw.toString();
	}

	public PublicKey getPubKeyFromCert( String certStr ) {
		String certStr1 = certStr.substring(0, 965) + certStr.substring(977);
		Certificate cert = getCertificateFromString(certStr1);
		if(cert == null) {
			String certStr2 = certStr.substring(0, 964) + certStr.substring(976);
			cert = getCertificateFromString(certStr2);
		}

		return cert.getPublicKey();
	}

	/**
	 * Encode bytes array to BASE64 string
	 * @param bytes
	 * @return Encoded string
	 */
	private static String encodeBASE64(byte[] bytes)
	{
		// BASE64Encoder b64 = new BASE64Encoder();
		// return b64.encode(bytes, false);
		return new String(Base64.encodeBase64(bytes));
	}

	/**
	 * Decode BASE64 encoded string to bytes array
	 * @param text The string
	 * @return Bytes array
	 * @throws IOException
	 */
	private static byte[] decodeBASE64(String text) throws IOException
	{
		// BASE64Decoder b64 = new BASE64Decoder();
		// return b64.decodeBuffer(text);
		//Log.d("TEG", Base64.class.getProtectionDomain().getCodeSource().getLocation().toString());
		return decodeBase64(text.getBytes());
	}

	/**
	 * Convert a Key to string encoded as BASE64
	 * @param key The key (private or public)
	 * @return A string representation of the key
	 */
	public static String getKeyAsString(Key key)
	{
		// Get the bytes of the key
		byte[] keyBytes = key.getEncoded();
		return encodeBASE64(keyBytes);
	}

	public PublicKey getPubKey() {
        return pubKey;
    }

    public PrivateKey getPrivKey() {
        return privKey;
    }

	public String getCertPEM() throws IOException {
		return convertToBase64PEMString(cert);
	}

	public String getCertStr() {
		return certStr;
	}

	private byte[] blockCipher(byte[] bytes, int mode) throws IllegalBlockSizeException, BadPaddingException {
		// string initialize 2 buffers.
		// scrambled will hold intermediate results
		byte[] scrambled = new byte[0];

		// toReturn will hold the total result
		byte[] toReturn = new byte[0];
		// if we encrypt we use 100 byte long blocks. Decryption requires 128 byte long blocks (because of RSA)
		int length = (mode == Cipher.ENCRYPT_MODE)? 100 : 128;

		// another buffer. this one will hold the bytes that have to be modified in this step
		byte[] buffer = new byte[length];

		for (int i=0; i< bytes.length; i++){

			// if we filled our buffer array we have our block ready for de- or encryption
			if ((i > 0) && (i % length == 0)){
				//execute the operation
				scrambled = cipher.doFinal(buffer);
				// add the result to our total result.
				toReturn = append(toReturn,scrambled);
				// here we calculate the length of the next buffer required
				int newlength = length;

				// if newlength would be longer than remaining bytes in the bytes array we shorten it.
				if (i + length > bytes.length) {
					newlength = bytes.length - i;
				}
				// clean the buffer array
				buffer = new byte[newlength];
			}
			// copy byte into our buffer.
			buffer[i%length] = bytes[i];
		}

		// this step is needed if we had a trailing buffer. should only happen when encrypting.
		// example: we encrypt 110 bytes. 100 bytes per run means we "forgot" the last 10 bytes. they are in the buffer array
		scrambled = cipher.doFinal(buffer);

		// final step before we can return the modified data.
		toReturn = append(toReturn,scrambled);

		return toReturn;
	}

	private byte[] append(byte[] prefix, byte[] suffix){
		byte[] toReturn = new byte[prefix.length + suffix.length];
		for (int i=0; i< prefix.length; i++){
			toReturn[i] = prefix[i];
		}
		for (int i=0; i< suffix.length; i++){
			toReturn[i+prefix.length] = suffix[i];
		}
		return toReturn;
	}

	public String encrypt(String plaintext,  PublicKey pubkey) throws Exception{
		this.cipher.init(Cipher.ENCRYPT_MODE, pubkey);
		byte[] bytes = plaintext.getBytes("UTF-8");

		byte[] encrypted = blockCipher(bytes,Cipher.ENCRYPT_MODE);

		char[] encryptedTranspherable = Hex.encodeHex(encrypted);
		return new String(encryptedTranspherable);
	}

	public String decrypt(String encrypted, PrivateKey privKey) throws Exception{
		//Remove BOM chars
        //Log.d("RSAEncryption", "Encrypted string: " + encrypted);
        encrypted = encrypted.replaceAll("[^a-f0-9]", "");
        //Log.d("RSAEncryption", "Encrypted string: " + encrypted);

		this.cipher.init(Cipher.DECRYPT_MODE, privKey);
		byte[] bts = Hex.decodeHex(encrypted.toCharArray());

		byte[] decrypted = blockCipher(bts,Cipher.DECRYPT_MODE);

		return new String(decrypted,"UTF-8");
	}

    public static String sign(String plainText, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(plainText.getBytes("UTF-8"));

        byte[] signature = privateSignature.sign();

        return encodeBASE64(signature);
    }

    public static boolean verify(String plainText, String signature, PublicKey publicKey) throws Exception {
		plainText = plainText.replaceAll("[^a-f0-9]", "");
		Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update(plainText.getBytes("UTF-8"));

        byte[] signatureBytes = decodeBASE64(signature);

        return publicSignature.verify(signatureBytes);
    }

}

//http://www.pixelstech.net/article/1433764001-Generate-certificate-from-cert-file-in-Java
//http://stackoverflow.com/questions/11410770/load-rsa-public-key-from-file