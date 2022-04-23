package cz.cuni.mff.kotal.frontend.menu.tabs.myNodes;


import javafx.beans.value.ChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;


/**
 * Custom slider with added text using long values.
 */
public class MySlider extends HBox {
	private final Slider slider;
	private final TextField textField;

	/**
	 * Create new slider with specified values.
	 *
	 * @param min   Slider minimal value
	 * @param max   Slider maximal value
	 * @param value Slider initial value
	 */
	public MySlider(double min, double max, long value) {

		// TODO remove constant
		setSpacing(20);

		slider = createSlider(min, max, value);

		textField = new TextField(String.valueOf(value));
		// TODO remove constant
		textField.setPrefWidth(40);

		createSliderActionListener();

		createTextFiledActionListener();

		getChildren().addAll(slider, textField);
	}

	/**
	 * Crete slider action triggered on value change.
	 * In action set the new value to text field too.
	 */
	private void createSliderActionListener() {
		slider.valueProperty().addListener((observable, oldValue, newValue) -> {
			setDisable(true);

			long roundedValue = Math.round(newValue.doubleValue());
			slider.setValue(roundedValue);
			textField.setText(String.valueOf(roundedValue));

			setDisable(false);
		});
	}

	/**
	 * Create listener to set slider value to the text field value if text field was focused.
	 */
	private void createTextFiledActionListener() {
		textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
			setDisable(true);

			if (newValue == null || !newValue) {
				String sliderValue = String.valueOf(Math.round(slider.getValue()));
				if (textField.getText().isEmpty()) {
					textField.setText(sliderValue);
					return;
				}
				try {
					long newVal = Long.parseLong(textField.getText());
					if (newVal > slider.getMax() || newVal < slider.getMin()) {
						// TODO add exception
						throw new Exception("Entered number is out of range.");
					}
					slider.setValue(newVal);
				} catch (NumberFormatException e) {
					new Alert(Alert.AlertType.ERROR, "Cannot parse entered value to integer: " + e.getMessage(), ButtonType.OK).showAndWait();
					textField.setText(sliderValue);
				} catch (Exception e) {
					new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
					textField.setText(sliderValue);
				}
			}

			setDisable(false);
		});
	}

	/**
	 * Asssign another action listener to the slider.
	 *
	 * @param listener Listener to be added
	 * @return Affected MySlider object
	 */
	public MySlider addAction(ChangeListener<? super Number> listener) {
		slider.valueProperty().addListener(listener);
		return this;
	}

	/**
	 * @return Long value of the slider
	 */
	public int getValue() {
		return (int) Math.round(slider.getValue());
	}

	/**
	 * Set value to slider and text field.
	 *
	 * @param value New value
	 * @return Affected MySlider object
	 */
	public MySlider setValue(long value) {
		slider.setValue(value);
		textField.setText(String.valueOf(value));
		return this;
	}

	/**
	 * @return Minimum value of the slider in long
	 */
	public long getMin() {
		return (long) slider.getMin();
	}

	/**
	 * @return Maximum value of the slider in long
	 */
	public long getMax() {
		return (long) slider.getMax();
	}

	/**
	 * Set new minimum value of the slider.
	 *
	 * @param value New value to be set
	 * @return Affected MySlider object
	 */
	public MySlider setMin(long value) {
		slider.setMin(value);
		return this;
	}

	/**
	 * Set new maximum value of the slider.
	 *
	 * @param value New value to be set
	 * @return Affected MySlider object
	 */
	public MySlider setMax(long value) {
		slider.setMax(value);
		return this;
	}

	/**
	 * Create slider with specified values.
	 *
	 * @param min   Minimum value of the slider
	 * @param max   Maximum value of the slider
	 * @param value Initial value of the slider
	 * @return Created slider
	 */
	@NotNull
	private static Slider createSlider(double min, double max, long value) {
		final Slider slider = new Slider(min, max, value);
		slider.setShowTickLabels(true);
		slider.setShowTickMarks(true);
		long count = (long) (max - min + 1);
		int tickUnit = getSliderMajorTicks(count);

		if (tickUnit == 0) {
			slider.setMinorTickCount((int) (count - 2));
			slider.setBlockIncrement(1);
		} else {
			slider.setBlockIncrement(1);
			slider.setMajorTickUnit(tickUnit);
			slider.setMinorTickCount(getSliderMinorTicks(tickUnit));
		}
		return slider;
	}

	/**
	 * Compute slider major ticks based on slider values.
	 *
	 * @param count Difference between minimal and maximal slider values
	 * @return Computed size of major tick
	 */
	private static int getSliderMajorTicks(long count) {
		if (count <= 5) {
			return 0;
		} else {
			return getGoodNumber(count);
		}
	}

	/**
	 * Compute minor tick count based on major tick size.
	 *
	 * @param majorTick Major tick size
	 * @return Computed count of minor ticks
	 */
	private static int getSliderMinorTicks(int majorTick) {
		if (majorTick == 0) {
			return 0;
		}

		return getGoodNumber(majorTick);
	}

	/**
	 * Select number based on input.
	 * Don't remember how it works, but it works.
	 *
	 * @param number Number to be tested
	 * @return Dividend of the number of any was found
	 */
	private static int getGoodNumber(long number) {
		for (int i = (int) (number / 4); i >= number / 20; i--) {
			if (number % i == 0) {
				return i;
			}
		}
		return (int) (number / 2);
	}
}
