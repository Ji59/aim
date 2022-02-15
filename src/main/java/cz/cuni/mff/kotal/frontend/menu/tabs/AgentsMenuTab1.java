package cz.cuni.mff.kotal.frontend.menu.tabs;


import cz.cuni.mff.kotal.frontend.MyApplication;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MyComboBox;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MySlider;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Tab in menu window for setting generating agents parameters.
 */
public class AgentsMenuTab1 extends MyTabTemplate {

	// TODO don't use static, use Component
	private static final MyComboBox inputType = new MyComboBox(Arrays.stream(Parameters.Input.values()).map(Parameters.Input::getText).toList());
	//TODO extract constants
	private static final TextField steps = new TextField("0 ~ infinite simulation");
	private static final TextField filePath = new TextField("Enter file path here");
	private static final MySlider newAgentsMinimum = new MySlider(0, IntersectionMenuTab0.getRoads() * IntersectionMenuTab0.getEntries().getValue(), 0);
	private static final MySlider newAgentsMaximum = new MySlider(0, IntersectionMenuTab0.getRoads() * IntersectionMenuTab0.getEntries().getValue(), IntersectionMenuTab0.getRoads());
	private static final VBox directionDistribution = ((VBox) Parameters.DIRECTION.getParameter());

	/**
	 * Create new tab with nodes, add actions.
	 */
	public AgentsMenuTab1() {
		super(Tabs.T1.getText());

		addInputActions();

		createFileMenuAndAddActions();

		addStepsActions();

		createAmountMenuAndAddActions();

		createDirectionsMenuAndAddActions(IntersectionMenuTab0.Parameters.Models.SQUARE);

		Iterator<Parameters> iterator = Arrays.stream(Parameters.values()).iterator();
		for (int index = 0; iterator.hasNext(); index++) {
			Parameters parameter = iterator.next();
			if (parameter.equals(Parameters.FILE)) {
				addInvisibleRow(1, new MenuLabel(parameter.text), parameter.parameter);
				continue;
			}
			addRow(index, new MenuLabel(parameter.text), parameter.parameter);
		}

		// TODO pridat tlacitko na ulozeni konfigurace
	}

	/**
	 * Add input type combo box action.
	 */
	private void addInputActions() {
		inputType.valueProperty().addListener((observable, oldValue, newValue) -> {
			for (Node child : getGrid().getChildren()) {
				child.setVisible(!child.isVisible() || child.equals(inputType) || (child instanceof Label && ((Label) child).getText().equals(Parameters.INPUT.getText())));
			}
		});
	}

