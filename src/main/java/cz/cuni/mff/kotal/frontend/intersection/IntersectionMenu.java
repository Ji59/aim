package cz.cuni.mff.kotal.frontend.intersection;


import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.menu.tabs.SimulationMenuTab3;
import cz.cuni.mff.kotal.frontend.menu.tabs.SimulationMenuTab3.Parameters.Statistics;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.filechooser.FileSystemView;

/**
 * Class representing Side menu on main window.
 */
public class IntersectionMenu extends VBox {

	// TODO dont use static, use Component

	// TODO extract constants
	private static final Slider SPEED_SLIDER = new Slider(0, 1000, 815);
	private static final Slider TIMELINE_SLIDER = new Slider(0, 1, 0);

	private static final Button INTERSECTION_MODE = new Button("Real");
	private static final Button PLAY_BUTTON = new Button("Play");
	private static final Button RESTART_BUTTON = new Button("Restart");
	private static final Button SAVE_AGENTS_BUTTON = new Button("Save agents");

	private static final Label AGENTS_LABEL = new Label("#n");
	private static final Label STEPS_LABEL = new Label("#n");
	private static final Label DELAY_LABEL = new Label("#n");
	private static final Label REJECTIONS_LABEL = new Label("#n");
	private static final Label COLLISIONS_LABEL = new Label("#n");
	private static final Label REMAINING_LABEL = new Label();
	private static final Label SPEED_LABEL = new Label("Speed");
	private static final Label TIMELINE_LABEL = new Label("Timeline");


	private static boolean abstractMode = false;
	private static boolean playing = false;

	/**
	 * Create new instance with spacing between elements.
	 * Set all action buttons, etc.
	 *
	 * @param padding Spacing between elements.
	 */
	public IntersectionMenu(double padding) {
		super(padding);
		INTERSECTION_MODE.setPrefWidth(Double.MAX_VALUE);
		INTERSECTION_MODE.setOnMouseClicked(event -> {
			abstractMode = !abstractMode;
			setIntersectionModeButtonText();
			IntersectionScene.getIntersectionGraph().redraw(true);
		});

		getChildren().add(INTERSECTION_MODE);

		createControlNodes(padding);
	}

	/**
	 * Create all nodes and set their parameters.
	 *
	 * @param padding Padding between elements
	 */
	private void createControlNodes(double padding) {
		// TODO extract constants

		GridPane sliders = new GridPane();
		sliders.addRow(0, SPEED_LABEL, SPEED_SLIDER);
		sliders.addRow(1, TIMELINE_LABEL, TIMELINE_SLIDER);
		sliders.setVgap(padding);

		SPEED_SLIDER.valueProperty().addListener(getSliderValueListener(SimulationMenuTab3.getSpeedSlider()));
		TIMELINE_SLIDER.valueProperty().addListener(getSliderValueListener(SimulationMenuTab3.getTimeline()));
		SimulationMenuTab3.getSpeedSlider().valueProperty().addListener(getSliderValueListener(SPEED_SLIDER));
		SimulationMenuTab3.getTimeline().valueProperty().addListener(getSliderValueListener(TIMELINE_SLIDER));

		addPlayButtonAction();
		addResetButtonAction();
		addSaveAgentsButtonAction();

		SimulationMenuTab3.getPlayButton().setOnMouseClicked(PLAY_BUTTON.getOnMouseClicked());
		SimulationMenuTab3.getRestartButton().setOnMouseClicked(RESTART_BUTTON.getOnMouseClicked());
		SimulationMenuTab3.getSaveAgentsButton().setOnMouseClicked(SAVE_AGENTS_BUTTON.getOnMouseClicked());
		SAVE_AGENTS_BUTTON.setPrefWidth(Double.MAX_VALUE);

		GridPane statistics = new GridPane();
		Map<Statistics, Label> labels = Map.of(Statistics.AGENTS, AGENTS_LABEL, Statistics.STEPS, STEPS_LABEL, Statistics.DELAY, DELAY_LABEL, Statistics.REJECTIONS, REJECTIONS_LABEL, Statistics.COLLISIONS, COLLISIONS_LABEL, Statistics.REMAINS, REMAINING_LABEL);
		SimulationMenuTab3.createStatisticsGrid(statistics, labels);

		PLAY_BUTTON.setVisible(true);
		// TODO on Windows button overlay each other
//		HBox buttons = new HBox(PLAY_BUTTON /*, RESTART_BUTTON */);
		HBox buttons = new HBox(padding, PLAY_BUTTON);
//		buttons.getChildren().add(RESTART_BUTTON);

		buttons.setPrefWidth(Double.MAX_VALUE);
		getChildren().addAll(sliders, buttons, RESTART_BUTTON, SAVE_AGENTS_BUTTON, new Label("Statistics"), statistics);
	}

