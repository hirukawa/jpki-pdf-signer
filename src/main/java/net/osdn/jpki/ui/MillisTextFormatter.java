package net.osdn.jpki.ui;

import java.util.function.UnaryOperator;

import javafx.scene.control.TextFormatter;
import javafx.util.converter.DoubleStringConverter;

public class MillisTextFormatter extends TextFormatter<Double> {

	public MillisTextFormatter() {
		super(new DoubleStringConverter(), null, new UnaryOperator<Change>() {
			@Override
			public TextFormatter.Change apply(TextFormatter.Change change) {
				try {
					double v = Double.parseDouble(change.getControlNewText() + "0");
					if(1 / v > 0) {
						return change;
					}
				} catch(NumberFormatException e) {}
				return null;
			}
		});
	}
}
