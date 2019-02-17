package net.osdn.jpki.wrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDPropBuild;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDPropBuildDataDict;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import jp.go.jpki.appli.JPKICryptSignJNIException;
import jp.go.jpki.appli.JPKIUserCertBasicData;
import jp.go.jpki.appli.JPKIUserCertException;
import jp.go.jpki.appli.JPKIUserCertService;

public class JpkiWrapper {
	
	// NTE_PROVIDER_DLL_FAIL 0x8009001D -2146893795
	// 
	
	private String applicationName;
	private String applicationVersion;
	
	public void setApplicationName(String name) {
		applicationName = name;
	}
	
	public void setApplicationVersion(String version) {
		applicationVersion = version;
	}
	
	public void addSignature(OutputStream output, PDDocument document) throws JpkiException, IOException, IllegalAccessException, IllegalArgumentException {
		addSignature(output, document, null, null, null, null, null, null);
	}

	public void addSignature(OutputStream output, PDDocument document, SignatureOptions options) throws JpkiException, IOException, IllegalAccessException, IllegalArgumentException {
		addSignature(output, document, null, null, null, null, null, options);
	}

	public void addSignature(OutputStream output, PDDocument document, String name, String reason, Date date, String location, String contact, SignatureOptions options) throws IOException {
		try {
			addSignatureWithJNIException(output, document, name, reason, date, location, contact, options);
		} catch (JPKICryptSignJNIException e) {
			throw new JpkiException(e.getErrorCode(), e.getWinErrorCode(), e);
		} catch (JPKIUserCertException e) {
			throw new JpkiException(e.getErrorCode(), 0, e);
		}
	}
	
	private void addSignatureWithJNIException(OutputStream output, PDDocument document, String name, String reason, Date date, String location, String contact, SignatureOptions options) throws IOException, JPKICryptSignJNIException, JPKIUserCertException {
		int accessPermissions = getMDPPermission(document);
		if (accessPermissions == 1) {
			throw new IOException("この文書の変更は許可されていません。");
		}
		
		JPKICryptSignProvider jpki = null;
		try {
			jpki = new JPKICryptSignProvider();

			if(name == null) {
				byte[] cert = jpki.getCertificate();
				JPKIUserCertService ucs = new JPKIUserCertService(cert);
				JPKIUserCertBasicData basicData = ucs.getBasicData();
				name = basicData.getName();
			}
			if(reason == null) {
				reason = name + " によって署名されています。";
			}
			if(date == null) {
				date = new Date();
			}
			
			PDSignature signature = new PDSignature();
			signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
			signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
			signature.setName(name);
			signature.setReason(reason);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			signature.setSignDate(calendar);
			if(location != null) {
				signature.setLocation(location);
			}
			if(contact != null) {
				signature.setContactInfo(contact);
			}
			if(applicationName != null) {
				PDPropBuildDataDict dict = new PDPropBuildDataDict();
				dict.setName(applicationName);
				dict.setVersion(applicationVersion != null ? applicationVersion : "");
				dict.setTrustedMode(true);
				PDPropBuild propBuild = new PDPropBuild();
				propBuild.setPDPropBuildApp(dict);
				signature.setPropBuild(propBuild);
			}
			if(options != null) {
				document.addSignature(signature, new JPKISignatureInterface(jpki), options);
			} else {
				document.addSignature(signature, new JPKISignatureInterface(jpki));
			}
			document.saveIncremental(output);
		} finally {
			if(jpki != null) {
				try { jpki.close(); } catch(Exception e) {}
			}
		}
	}
	
	
	/*
	 * Copyright 2015 The Apache Software Foundation.
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 *
	 *      http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 */
	
    /**
     * Get the access permissions granted for this document in the DocMDP transform parameters
     * dictionary. Details are described in the table "Entries in the DocMDP transform parameters
     * dictionary" in the PDF specification.
     *
     * @param doc document.
     * @return the permission value. 0 means no DocMDP transform parameters dictionary exists. Other
     * return values are 1, 2 or 3. 2 is also returned if the DocMDP transform parameters dictionary
     * is found but did not contain a /P entry, or if the value is outside the valid range.
     */
    public int getMDPPermission(PDDocument doc)
    {
        COSBase base = doc.getDocumentCatalog().getCOSObject().getDictionaryObject(COSName.PERMS);
        if (base instanceof COSDictionary)
        {
            COSDictionary permsDict = (COSDictionary) base;
            base = permsDict.getDictionaryObject(COSName.DOCMDP);
            if (base instanceof COSDictionary)
            {
                COSDictionary signatureDict = (COSDictionary) base;
                base = signatureDict.getDictionaryObject("Reference");
                if (base instanceof COSArray)
                {
                    COSArray refArray = (COSArray) base;
                    for (int i = 0; i < refArray.size(); ++i)
                    {
                        base = refArray.getObject(i);
                        if (base instanceof COSDictionary)
                        {
                            COSDictionary sigRefDict = (COSDictionary) base;
                            if (COSName.DOCMDP.equals(sigRefDict.getDictionaryObject("TransformMethod")))
                            {
                                base = sigRefDict.getDictionaryObject("TransformParams");
                                if (base instanceof COSDictionary)
                                {
                                    COSDictionary transformDict = (COSDictionary) base;
                                    int accessPermissions = transformDict.getInt(COSName.P, 2);
                                    if (accessPermissions < 1 || accessPermissions > 3)
                                    {
                                        accessPermissions = 2;
                                    }
                                    return accessPermissions;
                                }
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    public void setMDPPermission(PDDocument doc, PDSignature signature, int accessPermissions)
    {
        COSDictionary sigDict = signature.getCOSObject();

        // DocMDP specific stuff
        COSDictionary transformParameters = new COSDictionary();
        transformParameters.setItem(COSName.TYPE, COSName.getPDFName("TransformParams"));
        transformParameters.setInt(COSName.P, accessPermissions);
        transformParameters.setName(COSName.V, "1.2");
        transformParameters.setNeedToBeUpdated(true);

        COSDictionary referenceDict = new COSDictionary();
        referenceDict.setItem(COSName.TYPE, COSName.getPDFName("SigRef"));
        referenceDict.setItem("TransformMethod", COSName.getPDFName("DocMDP"));
        referenceDict.setItem("DigestMethod", COSName.getPDFName("SHA1"));
        referenceDict.setItem("TransformParams", transformParameters);
        referenceDict.setNeedToBeUpdated(true);

        COSArray referenceArray = new COSArray();
        referenceArray.add(referenceDict);
        sigDict.setItem("Reference", referenceArray);
        referenceArray.setNeedToBeUpdated(true);

        // Catalog
        COSDictionary catalogDict = doc.getDocumentCatalog().getCOSObject();
        COSDictionary permsDict = new COSDictionary();
        catalogDict.setItem(COSName.PERMS, permsDict);
        permsDict.setItem(COSName.DOCMDP, signature);
        catalogDict.setNeedToBeUpdated(true);
        permsDict.setNeedToBeUpdated(true);
    }
}
