package net.osdn.jpki.pdf_signer.control;

import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class Toast extends StackPane {
	
	public static final Color RED   = Color.rgb(249, 38, 114);
	public static final Color GREEN = Color.rgb(166, 226, 46);
	public static final Color BLUE  = Color.rgb(102, 217, 239);
	
	public static final Duration SHORT_PERSISTENT = Duration.millis(2001);
	public static final Duration SHORT = Duration.millis(2000);
	public static final Duration LONG  = Duration.millis(3500);
	
	private static final Color DEFAULT_COLOR = Color.rgb(240, 240, 240);

	private final Data EMPTY_DATA = new Data(null, null, null, null, null);
	
	private BorderPane borderPane;
	private Button btnClose;
	private Label lblTitle;
	private Label lblMessage;
	private Runnable actionOnClick;
	
	private TranslateTransition SHOW_ANIMATION;
	private TranslateTransition HIDE_ANIMATION;
	private TranslateTransition currentTransition;
	private Data currentData;
	private Data nextData;

	public Toast() {
		getStylesheets().add(Toast.class.getResource("Toast.css").toExternalForm());

		btnClose = new Button("×");
		btnClose.setId("btnClose");
		btnClose.setOnAction(event-> { 
			btnClose_onAction(event);
		});
		
		lblTitle = new Label();
		lblTitle.setId("lblTitle");
		lblTitle.setWrapText(false);
		
		lblMessage = new Label();
		lblMessage.setId("lblMessage");
		lblMessage.setWrapText(true);
		
		ScrollPane scrollPane = new ScrollPane(lblMessage);
		scrollPane.prefViewportHeightProperty().bind(lblMessage.heightProperty());
		
		borderPane = new BorderPane();
		borderPane.setId("background");
		borderPane.centerProperty().set(scrollPane);
		borderPane.paddingProperty().bind(Bindings
				.when(Bindings.isNull(borderPane.topProperty()))
				.then(new Insets(3, 25, 3, 10))
				.otherwise(new Insets(3, 8, 3, 10)));
		
		AnchorPane layer = new AnchorPane(btnClose);
		layer.setPickOnBounds(false);
		AnchorPane.setTopAnchor(btnClose, 0.0);
		AnchorPane.setRightAnchor(btnClose, 0.0);

		getChildren().addAll(borderPane, layer);
		setVisible(false);
		
		maxWidthProperty().addListener((observable, oldValue, newValue)-> {
			double width = newValue.doubleValue()
					- borderPane.getPadding().getLeft()
					- borderPane.getPadding().getRight();
			lblTitle.setMaxWidth(width);
			lblMessage.setMaxWidth(width);
			scrollPane.setMaxWidth(width);
		});
		
		// SHOW_ANIMATION
		SHOW_ANIMATION = new TranslateTransition();
		SHOW_ANIMATION.setNode(this);
		SHOW_ANIMATION.setInterpolator(Interpolator.EASE_OUT);
		SHOW_ANIMATION.fromYProperty().bind(Bindings.add(heightProperty(), 40.0));
		SHOW_ANIMATION.durationProperty().bind(Bindings.createObjectBinding(()-> {
			return Duration.millis(SHOW_ANIMATION.getFromY() * 5.0);
		}, SHOW_ANIMATION.fromYProperty()));
		SHOW_ANIMATION.setToY(0);
		SHOW_ANIMATION.setOnFinished(event -> {
			currentTransition = (nextData != null) ? getTransition(nextData) : null;
			if(currentTransition != null) {
				currentTransition.play();
			} else if(currentData.duration != null && currentData.duration.toMillis() > 0.0) {
				Data data = currentData;
				new Timeline(new KeyFrame(data.duration, onFinished -> {
					if(data == currentData) {
						//hide
						show(null, null, null, null);
					}
				})).play();
			}
			nextData = null;
		});
		SHOW_ANIMATION.statusProperty().addListener((observable, oldValue, newValue)-> {
			if(newValue == Status.RUNNING) {
				setVisible(true);
			}
		});

		// HIDE_ANIMATION
		HIDE_ANIMATION = new TranslateTransition(Duration.millis(3000));
		HIDE_ANIMATION.setNode(this);
		HIDE_ANIMATION.setInterpolator(Interpolator.EASE_IN);
		HIDE_ANIMATION.setFromY(0);
		HIDE_ANIMATION.toYProperty().bind(Bindings.add(heightProperty(), 40.0));
		HIDE_ANIMATION.durationProperty().bind(Bindings.createObjectBinding(()-> {
			return Duration.millis(HIDE_ANIMATION.getToY() * 5.0);
		}, HIDE_ANIMATION.toYProperty()));
		HIDE_ANIMATION.setOnFinished(event -> {
			currentTransition = (nextData != null) ? getTransition(nextData) : null;
			if(currentTransition != null) {
				currentTransition.play();
			} else {
			}
			nextData = null;
		});
		HIDE_ANIMATION.statusProperty().addListener((observable, oldValue, newValue)-> {
			if(newValue == Status.STOPPED) {
				setVisible(false);
			}
		});

		// CLICK ACTION
		setOnMouseClicked(event -> {
			if(actionOnClick != null) {
				actionOnClick.run();
			}
		});
	}
	
	protected void btnClose_onAction(ActionEvent event) {
		show(null, null, null, null, null);
	}
	
	public void hide() {
		if(currentData != null && currentData.isPersistent) {
			return;
		}
		show(null, null, null, null, null);
	}

	public void show(String message) {
		show(null, null, message, null);
	}
	
	public void show(String title, String message) {
		show(null, title, message, null);
	}
	
	public void show(Color color, String title, String message) {
		show(color, title, message, null);
	}
	
	public void show(String title, String message, Duration duration) {
		show(null, title, message, duration);
	}
	
	public void show(Color color, String message, Duration duration) {
		show(color, null, message, duration);
	}

	public void show(Color color, String title, String message, Duration duration) {
		show(color, title, message, duration, null);
	}

	public void show(Color color, String title, String message, Duration duration, Runnable actionOnClick) {
		if(!Platform.isFxApplicationThread()) {
			Platform.runLater(()-> {
				show(color, title, message, duration);
			});
			return;
		}
		Data data = new Data(color, title, message, duration, actionOnClick);
		if(currentTransition == null) {
			if(isVisible()) {
				currentTransition = getTransition(EMPTY_DATA);
				currentTransition.play();
				if(!data.isEmpty) {
					nextData = data;
				}
			} else if(!data.isEmpty) {
				currentTransition = getTransition(data);
				currentTransition.play();
			}
		} else {
			nextData = data;
		}
	}
	
	protected TranslateTransition getTransition(Data data) {
		currentData = data;
		if(data.isEmpty) {
			return HIDE_ANIMATION;
		} else {
			if(data.title == null || data.title.length() == 0) {
				lblTitle.setText("");
			} else {
				lblTitle.setText(data.title);
				lblTitle.setTextFill(data.color != null ? data.color : DEFAULT_COLOR);
			}
			if(lblTitle.getText().isEmpty() && borderPane.getChildren().contains(lblTitle)) {
				borderPane.setTop(null);
			} else if(!lblTitle.getText().isEmpty() && !borderPane.getChildren().contains(lblTitle)) {
				borderPane.setTop(lblTitle);
			}
			lblMessage.setTextFill(lblTitle.getText().isEmpty() ? data.color : DEFAULT_COLOR);
			lblMessage.setText(data.message != null ? data.message : "");
			lblMessage.setCursor(data.actionOnClick != null ? Cursor.HAND : Cursor.DEFAULT);
			layout();
			actionOnClick = data.actionOnClick;
			return SHOW_ANIMATION;
		}
	}
	
	private class Data {
		
		public Color color;
		public String title;
		public String message;
		public Duration duration;
		public Runnable actionOnClick;
		public boolean isEmpty;
		public boolean isPersistent;
		
		public Data(Color color, String title, String message, Duration duration, Runnable actionOnClick) {
			this.color = color;
			this.title = title;
			this.message = message;
			this.duration = duration;
			this.actionOnClick = actionOnClick;
			this.isEmpty = (title == null || title.isEmpty()) && (message == null || message.isEmpty());
			this.isPersistent = (duration == SHORT_PERSISTENT);
		}
	}
}
