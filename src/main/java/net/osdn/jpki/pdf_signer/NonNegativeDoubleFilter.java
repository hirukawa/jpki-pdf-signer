package net.osdn.jpki.pdf_signer;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

import java.util.function.UnaryOperator;

public class NonNegativeDoubleFilter implements UnaryOperator<TextFormatter.Change> {
	@Override
	public TextFormatter.Change apply(TextFormatter.Change t) {
		if(!t.getControlNewText().matches("[0-9]{0,3}(\\.?|\\.[0-9]{0,2})")) {
			return null;
		}
		return t;
	}

	public static DoubleProperty applyTo(TextInputControl control) {
		Double defaultValue = null;
		double invalidValue = 0.0;

		StringConverter<Double> converter = new DoubleStringConverter();
		TextFormatter<Double> formatter = new TextFormatter<Double>(
				converter, defaultValue, new NonNegativeDoubleFilter());
		control.setTextFormatter(formatter);

		DoubleBinding binding = new DoubleBinding() {
			{
				bind(control.textProperty());
			}

			@Override
			protected double computeValue() {
				try {
					Object value = converter.fromString(control.getText());
					if (value instanceof Number) {
						return ((Number) value).doubleValue();
					}
				} catch (NumberFormatException ignore) {
				}
				return invalidValue;
			}
		};
		DoubleProperty property = new SimpleDoubleProperty() {
			{
				bind(binding);
			}

			@Override
			public void setValue(Number v) {
				if (v == null) {
					control.setText("");
				} else {
					set(v.doubleValue());
				}
			}

			@Override
			public void set(double newValue) {
				control.setText(converter.toString(newValue));
			}
		};
		return property;
	}
}
