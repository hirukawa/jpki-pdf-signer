package net.osdn.jpki.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class PdfPane extends Pane {
	
	private ImageView imageView;
	private SpinnerImageView spinner;
	private Color backgroundColor;
	private PDDocument document;
	private int pageIndex = -1;
	private FloatProperty scaleProperty = new SimpleFloatProperty(0f);
	private Bounds pageBounds = new BoundingBox(0, 0, 0, 0);
	private WritableImage wimg;
	private ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	});
	private FutureTask<BufferedImage> renderTask;
	
	public PdfPane() {
		setBackgroundColor(Color.GRAY);
		
		imageView = new ImageView();
		spinner = new SpinnerImageView();
		spinner.setVisible(false);
		getChildren().addAll(imageView, spinner);
		
		ChangeListener<Number> resizeListener = new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				try {
					spinner.setLayoutX((getWidth() - spinner.getWidth()) / 2);
					spinner.setLayoutY((getHeight() - spinner.getHeight()) / 2);
					render(document, pageIndex);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		widthProperty().addListener(resizeListener);
		heightProperty().addListener(resizeListener);
	}
	
	public FloatProperty scaleProperty() {
		return scaleProperty;
	}
	
	public void setBackgroundColor(Color color) {
		this.backgroundColor = color;
		setBackground(new Background(new BackgroundFill(this.backgroundColor, null, null)));
	}
	
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setDocument(PDDocument document) {
		this.pageIndex = 0;
		this.document = document;
	}
	
	public PDDocument getDocument() {
		return document;
	}
	
	public void setPage(int pageIndex) throws IOException {
		if(document != null) {
			if(pageIndex < 0) {
				pageIndex = 0;
			}
			if(pageIndex >= document.getNumberOfPages()) {
				pageIndex = document.getNumberOfPages() - 1;
			}
			this.pageIndex = pageIndex;
		}
		render(document, pageIndex);
	}
	
	public int getPageIndex() {
		return pageIndex;
	}
	
	public Bounds getPageBounds() {
		return pageBounds;
	}
	
	private void render(PDDocument document, int pageIndex) throws IOException {
		if(document == null) {
			return;
		}
		
		PDPage page = document.getPage(pageIndex);
		PDRectangle mediaBox = page.getMediaBox();
		float pageWidth = mediaBox.getWidth();
		float pageHeight = mediaBox.getHeight();
		
		double width = getWidth();
		double height = getHeight();

		double preferredWidth = height * pageWidth / pageHeight;
		double preferredHeight = width * pageHeight / pageWidth;
		
		int imgWidth;
		int imgHeight;
		if(preferredWidth <= width) {
			imgWidth = (int)preferredWidth;
			imgHeight = (int)height;
		} else {
			imgWidth = (int)width;
			imgHeight = (int)preferredHeight;
		}
		
		int x = (int)((width - imgWidth) / 2);
		int y = (int)((height - imgHeight) / 2);
		imageView.setLayoutX(x);
		imageView.setLayoutY(y);
		pageBounds = new BoundingBox(x, y, imgWidth, imgHeight);

		float scale = imgHeight / pageHeight;
		scaleProperty.set(scale);

		if(scale > 0f && imgWidth > 0 && imgHeight > 0) {
			if(renderTask != null && !renderTask.isDone()) {
				renderTask.cancel(true);
			}
			renderTask = new RenderTask(new RenderCallable(document, pageIndex, scale, imgWidth, imgHeight));
			executor.execute(renderTask);
		}
	}
	
	private class RenderTask extends FutureTask<BufferedImage> {
		private RenderCallable callable;
		
		public RenderTask(RenderCallable callable) {
			super(callable);
			this.callable = callable;
		}

		@Override
		protected void done() {
			if(!isCancelled()) {
				try {
					final BufferedImage bimg = get();
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							wimg = SwingFXUtils.toFXImage(bimg, wimg);
							imageView.setImage(wimg);
							spinner.setVisible(false);
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			callable.isCancelled = true;
			return super.cancel(mayInterruptIfRunning);
		}
	}
	
	private class RenderCallable implements Callable<BufferedImage> {
		
		private PDDocument document;
		private int pageIndex;
		private float scale;
		private int imgWidth;
		private int imgHeight;
		private BufferedImage bimg;
		private boolean isCancelled;
		private volatile boolean isDone;
		
		public RenderCallable(PDDocument document, int pageIndex, float scale, int imgWidth, int imgHeight) {
			this.document = document;
			this.pageIndex = pageIndex;
			this.scale = scale;
			this.imgWidth = imgWidth;
			this.imgHeight = imgHeight;
		}
		
		@Override
		public BufferedImage call() throws Exception {
			bimg = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
			
			Graphics2D graphics = (Graphics2D)bimg.getGraphics();
			graphics.setColor(java.awt.Color.WHITE);
			graphics.fillRect(0, 0, imgWidth, imgHeight);
			graphics.setBackground(java.awt.Color.WHITE);
			
			if(isCancelled) {
				return null;
			}
			
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					if(!isCancelled && !isDone) {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								spinner.setVisible(true);
								wimg = SwingFXUtils.toFXImage(bimg, wimg);
								imageView.setImage(wimg);
							}
						});
					}
				}
			}, 200);
			
			PDFRenderer renderer = new PDFRenderer(document);
			renderer.renderPageToGraphics(pageIndex, graphics, scale);
			isDone = true;
			
			if(isCancelled) {
				return null;
			}

			return bimg;
		}
	}
}
