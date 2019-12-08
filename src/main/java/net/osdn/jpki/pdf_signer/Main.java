package net.osdn.jpki.pdf_signer;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.osdn.jpki.pdf_signer.control.LicenseDialog;
import net.osdn.jpki.pdf_signer.control.Toast;
import net.osdn.jpki.wrapper.JpkiException;
import net.osdn.jpki.wrapper.JpkiWrapper;
import net.osdn.util.javafx.application.SingletonApplication;
import net.osdn.util.javafx.concurrent.Async;
import net.osdn.util.javafx.fxml.Fxml;
import net.osdn.util.javafx.scene.control.Dialogs;
import net.osdn.util.javafx.scene.control.pdf.Pager;
import net.osdn.util.javafx.scene.control.pdf.PdfView;
import net.osdn.util.javafx.stage.StageUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class Main extends SingletonApplication implements Initializable {

    public static final String APPLICATION_NAME = "JPKI PDF SIGNER";
    public static final String APPLICATION_VERSION;

    static {
        System.setProperty(
                "org.apache.commons.logging.LogFactory", "net.osdn.jpki.pdf_signer.LogFilter");
        LogFilter.setLevel("org.apache.pdfbox", LogFilter.Level.ERROR);
        LogFilter.setLevel("org.apache.fontbox", LogFilter.Level.ERROR);

        int[] version = Datastore.getApplicationVersion();
        if(version != null) {
            if (version[2] == 0) {
                APPLICATION_VERSION = String.format("%d.%d", version[0], version[1]);
            } else {
                APPLICATION_VERSION = String.format("%d.%d.%d", version[0], version[1], version[2]);
            }
        } else {
            APPLICATION_VERSION = "";
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = Fxml.load(this);

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/img/app-icon-16px.png")));
        primaryStage.setTitle(APPLICATION_NAME + " " + APPLICATION_VERSION);

        Scene scene = new Scene(root);
        scene.setOnDragOver(wrap(this::scene_onDragOver));
        scene.setOnDragDropped(wrap(this::scene_onDragDropped));
        scene.getAccelerators().putAll(pager.createDefaultAccelerators());

        StageUtil.setRestorable(primaryStage, Preferences.userNodeForPackage(getClass()));
        primaryStage.setOnShown(event -> { Platform.runLater(wrap(this::stage_onReady)); });
        primaryStage.setOnCloseRequest(wrap(this::stage_onCloseRequest));
        primaryStage.setMinWidth(448.0);
        primaryStage.setMinHeight(396.0);
        primaryStage.setOpacity(0.0);
        primaryStage.setScene(scene);
        primaryStage.show();

        Thread.currentThread().setUncaughtExceptionHandler(handler);
    }

    protected Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if(e instanceof Exception) {
                showException((Exception)e);
            } else if(Thread.getDefaultUncaughtExceptionHandler() != null) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(t, e);
            } else {
                e.printStackTrace();
            }
        }
    };

    protected void showException(Throwable exception) {
        exception.printStackTrace();

        Runnable r = ()-> {
            String title;
            if(exception instanceof JpkiException) {
                title = "エラー";
            } else {
                title = exception.getClass().getName();
            }
            String message = exception.getLocalizedMessage();
            toast.show(Toast.RED, title, message, null);
        };
        if(Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    @FXML Toast               toast;
    @FXML MenuItem            menuFileOpen;
    @FXML MenuItem            menuFileSave;
    @FXML MenuItem            menuFileExit;
    @FXML MenuItem            menuHelpAbout;
    @FXML Pager               pager;
    @FXML PdfView             pdfView;
    @FXML ImageView           ivCursor;
    @FXML Button              btnRemoveSignature;
    @FXML Button              btnEditSignature;
    @FXML Button              btnAddSignature;
    @FXML ListView<Signature> lvSignature;
    ObjectBinding<Signature>  signatureBinding;
    ObjectProperty<File>      signedTemporaryFileProperty = new SimpleObjectProperty<File>();
    File                      inputFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lvSignature.setCellFactory(new SignatureListCell.Factory(this));

        //
        // event handlers
        //
        menuFileOpen.setOnAction(wrap(this::menuFileOpen_onAction));
        menuFileSave.setOnAction(wrap(this::menuFileSave_onAction));
        menuFileExit.setOnAction(wrap(this::menuFileExit_onAction));
        menuHelpAbout.setOnAction(wrap(this::menuHelpAbout_onAction));
        pdfView.setOnMouseMoved(wrap(this::pdfView_onMouseMoved));
        pdfView.setOnMouseClicked(wrap(this::pdfView_onMouseClicked));
        btnRemoveSignature.setOnAction(wrap(this::btnRemoveSignature_onAction));
        btnEditSignature.setOnAction(wrap(this::btnEditSignature_onAction));
        btnAddSignature.setOnAction(wrap(this::btnAddSignature_onAction));

        //
        // bindings
        //
        signatureBinding = Bindings
                .when(lvSignature.getSelectionModel().selectedItemProperty().isNotNull())
                .then(lvSignature.getSelectionModel().selectedItemProperty())
                .otherwise(Signature.EMPTY);

        menuFileSave.disableProperty().bind(signedTemporaryFileProperty.isNull());

        pager.maxPageIndexProperty().bind(pdfView.maxPageIndexProperty());
        pager.pageIndexProperty().bindBidirectional(pdfView.pageIndexProperty());

        pdfView.cursorProperty().bind(Bindings
                .when(pdfView.documentProperty().isNotNull()
                        .and(ivCursor.visibleProperty())
                        .and(ivCursor.imageProperty().isNotNull()))
                .then(Cursor.NONE)
                .otherwise(Cursor.DEFAULT));

        ivCursor.imageProperty().bind(Bindings.select(signatureBinding, "image"));
        ivCursor.scaleXProperty().bind(
                pdfView.renderScaleProperty().multiply(Bindings.selectDouble(signatureBinding, "imageScaleX")));
        ivCursor.scaleYProperty().bind(
                pdfView.renderScaleProperty().multiply(Bindings.selectDouble(signatureBinding, "imageScaleY")));
        ivCursor.visibleProperty().bind(Bindings
                .selectBoolean(signatureBinding, "visible")
                .and(pdfView.hoverProperty()));

        btnRemoveSignature.disableProperty().bind(
                Bindings.not(Bindings.selectBoolean(signatureBinding, "visible")));
        btnEditSignature.disableProperty().bind(
                Bindings.not(Bindings.selectBoolean(signatureBinding, "visible")));

        toast.maxWidthProperty().bind(getPrimaryStage().widthProperty().subtract(32));
        toast.maxHeightProperty().bind(getPrimaryStage().heightProperty().subtract(32));
    }

    void stage_onReady() {
        getPrimaryStage().setOpacity(1.0);

        lvSignature.getItems().clear();
        lvSignature.getItems().add(Signature.INVISIBLE);
        Async.execute(() -> {
            return Datastore.loadSignatures();
        }).onSucceeded(signatures -> {
            for (Signature signature : signatures) {
                lvSignature.getItems().add(signature);
            }
            Platform.runLater(() -> {
                checkJpkiAvailability();
            });
        }).onFailed(exception -> {
            showException(exception);
        });
    }

    void stage_onCloseRequest(WindowEvent event) {
        Platform.exit();
    }

    void scene_onDragOver(DragEvent event) {
        if(isAcceptable(getFile(event))) {
            event.acceptTransferModes(TransferMode.COPY);
        } else {
            event.consume();
        }
    }

    void scene_onDragDropped(DragEvent event) {
        File file = getFile(event);
        if(isAcceptable(file)) {
            getPrimaryStage().toFront();
            toast.hide();
            signedTemporaryFileProperty.set(null);
            inputFile = file;
            pdfView.load(file);
            event.setDropCompleted(true);
        }
        event.consume();
    }

    void menuFileOpen_onAction(ActionEvent event) {
        toast.hide();
        Preferences preferences = Preferences.userNodeForPackage(getClass());

        FileChooser fc = new FileChooser();
        fc.setTitle("開く");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        String lastOpenDirectory = preferences.get("lastOpenDirectory", null);
        if(lastOpenDirectory != null) {
            File dir = new File(lastOpenDirectory);
            if(dir.isDirectory()) {
                fc.setInitialDirectory(dir);
            }
        }
        File file = fc.showOpenDialog(getPrimaryStage());
        if(file != null) {
            preferences.put("lastOpenDirectory", file.getParentFile().getAbsolutePath());
            if(isAcceptable(file)) {
                signedTemporaryFileProperty.set(null);
                inputFile = file;
                pdfView.load(file);
            }
        }
    }

    void menuFileSave_onAction(ActionEvent event) throws IOException {
        toast.hide();
        String defaultName = inputFile.getName();
        int i = defaultName.lastIndexOf('.');
        if(i > 0) {
            defaultName = defaultName.substring(0, i);
        }
        defaultName += "-signed.pdf";

        FileChooser fc = new FileChooser();
        fc.setTitle("名前を付けて保存");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialDirectory(inputFile.getParentFile());
        fc.setInitialFileName(defaultName);

        File file = fc.showSaveDialog(getPrimaryStage());
        if(file != null) {
            Files.copy(signedTemporaryFileProperty.get().toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    void menuFileExit_onAction(ActionEvent event) {
        Platform.exit();
    }

    void menuHelpAbout_onAction(ActionEvent event) throws IOException {
        String license = Datastore.getLicense();
        LicenseDialog dialog = new LicenseDialog(getPrimaryStage(), license);
        dialog.showAndWait();
    }

    void btnAddSignature_onAction(ActionEvent event) throws IOException {
        toast.hide();
        SignatureDialog dialog = new SignatureDialog(getPrimaryStage(), null);
        Signature newSignature = dialog.showAndWait().orElse(null);
        if(newSignature == null) {
            return;
        }
        lvSignature.getItems().add(newSignature);
        Datastore.saveSignatures(lvSignature.getItems().subList(0, lvSignature.getItems().size()));
    }

    void btnEditSignature_onAction(ActionEvent event) throws IOException {
        toast.hide();
        Signature currentSignature = lvSignature.getSelectionModel().getSelectedItem();
        if(currentSignature == null || currentSignature.getImage() == null) {
            return;
        }
        SignatureDialog dialog = new SignatureDialog(getPrimaryStage(), currentSignature);
        Signature newSignature = dialog.showAndWait().orElse(null);
        if(newSignature == null) {
            return;
        }
        lvSignature.getItems().add(lvSignature.getSelectionModel().getSelectedIndex(), newSignature);
        lvSignature.getItems().remove(lvSignature.getSelectionModel().getSelectedIndex());
        Datastore.saveSignatures(lvSignature.getItems().subList(0, lvSignature.getItems().size()));
    }

    void btnRemoveSignature_onAction(ActionEvent event) throws IOException {
        toast.hide();
        Signature currentSignature = lvSignature.getSelectionModel().getSelectedItem();
        if(currentSignature == null || currentSignature.getImage() == null) {
            return;
        }
        ButtonType result = Dialogs.showConfirmation(getPrimaryStage(),
                "印影の削除",
                currentSignature.getTitle() + " を削除しますか？");
        if(result != ButtonType.YES) {
            return;
        }

        lvSignature.getItems().remove(lvSignature.getSelectionModel().getSelectedIndex());
        lvSignature.getSelectionModel().clearSelection();
        Datastore.saveSignatures(lvSignature.getItems().subList(0, lvSignature.getItems().size()));
    }

    public void lvSignature_cell_onMousePressed(MouseEvent event) throws JpkiException, IOException, ReflectiveOperationException {
        toast.hide();
        @SuppressWarnings("unchecked")
        ListCell<Signature> cell = (ListCell<Signature>)event.getSource();

        //空のセルをクリックしたときにリストビューの選択を解除します。
        if(cell.isEmpty()) {
            lvSignature.getSelectionModel().clearSelection();
            return;
        }

        Signature signature = cell.getItem();
        if(signature != Signature.INVISIBLE) {
            return;
        }

        try {
            if(event.isPrimaryButtonDown() && pdfView.getDocument() != null && checkJpkiAvailability()) {
                ButtonType result = Dialogs.showConfirmation(getPrimaryStage(),
                        "印影を使わずに電子署名しますか？");
                if(result == ButtonType.YES) {
                    PDDocument document = pdfView.getDocument();
                    int pageIndex = pdfView.getPageIndex();
                    SignatureOptions options = null;
                    File tmpFile = sign(document, null, APPLICATION_NAME, APPLICATION_VERSION);
                    if(tmpFile != null) {
                        signedTemporaryFileProperty.set(tmpFile);
                        pdfView.load(tmpFile, pageIndex);

                        result = Dialogs.showConfirmation (getPrimaryStage(),
                                "署名が完了しました。\nファイルに名前を付けて保存しますか？");
                        if(result == ButtonType.YES) {
                            menuFileSave.fire();
                        }
                    }
                }
            }
        } finally {
            lvSignature.getSelectionModel().clearSelection();
        }
    }

    public void lvSignature_cell_onMouseClicked(MouseEvent event) {
        // 左ダブルクリックでない場合は何もしない。
        if(event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2) {
            return;
        }

        @SuppressWarnings("unchecked")
        ListCell<Signature> cell = (ListCell<Signature>)event.getSource();

        // 不可視署名では何もしない。
        Signature signature = cell.getItem();
        if(signature == Signature.INVISIBLE) {
            return;
        }

        if(cell.isEmpty()) {
            // 空のセルがダブルクリックされた場合は「新規」操作を発動します。
            btnAddSignature.fire();
        } else {
            // 可視署名がダブルクリックされた場合は「編集」操作を発動します。
            btnEditSignature.fire();
        }
    }

    void pdfView_onMouseMoved(MouseEvent event) {
        Image image = ivCursor.getImage();
        if(image != null) {
            ivCursor.setLayoutX(event.getX() - (int)(image.getWidth() / 2.0));
            ivCursor.setLayoutY(event.getY() - (int)(image.getHeight() / 2.0));
        }
    }

    void pdfView_onMouseClicked(MouseEvent event) throws  JpkiException, IOException, ReflectiveOperationException {
        toast.hide();

        // 必要な条件を満たしている場合、可視署名を実行します。

        if(event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        Rectangle2D renderBounds = pdfView.getRenderBounds();
        double x = event.getX() - renderBounds.getMinX();
        double y = event.getY() - renderBounds.getMinY();
        if(x < 0.0 || y < 0.0 || x > renderBounds.getWidth() || y > renderBounds.getHeight()) {
            return;
        }

        Signature signature = signatureBinding.get();
        if(!signature.isVisible()) {
            return;
        }

        if(!checkJpkiAvailability()) {
            return;
        }

        PDDocument document = pdfView.getDocument();
        int pageIndex = pdfView.getPageIndex();
        PDRectangle pageMediaBox = document.getPage(pageIndex).getMediaBox();
        double xPt = x * pageMediaBox.getWidth() / renderBounds.getWidth();
        double yPt = y * pageMediaBox.getHeight() / renderBounds.getHeight();

        PDVisibleSignDesigner designer;
        try(InputStream is = new FileInputStream(signature.getFile())) {
            designer = new PDVisibleSignDesigner(is);
            designer.width((float)mm2px(signature.getWidthMillis()));
            designer.height((float)mm2px(signature.getHeightMillis()));
            designer.xAxis((float)xPt - designer.getWidth() / 2);
            designer.yAxis((float)yPt - designer.getHeight() / 2 - pageMediaBox.getHeight());
        }
        PDVisibleSigProperties props = new PDVisibleSigProperties();
        props.setPdVisibleSignature(designer);
        props.visualSignEnabled(true);
        props.page(pageIndex + 1);
        props.buildSignature();

        SignatureOptions options = new SignatureOptions();
        options.setPage(pageIndex);
        options.setVisualSignature(props);

        File tmpFile = sign(document, options, APPLICATION_NAME, APPLICATION_VERSION);
        if(tmpFile != null) {
            signedTemporaryFileProperty.set(tmpFile);
            pdfView.load(tmpFile, pageIndex);

            ButtonType result = Dialogs.showConfirmation(getPrimaryStage(),
                    "署名が完了しました。\nファイルに名前を付けて保存しますか？");
            if(result == ButtonType.YES) {
                menuFileSave.fire();
            }
            lvSignature.getSelectionModel().clearSelection();
        }
    }

    protected File getFile(DragEvent event) {
        if(event.getDragboard().hasFiles()) {
            List<File> files = event.getDragboard().getFiles();
            if(files.size() == 1) {
                return files.get(0);
            }
        }
        return null;
    }

    protected boolean isAcceptable(File file) {
        return file != null && file.getName().matches("(?i).+(\\.pdf)");
    }

    protected boolean checkJpkiAvailability() {
        boolean isAvailable = JpkiWrapper.isAvailable();
        if(!isAvailable) {
            Runnable actionOnClick = wrap(() -> {
                toast.hide();
                Desktop.getDesktop().browse(URI.create("https://www.jpki.go.jp/download/win.html"));
            });
            toast.show(Toast.GREEN, "事前準備", ""
                    + "JPKI 利用者クライアントソフトをインストールしてください。\n"
                    + "ここをクリックするとブラウザーでダウンロードサイトを開きます。",
                    null,
                    actionOnClick);
        }
        return isAvailable;
    }

    protected File sign(PDDocument document, SignatureOptions options, String applicationName, String applicationVersion) throws JpkiException, IOException, ReflectiveOperationException {
        File tmpFile = Datastore.getMyDataDirectory(true).resolve("output.tmp").toFile();
        try (OutputStream output = new FileOutputStream(tmpFile)) {
            output.flush();
            JpkiWrapper jpki = new JpkiWrapper();
            jpki.setApplicationName(applicationName);
            jpki.setApplicationVersion(applicationVersion);
            jpki.addSignature(output, document, options);
            return tmpFile;
        } catch(JpkiException e) {
            //ユーザーがキャンセル操作をした場合はダイアログを表示しません。
            if(e.getWinErrorCode() != JpkiException.SCARD_W_CANCELLED_BY_USER) {
                throw e;
            }
        }
        return null;
    }

    public static double mm2px(double mm) {
        return mm * 72.0 / 25.4;
    }
}
