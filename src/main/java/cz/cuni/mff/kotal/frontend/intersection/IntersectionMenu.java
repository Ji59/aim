package cz.cuni.mff.kotal.frontend.intersection;


import cz.cuni.mff.kotal.backend.algorithm.BreadthFirstSearch;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.menu.tabs.SimulationMenuTab3;
import cz.cuni.mff.kotal.frontend.simulation.SimulationGraph;
import cz.cuni.mff.kotal.simulation.Simulation;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class IntersectionMenu extends VBox {

	private static boolean abstractMode = false;

	// TODO extract constants
	private static final Slider SPEED_SLIDER = new Slider(0, 1, 0),
		TIMELINE_SLIDER = new Slider(0, 1, 0);
	private static final Button INTERSECTION_MODE = new Button("Real");
	private static final Label stepsLabel = new Label("#n"),
		delayLabel = new Label("#n"),
		rejectionsLabel = new Label("#n"),
		collisionsLabel = new Label("#n"),
		remainingLabel = new Label(),
		speedLabel = new Label("Speed"),
		timelineLabel = new Label("Timeline");

	private static final Button PLAY_BUTTON = new Button("Play"),
		RESTART_BUTTON = new Button("Restart"),
		SAVE_AGENTS_BUTTON = new Button("Save agents");

	private static boolean playing = false;

	public IntersectionMenu(double padding) {
		super(padding);
		INTERSECTION_MODE.setPrefWidth(Double.MAX_VALUE);
		INTERSECTION_MODE.setOnMouseClicked(event -> {
			abstractMode = !abstractMode;
			setIntersectionModeButtonText();
			IntersectionScene.getIntersectionGraph().redraw();
		});

		getChildren().add(INTERSECTION_MODE);

		createControlNodes(padding);
	}

	private void createControlNodes(double padding) {
		// TODO extract constants

		GridPane sliders = new GridPane();
		sliders.addRow(0, speedLabel, SPEED_SLIDER);
		sliders.addRow(1, timelineLabel, TIMELINE_SLIDER);
		sliders.setVgap(padding);

		SPEED_SLIDER.valueProperty().addListener(getSliderValueListener(SimulationMenuTab3.getSpeed()));
		TIMELINE_SLIDER.valueProperty().addListener(getSliderValueListener(SimulationMenuTab3.getTimeline()));
		SimulationMenuTab3.getSpeed().valueProperty().addListener(getSliderValueListener(SPEED_SLIDER));
		SimulationMenuTab3.getTimeline().valueProperty().addListener(getSliderValueListener(TIMELINE_SLIDER));

		addPlayButtonAction();

		SimulationMenuTab3.getPlayButton().setOnMouseClicked(PLAY_BUTTON.getOnMouseClicked());
		SimulationMenuTab3.getRestartButton().setOnMouseClicked(RESTART_BUTTON.getOnMouseClicked());
		SimulationMenuTab3.getSaveAgentsButton().setOnMouseClicked(SAVE_AGENTS_BUTTON.getOnMouseClicked());
		SAVE_AGENTS_BUTTON.setPrefWidth(Double.MAX_VALUE);

		GridPane statistics = new GridPane();
		SimulationMenuTab3.createStatisticsGrid(statistics);

		HBox buttons = new HBox(PLAY_BUTTON/*, RESTART_BUTTON*/);
		PLAY_BUTTON.setVisible(true);
		buttons.setPrefWidth(Double.MAX_VALUE);
		getChildren().addAll(sliders, buttons, SAVE_AGENTS_BUTTON, new Label("Statistics"), statistics);
	}

	private ChangeListener<Number> getSliderValueListener(Slider affectedSlider) {
		return (observable, oldValue, newValue) -> affectedSlider.setValue(newValue.doubleValue());
	}

	private void addPlayButtonAction() {
		// TODO extract constants
		PLAY_BUTTON.setOnMouseClicked(e -> {
			// TODO add pause simulation
			String newText;
			if (playing) {
				newText = "Play";
				IntersectionScene.stopSimulation();
			} else {
				if (AlgorithmMenuTab2.Parameters.Algorithm.BFS.equals(AlgorithmMenuTab2.getAlgorithm())) {
					SimulationGraph graph = IntersectionModel.getGraph();
					BreadthFirstSearch bfs = new BreadthFirstSearch(graph);
					IntersectionScene.startSimulation(bfs);
				} else {
					return;
				}
				newText = "Pause";
			}
			PLAY_BUTTON.setText(newText);
			SimulationMenuTab3.getPlayButton().setText(newText);
			playing = !playing;
		});
	}

	private static void setIntersectionModeButtonText() {
		INTERSECTION_MODE.setText(abstractMode ? "Abstract" : "Real");
	}

	public static boolean isAbstract() {
		return abstractMode;
	}

	public static void setAbstractMode(boolean abstractMode) {
		IntersectionMenu.abstractMode = abstractMode;
		setIntersectionModeButtonText();
	}

	public static Button getPlayButton() {
		return PLAY_BUTTON;
	}

	public static Button getRestartButton() {
		return RESTART_BUTTON;
	}

	public static Button getSaveAgentsButton() {
		return SAVE_AGENTS_BUTTON;
	}

	public static Label getStepsLabel() {
		return stepsLabel;
	}

	public static void setStep(long step) {
		Platform.runLater(() -> {
			stepsLabel.setText(String.valueOf(step));
			stepsLabel.textProperty().set(String.valueOf(step));
		});
	}

	public static Label getDelayLabel() {
		return delayLabel;
	}

	public static Label getRejectionsLabel() {
		return rejectionsLabel;
	}

	public static Label getCollisionsLabel() {
		return collisionsLabel;
	}

	public static Label getRemainingLabel() {
		return remainingLabel;
	}
}
