package net.osdn.jpki.pdf_signer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class Signature {

    static final Signature EMPTY = new Signature(null, null, null);
    static final Signature INVISIBLE = new Signature("/img/invisible-signature.png", "印影なしで", "電子署名する");

    private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<Image>(this, "image");
    public ReadOnlyProperty<Image> imageProperty() {
        return imageProperty;
    }

    private DoubleProperty imageScaleXProperty = new SimpleDoubleProperty(this, "imageScaleX");
    public ReadOnlyDoubleProperty imageScaleXProperty() {
        return imageScaleXProperty;
    }

    private DoubleProperty imageScaleYProperty = new SimpleDoubleProperty(this, "imageScaleY");
    public ReadOnlyDoubleProperty imageScaleYProperty() {
        return imageScaleYProperty;
    }

    private BooleanProperty visibleProperty = new SimpleBooleanProperty(this, "visible");
    public ReadOnlyBooleanProperty visibleProperty() {
        return visibleProperty;
    }

    private File   file;
    private double widthMillis;
    private double heightMillis;
    private String title;
    private String description;

    private Signature(String imageResourcePath, String title, String description) {
        if(imageResourcePath != null) {
            try (InputStream is = getClass().getResourceAsStream(imageResourcePath)) {
                Image i = new Image(is);
                this.imageProperty.set(i);
            } catch(IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        this.title = title;
        this.description = description;
    }

    public Signature(File image, double widthMillis, double heightMillis, String title, String description) throws IOException {
        this.file = image;
        if(this.file != null) {
            try (InputStream is = new FileInputStream(this.file)) {
                Image i = new Image(is);
                this.imageProperty.set(i);
                this.imageScaleXProperty.set((widthMillis * 72.0 / 25.4) / i.getWidth());
                this.imageScaleYProperty.set((heightMillis * 72.0 / 25.4) / i.getHeight());
            }
        }
        this.widthMillis = widthMillis;
        this.heightMillis = heightMillis;
        this.title = title;
        this.description = description;

        visibleProperty.set(widthMillis > 0.0 && heightMillis > 0.0);
    }

    public boolean isVisible() {
        return visibleProperty.get();
    }

    public File getFile() {
        return file;
    }

    public Image getImage() {
        return imageProperty.get();
    }

    public double getWidthMillis() {
        return widthMillis;
    }

    public double getHeightMillis() {
        return heightMillis;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        if(description == null) {
            return String.format("%.1f x %.1f mm", widthMillis, heightMillis);
        }
        return description;
    }
}
