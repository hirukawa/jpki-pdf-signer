package net.osdn.jpki;

import java.awt.SplashScreen;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.stage.FileChooser.ExtensionFilter;
import net.osdn.jpki.ui.MainLayout;
import net.osdn.jpki.ui.Signature;
import net.osdn.jpki.ui.SignatureDialog;
import net.osdn.jpki.wrapper.JpkiException;
import net.osdn.jpki.wrapper.JpkiWrapper;
import net.osdn.util.fx.Dialogs;

public class Main extends MainLayout {
	public static String APP_TITLE = "JPKI PDF SIGNER";
	public static String APP_VERSION;
	public static String APP_WINDOW_TITLE;

	private static volatile int count = 0;
	private static Stage stage;
	private static File lastSaveDir;
	
	public static void main(String[] args) {
		if(count++ == 0) {
			int[] version = Util.getApplicationVersion();
			if(version != null) {
				if(version[2] == 0) {
					APP_VERSION = String.format("%d.%d", version[0], version[1]);
				} else {
					APP_VERSION = String.format("%d.%d.%d", version[0], version[1], version[2]);
				}
				APP_WINDOW_TITLE = APP_TITLE + " " + APP_VERSION;
			} else {
				APP_WINDOW_TITLE = APP_TITLE;
			}
			launch(args);
		} else {
			Platform.runLater(() -> {
				if(stage != null) {
					stage.setIconified(false);
					stage.toFront();
				}
			});
		}
	}
	
	private File input;
	private File dirty;

	@Override
	public void start(Stage stage) throws Exception {
		super.start(stage);
		Main.stage = stage;
		
		SplashScreen splash = SplashScreen.getSplashScreen();
		if(splash != null) {
			splash.close();
		}
	}
	
	@Override
	protected void stage_onCloseRequest(WindowEvent event) {
		PDDocument document = pdfPane.getDocument();
		if(document != null) {
			try { document.close(); } catch(Exception e) {}
		}
	}

	@Override
	protected void menu_open() {
		FileChooser fc = new FileChooser();
		fc.setTitle("開く");
		fc.getExtensionFilters().add(new ExtensionFilter("ファイル", "*.pdf", "*.yml", "*.yaml"));
		fc.getExtensionFilters().add(new ExtensionFilter("PDF", "*.pdf"));
		fc.getExtensionFilters().add(new ExtensionFilter("YAML", "*.yml", "*.yaml"));
		String s = Resources.getPreferences().get("lastOpenDirectory", null);
		if(s != null) {
			File dir = new File(s);
			if(dir.exists() && dir.isDirectory()) {
				fc.setInitialDirectory(dir);
			}
		}
		File file = fc.showOpenDialog(stage);
		if(file != null) {
			if(file.getName().toLowerCase().endsWith(".pdf")) {
				pdf_open(file, 0);
			}
			Resources.getPreferences().put("lastOpenDirectory", file.getParentFile().getAbsolutePath());
		}
	}
	
	@Override
	protected void menu_save() {
		if(dirty == null || !dirty.exists()) {
			return;
		}
		
		FileChooser fc = new FileChooser();
		fc.setTitle("名前を付けて保存");
		if(lastSaveDir != null && lastSaveDir.isDirectory() && lastSaveDir.exists()) {
			fc.setInitialDirectory(lastSaveDir);
		} else {
			lastSaveDir = null;
			if(input != null) {
				fc.setInitialDirectory(input.getParentFile());
			}
		}
		String defaultName = "signed.pdf";
		if(input != null) {
			defaultName = input.getName();
			int i = defaultName.lastIndexOf('.');
			if(i > 0) {
				defaultName = defaultName.substring(0, i);
			}
			defaultName += "-signed.pdf";
		}
		fc.setInitialFileName(defaultName);
		File file = fc.showSaveDialog(stage);
		if(file != null) {
			lastSaveDir = file.getParentFile();
			if(dirty != null && dirty.exists()) {
				OutputStream output = null;
				try {
					output = new FileOutputStream(file);
					Files.copy(dirty.toPath(), output);
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if(output != null) {
						try { output.close(); } catch(Exception e) {}
					}
				}
			}
			int pageIndex = pdfPane.getPageIndex();
			pdf_open(file, pageIndex);
		}
	}
	
