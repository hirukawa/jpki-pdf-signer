package net.osdn.jpki.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.pdfbox.io.IOUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javafx.scene.image.Image;
import net.osdn.jpki.Resources;
import net.osdn.jpki.Util;

public class Signature {

	private File file;
	private byte[] image;
	private double widthMillis;
	private double heightMillis;
	private String title;
	private String description;
	
	public Signature(InputStream image, String title, String description, double widthMillis, double heightMillis) throws IOException {
		if(image != null) {
			this.image = IOUtils.toByteArray(image);
		}
		this.title = title;
		this.description = description;
		this.widthMillis = widthMillis;
		this.heightMillis = heightMillis;
	}
	
	public boolean isVisible() {
		return (widthMillis > 0.0) && (heightMillis > 0.0);
	}
	
	public void setImage(InputStream image) throws IOException {
		this.image = IOUtils.toByteArray(image);
	}
	
	public Image getImage(double scale) {
		if(image == null || scale <= 0.0) {
			return null;
		}
		InputStream is = new ByteArrayInputStream(image);
		double requestedWidth = widthMillis * 72.0 / 25.4 * scale;
		double requestedHeight = heightMillis * 72.0 / 25.4 * scale;
		return new Image(is, requestedWidth, requestedHeight, true, true);
	}
	
	public byte[] getImageBytes() {
		return image;
	}
	
	public void setWidthMillis(double widthMillis) {
		this.widthMillis = widthMillis;
	}
	
	public double getWidthMillis() {
		return widthMillis;
	}
	
	public void setHeightMillis(double heightMillis) {
		this.heightMillis = heightMillis;
	}
	
	public double getHeightMillis() {
		return heightMillis;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		if(description == null) {
			return String.format("%.1f x %.1f mm", widthMillis, heightMillis);
		}
		return description;
	}
	
	public File getFile() {
		return file;
	}
	
	private static Signature invisibleSignature;
	
	public static Signature getInvisibleSignature() throws IOException {
		if(invisibleSignature == null) {
			InputStream is = Resources.getResourceAsStream("/img/invisible-signature.png");
			invisibleSignature = new Signature(is, "印影なしで", "電子署名する", 0.0, 0.0);
		}
		return invisibleSignature;
	}
	
	public static List<Signature> load() throws JsonParseException, JsonMappingException, IOException {
		List<Signature> signatures = new ArrayList<Signature>();
		File index = new File(Util.getMyDataDirectory(), "index.json");
		if(index.exists()) {
			ObjectMapper mapper = new ObjectMapper();
			List<POJO> list = mapper.readValue(index, new TypeReference<List<POJO>>(){});
			for(POJO s : list) {
				File file = new File(Util.getMyDataDirectory(), s.filename);
				if(file.exists()) {
					try(InputStream is = new FileInputStream(file)) {
						Signature signature = new Signature(is, s.title, null, s.widthMillis, s.heightMillis);
						signature.file = file;
						signatures.add(signature);
					}
				}
			}
		}
		return signatures;
	}
	
	public static void save(List<Signature> signatures) throws IOException, NoSuchAlgorithmException {
		File dir = Util.getMyDataDirectory();
		if(!dir.exists()) {
			dir.mkdirs();
		}
		List<POJO> list = new ArrayList<POJO>();
		for(Signature signature : signatures) {
			if(signature.isVisible()) {
				POJO s = new POJO();
				s.title = signature.getTitle();
				s.widthMillis = signature.getWidthMillis();
				s.heightMillis = signature.getHeightMillis();
				s.filename = getFilename(signature.image);
				File file = new File(dir, s.filename);
				if(!file.exists()) {
					OutputStream out = new FileOutputStream(file);
					out.write(signature.image);
					out.close();
				}
				list.add(s);
			}
		}
		File index = new File(dir, "index.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(index, list);
	}
	
	private static String getFilename(byte[] image) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		return DatatypeConverter.printHexBinary(md.digest(image)) + ".png";
	}
	
	private static class POJO {
		public String title;
		public double widthMillis;
		public double heightMillis;
		public String filename;
	}
}
