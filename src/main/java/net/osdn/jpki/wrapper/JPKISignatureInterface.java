package net.osdn.jpki.wrapper;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

public class JPKISignatureInterface implements SignatureInterface {

	private JPKICryptSignProvider jpki;
	
	public JPKISignatureInterface(JPKICryptSignProvider jpki) {
		this.jpki = jpki;
	}
	
	@Override
	public byte[] sign(InputStream content) throws IOException {
		try {
        	X509Certificate userCert = generateCertificate(jpki.getCertificate());
        	X509Certificate rootCert = generateCertificate(jpki.getRootCertificate());
        	JcaCertStore certs = new JcaCertStore(Arrays.asList(userCert, rootCert));
        	
			DigestCalculatorProvider digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().build();
			ContentSigner contentSigner = new JPKIContentSigner(jpki);
			SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider).build(contentSigner, userCert);
			CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
			generator.addSignerInfoGenerator(signerInfoGenerator);
			generator.addCertificates(certs);
			
			CMSProcessableByteArray message = new CMSProcessableByteArray(IOUtils.toByteArray(content));
			CMSSignedData signedData = generator.generate(message, false);
			return signedData.getEncoded();
		} catch(Exception e) {
			throw new IOException(e);
		}
	}
	
	public static X509Certificate generateCertificate(byte[] bytes) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(bytes);
			return (X509Certificate)factory.generateCertificate(in);
		} finally {
			if(in != null) {
				try { in.close(); } catch (IOException e) {}
			}
		}
	}
}
