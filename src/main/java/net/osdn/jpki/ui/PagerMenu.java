package net.osdn.jpki.ui;

import javafx.scene.control.Menu;

public class PagerMenu extends Menu {

	public PagerMenu(PagerButton button) {
		super("", button);
		setStyle(
			"-fx-padding: 0 0 0 0;" + 
			"-fx-opacity: 1.0;" + 
			"-fx-background-color: transparent;"
		);
	}
}
