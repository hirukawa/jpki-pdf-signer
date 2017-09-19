package net.osdn.jpki;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class Util {
	
	private static File appDir;
	private static File myDataDir;
	
	
	public static File getMyDataDirectory() {
		if(myDataDir == null) {
			myDataDir = new File(getApplicationDirectory(), "mydata");
		}
		return myDataDir;
	}
	
	public static File getApplicationDirectory() {
		if(appDir == null) {
			appDir = getApplicationDirectory(Util.class);
		}
		return appDir;
	}

	public static File getApplicationDirectory(Class<?> cls) {
		try {
			ProtectionDomain pd = cls.getProtectionDomain();
			CodeSource cs = pd.getCodeSource();
			URL location = cs.getLocation();
			URI uri = location.toURI();
			String path = uri.getPath();
			File file = new File(path);
			return file.getParentFile();
		} catch (Exception e) {
			try {
				return new File(".").getCanonicalFile();
			} catch (IOException e1) {
				return new File(".").getAbsoluteFile();
			}
		}
	}
	
	public static int[] getApplicationVersion() {
		String s = System.getProperty("java.application.version");
		if(s == null || s.trim().length() == 0) {
			return null;
		}
		
		s = s.trim() + ".0.0.0.0";
		String[] array = s.split("\\.", 5);
		int[] version = new int[4];
		for(int i = 0; i < 4; i++) {
			try {
				version[i] = Integer.parseInt(array[i]);
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
		}
		if(version[0] == 0 && version[1] == 0 && version[2] == 0 && version[3] == 0) {
			return null;
		}
		return version;
	}
}
