package cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes;


import org.jetbrains.annotations.NotNull;


/**
 * Custom slider with added text using integer values.
 */
public class IntSlider extends DoubleSlider {

	/**
	 * Create new slider with specified values.
	 *
	 * @param min   Slider minimal value
	 * @param max   Slider maximal value
	 * @param value Slider initial value
	 */
	public IntSlider(double min, double max, int value) {
		super(min, max, value);
	}

	/**
	 * Crete slider action triggered on value change.
	 * In action set the new value to text field too.
	 */
	@Override
	protected void createSliderActionListener() {
		slider.valueProperty().addListener((observable, oldValue, newValue) -> {
			setDisable(true);

			int roundedValue = (int) Math.round(newValue.doubleValue());
			slider.setValue(roundedValue);
			textField.setText(formatString(roundedValue));

			setDisable(false);
		});
	}

	@Override
	protected String formatString(final double value) {
		return String.valueOf((int) value);
	}

	@Override
	protected double textFieldValue() {
		return Integer.parseInt(textField.getText());
	}

	/**
	 * @return Integer value of the slider
	 */
	public int getIntValue() {
		return (int) Math.round(slider.getValue());
	}

	/**
	 * Set value to slider and text field.
	 *
	 * @param value New value
	 *
	 * @return Affected MySlider object
	 */
	public @NotNull IntSlider setValue(int value) {
		slider.setValue(value);
		textField.setText(formatString(value));
		return this;
	}

	/**
	 * @return Minimum value of the slider in integer
	 */
	public int getMin() {
		return (int) slider.getMin();
	}

	/**
	 * Set new minimum value of the slider.
	 *
	 * @param value New value to be set
	 *
	 * @return Affected MySlider object
	 */
	public @NotNull IntSlider setMin(int value) {
		slider.setMin(value);
		return this;
	}

	/**
	 * @return Maximum value of the slider in integer
	 */
	public int getMax() {
		return (int) slider.getMax();
	}

	/**
	 * Set new maximum value of the slider.
	 *
	 * @param value New value to be set
	 *
	 * @return Affected MySlider object
	 */
	public @NotNull IntSlider setMax(int value) {
		slider.setMax(value);
		return this;
	}
}
