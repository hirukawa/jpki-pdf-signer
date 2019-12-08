package net.osdn.jpki.pdf_signer;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import net.osdn.util.javafx.event.SilentCallback;
import net.osdn.util.javafx.event.SilentEventHandler;
import net.osdn.util.javafx.fxml.Fxml;
import net.osdn.util.javafx.scene.control.DialogEx;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class SignatureDialog extends DialogEx<Signature> implements Initializable {

	public static final double MAX_IMAGE_WIDTH = Math.ceil(30.0 * 72.0 / 25.4);
	public static final double MAX_IMAGE_HEIGHT = Math.ceil(30.0 * 72.0 / 25.4);

	private Signature signature;
	private byte[]    newImageBytes;

	public SignatureDialog(Window owner, Signature signature) {
		super(owner);
		this.signature = signature;

		Stage stage = (Stage)getDialogPane().getScene().getWindow();
		stage.setTitle(signature == null ? "新しい印影の追加" : "印影の編集");

		Node content = Fxml.load(this);
		getDialogPane().setContent(content);
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		setOnShowing(wrap(this::dialog_onShowing));
		setResultConverter(wrap(this::dialog_onResult));

		if(signature != null) {
			ivImage.setImage(signature.getImage());
			tfTitle.setText(signature.getTitle());
			widthMillisProperty.set(signature.getWidthMillis());
			heightMillisProperty.set(signature.getHeightMillis());
		}
	}

	@FXML Button btnBrowse;
	@FXML ImageView ivImage;
	@FXML TextField tfTitle;
	@FXML TextField tfWidthMillis;
	@FXML TextField tfHeightMillis;
	DoubleProperty widthMillisProperty;
	DoubleProperty heightMillisProperty;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		widthMillisProperty = NonNegativeDoubleFilter.applyTo(tfWidthMillis);
		heightMillisProperty = NonNegativeDoubleFilter.applyTo(tfHeightMillis);

		ivImage.fitWidthProperty().bind(new DoubleBinding() {
			{
				bind(ivImage.imageProperty(), widthMillisProperty, heightMillisProperty);
			}
			@Override
			protected double computeValue() {
				Dimension2D prefSize = computePrefSize(ivImage.getImage(), widthMillisProperty.get(), heightMillisProperty.get());
				double scaleWidth = prefSize.getWidth() > MAX_IMAGE_WIDTH ? MAX_IMAGE_WIDTH / prefSize.getWidth() : 1.0;
				double scaleHeight = prefSize.getHeight() > MAX_IMAGE_HEIGHT ? MAX_IMAGE_HEIGHT / prefSize.getHeight() : 1.0;
				return prefSize.getWidth() * Math.min(scaleWidth, scaleHeight);
			}
		});
		ivImage.fitHeightProperty().bind(new DoubleBinding() {
			{
				bind(ivImage.imageProperty(), widthMillisProperty, heightMillisProperty);
			}
			@Override
			protected double computeValue() {
				Dimension2D prefSize = computePrefSize(ivImage.getImage(), widthMillisProperty.get(), heightMillisProperty.get());
				double scaleWidth = prefSize.getWidth() > MAX_IMAGE_WIDTH ? MAX_IMAGE_WIDTH / prefSize.getWidth() : 1.0;
				double scaleHeight = prefSize.getHeight() > MAX_IMAGE_HEIGHT ? MAX_IMAGE_HEIGHT / prefSize.getHeight() : 1.0;
				return prefSize.getHeight() * Math.min(scaleWidth, scaleHeight);
			}
		});

		btnBrowse.setOnAction(wrap(this::tfBrowse_onAction));
		tfTitle.setOnAction(wrap(this::tfTitle_onAction));
		tfWidthMillis.setOnAction(wrap(this::tfWidthMillis_onAction));
		tfHeightMillis.setOnAction(wrap(this::tfHeightMillis_onAction));
	}

	void dialog_onShowing(DialogEvent event) {
		getDialogPane().lookupButton(ButtonType.OK).disableProperty()
				.bind(ivImage.imageProperty().isNull()
				.or(tfTitle.textProperty().isEmpty())
				.or(widthMillisProperty.lessThanOrEqualTo(0.0))
				.or(heightMillisProperty.lessThanOrEqualTo(0.0)));
	}

	Signature dialog_onResult(ButtonType param) throws IOException, NoSuchAlgorithmException {
		if(param.equals(ButtonType.OK)) {
			File file;
			if(newImageBytes != null) {
				MessageDigest md = MessageDigest.getInstance("MD5");
				String filename = printHexBinary(md.digest(newImageBytes)) + ".png";
				Path path = Datastore.getMyDataDirectory(true).resolve(filename);
				Files.write(path, newImageBytes);
				file = path.toFile();
			} else {
				file = signature.getFile();
			}
			return new Signature(
					file,
					widthMillisProperty.get(),
					heightMillisProperty.get(),
					tfTitle.getText().trim(),
					null);
		}
		return null;
	}

	void tfBrowse_onAction(ActionEvent event) throws IOException {
		Preferences preferences = Preferences.userNodeForPackage(getClass());

		FileChooser fc = new FileChooser();
		fc.setTitle("印影に使用する画像を選択してください");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", Arrays.asList("*.png")));

		String lastSignatureDirectory = preferences.get("lastSignatureDirectory", null);
		if(lastSignatureDirectory != null) {
			File dir = new File(lastSignatureDirectory);
			if(dir.isDirectory()) {
				fc.setInitialDirectory(dir);
			}
		}

		Window owner = getDialogPane().getScene().getWindow();
		File file = fc.showOpenDialog(owner);
		if(file != null && file.exists()) {
			newImageBytes = Files.readAllBytes(file.toPath());
			ivImage.setImage(new Image(new ByteArrayInputStream(newImageBytes)));
			if(tfTitle.getText().isBlank()) {
				String title = file.getName();
				int i = title.lastIndexOf('.');
				if(i >= 0) {
					title = title.substring(0, i);
				}
				tfTitle.setText(title);
			}
			preferences.put("lastSignatureDirectory", file.getParentFile().getAbsolutePath());
			tfTitle.requestFocus();
		}
	}

	void tfTitle_onAction(ActionEvent event) {
		if(tfTitle.getText().length() > 0) {
			tfWidthMillis.requestFocus();
		}
	}

	void tfWidthMillis_onAction(ActionEvent event) {
		if(tfWidthMillis.getText().length() > 0) {
			tfHeightMillis.requestFocus();
		}
	}

	void tfHeightMillis_onAction(ActionEvent event) {
		if(tfHeightMillis.getText().length() > 0) {
			Node btnOK = getDialogPane().lookupButton(ButtonType.OK);
			if(btnOK.isDisabled()) {
				tfTitle.requestFocus();
			} else {
				btnOK.requestFocus();
			}
		}
	}

	private static CharSequence printHexBinary(byte[] val) {
		StringBuilder hex = new StringBuilder(val.length * 2);
		for(int i = 0; i < val.length; i++) {
			int h = (val[i] & 0xFF) >>> 4;
			int l = val[i] & 0x0F;
			hex.append((char)(h > 9 ? h + 55 : h + 48));
			hex.append((char)(l > 9 ? l + 55 : l + 48));
		}
		return hex;
	}

	private static Dimension2D computePrefSize(Image image, double widthMillis, double heightMillis) {
		double prefWidth = 0.0;
		double prefHeight = 0.0;
		if(image != null) {
			if(widthMillis < 4.0 && heightMillis < 4.0) {
				prefWidth = image.getWidth();
				prefHeight = image.getHeight();
			} else if(widthMillis < 4.0) {
				prefHeight = heightMillis * 72.0 / 25.4;
				prefWidth = image.getWidth() * prefHeight / image.getHeight();
			} else if(heightMillis < 4.0) {
				prefWidth = widthMillis * 72.0 / 25.4;
				prefHeight = image.getHeight() * prefWidth / image.getWidth();
			} else {
				prefWidth = widthMillis * 72.0 / 25.4;
				prefHeight = heightMillis * 72.0 / 25.4;
			}
		}
		return new Dimension2D(prefWidth, prefHeight);
	}

	@SuppressWarnings("overloads")
	protected <T extends Event> EventHandler<T> wrap(SilentEventHandler<T> handler) {
		return SilentEventHandler.wrap(handler);
	}

	@SuppressWarnings("overloads")
	protected <P, R> Callback<P, R> wrap(SilentCallback<P, R> callback) {
		return SilentCallback.wrap(callback);
	}
}
