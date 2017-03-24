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

import org.apache.commons.io.FileUtils;
import org.spongycastle.asn1.cms.EnvelopedData;

public class RSAEncryption {

	String pubKeyFileName = "keys/pub/key.pub";
	String privKeyFileName = "keys/priv/key.pem";
	String certFileName = "keys/cert/cert.crt";
	String pubKeyFileNameDER = "keys/pub_der_format/pub.der";
	
	PublicKey pubKey;
	PrivateKey privKey;
	Certificate cert;

	private final File storageDirectory;
	
	public RSAEncryption() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
//		//Read keys from File to String

		storageDirectory = Environment.getExternalStorageDirectory();

		privKey = getPrivKeyFromFile("keys/priv_pkcs8_format/key_pkcs8.der");
		
		pubKey = getPubKeyFromFile("keys/pub_der_format/pub.der");
		
		cert = getCertificateFromFile(certFileName);
		
		//Encryption		
		String text = "Television Rules The Nation";
		String encryptedText = null;
		String decryptedText = null;
		
		//Encrypt text
		try {
			encryptedText = RSAEncryptUtil.encrypt(text, pubKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(encryptedText);
		
		//Decrypt text
		try {
			decryptedText = RSAEncryptUtil.decrypt(encryptedText, privKey);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(decryptedText.toString());
		
		//Encrypt cert, gives IllegalBlockSizeException
//		try {
//			encryptedText = RSAEncryptUtil.encrypt(certA.toString(), pubKeyA);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
////		http://stackoverflow.com/questions/10007147/getting-a-illegalblocksizeexception-data-must-not-be-longer-than-256-bytes-when
	}
	
	private PrivateKey getPrivKeyFromFile( String fileName ) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		//byte[] keyBytes = Files.readAllBytes(new File(fileName).toPath());
		byte keyBytes[] = FileUtils.readFileToByteArray(new File(storageDirectory, fileName));

	    PKCS8EncodedKeySpec spec =
	      new PKCS8EncodedKeySpec(keyBytes);
	    KeyFactory kf = KeyFactory.getInstance("RSA");
	    return kf.generatePrivate(spec);
	}
	
	private PublicKey getPubKeyFromFile( String fileName ) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		//byte[] keyBytes = Files.readAllBytes(new File(fileName).toPath());
		byte keyBytes[] = FileUtils.readFileToByteArray(new File(storageDirectory, fileName));

	    X509EncodedKeySpec spec =
	      new X509EncodedKeySpec(keyBytes);
	    KeyFactory kf = KeyFactory.getInstance("RSA");
	    return kf.generatePublic(spec);
	}
	
	private Certificate getCertificateFromFile( String fileName ) {
		Certificate cert = null;
		File file = new File(storageDirectory, fileName);
		try{
		    CertificateFactory cf = CertificateFactory.getInstance("X.509");
		    cert = cf.generateCertificate(new FileInputStream(file));
		}catch(Exception ex){
		    ex.printStackTrace();
		}
		return cert;
	}
}

//http://www.pixelstech.net/article/1433764001-Generate-certificate-from-cert-file-in-Java
//http://stackoverflow.com/questions/11410770/load-rsa-public-key-from-file