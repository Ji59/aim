package cz.cuni.mff.kotal.frontend.intersection;


import cz.cuni.mff.kotal.backend.algorithm.BreadthFirstSearch;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.menu.tabs.SimulationMenuTab3;
import cz.cuni.mff.kotal.frontend.menu.tabs.SimulationMenuTab3.Parameters.Statistics;
import cz.cuni.mff.kotal.frontend.simulation.SimulationGraph;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;


public class IntersectionMenu extends VBox {

	// TODO extract constants
	private static final Slider SPEED_SLIDER = new Slider(0, 1000, 815);
	private static final Slider TIMELINE_SLIDER = new Slider(0, 1, 0);

	private static final Button INTERSECTION_MODE = new Button("Real");
	private static final Button PLAY_BUTTON = new Button("Play");
	private static final Button RESTART_BUTTON = new Button("Restart");
	private static final Button SAVE_AGENTS_BUTTON = new Button("Save agents");

	private static final Label STEPS_LABEL = new Label("#n");
	private static final Label DELAY_LABEL = new Label("#n");
	private static final Label REJECTIONS_LABEL = new Label("#n");
	private static final Label COLLISIONS_LABEL = new Label("#n");
	private static final Label REMAINING_LABEL = new Label();
	private static final Label SPEED_LABEL = new Label("Speed");
	private static final Label TIMELINE_LABEL = new Label("Timeline");


	private static boolean abstractMode = false;
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
		sliders.addRow(0, SPEED_LABEL, SPEED_SLIDER);
		sliders.addRow(1, TIMELINE_LABEL, TIMELINE_SLIDER);
		sliders.setVgap(padding);

		SPEED_SLIDER.valueProperty().addListener(getSliderValueListener(SimulationMenuTab3.getSpeedSlider()));
		TIMELINE_SLIDER.valueProperty().addListener(getSliderValueListener(SimulationMenuTab3.getTimeline()));
		SimulationMenuTab3.getSpeedSlider().valueProperty().addListener(getSliderValueListener(SPEED_SLIDER));
		SimulationMenuTab3.getTimeline().valueProperty().addListener(getSliderValueListener(TIMELINE_SLIDER));

		addPlayButtonAction();
		addResetButtonAction();

		SimulationMenuTab3.getPlayButton().setOnMouseClicked(PLAY_BUTTON.getOnMouseClicked());
		SimulationMenuTab3.getRestartButton().setOnMouseClicked(RESTART_BUTTON.getOnMouseClicked());
		SimulationMenuTab3.getSaveAgentsButton().setOnMouseClicked(SAVE_AGENTS_BUTTON.getOnMouseClicked());
		SAVE_AGENTS_BUTTON.setPrefWidth(Double.MAX_VALUE);

		GridPane statistics = new GridPane();
		Map<Statistics, Label> labels = Map.of(Statistics.STEPS, STEPS_LABEL, Statistics.DELAY, DELAY_LABEL, Statistics.REJECTIONS, REJECTIONS_LABEL, Statistics.COLLISIONS, COLLISIONS_LABEL, Statistics.REMAINS, REMAINING_LABEL);
		SimulationMenuTab3.createStatisticsGrid(statistics, labels);

		PLAY_BUTTON.setVisible(true);
		// TODO on Windows button overlay each other
//		HBox buttons = new HBox(PLAY_BUTTON /*, RESTART_BUTTON */);
		HBox buttons = new HBox(padding, PLAY_BUTTON);
//		buttons.getChildren().add(RESTART_BUTTON);

		buttons.setPrefWidth(Double.MAX_VALUE);
		getChildren().addAll(sliders, buttons, RESTART_BUTTON, SAVE_AGENTS_BUTTON, new Label("Statistics"), statistics);
	}

	private static void addResetButtonAction() {
		RESTART_BUTTON.setOnMouseClicked(e -> IntersectionScene.resetSimulation());
	}

	private static ChangeListener<Number> getSliderValueListener(Slider affectedSlider) {
		return (observable, oldValue, newValue) -> affectedSlider.setValue(newValue.doubleValue());
	}

	private static void addPlayButtonAction() {
		// TODO extract constants
		PLAY_BUTTON.setOnMouseClicked(e -> {
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

	public static boolean isAbstract() {
		return abstractMode;
	}

	public static void setAbstractMode(boolean abstractMode) {
		IntersectionMenu.abstractMode = abstractMode;
		setIntersectionModeButtonText();
	}

	public static double getSpeed() {
		return SPEED_SLIDER.getValue();
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
		return STEPS_LABEL;
	}

	public static void setStep(long step) {
		Platform.runLater(() -> {
			STEPS_LABEL.setText(String.valueOf(step));
			SimulationMenuTab3.getStepsLabel().setText(String.valueOf(step));
		});
	}

	public static void setCollisions(long collisions) {
		Platform.runLater(() -> {
			COLLISIONS_LABEL.setText(String.valueOf(collisions));
			SimulationMenuTab3.getCollisionsLabel().setText(String.valueOf(collisions));
		});
	}

	public static Label getDelayLabel() {
		return DELAY_LABEL;
	}

	public static Label getRejectionsLabel() {
		return REJECTIONS_LABEL;
	}

	public static Label getCollisionsLabel() {
		return COLLISIONS_LABEL;
	}

	public static Label getRemainingLabel() {
		return REMAINING_LABEL;
	}
}
