package net.osdn.jpki.wrapper;


import java.io.Closeable;
import java.io.IOException;

import jp.go.jpki.appli.JPKICryptSignJNI;
import jp.go.jpki.appli.JPKICryptSignJNIException;

public class JPKICryptSignProvider implements Closeable {
	
	private JPKICryptSignJNI jpkiCryptSign;
	private long hProv;
	
	public JPKICryptSignProvider() throws JPKICryptSignJNIException {
		jpkiCryptSign = new JPKICryptSignJNI();
		hProv = jpkiCryptSign.cryptAcquireContext(0);
	}
	
	public JPKICryptSignJNI getJPKICryptSignJNI() {
		return jpkiCryptSign;
	}
	
	/* package private */ long getProviderHandle() {
		return hProv;
	}
	
	public byte[] getCertificate() throws JPKICryptSignJNIException {
		long hKey = 0;
		try {
			hKey = jpkiCryptSign.cryptGetUserKey(hProv);
			byte[] cert = jpkiCryptSign.cryptGetCertificateValue(hKey);
			return cert;
		} finally {
			if(jpkiCryptSign != null && hKey != 0) {
				jpkiCryptSign.cryptDestroyKey(hKey);
			}
		}
	}
	
	public byte[] getRootCertificate() throws JPKICryptSignJNIException {
		return jpkiCryptSign.cryptGetRootCertificateValue(hProv);
	}
	
	@Override
	public void close() throws IOException {
		if(jpkiCryptSign != null) {
			if(hProv != 0) {
				try {
					jpkiCryptSign.cryptReleaseContext(hProv);
					hProv = 0;
				} catch (JPKICryptSignJNIException e) {
					throw new IOException(e);
				}
			}
		}
		jpkiCryptSign = null;
	}
}
