package com.example.gr00v3.p2papplication;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.spongycastle.asn1.cms.EnvelopedData;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSAEncryption {

	private final String pubKeyFileName = "keys/pub/key.pub";
	private final String privKeyFileName = "keys/priv/key.pem";
	private final String certFileName = "keys/cert/cert.crt";
	private final String pubKeyFileNameDER = "keys/pub_der_format/pub.der";
	
	private PublicKey pubKey;
	private PrivateKey privKey;
	private Certificate cert;

	private Cipher cipher;

	private final String storageDirectory;
	
	public RSAEncryption() {
		try {
			this.cipher = Cipher.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}

//		//Read keys from File to String

		//storageDirectory = internalStorageDirectory;
		storageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/");

		try {
			privKey = getPrivKeyFromFile("keys/priv_pkcs8_format/key_pkcs8.der");
			pubKey = getPubKeyFromFile("keys/pub_der_format/pub.der");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		cert = getCertificateFromFile(certFileName);

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

    private PublicKey getPubKeyFromString( String str ) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        //byte[] keyBytes = Files.readAllBytes(new File(fileName).toPath());
        byte keyBytes[] = str.getBytes();

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

	public PublicKey getPubKey() {
        return pubKey;
    }

    public PrivateKey getPrivKey() {
        return privKey;
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
		this.cipher.init(Cipher.DECRYPT_MODE, privKey);
		byte[] bts = Hex.decodeHex(encrypted.toCharArray());

		byte[] decrypted = blockCipher(bts,Cipher.DECRYPT_MODE);

		return new String(decrypted,"UTF-8");
	}
}

//http://www.pixelstech.net/article/1433764001-Generate-certificate-from-cert-file-in-Java
//http://stackoverflow.com/questions/11410770/load-rsa-public-key-from-file