package net.osdn.jpki.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.pdfbox.io.IOUtils;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import net.osdn.jpki.Resources;
import javafx.stage.Stage;
import javafx.stage.Window;

public class SignatureDialog extends Dialog<Signature> {
	
	private Button btnBrowse;
	private ImageView imgView;
	private TextField tfTitle;
	private TextField tfWidthMillis;
	private TextField tfHeightMillis;
	private ButtonType buttonTypeOK;
	private ButtonType buttonTypeCancel;
	
	private File file;
	private byte[] image;
	
	
	public SignatureDialog(final Window owner, Signature signature) {
		
		Stage stage = (Stage)getDialogPane().getScene().getWindow();
		stage.getIcons().add(Resources.IMAGE_APPLICATION_16PX);

		btnBrowse = new Button("ファイル参照...");
		btnBrowse.setMinWidth(SignatureListCell.MAX_IMAGE_WIDTH);
		btnBrowse.getStyleClass().add("signature-dialog-browse-button");
		btnBrowse.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					btnBrowse_onClick(event);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		imgView = new ImageView();
		imgView.setPreserveRatio(true);
		
		StackPane sp1 = new StackPane(imgView);
		sp1.setStyle("-fx-background-color: white;");
		sp1.setPrefSize(SignatureListCell.MAX_IMAGE_WIDTH, SignatureListCell.MAX_IMAGE_HEIGHT);
		StackPane.setAlignment(sp1, Pos.CENTER);

		tfTitle = new TextField();
		tfTitle.setPrefWidth(0);
		tfTitle.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(tfTitle.getText().length() > 0) {
					tfWidthMillis.requestFocus();
				}
			}
		});
		tfTitle.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				updateButtons();
			}
		});
		
		tfWidthMillis = new TextField();
		tfWidthMillis.setTextFormatter(new MillisTextFormatter());
		tfWidthMillis.setPrefWidth(50);
		tfWidthMillis.setAlignment(Pos.BASELINE_RIGHT);
		tfWidthMillis.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				tfWidthMillis.commitValue();
				if(getWidthMillis() > 0.0) {
					tfHeightMillis.requestFocus();
				}
			}
		});
		tfWidthMillis.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				updateImage();
				updateButtons();
			}
		});
		
		tfHeightMillis = new TextField();
		tfHeightMillis.setTextFormatter(new MillisTextFormatter());
		tfHeightMillis.setPrefWidth(50);
		tfHeightMillis.setAlignment(Pos.BASELINE_RIGHT);
		tfHeightMillis.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				tfHeightMillis.commitValue();
				if(getHeightMillis() > 0.0) {
					Button btnOK = (Button)getDialogPane().lookupButton(buttonTypeOK);
					if(btnOK.isDisabled()) {
						tfTitle.requestFocus();
					} else {
						btnOK.requestFocus();
					}
				}
			}
		});
		tfHeightMillis.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				updateImage();
				updateButtons();
			}
		});
		
		Label lblTitle = new Label("表示名");
		Label lblWidth = new Label("横の長さ");
		Label lblHeight = new Label("縦の長さ");
		Label lblWidthUnit = new Label("ミリメートル");
		Label lblHeightUnit = new Label("ミリメートル");
		
		GridPane grid1 = new GridPane();
		grid1.setPadding(new Insets(0, 0, 0, 10));
		grid1.setHgap(10);
		grid1.setVgap(10);
		grid1.add(lblTitle, 0, 0);
		grid1.add(tfTitle, 1, 0);
		grid1.add(lblWidth, 0, 1);
		HBox hbox1 = new HBox(tfWidthMillis, lblWidthUnit);
		hbox1.setAlignment(Pos.BASELINE_LEFT);
		hbox1.setSpacing(5);
		grid1.add(hbox1, 1, 1);
		grid1.add(lblHeight, 0, 2);
		HBox hbox2 = new HBox(tfHeightMillis, lblHeightUnit);
		hbox2.setAlignment(Pos.BASELINE_LEFT);
		hbox2.setSpacing(5);
		grid1.add(hbox2, 1, 2);
		
		BorderPane bp1 = new BorderPane();
		bp1.setLeft(new VBox(10, btnBrowse, sp1));
		bp1.setCenter(grid1);
		
		getDialogPane().setContent(bp1);
		
		buttonTypeOK = new ButtonType("OK", ButtonData.OK_DONE);
		buttonTypeCancel = new ButtonType("キャンセル", ButtonData.CANCEL_CLOSE);
		getDialogPane().getButtonTypes().addAll(buttonTypeOK, buttonTypeCancel);

		setResultConverter(new Callback<ButtonType, Signature>() {
			@Override
			public Signature call(ButtonType param) {
				if(param == buttonTypeOK) {
					InputStream is = new ByteArrayInputStream(image);
					String title = tfTitle.getText();
					double widthMillis = getWidthMillis();
					double heightMillis = getHeightMillis();
					Signature signature = null;
					try {
						signature = new Signature(is, title, null, widthMillis, heightMillis);
					} catch (IOException e) {
						e.printStackTrace();
					}
					return signature;
				}
				return null;
			}
		});
		
		if(signature == null) {
			stage.setTitle("新しい印影の追加");
		} else {
			stage.setTitle("印影の編集");
			tfTitle.setText(signature.getTitle());
			setWidthMillis(signature.getWidthMillis());
			setHeightMillis(signature.getHeightMillis());
			this.image = signature.getImageBytes();
		}
		
		btnBrowse.requestFocus();
		updateImage();
		updateButtons();
		
		if(owner != null) {
			getDialogPane().layoutBoundsProperty().addListener(new ChangeListener<Bounds>() {
				@Override
				public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
					if(getWidth() > 0 && getHeight() > 0) {
						double x = owner.getX() + owner.getWidth() / 2;
						double y = owner.getY() + owner.getHeight() / 2;
						setX(x - getWidth() / 2);
						setY(y - getHeight() / 2);
						getDialogPane().layoutBoundsProperty().removeListener(this);
					}
				}
			});
		}
	}
	
	public void setWidthMillis(double widthMillis) {
		if(widthMillis > 0.0) {
			tfWidthMillis.setText(String.format("%.1f", widthMillis));
		} else {
			tfWidthMillis.setText("");
		}
	}
	
	public double getWidthMillis() {
		try {
			return Double.parseDouble(tfWidthMillis.getText());
		} catch(NumberFormatException e) {
			return 0.0;
		}
	}
	
	public void setHeightMillis(double heightMillis) {
		if(heightMillis > 0.0) {
			tfHeightMillis.setText(String.format("%.1f", heightMillis));
		} else {
			tfHeightMillis.setText("");
		}
	}
	
	public double getHeightMillis() {
		try {
			return Double.parseDouble(tfHeightMillis.getText());
		} catch(NumberFormatException e) {
			return 0.0;
		}
		/*
		Object obj = tfHeightMillis.getTextFormatter().getValue();
		if(obj instanceof Double) {
			return ((Double)obj).doubleValue();
		}
		return 0.0;
		*/
	}

	protected void updateButtons() {
		Button btnOK = (Button)getDialogPane().lookupButton(buttonTypeOK);
		
		if(imgView.getImage() == null) {
			btnOK.setDisable(true);
			return;
		}
		if(tfTitle.getText().length() == 0) {
			btnOK.setDisable(true);
			return;
		}
		if(getWidthMillis() <= 0.0) {
			btnOK.setDisable(true);
			return;
		}
		if(getHeightMillis() <= 0.0) {
			btnOK.setDisable(true);
			return;
		}
		
		btnOK.setDisable(false);
	}
	
	protected void btnBrowse_onClick(ActionEvent event) throws IOException {
		FileChooser fc = new FileChooser();
		fc.setTitle("印影に使用する画像を選択してください");
		fc.getExtensionFilters().add(new ExtensionFilter("PNG", Arrays.asList(new String[] { "*.png" })));
		if(file != null && file.exists() && file.isFile()) {
			fc.setInitialDirectory(file.getParentFile());
		} else {
			String s = Resources.getPreferences().get("lastSignatureDirectory", null);
			if(s != null) {
				File dir = new File(s);
				if(dir.exists() && dir.isDirectory()) {
					fc.setInitialDirectory(dir);
				}
			}
		}
		
		Stage stage = (Stage)getDialogPane().getScene().getWindow();
		file = fc.showOpenDialog(stage);
		if(file != null && file.exists()) {
			byte[] image = IOUtils.toByteArray(new FileInputStream(file));
			try {
				new Image(new ByteArrayInputStream(image)); //画像として読み込めるかテストします。ダメなら例外が発生して以下の処理はスキップされます。
				this.image = image;
			} catch(Exception e) {
				this.image = null;
			}
			updateImage();
			if(tfTitle.getText().length() == 0) {
				String title = file.getName();
				int i = title.lastIndexOf('.');
				if(i >= 0) {
					title = title.substring(0, i);
				}
				tfTitle.setText(title);
			}
			Resources.getPreferences().put("lastSignatureDirectory", file.getParentFile().getAbsolutePath());
		}
	}
	
	protected void updateImage() {
		if(image == null) {
			imgView.setImage(null);
			return;
		}
		
		double requestedWidth = getWidthMillis() * 72.0 / 25.4;
		double requestedHeight = getHeightMillis() * 72.0 / 25.4;
		if(requestedWidth <= 0.0 && requestedHeight <= 0.0) {
			requestedWidth = SignatureListCell.MAX_IMAGE_WIDTH;
			requestedHeight = SignatureListCell.MAX_IMAGE_HEIGHT;
		}
		if(requestedWidth > SignatureListCell.MAX_IMAGE_WIDTH) {
			requestedWidth = SignatureListCell.MAX_IMAGE_WIDTH;
		}
		if(requestedHeight > SignatureListCell.MAX_IMAGE_HEIGHT) {
			requestedHeight = SignatureListCell.MAX_IMAGE_HEIGHT;
		}
		InputStream is = new ByteArrayInputStream(image);
		Image img = new Image(is, requestedWidth, requestedHeight, true, true);
		imgView.setImage(img);
	}
}
