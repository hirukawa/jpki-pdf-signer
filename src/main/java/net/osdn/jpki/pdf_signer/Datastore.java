package net.osdn.jpki.pdf_signer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Datastore {

	private static Path appDir;
	private static Path myDataDir;
	private static String license;
	private static Boolean isRunningAsUWP;

	public static Path getMyDataDirectory(boolean createIfNotExists) throws IOException {
		Path dir = getMyDataDirectory();
		if(createIfNotExists || !Files.notExists(dir)) {
			Files.createDirectories(dir);
		}
		return dir;
	}

	public static Path getMyDataDirectory() throws IOException {
		if(myDataDir == null) {
			String s = System.getenv("APPDATA");
			if(s == null) {
				throw new IOException("APPDATA環境変数が定義されていないためデータ保存ディレクトリを参照できません。");
			}
			Path APPDATA = Paths.get(s);
			if (Files.isDirectory(APPDATA)) {
				myDataDir = APPDATA.resolve("jpki-pdf-signer");
			}

			// バージョン0.4以前はアプリケーションディレクトリ直下の "mydata" ディレクトリにデータを保存していましたが、
			// バージョン0.5以降は環境変数 APPDATA 直下の "jpki-pdf-signer" ディレクトリにデータを保存するように変更しました。
			// APPDATA 直下の "jpki-pdf-signer" ディレクトリが存在せず、旧バージョンの "mydata" ディレクトリが存在する場合、
			// "mydata" の内容を %APPDATA%/jpki-pdf-signer にコピーします。
			if(Files.notExists(myDataDir)) {
				Path oldMyDataDir = getApplicationDirectory().resolve("mydata");
				if(Files.isDirectory(oldMyDataDir)) {
					Files.createDirectory(myDataDir);
					try(Stream<Path> files = Files.list(oldMyDataDir)) {
						files.forEach(source -> {
							try {
								Path target = myDataDir.resolve(source.getFileName());
								Files.copy(source, target);
							} catch(IOException e) {
								throw new UncheckedIOException(e);
							}
						});
					}
				}
			}
		}
		if(myDataDir == null) {
			myDataDir = getApplicationDirectory().resolve("mydata");
		}
		return myDataDir;
	}

	public static Path getApplicationDirectory() {
		if(appDir == null) {
			appDir = getApplicationDirectory(Datastore.class);
		}
		return appDir;
	}

	public static Path getApplicationDirectory(Class<?> cls) {
		try {
			ProtectionDomain pd = cls.getProtectionDomain();
			CodeSource cs = pd.getCodeSource();
			URL location = cs.getLocation();
			URI uri = location.toURI();
			Path path = Paths.get(uri);
			// IntelliJで実行した場合にプロジェクトディレクトリが返されるように classes/java/main を遡ります。
			while(Files.isDirectory(path)) {
				if(!"classes/java/main/".contains(path.getFileName().toString() + "/")) {
					break;
				}
				path = path.getParent();
			}
			return path.getParent().toRealPath();
		} catch (Exception e) {
			try {
				return Paths.get(".").toRealPath();
			} catch (IOException e1) {
				return new File(".").getAbsoluteFile().toPath();
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

	public static String getLicense() throws IOException {
		if(license == null) {
			InputStream is = null;
			BufferedReader reader = null;

			if(reader == null) {
				Class<?> cls = Datastore.class;
				String name = "/" + cls.getPackageName().replace('.', '/') + "/LICENSE.txt";
				is = cls.getResourceAsStream(name);
				if(is != null) {
					reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
				}
			}
			if(reader == null) {
				Path path = getApplicationDirectory().resolve("LICENSE.txt");
				if(Files.isReadable(path)) {
					reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
				}
			}
			if(reader != null) {
				try {
					StringBuilder sb = new StringBuilder();
					String line;
					while((line = reader.readLine()) != null) {
						sb.append(line);
						sb.append("\r\n");
					}
					license = sb.toString();
				} finally {
					reader.close();
					if(is != null) {
						is.close();
					}
				}
			}
			if(license == null) {
				license = "";
			}
		}
		return license.isEmpty() ? null : license;
	}

	public static class SignatureEntry {
		public String title;
		public double widthMillis;
		public double heightMillis;
		public String filename;
	}

	public static List<Signature> loadSignatures() throws IOException {
		List<Signature> list = new ArrayList<Signature>();
		File index = getMyDataDirectory().resolve("index.json").toFile();
		if(index.exists()) {
			ObjectMapper mapper = new ObjectMapper();
			for(SignatureEntry item : mapper.readValue(index, SignatureEntry[].class)) {
				try {
					File file = getMyDataDirectory().resolve(item.filename).toFile();
					Signature signature = new Signature(
							file,
							item.widthMillis,
							item.heightMillis,
							item.title,
							null);
					list.add(signature);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		return list;
	}

	public static void saveSignatures(List<Signature> signatures) throws IOException {
		List<SignatureEntry> list = new ArrayList<SignatureEntry>();
		for(Signature signature : signatures) {
			if(!signature.isVisible()) {
				continue;
			}
			SignatureEntry item = new SignatureEntry();
			item.title = signature.getTitle();
			item.widthMillis = signature.getWidthMillis();
			item.heightMillis = signature.getHeightMillis();
			item.filename = signature.getFile().getName();
			list.add(item);
		}
		File index = getMyDataDirectory(true).resolve("index.json").toFile();
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(index, list);
	}
}
