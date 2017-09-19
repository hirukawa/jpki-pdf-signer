package net.osdn.jpki.ui;

import java.io.File;
import java.net.URL;
import java.util.List;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import net.osdn.jpki.Main;
import net.osdn.jpki.Resources;

public class MainLayout extends Application {
	private static final int PREF_PDF_PANE_WIDTH = 612;
	private static final int PREF_PDF_PANE_HEIGHT = 841;
	
	public static void main(String[] args) {
		launch(args);
	}
	
	protected MenuBar menuBar;
	protected MenuItem menuFileOpen;
	protected MenuItem menuFileSave;
	protected MenuItem menuFileExit;
	protected Menu menuPdfFirst;
	protected Menu menuPdfPrevious;
	protected Menu menuPdfNext;
	protected Menu menuPdfLast;
	protected Menu menuPdfPageNumber;
	protected Label pdfPageNumberLabel;
	protected PdfPane pdfPane;
	protected ImageView signatureImageView;
	protected ListView<Signature> signatureList;
	
	@Override
	public void start(Stage stage) throws Exception {
		stage.getIcons().add(Resources.IMAGE_APPLICATION_16PX);
		stage.setTitle(Main.APP_TITLE + ((Main.APP_VERSION != null) ? (" " + Main.APP_VERSION) : ""));
		stage.setMinWidth(640);
		stage.setMinHeight(380);
		stage.setScene(createScene());
		
		Rectangle2D screen = Screen.getPrimary().getVisualBounds();
		if(screen.getWidth() >= PREF_PDF_PANE_WIDTH && screen.getHeight() >= PREF_PDF_PANE_HEIGHT) {
			pdfPane.setPrefSize(PREF_PDF_PANE_WIDTH, PREF_PDF_PANE_HEIGHT);
		} else {
			pdfPane.setPrefSize(PREF_PDF_PANE_WIDTH / 2, PREF_PDF_PANE_HEIGHT / 2);
		}
		
		signatureList.getItems().add(Signature.getInvisibleSignature());
		signatureList.getItems().addAll(Signature.load());
		
		stage.show();
	}
	
