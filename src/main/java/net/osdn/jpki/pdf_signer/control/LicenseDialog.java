package net.osdn.jpki.pdf_signer.control;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import net.osdn.jpki.pdf_signer.Datastore;
import net.osdn.util.javafx.event.SilentEventHandler;
import net.osdn.util.javafx.fxml.Fxml;
import net.osdn.util.javafx.scene.control.DialogEx;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LicenseDialog extends DialogEx<Void> {

	private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

	public LicenseDialog(Window owner, String title, String license) throws IOException {
		super(owner);

		setTitle(title);
		Node content = Fxml.load(this);
		getDialogPane().setContent(content);
		getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
		scrollPane.prefViewportWidthProperty().bind(textFlow.widthProperty());
		scrollPane.prefViewportHeightProperty().bind(textFlow.heightProperty());
		setOnShown(event -> { Platform.runLater(wrap(this::dialog_onReady)); });
		getDialogPane().getContent().setOpacity(0.0);

		Node[] nodes = build(license);
		textFlow.getChildren().addAll(nodes);
	}

	@FXML ScrollPane scrollPane;
	@FXML TextFlow textFlow;

	void dialog_onReady() throws IOException {
		getDialogPane().getContent().setOpacity(1.0);
	}

	void link_onClick(String url) {
		try {
			Desktop.getDesktop().browse(URI.create(url));
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Node[] build(String license) throws IOException {
		List<Node> nodes = new ArrayList<Node>();

		try(BufferedReader reader = new BufferedReader(new StringReader(license))) {
			String line;
			while((line = reader.readLine()) != null) {
				if(!line.isBlank() && line.replace("-", "").isBlank()) {
					Separator separator = new Separator(Orientation.HORIZONTAL);
					separator.prefWidthProperty().bind(new DoubleBinding() {
						{
							bind(textFlow.widthProperty());
						}
						@Override
						protected double computeValue() {
							return textFlow.getWidth()
									- textFlow.getInsets().getLeft()
									- textFlow.getInsets().getRight();
						}
					});
					nodes.add(separator);
				} else if(line.startsWith("* ")) {
					Text title = new Text(" " + line.substring(1));
					title.getStyleClass().add("bold");
					nodes.add(title);
				} else {
					Matcher m = URL_PATTERN.matcher(line);
					int start = 0;
					while(m.find(start)) {
						if(start < m.start()) {
							nodes.add(new Text(line.substring(start, m.start())));
						}
						Text link = new Text(m.group());
						link.getStyleClass().add("link");
						link.setCursor(Cursor.HAND);
						link.setOnMouseClicked(event -> {
							link_onClick(link.getText());
						});
						nodes.add(link);
						start = m.end();
					}
					if(start < line.length() - 1) {
						nodes.add(new Text(line.substring(start, line.length())));
					}
				}
				nodes.add(new Text("\n"));
			}
		}
		return nodes.toArray(new Node[] {});
	}
}
