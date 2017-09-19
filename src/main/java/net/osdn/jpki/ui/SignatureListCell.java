package net.osdn.jpki.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class SignatureListCell extends ListCell<Signature> {

	public static final int MAX_IMAGE_WIDTH = (int)Math.ceil(30.0 * 72.0 / 25.4);
	public static final int MAX_IMAGE_HEIGHT = (int)Math.ceil(30.0 * 72.0 / 25.4);

	private ListCellUpdateItemListener<Signature> updateItemListener;
	private Node node;
	private ImageView imgView;
	private Label title;
	private Label description;

	public SignatureListCell(ListCellUpdateItemListener<Signature> updateItemListener) {
		this.updateItemListener = updateItemListener;
		
		setPadding(new Insets(0, 0, 0, 0));
		setPrefHeight(MAX_IMAGE_HEIGHT + getPadding().getTop() + getPadding().getBottom());
		
		imgView = new ImageView();
		imgView.setPreserveRatio(true);
		title = new Label("");
		title.getStyleClass().add("signature-cell-text");
		description = new Label("");
		description.getStyleClass().add("signature-cell-text");
		
		StackPane sp1 = new StackPane();
		sp1.setPrefSize(MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
		sp1.getChildren().add(imgView);
		StackPane.setAlignment(sp1, Pos.CENTER);
		
		VBox vbox = new VBox(title, description);
		vbox.setAlignment(Pos.CENTER_LEFT);

		HBox hbox = new HBox(sp1, vbox);
		this.node = hbox;
		
		setText(null);
		setGraphic(node);
	}
	
	@Override
	protected void updateItem(Signature item, boolean empty) {
		super.updateItem(item, empty);
		
		if(empty) {
			imgView.setImage(null);
			imgView.setVisible(false);
			title.setText("");
			description.setText("");
		} else {
			Image image = item.getImage(1.0);
			if(image != null) {
				if(image.getWidth() < MAX_IMAGE_WIDTH) {
					imgView.setFitWidth(0);
				} else {
					imgView.setFitWidth(MAX_IMAGE_WIDTH);
				}
				if(image.getHeight() < MAX_IMAGE_HEIGHT) {
					imgView.setFitHeight(0);
				} else {
					imgView.setFitHeight(MAX_IMAGE_HEIGHT);
				}
			}
			imgView.setImage(image);
			imgView.setVisible(true);
			title.setText(item.getTitle());
			description.setText(item.getDescription());
		}
		
		if(updateItemListener != null) {
			updateItemListener.updateItem(this, item, empty);
		}
	}

	@Override
	public void updateSelected(boolean selected) {
		super.updateSelected(selected);
	}
}
