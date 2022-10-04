package cz.cuni.mff.kotal.frontend.menu.tabs.myNodes;


import javafx.beans.value.ChangeListener;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.events.Event;

import java.util.function.IntConsumer;


/**
 * Custom slider with added text using long values.
 */
public class MySlider extends HBox {
	private final @NotNull Slider slider;
	private final @NotNull TextField textField;

	private @Nullable Runnable mouseReleaseAction = null;

	/**
	 * Create new slider with specified values.
	 *
	 * @param min   Slider minimal value
	 * @param max   Slider maximal value
	 * @param value Slider initial value
	 */
	public MySlider(double min, double max, int value) {

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
				@NotNull String sliderValue = String.valueOf(Math.round(slider.getValue()));
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
					if (mouseReleaseAction != null) {
						mouseReleaseAction.run();
					}
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
	 * Assign another action listener to the slider.
	 *
	 * @param listener Listener to be added
	 * @return Affected MySlider object
	 */
	public @NotNull MySlider addAction(ChangeListener<? super Number> listener) {
		slider.valueProperty().addListener(listener);
		return this;
	}

	public @NotNull MySlider addAction(@NotNull Runnable action) {
		mouseReleaseAction = action;
		slider.setOnMouseReleased(event -> action.run());
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
	public @NotNull MySlider setValue(long value) {
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
	public @NotNull MySlider setMin(long value) {
		slider.setMin(value);
		return this;
	}

	/**
	 * Set new maximum value of the slider.
	 *
	 * @param value New value to be set
	 * @return Affected MySlider object
	 */
	public @NotNull MySlider setMax(int value) {
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
	private static Slider createSlider(double min, double max, int value) {
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
		if (majorTick < 4) {
			return 0;
		}

		return getGoodNumber(majorTick);
	}

	/**
	 * Select number based on input.
	 * Don't remember how it works, but it works.
	 *
	 * @param number Number to be tested
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
}