	protected Scene createScene() {
		
		menuBar = createMenuBar();

		pdfPane = new PdfPane();
		pdfPane.setOnMouseEntered(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				try {
					pdf_mouseEntered(event);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		pdfPane.setOnMouseExited(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				try {
					pdf_mouseExited(event);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		pdfPane.setOnMouseMoved(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				try {
					pdf_mouseMoved(event);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		pdfPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				try {
					pdf_mouseClicked(event);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		pdfPane.scaleProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				try {
					pdf_scaleChanged(newValue.floatValue());
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});

		signatureImageView = new ImageView();
		signatureImageView.setVisible(false);
		pdfPane.getChildren().add(signatureImageView);
		
		signatureList = new ListView<Signature>();
		signatureList.getStyleClass().add("signature-list");
		signatureList.setFocusTraversable(false);
		
		// 印影のリストは、(1) PDFを表示していないとき左クリック無効 (2) 右クリックでの選択無効
		signatureList.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if(event.isPrimaryButtonDown() && pdfPane.getDocument() == null) {
					event.consume();
				} else if(event.isSecondaryButtonDown()) {
					event.consume();
				}
			}
		});
		signatureList.setCellFactory(new Callback<ListView<Signature>, ListCell<Signature>>() {
			@Override
			public ListCell<Signature> call(ListView<Signature> param) {
				return new SignatureListCell(new ListCellUpdateItemListener<Signature>() {
					@Override
					public void updateItem(ListCell<Signature> cell, Signature item, boolean empty) {
						if(empty || !item.isVisible()) {
							MenuItem menuItemAdd = new MenuItem("追加", new ImageView(Resources.IMAGE_ADD_16PX));
							menuItemAdd.setOnAction(event -> {
								try {
									signatureList_add();
								} catch(Exception e) {
									e.printStackTrace();
								}
							});
							ContextMenu contextMenu = new ContextMenu(menuItemAdd);
							cell.setContextMenu(contextMenu);
						} else {
							MenuItem menuItemEdit = new MenuItem("編集", new ImageView(Resources.IMAGE_EDIT_16PX));
							menuItemEdit.setOnAction(event -> {
								try {
									signatureList_edit(item);
								} catch(Exception e) {
									e.printStackTrace();
								}
							});
							MenuItem menuItemDelete = new MenuItem("削除", new ImageView(Resources.IMAGE_DELETE_16PX));
							menuItemDelete.setOnAction(event -> {
								try {
									signatureList_delete(item);
								} catch(Exception e) {
									e.printStackTrace();
								}
								
							});
							ContextMenu contextMenu = new ContextMenu(menuItemEdit, menuItemDelete);
							cell.setContextMenu(contextMenu);
						}
					}
				});
			}
		});
		signatureList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Signature>() {
			@Override
			public void changed(ObservableValue<? extends Signature> observable, Signature oldValue, Signature newValue) {
				try {
					signatureList_selectedItemChanged(newValue);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		BorderPane bp1 = new BorderPane();
		bp1.setCenter(pdfPane);
		bp1.setRight(signatureList);
		bp1.setTop(menuBar);
		
		Scene scene = new Scene(bp1);
		scene.setOnDragOver(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				try {
					onDragOver(event);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		scene.setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(DragEvent event) {
				try {
					onDragDropped(event);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.HOME), new Runnable() {
			@Override
			public void run() {
				if(!menuPdfFirst.isDisable()) {
					try {
						pdf_moveFirstPage();
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		scene.getAccelerators().put(
			new KeyCombinations(
				new KeyCodeCombination(KeyCode.PAGE_UP),
				new KeyCodeCombination(KeyCode.UP),
				new KeyCodeCombination(KeyCode.KP_UP),
				new KeyCodeCombination(KeyCode.LEFT),
				new KeyCodeCombination(KeyCode.KP_LEFT)
			),
			new Runnable() {
				@Override
				public void run() {
					if(!menuPdfPrevious.isDisable()) {
						try {
							pdf_movePreviousPage();
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		);
		scene.getAccelerators().put(
			new KeyCombinations(
				new KeyCodeCombination(KeyCode.PAGE_DOWN),
				new KeyCodeCombination(KeyCode.DOWN),
				new KeyCodeCombination(KeyCode.KP_DOWN),
				new KeyCodeCombination(KeyCode.RIGHT),
				new KeyCodeCombination(KeyCode.KP_RIGHT)
			),
			new Runnable() {
				@Override
				public void run() {
					if(!menuPdfNext.isDisable()) {
						try {
							pdf_moveNextPage();
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		);
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.END), new Runnable() {
			@Override
			public void run() {
				if(!menuPdfLast.isDisable()) {
					try {
						pdf_moveLastPage();
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		String pkg = MainLayout.class.getPackage().getName();
		URL url = MainLayout.class.getResource("/" + pkg.replace('.', '/') + "/stylesheet.css");
		if(url != null) {
			scene.getStylesheets().add(url.toExternalForm());
		}
		return scene;
	}
	
	protected MenuBar createMenuBar() {
		MenuBar menuBar = new MenuBar();

		Menu menuFile = new Menu("ファイル");
		menuFile.setId("file");
		menuFileOpen = new MenuItem("開く...", new ImageView(Resources.IMAGE_FILE_16PX));
		menuFileOpen.setId("open");
		menuFileOpen.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
		menuFileOpen.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					menu_open();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		menuFileSave = new MenuItem("名前を付けて保存...", new ImageView(Resources.IMAGE_SAVE_16PX));
		menuFileSave.setId("save");
		menuFileSave.setDisable(true);
		menuFileSave.setAccelerator(KeyCombination.keyCombination("Ctrl+S"));
		menuFileSave.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					menu_save();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		menuFileExit = new MenuItem("終了");
		menuFileExit.setId("exit");
		menuFileExit.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					menu_exit();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		menuFile.getItems().addAll(
			menuFileOpen,
			menuFileSave,
			new SeparatorMenuItem(),
			menuFileExit
		);
		
		pdfPageNumberLabel = new Label("");
		StackPane sp = new StackPane(pdfPageNumberLabel);
		sp.setMinWidth(40);
		StackPane.setAlignment(pdfPageNumberLabel, Pos.CENTER);
		menuPdfPageNumber = new Menu("", sp);
		menuPdfPageNumber.setId("pdf-page-number");
		
		PagerButton btnFirst = new PagerButton(Resources.IMAGE_PAGE_FIRST_BLACK_16PX, Resources.IMAGE_PAGE_FIRST_WHITE_16PX);
		btnFirst.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					pdf_moveFirstPage();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		menuPdfFirst = new PagerMenu(btnFirst);
		
		PagerButton btnPrevious = new PagerButton(Resources.IMAGE_PAGE_PREVIOUS_BLACK_16PX, Resources.IMAGE_PAGE_PREVIOUS_WHITE_16PX);
		btnPrevious.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					pdf_movePreviousPage();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		menuPdfPrevious = new PagerMenu(btnPrevious);
		
		PagerButton btnNext = new PagerButton(Resources.IMAGE_PAGE_NEXT_BLACK_16PX, Resources.IMAGE_PAGE_NEXT_WHITE_16PX);
		btnNext.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					pdf_moveNextPage();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		menuPdfNext = new PagerMenu(btnNext);

		PagerButton btnLast = new PagerButton(Resources.IMAGE_PAGE_LAST_BLACK_16PX, Resources.IMAGE_PAGE_LAST_WHITE_16PX);
		btnLast.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					pdf_moveLastPage();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		menuPdfLast = new PagerMenu(btnLast);
		
		menuPdfFirst.setDisable(true);
		menuPdfPrevious.setDisable(true);
		menuPdfNext.setDisable(true);
		menuPdfLast.setDisable(true);
		menuPdfPageNumber.setDisable(true);
		menuPdfFirst.setVisible(false);
		menuPdfPrevious.setVisible(false);
		menuPdfNext.setVisible(false);
		menuPdfLast.setVisible(false);
		menuPdfPageNumber.setVisible(false);
		
		menuBar.getMenus().addAll(
			menuFile,
			menuPdfFirst,
			menuPdfPrevious,
			menuPdfPageNumber,
			menuPdfNext,
			menuPdfLast
		);
		return menuBar;
	}
	
	protected void onDragOver(DragEvent event) {
		if(event.getDragboard().hasFiles()) {
			event.acceptTransferModes(TransferMode.COPY);
		} else {
			event.consume();
		}
	}
	
	protected void onDragDropped(DragEvent event) {
		Dragboard db = event.getDragboard();
		boolean success = false;
		if(db.hasFiles()) {
			List<File> files = db.getFiles();
			if(files.size() == 1) {
				File file = files.get(0);
				String ext = file.getName().toLowerCase();
				int i = ext.lastIndexOf('.');
				if(i >= 0) {
					ext = ext.substring(i);
				}
				if(ext.equals(".pdf")) {
					try {
						pdf_open(file, 0);
					} catch(Exception e) {
						e.printStackTrace();
					}
					success = true;
				} else if(ext.equals(".yml") || ext.equals(".yaml")) {
					
				}
			}
		}
		event.setDropCompleted(success);
		event.consume();
	}
	
	protected void menu_open() throws Exception {
	}
	
	protected void menu_save() throws Exception {
	}
	
	protected void menu_exit() throws Exception {
		
	}

	protected void pdf_open(File file, int pageIndex) throws Exception {
	}
	
	protected void pdf_moveFirstPage() throws Exception {
	}
	
	protected void pdf_movePreviousPage() throws Exception {
	}
	
	protected void pdf_moveNextPage() throws Exception {
	}
	
	protected void pdf_moveLastPage() throws Exception {
	}
	
	protected void pdf_mouseEntered(MouseEvent event) throws Exception {
	}
	
	protected void pdf_mouseExited(MouseEvent event) throws Exception {
	}
	
	protected void pdf_mouseMoved(MouseEvent event) throws Exception {
	}
	
	protected void pdf_mouseClicked(MouseEvent event) throws Exception {
	}
	
	protected void pdf_scaleChanged(float scale) throws Exception {
	}
	
	protected void signatureList_selectedItemChanged(Signature signature) throws Exception {
	}
	
	protected void signatureList_add() throws Exception {
	}
	
	protected void signatureList_edit(Signature signature) throws Exception {
	}
	
	protected void signatureList_delete(Signature signature) throws Exception {
	}
}