	@Override
	protected void menu_exit() {
		Platform.exit();
	}
	
	@Override
	protected void pdf_open(final File file, int pageIndex) {
		if(file != null && file.getName().endsWith(".tmp")) {
			menuFileSave.setDisable(false);
			dirty = file;
		} else {
			stage.setTitle(file.getAbsolutePath() + " - " + APP_WINDOW_TITLE);
			menuFileSave.setDisable(true);
			input = file;
			dirty = null;
			signatureList.getSelectionModel().clearSelection();
		}
		
		PDDocument document = null;
		InputStream in = null;
		try {
			document = pdfPane.getDocument();
			if(document != null) {
				try { document.close(); } catch(Exception e) {}
				document = null;
			}
			in = new FileInputStream(file);
			byte[] bytes = IOUtils.toByteArray(in);
			document = PDDocument.load(bytes);
			pdfPane.setDocument(document);
			pdfPane.setPage(pageIndex);
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(in != null) {
				try { in.close(); } catch(Exception e) {}
			}
		}
		updatePagerButtons(document, pageIndex);
	}
	
	@Override
	protected void pdf_moveFirstPage() throws IOException {
		PDDocument document = pdfPane.getDocument();
		int pageIndex = 0;
		if(document != null) {
			pdfPane.setPage(pageIndex);
		}
		updatePagerButtons(document, pageIndex);
	}
	
	@Override
	protected void pdf_movePreviousPage() throws IOException {
		PDDocument document = pdfPane.getDocument();
		int pageIndex = -1;
		if(document != null) {
			pageIndex = pdfPane.getPageIndex();
			if(pageIndex > 0) {
				pdfPane.setPage(--pageIndex);
			}
		}
		updatePagerButtons(document, pageIndex);
	}
	
	@Override
	protected void pdf_moveNextPage() throws IOException {
		PDDocument document = pdfPane.getDocument();
		int pageIndex = -1;
		if(document != null) {
			pageIndex = pdfPane.getPageIndex();
			if(pageIndex + 1 < document.getNumberOfPages()) {
				pdfPane.setPage(++pageIndex);
			}
		}
		updatePagerButtons(document, pageIndex);
	}
	
	@Override
	protected void pdf_moveLastPage() throws IOException {
		PDDocument document = pdfPane.getDocument();
		int pageIndex = -1;
		if(document != null) {
			pageIndex = document.getNumberOfPages() - 1;
			pdfPane.setPage(pageIndex);
		}
		updatePagerButtons(document, pageIndex);
	}
	
	@Override
	protected void pdf_mouseEntered(MouseEvent event) throws Exception {
		//System.out.println("pdf_mouseEntered");
		signatureImageView.setVisible(true);
	}

	@Override
	protected void pdf_mouseExited(MouseEvent event) throws Exception {
		//System.out.println("pdf_mouseExited");
		signatureImageView.setVisible(false);
		
	}

	@Override
	protected void pdf_mouseMoved(MouseEvent event) throws Exception {
		//System.out.println("pdf_mouseMoved");
		Image img = signatureImageView.getImage();
		if(img != null) {
			signatureImageView.setLayoutX(event.getX() - (int)(img.getWidth() / 2));
			signatureImageView.setLayoutY(event.getY() - (int)(img.getHeight() / 2));
		}
	}

