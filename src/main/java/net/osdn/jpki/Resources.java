package net.osdn.jpki;

import java.io.InputStream;
import java.util.prefs.Preferences;

import javafx.scene.image.Image;

public class Resources {
	
	private static Preferences preferences = Preferences.userNodeForPackage(Resources.class);
	
	public static Preferences getPreferences() {
		return preferences;
	}

	public static final Image IMAGE_APPLICATION_16PX = new Image(getResourceAsStream("/img/jpki-pdf-signer-icon-16px.png"));
	public static final Image IMAGE_ADD_16PX = new Image(getResourceAsStream("/img/add-16px.png"));
	public static final Image IMAGE_EDIT_16PX = new Image(getResourceAsStream("/img/edit-16px.png"));
	public static final Image IMAGE_DELETE_16PX = new Image(getResourceAsStream("/img/delete-16px.png"));
	
	public static final Image IMAGE_FILE_16PX = new Image(getResourceAsStream("/img/file-16px.png"));
	public static final Image IMAGE_SAVE_16PX = new Image(getResourceAsStream("/img/save-16px.png"));
	
	public static final Image IMAGE_PAGE_FIRST_BLACK_16PX = new Image(getResourceAsStream("/img/page-first-black-16px.png"));
	public static final Image IMAGE_PAGE_FIRST_WHITE_16PX = new Image(getResourceAsStream("/img/page-first-white-16px.png"));
	public static final Image IMAGE_PAGE_PREVIOUS_BLACK_16PX = new Image(getResourceAsStream("/img/page-previous-black-16px.png"));
	public static final Image IMAGE_PAGE_PREVIOUS_WHITE_16PX = new Image(getResourceAsStream("/img/page-previous-white-16px.png"));
	public static final Image IMAGE_PAGE_NEXT_BLACK_16PX = new Image(getResourceAsStream("/img/page-next-black-16px.png"));
	public static final Image IMAGE_PAGE_NEXT_WHITE_16PX = new Image(getResourceAsStream("/img/page-next-white-16px.png"));
	public static final Image IMAGE_PAGE_LAST_BLACK_16PX = new Image(getResourceAsStream("/img/page-last-black-16px.png"));
	public static final Image IMAGE_PAGE_LAST_WHITE_16PX = new Image(getResourceAsStream("/img/page-last-white-16px.png"));

	public static final Image IMAGE_BROWSER_BLACK_16PX = new Image(getResourceAsStream("/img/browser-black-16px.png"));
	public static final Image IMAGE_BROWSER_WHITE_16PX = new Image(getResourceAsStream("/img/browser-white-16px.png"));
	
	public static InputStream getResourceAsStream(String name) {
		return Resources.class.getResourceAsStream(name);
	}

}