	/**
	 * Set action performed on reset button press.
	 */
	private static void addResetButtonAction() {
		RESTART_BUTTON.setOnMouseClicked(e -> IntersectionScene.resetSimulation());
	}

	/**
	 * TODO javadoc
	 */
	private static void addSaveAgentsButtonAction() {
		SAVE_AGENTS_BUTTON.setOnMouseClicked(e -> {
			try {
				// TODO check simulation

				IntersectionScene.stopSimulation();

				File agentsFile = getAgentsSaveFile();
				if (agentsFile == null) {
					return;
				}
				while (!agentsFile.getPath().endsWith(".json")) {
					AtomicInteger result = getWarningResult(agentsFile);

					if (result.get() == 2) {
						return;
					} else if (result.get() == 1) {
						break;
					}

					agentsFile = getAgentsSaveFile();

					if (agentsFile == null) {
						return;
					}
				}

				IntersectionScene.getSimulation().saveAgents(agentsFile);
				// FIXME exceptions
			} catch (IOException ex) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Saving agents failed.");
				alert.setHeaderText("Input/output exception occurred while saving agents. \n Exception info below.");
				alert.setContentText(ex.getMessage());
				alert.show();
			} catch (NullPointerException ex) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Saving agents failed.");
				alert.setHeaderText("Found null value. \n Exception info below.");
				alert.setContentText(ex.getMessage());
				alert.show();
			} catch (Exception ex) {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Saving agents failed.");
				alert.setHeaderText("Unexpected exception occurred. \n Exception info below.");
				alert.setContentText(ex.getMessage());
				alert.show();
			}
		});
	}

	/**
	 * TODO javadoc
	 *
	 * @param agentsFile
	 * @return
	 */
	@NotNull
	private static AtomicInteger getWarningResult(File agentsFile) {
		AtomicInteger result = new AtomicInteger();
		String contentText = "Agents are saved in json format, so using \".json\" extension is recommended.\n" +
			"Entered filename: " + agentsFile.getName() + "\n" +
			"Are you sure you want to save agents into selected file?";
		Alert extensionConfirmation = new Alert(Alert.AlertType.CONFIRMATION, contentText, ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
		extensionConfirmation.setTitle("Unexpected file extension");
		extensionConfirmation.setHeaderText("File with unexpected extension entered.");
		extensionConfirmation.showAndWait().ifPresentOrElse(response -> {
			if (response == ButtonType.NO) {
				result.set(0);
			} else if (response == ButtonType.YES) {
				result.set(1);
			} else {
				result.set(2);
			}
		}, () -> result.set(1));
		return result;
	}

	/**
	 * TODO javadoc
	 *
	 * @return
	 */
	private static File getAgentsSaveFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Agents data save file"); // FIXME get rid of this stoopid name
		fileChooser.setInitialFileName("agents.json");
		fileChooser.getExtensionFilters().add(new ExtensionFilter("Json documents", "*.json"));
		fileChooser.setInitialDirectory(FileSystemView.getFileSystemView().getDefaultDirectory());
		return fileChooser.showSaveDialog(null);
	}

	/**
	 * Create new listener which sets its new value also to other slider.
	 *
	 * @param affectedSlider Slider to have same values as the one calling the listener
	 * @return Listener with defined action
	 */
	private static ChangeListener<Number> getSliderValueListener(Slider affectedSlider) {
		return (observable, oldValue, newValue) -> affectedSlider.setValue(newValue.doubleValue());
	}

	/**
	 * Set action performed on play button press.
	 */
	private static void addPlayButtonAction() {
		// TODO extract constants
		PLAY_BUTTON.setOnMouseClicked(e -> {
			String newText;
			if (playing) {
				newText = "Play"; // TODO
				IntersectionScene.stopSimulation();
			} else {
				SimulationGraph graph = IntersectionModel.getGraph();
				AlgorithmMenuTab2.Parameters.Algorithm algorithmEnum = AlgorithmMenuTab2.getAlgorithm();
				if (algorithmEnum == null) {
					// TODO exception
					return;
				}
				Algorithm algorithm = null;
				try {
					// TODO
					algorithm = algorithmEnum.getAlgorithmClass().getConstructor(SimulationGraph.class).newInstance(graph);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException ex) {
					ex.printStackTrace();
					return;
				}
				IntersectionScene.startSimulation(algorithm);
				newText = "Pause"; // TODO
			}
			PLAY_BUTTON.setText(newText);
			SimulationMenuTab3.getPlayButton().setText(newText);
			playing = !playing;
		});
	}

	/**
	 * Set intersection button text based on actual graph visual mode.
	 */
	private static void setIntersectionModeButtonText() {
		INTERSECTION_MODE.setText(abstractMode ? "Abstract" : "Real");
	}

	/**
	 * Set text on both play buttons according to parameter.
	 *
	 * @param playing True if simulation is running, otherwise false
	 */
	public static void setPlayButtonPlaying(boolean playing) {
		// TODO extract constants
		IntersectionMenu.playing = playing;
		if (playing) {
			PLAY_BUTTON.setText("Pause");
			SimulationMenuTab3.getPlayButton().setText("Pause");
		} else {
			PLAY_BUTTON.setText("Play");
			SimulationMenuTab3.getPlayButton().setText("Play");
		}
	}

	/**
	 * @return If the shown graph is in abstract mode
	 */
	public static boolean isAbstract() {
		return abstractMode;
	}

	/**
	 * Set abstract mode according to parameter.
	 *
	 * @param abstractMode True if graph should be abstract, otherwise False.
	 */
	public static void setAbstractMode(boolean abstractMode) {
		IntersectionMenu.abstractMode = abstractMode;
		setIntersectionModeButtonText();
	}

	/**
	 * @return Speed set on slider
	 */
	public static double getSpeed() {
		return SPEED_SLIDER.getValue();
	}

	/**
	 * @return Play button node
	 */
	public static Button getPlayButton() {
		return PLAY_BUTTON;
	}

	/**
	 * @return Restart button node
	 */
	public static Button getRestartButton() {
		return RESTART_BUTTON;
	}

	/**
	 * @return Save button node
	 */
	public static Button getSaveAgentsButton() {
		return SAVE_AGENTS_BUTTON;
	}

	/**
	 * @return Label for number of agents that arrived
	 */
	public static Label getAgentsLabel() {
		return AGENTS_LABEL;
	}

	/**
	 * @return Step label node
	 */
	public static Label getStepsLabel() {
		return STEPS_LABEL;
	}

	/**
	 * Set value of both agent labels. Should be total number of agents that arrived.
	 *
	 * @param agents Value to be shown on agent labels
	 */
	public static void setAgents(long agents) {
		Platform.runLater(() -> {
			String agentsFormatted = String.format("%,d", agents);
			AGENTS_LABEL.setText(agentsFormatted);
			SimulationMenuTab3.getAgentsLabel().setText(agentsFormatted);
		});
	}

	/**
	 * Set value of both step labels.
	 *
	 * @param step Value to be shown on labels
	 */
	public static void setStep(long step) {
		Platform.runLater(() -> {
			String stepFormatted = String.format("%,d", step);
			STEPS_LABEL.setText(stepFormatted);
			SimulationMenuTab3.getStepsLabel().setText(stepFormatted);
		});
	}

	/**
	 * Set value of both step labels.
	 *
	 * @param step Value to be shown on labels
	 */
	public static void setStep(double step) {
		Platform.runLater(() -> {
			String stepFormatted = String.format("%,.2f", step);  // TODO extract constant
			STEPS_LABEL.setText(stepFormatted);
			SimulationMenuTab3.getStepsLabel().setText(stepFormatted);
		});
	}

	/**
	 * Set value of both delay labels.
	 *
	 * @param delay Value to be shown on labels
	 */
	public static void setDelay(long delay) {
		Platform.runLater(() -> {
			String delayFormatted = String.format("%,d", delay);
			DELAY_LABEL.setText(delayFormatted);
			SimulationMenuTab3.getDelayLabel().setText(delayFormatted);
		});
	}

	/**
	 * Set value of both rejections labels.
	 *
	 * @param rejections Value to be shown on labels
	 */
	public static void setRejections(long rejections) {
		Platform.runLater(() -> {
			String rejectionsFormatted = String.format("%,d", rejections);
			REJECTIONS_LABEL.setText(rejectionsFormatted);
			SimulationMenuTab3.getRejectionsLabel().setText(rejectionsFormatted);
		});
	}

	/**
	 * Set value of both collision labels.
	 *
	 * @param collisions Value to be shown on labels
	 */
	public static void setCollisions(long collisions) {
		Platform.runLater(() -> {
			String collisionsFormatted = String.valueOf(collisions);
			COLLISIONS_LABEL.setText(collisionsFormatted);
			SimulationMenuTab3.getCollisionsLabel().setText(collisionsFormatted);
		});
	}

	/**
	 * @return Label displaying total rejections since start of the simulation
	 */
	public static Label getDelayLabel() {
		return DELAY_LABEL;
	}

	/**
	 * @return Label displaying number of rejections
	 */
	public static Label getRejectionsLabel() {
		return REJECTIONS_LABEL;
	}

	/**
	 * @return Label displaying number of collisions
	 */
	public static Label getCollisionsLabel() {
		return COLLISIONS_LABEL;
	}

	/**
	 * @return Label displaying remaining running time
	 */
	public static Label getRemainingLabel() {
		return REMAINING_LABEL;
	}
}