	/**
	 * Add steps text field action.
	 */
	private void addStepsActions() {
		steps.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && newValue) {
				if (steps.getText().startsWith("0")) {
					steps.setText("");
				}
			} else {
				if (steps.getText().isEmpty()) {
					steps.setText(String.valueOf(0));
				} else {
					try {
						long value = Long.parseLong(steps.getText());
						if (value < 0) {
							// TODO create exception
							throw new Exception("Entered number: " + value);
						}
					} catch (Exception e) {
						new Alert(Alert.AlertType.ERROR, "Please enter non-negative number. " + e.getMessage(), ButtonType.OK).showAndWait();
						steps.setText(String.valueOf(0));
					}
				}
				if (steps.getText().startsWith("0")) {
					SimulationMenuTab3.getRemainingLabel().setText("");
				} else {
					// TODO do something with that
					SimulationMenuTab3.getRemainingLabel().setText("n s");
				}
			}
		});
	}

	/**
	 * Add file path text field and file button actions and edit parameters.
	 */
	private void createFileMenuAndAddActions() {
		//TODO extract constants
		filePath.setPrefWidth(200);
		filePath.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				if (filePath.getText().equals("Enter file path here")) {
					filePath.setText("");
				}
			} else {
				if (filePath.getText().isEmpty()) {
					filePath.setText("Enter file path here");
					return;
				}
				try {
					File file = new File(filePath.getText());
					if (!file.isFile()) {
						//TODO extract constant and create own exception
						throw new Exception("File does not exist. Entered file: " + file.getPath());
					}
				} catch (Exception e) {
					new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
					filePath.requestFocus();
				}
			}
		});
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Select agents file input");
		fileChooser.setInitialFileName("agents.json");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Json documents", "*.json"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
		fileChooser.setInitialDirectory(FileSystemView.getFileSystemView().getDefaultDirectory());
		Button fileButton = new Button("Select file");
		fileButton.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
			File file = fileChooser.showOpenDialog(MyApplication.getMenuStage());
			if (file != null && file.isFile()) {
				filePath.setText(file.getAbsolutePath());
			}
		});
		HBox fileBox = (HBox) Parameters.FILE.parameter;
		fileBox.getChildren().addAll(filePath, fileButton);
	}

	/**
	 * Add new agents minimum and maximum slider actions and edit parameters.
	 */
	private void createAmountMenuAndAddActions() {
		newAgentsMinimum.addAction((observable, oldValue, newValue) -> {
			if (newAgentsMinimum.getValue() > newAgentsMaximum.getValue()) {
				newAgentsMaximum.setValue(newValue.longValue());
			}
		});
		newAgentsMaximum.addAction((observable, oldValue, newValue) -> {
			if (newAgentsMaximum.getValue() < newAgentsMinimum.getValue()) {
				newAgentsMinimum.setValue(newValue.longValue());
			}
		});
		Label minLabel = new Label("Minimum: ");
		Label maxLabel = new Label("Maximum:");
		((GridPane) Parameters.AMOUNT.getParameter()).getChildren().addAll(minLabel, newAgentsMinimum, maxLabel, newAgentsMaximum);
		GridPane.setConstraints(minLabel, 0, 0);
		GridPane.setConstraints(maxLabel, 0, 1);
		GridPane.setConstraints(newAgentsMinimum, 1, 0);
		GridPane.setConstraints(newAgentsMaximum, 1, 1);
	}

	/**
	 * Create direction sliders, add actions and edit parameters.
	 *
	 * @param model Actual intersection model type
	 */
	static void createDirectionsMenuAndAddActions(IntersectionMenuTab0.Parameters.Models model) {
		if (model == null || model.getDirections() == null) {
			return;
		}
		directionDistribution.getChildren().clear();


		List<DirectionSlider> sliders = new ArrayList<>();

		int row = 0;
		int remains = 100;
		for (Character direction : model.getDirections()) {
			long value = remains / (model.getDirections().size() - row);
			DirectionSlider directionSlider = new DirectionSlider(direction, value);
			sliders.add(directionSlider);

			directionSlider.addSliderAction((observable, oldValue, newValue) -> {
				if (directionSlider.sliderActionOn) {
					double sum = 0;
					for (DirectionSlider sl : sliders) {
						sum += sl.getSliderValue();
					}

					int remainingPercentages = 100;
					for (DirectionSlider sl : sliders) {
						double oldVal = sl.getSliderValue();
						remainingPercentages -= sl.setValue(Math.round(sl.getSliderValue() / sum * remainingPercentages));
						sum -= oldVal;
					}
				}
			});

			directionSlider.addValueLabelFocusedAction((observable, oldValue, newValue) -> {
				if (newValue != null && newValue) {
					// if focused
					TextField valueText = directionSlider.valueText;
					directionSlider.valueText.setText(valueText.getText(0, valueText.getText().lastIndexOf('%')));
					directionSlider.oldValue = Long.parseLong(directionSlider.valueText.getText());
				} else {
					// if not focused anymore
					try {
						int newVal = Integer.parseInt(directionSlider.valueText.getText());
						if (newVal < Math.round(directionSlider.slider.getMin() - 0.5) || newVal > directionSlider.slider.getMax()) {
							directionSlider.setValue(directionSlider.oldValue + '%');
							// TODO create exception
							throw new Exception("Please enter integer between 0 and 100. Entered value: " + newVal);
						} else {
							double sum = 0;
							double available = 100 - newVal;
							for (DirectionSlider sl : sliders) {
								if (sl != directionSlider) {
									sum += sl.getValue();
								}
							}
							for (DirectionSlider sl : sliders) {
								long newSliderValue;
								if (sl != directionSlider) {
									sl.setValue(newSliderValue = Math.round(sl.getValue() / sum * available));
								} else {
									newSliderValue = newVal;
								}
								sl.sliderActionOn = false;
								sl.setSliderValue(newSliderValue);
								sl.sliderActionOn = true;
							}
							directionSlider.valueText.setText(newVal + "%");
						}
					} catch (Exception e) {
						new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
					}
				}
			});

			remains -= value;
			row++;
		}

		directionDistribution.getChildren().addAll(sliders);
	}

	/**
	 * @return Input time slider
	 */
	public static Parameters.Input getInputType() {

		return Parameters.Input.value(inputType.getValue());
	}

	/**
	 * @return Maximum number of step in which are agents generated.
	 */
	public static long getSteps() {
		long stepsValue;
		try {
			stepsValue = Long.parseLong(steps.getText());
		} catch (NumberFormatException ignored) {
			stepsValue = 0;
		}
		return stepsValue;
	}

	/**
	 * @return File path text field
	 */
	public static TextField getFilePath() {
		return filePath;
	}

	/**
	 * @return Minimum new agents slider
	 */
	public static MySlider getNewAgentsMinimum() {
		return newAgentsMinimum;
	}

	/**
	 * @return Maximum new agents slider
	 */
	public static MySlider getNewAgentsMaximum() {
		return newAgentsMaximum;
	}

	/**
	 * @return Direction distribution vertical box
	 */
	public static VBox getDirectionDistribution() {
		return directionDistribution;
	}

	/**
	 * Parameters shown in this tab.
	 */
	public enum Parameters {
		INPUT("Agents input type: ", inputType),
		STEPS("Number of steps: ", steps),
		AMOUNT("Amount of new agents:", new GridPane()),
		DIRECTION("Direction distribution: ", new VBox()),
		// TODO make spacing constant
		FILE("File name: ", new HBox(5)),
		;

		private final String text;
		private final Node parameter;

		Parameters(String text, Node parameter) {
			this.text = text;
			this.parameter = parameter;
		}

		public String getText() {
			return text;
		}

		public Node getParameter() {
			return parameter;
		}

		public enum Input {
			RNG("Randomly generated"),
			FILE("From file"),
			;

			private final String text;

			Input(String text) {
				this.text = text;
			}

			public String getText() {
				return text;
			}

			public static Input value(String name) {
				for (Input inputType: values()) {
					if (inputType.text.equals(name)) {
						return inputType;
					}
				}
				return null;
			}
		}
	}


	/**
	 * Horizontal box of direction slider and text field.
	 */
	public static class DirectionSlider extends HBox {
		private final Slider slider = new Slider(0.002, 100, 50);
		private final TextField valueText;
		private long oldValue = 0;
		private boolean sliderActionOn = true;

		/**
		 * Create new HBox and set parameters.
		 *
		 * @param directionName Name of direction
		 * @param value         Initial percent value
		 */
		private DirectionSlider(char directionName, long value) {
			// TODO introduce constant
			super(10);

			valueText = new TextField(String.valueOf(value) + '%');
			valueText.setPrefWidth(45);

			Label directionLabel = new Label(String.valueOf(directionName));
			directionLabel.setPrefWidth(15);
			getChildren().addAll(directionLabel, slider, valueText);
		}

		/**
		 * @return Value in the text field
		 * @throws NumberFormatException Parsing text from text field failed
		 */
		public long getValue() throws NumberFormatException {
			return Long.parseLong(valueText.getText(0, valueText.getText().lastIndexOf('%')));
		}

		/**
		 * Set new value to the text field.
		 *
		 * @param value Value to be set in text field
		 * @return New Value
		 */
		long setValue(long value) {
			valueText.setText(String.valueOf(value) + '%');
			return value;
		}

		/**
		 * @return Value of the slider
		 */
		double getSliderValue() {
			return slider.getValue();
		}

		/**
		 * Set value to slider.
		 *
		 * @param value Value to be set
		 * @return This direction slider
		 */
		DirectionSlider setSliderValue(double value) {
			slider.setValue(value);
			return this;
		}

		/**
		 * Add action to the slider.
		 *
		 * @param listener Action to be added
		 * @return This direction slider
		 */
		DirectionSlider addSliderAction(ChangeListener<? super Number> listener) {
			slider.valueProperty().addListener(listener);
			return this;
		}

		/**
		 * Add action to be performed at text field focus.
		 *
		 * @param listener Action to be added
		 * @return This direction slider
		 */
		DirectionSlider addValueLabelFocusedAction(ChangeListener<? super Boolean> listener) {
			valueText.focusedProperty().addListener(listener);
			return this;
		}
	}
}