	@Override
	protected void pdf_mouseClicked(MouseEvent event) throws Exception {
		if(event.getButton() != MouseButton.PRIMARY) {
			return;
		}
		Bounds pageBounds = pdfPane.getPageBounds();
		if(pageBounds == null || pageBounds.getWidth() <= 0 || pageBounds.getHeight() <= 0) {
			return;
		}
		double x = event.getX() - pageBounds.getMinX();
		double y = event.getY() - pageBounds.getMinY();
		if(x < 0.0 || y < 0.0 || x >= pageBounds.getWidth() || y >= pageBounds.getHeight()) {
			return;
		}
		Image img = signatureImageView.getImage();
		if(img == null) {
			return;
		}
		Signature signature = signatureList.getSelectionModel().getSelectedItem();
		if(signature == null || !signature.isVisible()) {
			return;
		}
		PDDocument document = pdfPane.getDocument();
		if(document == null) {
			return;
		}
		signatureList.refresh();
		
		int pageIndex = pdfPane.getPageIndex();
		PDRectangle pageMediaBox = document.getPage(pageIndex).getMediaBox();
		double xPt = (x - (int)(img.getWidth() / 2)) *  pageMediaBox.getWidth() / pageBounds.getWidth();
		double yPt = (y - (int)(img.getHeight() / 2)) * pageMediaBox.getHeight() / pageBounds.getHeight();

		InputStream is = null;
		try {
			is = new ByteArrayInputStream(signature.getImageBytes());
			PDVisibleSignDesigner designer = new PDVisibleSignDesigner(is);
			designer.width((float)mm2px(signature.getWidthMillis()));
			designer.height((float)mm2px(signature.getHeightMillis()));
			designer.xAxis((float)xPt);
			designer.yAxis(-pageMediaBox.getHeight() + (float)yPt);
			
			PDVisibleSigProperties props = new PDVisibleSigProperties();
			props.setPdVisibleSignature(designer);
			props.visualSignEnabled(true);
			props.page(pageIndex + 1);
			props.buildSignature();
			SignatureOptions options = new SignatureOptions();
			options.setPage(pageIndex);
			options.setVisualSignature(props);
			
			Platform.runLater(() -> {
				signatureList.getSelectionModel().clearSelection();
				signatureList.refresh();
				try {
					addVisibleSignature(document, pageIndex, signature, options);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} finally {
			if(is != null) {
				try { is.close(); } catch(Exception e) {}
			}
		}
	}

	@Override
	protected void pdf_scaleChanged(float scale) throws Exception {
		//System.out.println("scaleChanged: scale=" + scale);
		Signature signature = signatureList.getSelectionModel().getSelectedItem();
		if(signature != null && signature.isVisible() && scale > 0f) {
			signatureImageView.setImage(signature.getImage(scale));
			pdfPane.setCursor(Cursor.NONE);
		} else {
			signatureImageView.setImage(null);
			pdfPane.setCursor(Cursor.DEFAULT);
		}
	}
	
	@Override
	protected void signatureList_selectedItemChanged(Signature signature) throws InvocationTargetException, IllegalAccessException, IllegalArgumentException, ClassNotFoundException, InstantiationException, NoSuchMethodException, SecurityException, JpkiException, IOException, NoSuchFieldException {
		PDDocument document = pdfPane.getDocument();
		if(document == null) {
			Platform.runLater(() -> {
				signatureList.getSelectionModel().clearSelection();
			});
			return;
		}
		int pageIndex = pdfPane.getPageIndex();

		float scale = pdfPane.scaleProperty().get();
		if(signature != null && signature.isVisible() && scale > 0f) {
			signatureImageView.setImage(signature.getImage(scale));
			pdfPane.setCursor(Cursor.NONE);
		} else {
			signatureImageView.setImage(null);
			pdfPane.setCursor(Cursor.DEFAULT);
		}
		
		if(signature != null && !signature.isVisible()) {
			signatureList.refresh();
			ButtonType result = Dialogs.showConfirmation(stage, APP_WINDOW_TITLE, "印影を使わずに電子署名をしますか？");
			if(result != ButtonType.OK) {
				Platform.runLater(() -> {
					signatureList.getSelectionModel().clearSelection();
				});
				return;
			}
			Platform.runLater(() -> {
				signatureList.getSelectionModel().clearSelection();
				signatureList.refresh();
				try {
					addInvisibleSignature(document, pageIndex, signature);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}
	
	@Override
	protected void signatureList_add() throws NoSuchAlgorithmException, IOException {
		SignatureDialog dialog = new SignatureDialog(stage, null);
		Optional<Signature> opt = dialog.showAndWait();
		if(opt.isPresent()) {
			Signature signature = opt.get();
			signatureList.getItems().add(signature);
			Signature.save(signatureList.getItems().subList(0, signatureList.getItems().size()));
		}
	}
	
	@Override
	protected void signatureList_edit(Signature signature) throws IOException, NoSuchAlgorithmException {
		if(signature == null) {
			return;
		}
		SignatureDialog dialog = new SignatureDialog(stage, signature);
		Optional<Signature> opt = dialog.showAndWait();
		if(opt.isPresent()) {
			Signature s = opt.get();
			signature.setTitle(s.getTitle());
			signature.setWidthMillis(s.getWidthMillis());
			signature.setHeightMillis(s.getHeightMillis());
			signature.setImage(new ByteArrayInputStream(s.getImageBytes()));
			Signature.save(signatureList.getItems().subList(0, signatureList.getItems().size()));
			signatureList.refresh();
		}
	}
	
	@Override
	protected void signatureList_delete(Signature signature) throws NoSuchAlgorithmException, IOException {
		if(signature == null) {
			return;
		}
		
		ButtonType result = Dialogs.showConfirmation(stage, "印影の削除", signature.getTitle() + " を削除しますか？");
		if(result == ButtonType.OK) {
			File file = signature.getFile();
			if(file != null) {
				boolean isShared = false;
				for(Signature s : signatureList.getItems()) {
					if(s != signature) {
						File f = s.getFile();
						if(f != null && f.getName().equalsIgnoreCase(file.getName())) {
							isShared = true;
						}
					}
				}
				if(!isShared) {
					file.delete();
				}
			}
			signatureList.getSelectionModel().clearSelection();
			signatureList.getItems().remove(signature);
			Signature.save(signatureList.getItems().subList(0, signatureList.getItems().size()));
		}
	}
	
	protected void updatePagerButtons(PDDocument document, int pageIndex) {
		if(document == null || pageIndex < 0) {
			menuPdfFirst.setDisable(true);
			menuPdfPrevious.setDisable(true);
			menuPdfNext.setDisable(true);
			menuPdfLast.setDisable(true);
			menuPdfPageNumber.setText("");
			
			menuPdfFirst.setVisible(false);
			menuPdfPrevious.setVisible(false);
			menuPdfNext.setVisible(false);
			menuPdfLast.setVisible(false);
			menuPdfPageNumber.setVisible(false);
			return;
		} else {
			menuPdfFirst.setVisible(true);
			menuPdfPrevious.setVisible(true);
			menuPdfNext.setVisible(true);
			menuPdfLast.setVisible(true);
			menuPdfPageNumber.setVisible(true);
		}
		if(pageIndex > 0) {
			menuPdfFirst.setDisable(false);
			menuPdfPrevious.setDisable(false);
		} else {
			menuPdfFirst.setDisable(true);
			menuPdfPrevious.setDisable(true);
		}
		if(pageIndex + 1 < document.getNumberOfPages()) {
			menuPdfNext.setDisable(false);
			menuPdfLast.setDisable(false);
		} else {
			menuPdfNext.setDisable(true);
			menuPdfLast.setDisable(true);
		}
		pdfPageNumberLabel.setText((pageIndex + 1) + " / " + document.getNumberOfPages());
	}
	
	protected void addInvisibleSignature(PDDocument document, int pageIndex, Signature signature) throws InvocationTargetException, IllegalAccessException, IllegalArgumentException, JpkiException, IOException, ClassNotFoundException, InstantiationException, NoSuchMethodException, SecurityException, NoSuchFieldException {
		File tmp = null;
		OutputStream output = null;
		JpkiWrapper jpki = null;
		try {
			File dir = Util.getMyDataDirectory();
			if(!dir.exists()) {
				dir.mkdirs();
			}
			tmp = new File(dir, "output.tmp");
			output = new FileOutputStream(tmp);
			jpki = new JpkiWrapper();
			jpki.setApplicationName(APP_TITLE);
			if(APP_VERSION != null) {
				jpki.setApplicationVersion(APP_VERSION);
			}
			jpki.addSignature(output, document);
			output.close();
			output = null;
			
			pdf_open(tmp, pageIndex);
			
			Map<ButtonType, String> captions = new HashMap<ButtonType, String>();
			captions.put(ButtonType.OK, "はい");
			captions.put(ButtonType.CANCEL, "いいえ");
			ButtonType result = Dialogs.show(AlertType.CONFIRMATION, stage, Dialogs.IMAGE_SUCCESS, APP_WINDOW_TITLE, "署名が完了しました。\nファイルに名前を付けて保存しますか？", captions);
			if(result == ButtonType.OK) {
				Platform.runLater(() -> {
					menu_save();
				});
			}
		} catch(JpkiException e) {
			if(e.getWinErrorCode() == JpkiException.SCARD_W_CANCELLED_BY_USER) {
				//ユーザーがキャンセル操作をした場合はダイアログを表示しません。
			} else {
				Dialogs.showError(stage, APP_WINDOW_TITLE, e.getLocalizedMessage());
			}
		} catch(IOException e) {
			Dialogs.showError(stage, APP_WINDOW_TITLE, e.getLocalizedMessage());
		} finally {
			if(output != null) {
				try { output.close(); } catch(Exception e) {}
			}
		}
	}
	
	protected void addVisibleSignature(PDDocument document, int pageIndex, Signature signature, SignatureOptions options) throws InvocationTargetException, IllegalAccessException, IllegalArgumentException, JpkiException, IOException, ClassNotFoundException, InstantiationException, NoSuchMethodException, SecurityException, NoSuchFieldException {
		File tmp = null;
		OutputStream output = null;
		JpkiWrapper jpki = null;
		try {
			File dir = Util.getMyDataDirectory();
			if(!dir.exists()) {
				dir.mkdirs();
			}
			tmp = new File(dir, "output.tmp");
			output = new FileOutputStream(tmp);
			jpki = new JpkiWrapper();
			jpki.setApplicationName(APP_TITLE);
			if(APP_VERSION != null) {
				jpki.setApplicationVersion(APP_VERSION);
			}
			jpki.addSignature(output, document, options);
			output.close();
			output = null;
			
			pdf_open(tmp, pageIndex);
			
			Map<ButtonType, String> captions = new HashMap<ButtonType, String>();
			captions.put(ButtonType.OK, "はい");
			captions.put(ButtonType.CANCEL, "いいえ");
			ButtonType result = Dialogs.show(AlertType.CONFIRMATION, stage, Dialogs.IMAGE_SUCCESS, APP_WINDOW_TITLE, "署名が完了しました。\nファイルに名前を付けて保存しますか？", captions);
			if(result == ButtonType.OK) {
				Platform.runLater(() -> {
					menu_save();
				});
			}
		} catch(JpkiException e) {
			if(e.getWinErrorCode() == JpkiException.SCARD_W_CANCELLED_BY_USER) {
				//ユーザーがキャンセル操作をした場合はダイアログを表示しません。
			} else {
				Dialogs.showError(stage, APP_WINDOW_TITLE, e.getLocalizedMessage());
			}
		} catch(IOException e) {
			Dialogs.showError(stage, APP_WINDOW_TITLE, e.getLocalizedMessage());
		} finally {
			if(output != null) {
				try { output.close(); } catch(Exception e) {}
			}
		}
	}
	
	public static double mm2px(double mm) {
		return mm * 72.0 / 25.4;
	}
	
}
