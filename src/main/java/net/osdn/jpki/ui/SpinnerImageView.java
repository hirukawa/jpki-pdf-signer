package net.osdn.jpki.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SpinnerImageView extends ImageView {

	private Image[] frame = new Image[] {
		new Image(getClass().getResourceAsStream("/img/spinner-01.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-02.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-03.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-04.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-05.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-06.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-07.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-08.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-09.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-10.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-11.png")),
		new Image(getClass().getResourceAsStream("/img/spinner-12.png"))
	};
	private int cycleTime = 750;
	private int frameTime = (int)Math.ceil((double)cycleTime / (double)frame.length);
	private AnimationTimer timer;
	private int previous = -1;
	
	public SpinnerImageView() {
		timer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				int i = (int)((now / 1000000) % cycleTime) / frameTime;
				if(i != previous) {
					setImage(frame[i]);
					previous = i;
				}
			}
		};
		timer.start();
	}
	
	public void start() {
		timer.start();
	}
	
	public void stop() {
		timer.stop();
	}
	
	public double getWidth() {
		return frame[0].getWidth();
	}
	
	public double getHeight() {
		return frame[0].getHeight();
	}
}
