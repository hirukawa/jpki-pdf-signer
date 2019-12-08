package net.osdn.jpki.pdf_signer;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import net.osdn.util.javafx.event.SilentEventHandler;
import net.osdn.util.javafx.fxml.Fxml;

public class SignatureListCell extends ListCell<Signature> {

    public static final double MAX_IMAGE_WIDTH = Math.ceil(30.0 * 72.0 / 25.4);
    public static final double MAX_IMAGE_HEIGHT = Math.ceil(30.0 * 72.0 / 25.4);

    private Factory factory;
    @FXML ImageView ivImage;
    @FXML Label lblTitle;
    @FXML Label lblDescription;

    public SignatureListCell(Factory factory) {
        this.factory = factory;
        Node node = Fxml.load(this);
        setText(null);
        setGraphic(node);
        setOnMousePressed(SilentEventHandler.wrap(factory.main::lvSignature_cell_onMousePressed));
        setOnMouseClicked(SilentEventHandler.wrap(factory.main::lvSignature_cell_onMouseClicked));
    }

    @Override
    protected void updateItem(Signature item, boolean empty) {
        super.updateItem(item, empty);
        if(empty) {
            ivImage.setFitWidth(0.0);
            ivImage.setFitHeight(0.0);
            ivImage.setImage(null);
            lblTitle.setText("");
            lblDescription.setText("");
        } else {
            double prefWidth = item.getWidthMillis() * 72.0 / 25.4;
            double prefHeight = item.getHeightMillis() * 72.0 / 25.4;
            double scaleWidth = prefWidth > MAX_IMAGE_WIDTH ? MAX_IMAGE_WIDTH / prefWidth : 1.0;
            double scaleHeight = prefHeight > MAX_IMAGE_HEIGHT ? MAX_IMAGE_HEIGHT / prefHeight : 1.0;
            ivImage.setFitWidth(prefWidth * Math.min(scaleWidth, scaleHeight));
            ivImage.setFitHeight(prefHeight * Math.min(scaleWidth, scaleHeight));
            ivImage.setImage(item.getImage());
            lblTitle.setText(item.getTitle());
            lblDescription.setText(item.getDescription());
        }
        if(empty) {
            setContextMenu(factory.contextMenu1);
        } else if(item.isVisible()){
            setContextMenu(factory.contextMenu2);
        } else {
            setContextMenu(null);
        }
    }

    public static class Factory implements Callback<ListView<Signature>, ListCell<Signature>> {

        Main main;
        ContextMenu contextMenu1 = Fxml.load(this, "SignatureListCellContextMenu1.fxml");
        ContextMenu contextMenu2 = Fxml.load(this, "SignatureListCellContextMenu2.fxml");

        @FXML MenuItem menuAddSignature;
        @FXML MenuItem menuEditSignature;
        @FXML MenuItem menuRemoveSignature;

        public Factory(Main main) {
            this.main = main;
            menuAddSignature.setOnAction(SilentEventHandler.wrap(main::btnAddSignature_onAction));
            menuEditSignature.setOnAction(SilentEventHandler.wrap(main::btnEditSignature_onAction));
            menuRemoveSignature.setOnAction(SilentEventHandler.wrap(main::btnRemoveSignature_onAction));
        }

        @Override
        public ListCell<Signature> call(ListView<Signature> param) {
            return new SignatureListCell(this);
        }
    }
}
