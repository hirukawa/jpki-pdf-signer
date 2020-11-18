package net.osdn.jpki.pdf_signer;

import javafx.application.Platform;
import net.osdn.util.javafx.application.SingletonApplication;

public class Main {

	public static void main(String[] args) {
		Platform.setImplicitExit(false);
		SingletonApplication.launch(MainApp.class, args);
	}
}
