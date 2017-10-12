package net.osdn.jpki.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

public class MenuLabel extends Button {

	public static final Color COLOR_ACCENT = Color.rgb(0, 150, 201);
	
	private ImageView imgView1;
	private ImageView imgView2;
	
	public MenuLabel(Image image1, Image image2, String text) {
		super(text);
		if(image1 != null) {
			imgView1 = new ImageView(image1);
		}
		if(image2 != null) {
			imgView2 = new ImageView(image2);
		}
		setGraphic(imgView1);
		setFocusTraversable(false);
		setOpacity(1.0);
		setPadding(new Insets(0));
		setBackgroundColor(Color.TRANSPARENT);
		
		hoverProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				System.out.println("hover=" + newValue);
				if(newValue) {
					setBackgroundColor(COLOR_ACCENT);
					setGraphic(imgView2);
				} else {
					setBackgroundColor(Color.TRANSPARENT);
					setGraphic(imgView1);
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
