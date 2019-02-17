package net.osdn.jpki.wrapper;


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.SignatureAlgorithmIdentifierFinder;

import jp.go.jpki.appli.JPKICryptSignJNI;
import jp.go.jpki.appli.JPKICryptSignJNIException;

public class JPKIContentSigner implements ContentSigner {

	public static final AlgorithmIdentifier SHA256withRSA;
	
	static {
		SignatureAlgorithmIdentifierFinder finder = new DefaultSignatureAlgorithmIdentifierFinder();
		SHA256withRSA = finder.find("SHA256withRSA");
	}
	
	private JPKICryptSignJNI jpkiCryptSign;
	private long hProv;
	private ByteArrayOutputStream out = new ByteArrayOutputStream();
	
	public JPKIContentSigner(JPKICryptSignProvider jpki) {
		this.jpkiCryptSign = jpki.getJPKICryptSignJNI();
		this.hProv = jpki.getProviderHandle();
	}
	
	@Override
	public AlgorithmIdentifier getAlgorithmIdentifier() {
		return SHA256withRSA;
	}

	@Override
	public OutputStream getOutputStream() {
		return out;
	}

	@Override
	public byte[] getSignature() {
		long hHash = 0;
		try {
			byte[] message = out.toByteArray();
			
			hHash = jpkiCryptSign.cryptCreateHash(hProv, JPKICryptSignJNI.JPKI_CALG_SHA_256);
			jpkiCryptSign.cryptHashData(hHash, message);
			byte[] bSign = jpkiCryptSign.cryptSignHash(hHash);
			
			return bSign;
		} catch (JPKICryptSignJNIException e) {
			throw new RuntimeException(e);
		} finally {
			if(hHash != 0) {
				try {
					jpkiCryptSign.cryptDestroyHash(hHash);
					hHash = 0;
				} catch (JPKICryptSignJNIException e) {}
			}
		}
	}
}
