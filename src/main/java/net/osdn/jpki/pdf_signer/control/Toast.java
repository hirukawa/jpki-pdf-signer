package net.osdn.jpki.pdf_signer.control;

import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import net.osdn.util.javafx.Unchecked;
import net.osdn.util.javafx.fxml.Fxml;

public class Toast extends Pane {

	public static final Color COLOR_ERROR   = Color.rgb(249, 38, 114);
	public static final Color COLOR_SUCCESS = Color.rgb(166, 226, 46);
	private static final Duration DURATION = Duration.millis(5000);

	private final Data EMPTY_DATA = new Data(null, null, null, null);

	@FXML private Label lblTitle;
	@FXML private Label lblMessage;
	private Runnable action;

	private TranslateTransition SHOW_ANIMATION;
	private TranslateTransition HIDE_ANIMATION;
	private TranslateTransition currentTransition;
	private Data currentData;
	private Data nextData;

	public Toast() {
		Fxml.load(this, this);

		//
		// bindings
		//

		//
		// event handlers
		//

		// SHOW_ANIMATION
		SHOW_ANIMATION = new TranslateTransition();
		SHOW_ANIMATION.setNode(this);
		SHOW_ANIMATION.setInterpolator(Interpolator.EASE_OUT);
		SHOW_ANIMATION.fromYProperty().bind(Bindings.add(heightProperty(), 80.0));
		SHOW_ANIMATION.durationProperty().bind(Bindings.createObjectBinding(()-> {
			return Duration.millis(SHOW_ANIMATION.getFromY() * 5.0);
		}, SHOW_ANIMATION.fromYProperty()));
		SHOW_ANIMATION.setToY(0);
		SHOW_ANIMATION.setOnFinished(event -> {
			currentTransition = (nextData != null) ? getTransition(nextData) : null;
			if(currentTransition != null) {
				currentTransition.play();
			} else {
				Data data = currentData;
				new Timeline(new KeyFrame(DURATION, onFinished -> {
					if(data == currentData) {
						//hide
						show(null,null, null);
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
		HIDE_ANIMATION = new TranslateTransition(Duration.millis(5000));
		HIDE_ANIMATION.setNode(this);
		HIDE_ANIMATION.setInterpolator(Interpolator.EASE_IN);
		HIDE_ANIMATION.setFromY(0);
		HIDE_ANIMATION.toYProperty().bind(Bindings.add(heightProperty(), 80.0));
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
		setOnMouseClicked(event -> Unchecked.execute(() -> {
			if(action != null) {
				action.run();
			}
		}));
	}

	public void hide() {
		show(null, null, null);
	}

	public void show(String title, String message) {
		show(null, title, message);
	}

	public void showError(String title, String message) {
		show(COLOR_ERROR, title, message);
	}

	public void show(Color color, String title, String message) {
		show(color, title, message, null);
	}

	public void show(Color color, String title, String message, Runnable action) {
		if(!Platform.isFxApplicationThread()) {
			Platform.runLater(()-> {
				show(color, title, message, action);
			});
			return;
		}
		Data data = new Data(color, title, message, action);
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
		action = data.action;
		if(data.isEmpty) {
			return HIDE_ANIMATION;
		} else {
			if(data.color == null) {
				lblTitle.setTextFill(COLOR_SUCCESS);
			} else {
				lblTitle.setTextFill(data.color);
			}
			if(data.title == null || data.title.length() == 0) {
				lblTitle.setText("");
			} else {
				lblTitle.setText(data.title);
			}
			lblMessage.setText(data.message != null ? data.message : "");
			layout();
			return SHOW_ANIMATION;
		}
	}

	private class Data {

		public Color color;
		public String title;
		public String message;
		public Runnable action;
		public boolean isEmpty;

		public Data(Color color, String title, String message, Runnable action) {
			this.color = color;
			this.title = title;
			this.message = message;
			this.isEmpty = (title == null || title.isEmpty()) && (message == null || message.isEmpty());
			this.action = this.isEmpty ? null : action;
		}
	}
}
