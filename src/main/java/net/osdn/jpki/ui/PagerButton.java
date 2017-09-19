package net.osdn.jpki.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

public class PagerButton extends Button {

	public static final Color COLOR_ACCENT = Color.rgb(0, 150, 201);
	
	private ImageView imgView1;
	private ImageView imgView2;
	
	public PagerButton(Image image1, Image image2) {
		if(image1 != null) {
			imgView1 = new ImageView(image1);
		}
		if(image2 != null) {
			imgView2 = new ImageView(image2);
		}
		setGraphic(imgView1);
		setFocusTraversable(false);
		setOpacity(1.0);
		setPrefSize(25, 25);
		setBackgroundColor(Color.TRANSPARENT);
		
		hoverProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					setBackgroundColor(COLOR_ACCENT);
				} else {
					setBackgroundColor(Color.TRANSPARENT);
				}
			}
		});
		
		addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				setGraphic(imgView2);
			}
		});
		
		addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				setGraphic(imgView1);
			}
		});
	}
	
	public void setBackgroundColor(Color color) {
		setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
	}
}
