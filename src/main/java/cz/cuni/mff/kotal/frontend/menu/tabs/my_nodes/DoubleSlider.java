package cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes;


import javafx.beans.value.ChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Custom slider with added text using double values.
 */
public class DoubleSlider extends HBox {
	protected final @NotNull Slider slider;
	protected final @NotNull TextField textField;

	private @Nullable Runnable mouseReleaseAction = null;

	/**
	 * Create new slider with specified values.
	 *
	 * @param min   Slider minimal value
	 * @param max   Slider maximal value
	 * @param value Slider initial value
	 */
	public DoubleSlider(double min, double max, double value) {

		// TODO remove constant
		setSpacing(20);

		slider = createSlider(min, max, value);

		textField = new TextField(formatString(value));
		// TODO remove constant
		textField.setPrefWidth(69);

		createSliderActionListener();

		createTextFiledActionListener();

		getChildren().addAll(slider, textField);
	}

	/**
	 * Crete slider action triggered on value change.
	 * In action set the new value to text field too.
	 */
	protected void createSliderActionListener() {
		slider.valueProperty().addListener((observable, oldValue, newValue) -> {

			setDisable(true);
			textField.setText(formatString(newValue.doubleValue()));
			setDisable(false);
		});
	}

	protected String formatString(final double value) {
		return String.format("%,.2f", value);
	}

	/**
	 * Create listener to set slider value to the text field value if text field was focused.
	 */
	protected void createTextFiledActionListener() {
		textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
			setDisable(true);

			if (newValue == null || !newValue) {
				final @NotNull String sliderValue = formatString(slider.getValue());
				if (textField.getText().isEmpty()) {
					textField.setText(sliderValue);
					return;
				}
				try {
					final double newVal = textFieldValue();
					if (newVal > slider.getMax() || newVal < slider.getMin()) {
						// TODO add exception
						throw new Exception("Entered number is out of range.");
					}
					slider.setValue(newVal);
					if (mouseReleaseAction != null) {
						mouseReleaseAction.run();
					}
				} catch (NumberFormatException e) {
					new Alert(Alert.AlertType.ERROR, "Cannot parse entered value to double: " + e.getMessage(), ButtonType.OK).showAndWait();
					textField.setText(sliderValue);
				} catch (Exception e) {
					new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
					textField.setText(sliderValue);
				}
			}

			setDisable(false);
		});
	}

	protected double textFieldValue() {
		return Double.parseDouble(textField.getText());
	}

	/**
	 * Create slider with specified values.
	 *
	 * @param min   Minimum value of the slider
	 * @param max   Maximum value of the slider
	 * @param value Initial value of the slider
	 *
	 * @return Created slider
	 */
	@NotNull
	private static Slider createSlider(double min, double max, double value) {
		final @NotNull Slider slider = new Slider(min, max, value);
		slider.setShowTickLabels(true);
		slider.setShowTickMarks(true);
		int count = (int) (max - min + 1);
		int tickUnit = getSliderMajorTicks(count);


		if (tickUnit == 0) {
			slider.setMinorTickCount((count - 2));
		} else {
			slider.setMajorTickUnit(tickUnit);
			slider.setMinorTickCount(getSliderMinorTicks(tickUnit));
		}

		slider.setBlockIncrement(1);
		return slider;
	}

	/**
	 * Compute slider major ticks based on slider values.
	 *
	 * @param count Difference between minimal and maximal slider values
	 *
	 * @return Computed size of major tick
	 */
	protected static int getSliderMajorTicks(long count) {
		if (count <= 5) {
			return 0;
		} else {
			return getGoodNumber(count);
		}
	}

	/**
	 * Select number based on input.
	 * Don't remember how it works, but it works.
	 *
	 * @param number Number to be tested
	 *
	 * @return Dividend of the number if any was found
	 */
	private static int getGoodNumber(long number) {
		for (int i = (int) (number / 4); i >= number / 20; i--) {
			if (number % i == 0) {
				return i;
			}
		}
		return (int) (number / 2);
	}

	/**
	 * Compute minor tick count based on major tick size.
	 *
	 * @param majorTick Major tick size
	 *
	 * @return Computed count of minor ticks
	 */
	protected static int getSliderMinorTicks(int majorTick) {
		if (majorTick < 4) {
			return 0;
		}

		return getGoodNumber(majorTick);
	}

	/**
	 * Assign another action listener to the slider.
	 *
	 * @param listener Listener to be added
	 *
	 * @return Affected MySlider object
	 */
	public @NotNull DoubleSlider addAction(ChangeListener<? super Number> listener) {
		slider.valueProperty().addListener(listener);
		return this;
	}

	public @NotNull DoubleSlider addAction(@NotNull Runnable action) {
		mouseReleaseAction = action;
		slider.setOnMouseReleased(event -> action.run());
		return this;
	}

	/**
	 * Set value to slider and text field.
	 *
	 * @param value New value
	 *
	 * @return Affected MySlider object
	 */
	public @NotNull DoubleSlider setValue(double value) {
		slider.setValue(value);
		textField.setText(formatString(value));
		return this;
	}

	/**
	 * @return Minimum value of the slider in long
	 */
	public double getSliderMin() {
		return slider.getMin();
	}

	/**
	 * Set new minimum value of the slider.
	 *
	 * @param value New value to be set
	 *
	 * @return Affected MySlider object
	 */
	public @NotNull DoubleSlider setMin(double value) {
		slider.setMin(value);
		return this;
	}

	/**
	 * @return Maximum value of the slider in long
	 */
	public double getSliderMax() {
		return slider.getMax();
	}

	/**
	 * Set new maximum value of the slider.
	 *
	 * @param value New value to be set
	 *
	 * @return Affected MySlider object
	 */
	public @NotNull DoubleSlider setMax(double value) {
		slider.setMax(value);
		return this;
	}

	public double getValue() {
		return slider.getValue();
	}
}
