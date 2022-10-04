package cz.cuni.mff.kotal.frontend.intersection;


import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.menu.tabs.AlgorithmMenuTab2;
import cz.cuni.mff.kotal.frontend.menu.tabs.SimulationMenuTab3;
import cz.cuni.mff.kotal.frontend.menu.tabs.SimulationMenuTab3.Parameters.Statistics;
import cz.cuni.mff.kotal.helpers.SimulationSaver;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.DoubleConsumer;


/**
 * Class representing Side menu on main window.
 */
public class IntersectionMenu extends VBox {

	// TODO dont use static, use Component

	// TODO extract constants
	private static final Slider SPEED_SLIDER = new Slider(0, 4096, 2048);

	private static double timelineMaximum = 0;
	private static double timelineValue = 0;
	private static final Slider TIMELINE_SLIDER = new Slider(0, timelineMaximum, timelineValue);
	private static boolean timelineInUse = false;
	private static final Button INTERSECTION_MODE = new Button("Real");
	private static final Button PLAY_BUTTON = new Button("Play");
	private static final Button RESTART_BUTTON = new Button("Restart");
	private static final CheckBox PLAY_IN_BACKGROUND = new CheckBox("Background");
	private static final Button SAVE_AGENTS_BUTTON = new Button("Save agents");
	private static final Button SAVE_STATISTICS_BUTTON = new Button("Save statistics");
	private static final Label AGENTS_LABEL = new Label("#n");
	private static long agentsValue = 0;
	private static final Label STEPS_LABEL = new Label("#n");
	private static double stepValue = 0;
	private static final Label DELAY_LABEL = new Label("#n");
	private static long delayValue = 0;
	private static final Label REJECTIONS_LABEL = new Label("#n");
	private static long rejectionsValue = 0;
	private static final Label COLLISIONS_LABEL = new Label("#n");
	private static long collisionsValue = 0;
	private static final Label REMAINING_LABEL = new Label();
	private static final Label SPEED_LABEL = new Label("Speed");
	private static final Label TIMELINE_LABEL = new Label("Timeline");
	private static boolean statisticsUpdateRunning = false;
	private static final Lock statisticsLock = new ReentrantLock(false);

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

	private static void startSimulation() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		SimulationGraph graph = IntersectionModel.getGraph();
		AlgorithmMenuTab2.Parameters.Algorithm algorithmEnum = AlgorithmMenuTab2.getAlgorithm();
		if (algorithmEnum == null) {
			// TODO exception
			return;
		}
		Algorithm algorithm;
			algorithm = algorithmEnum.getAlgorithmClass().getConstructor(SimulationGraph.class).newInstance(graph);
		IntersectionScene.startSimulation(algorithm);
		setPlayButtonPlaying(true);
	}

	public static void pauseSimulation() {
		IntersectionScene.stopSimulation();
		setPlayButtonPlaying(false);
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

		Slider tabSpeedSlider = SimulationMenuTab3.getSpeedSlider();
		tabSpeedSlider.setMax(SPEED_SLIDER.getMax());
		tabSpeedSlider.setMin(SPEED_SLIDER.getMin());
		tabSpeedSlider.setValue(SPEED_SLIDER.getValue());
		setSliderValuePropertyListeners(SPEED_SLIDER, tabSpeedSlider);
		Slider tabTimelineSlider = SimulationMenuTab3.getTimelineSlider();
		setSliderValuePropertyListeners(TIMELINE_SLIDER, tabTimelineSlider);
		setSliderMaxPropertyListeners(TIMELINE_SLIDER, tabTimelineSlider);

		addSpeedSliderActions();
		addTimelineSliderActions();
		addPlayButtonAction();
		addResetButtonAction();
		addSaveAgentsButtonAction();
		addSaveStatisticsButtonAction();

		SimulationMenuTab3.getPlayButton().setOnMouseClicked(PLAY_BUTTON.getOnMouseClicked());
		SimulationMenuTab3.getRestartButton().setOnMouseClicked(RESTART_BUTTON.getOnMouseClicked());
		SimulationMenuTab3.getSaveAgentsButton().setOnMouseClicked(SAVE_AGENTS_BUTTON.getOnMouseClicked());

		PLAY_IN_BACKGROUND.setSelected(false);

		GridPane statistics = new GridPane();
		Map<Statistics, Label> labels = Map.of(Statistics.AGENTS, AGENTS_LABEL, Statistics.STEPS, STEPS_LABEL, Statistics.DELAY, DELAY_LABEL, Statistics.REJECTIONS, REJECTIONS_LABEL, Statistics.COLLISIONS, COLLISIONS_LABEL /*, Statistics.REMAINS, REMAINING_LABEL*/);
		SimulationMenuTab3.createStatisticsGrid(statistics, labels);

		HBox buttons = new HBox(padding, SAVE_AGENTS_BUTTON, SAVE_STATISTICS_BUTTON);
		buttons.setPrefWidth(Double.MAX_VALUE);
		SAVE_AGENTS_BUTTON.setPrefWidth(4096);
		SAVE_STATISTICS_BUTTON.setPrefWidth(SAVE_AGENTS_BUTTON.getPrefWidth());

		getChildren().addAll(sliders, PLAY_BUTTON, RESTART_BUTTON, PLAY_IN_BACKGROUND, buttons, new Label("Statistics"), statistics);
	}

	/**
	 * Create new listener which sets its new value also to other slider.
	 *
	 * @return
	 */
	@NotNull
	private ChangeListener<Number> getSliderValueListener(DoubleConsumer function) {
		return (observable, oldValue, newValue) -> function.accept(newValue.doubleValue());
	}

	/**
	 * TODO
	 *
	 * @param slider0
	 * @param slider1
	 */
	private void setSliderValuePropertyListeners(Slider slider0, Slider slider1) {
		slider0.valueProperty().addListener(getSliderValueListener(slider1::setValue));
		slider1.valueProperty().addListener(getSliderValueListener(slider0::setValue));
	}

	/**
	 * TODO
	 */
	private void setSliderMaxPropertyListeners(Slider slider0, Slider slider1) {
		slider0.maxProperty().addListener(getSliderValueListener(slider1::setMax));
		slider1.maxProperty().addListener(getSliderValueListener(slider0::setMax));
	}


	/**
	 * Add action to speed slider.
	 */
	private void addSpeedSliderActions() {
		SPEED_SLIDER.setBlockIncrement(8);
		SPEED_SLIDER.valueProperty().addListener((observable, oldValue, newValue) -> IntersectionScene.changeSimulation());
	}

	private void addTimelineSliderActions() {
		Slider menuTimelineSlider = SimulationMenuTab3.getTimelineSlider();
		menuTimelineSlider.setBlockIncrement(1); // TODO extract constant
		TIMELINE_SLIDER.setBlockIncrement(1);
		ChangeListener<Boolean> focusListener = (observable, oldValue, newValue) -> {
			if (Boolean.TRUE.equals(newValue)) {
				pauseSimulation();
			}
		};
		TIMELINE_SLIDER.focusedProperty().addListener(focusListener);
		menuTimelineSlider.focusedProperty().addListener(focusListener);

		ChangeListener<Number> valueListener = (observable, oldValue, newValue) -> {
			if (!playing && (TIMELINE_SLIDER.isFocused() || menuTimelineSlider.isFocused())) {
				setStep(newValue.doubleValue());
				IntersectionScene.startSimulationAt(newValue.doubleValue(), false);  // FIXME set play parameter to playing
			}
		};
		TIMELINE_SLIDER.valueProperty().addListener(valueListener);
		menuTimelineSlider.valueProperty().addListener(valueListener);
	}

	/**
	 * Set action performed on play button press.
	 */
	private static void addPlayButtonAction() {
		// TODO extract constants
		PLAY_BUTTON.setOnMouseClicked(e -> {
			PLAY_BUTTON.setDisable(true);
			if (playing) {
				pauseSimulation();
			} else {
				try {
					startSimulation();
				} catch (Exception ex) {
					final Alert alert = new Alert(Alert.AlertType.ERROR);
					alert.setTitle("Starting simulation failed");
					alert.setHeaderText("Exception occurred during simulation initialization");
					alert.setContentText(ex.getMessage());
					alert.showAndWait();
				}
			}
			PLAY_BUTTON.setDisable(false);
		});
	}

	/**
	 * Set action performed on reset button press.
	 */
	private static void addResetButtonAction() {
		RESTART_BUTTON.setOnMouseClicked(e -> {
			PLAY_BUTTON.setDisable(true);
			pauseSimulation();
			IntersectionScene.resetSimulation();
			PLAY_BUTTON.setDisable(false);
		});
	}

	/**
	 * TODO javadoc
	 */
	private static void addSaveAgentsButtonAction() {
		SAVE_AGENTS_BUTTON.setOnMouseClicked(e -> {
			try {
				// TODO check simulation

				IntersectionMenu.pauseSimulation();

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

				SimulationSaver.saveAgents(IntersectionScene.getSimulation(), agentsFile);
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
	 * TODO
	 */
	private static void addSaveStatisticsButtonAction() {
		SAVE_STATISTICS_BUTTON.setOnMouseClicked(e -> {
			boolean deleteInitialDirectory = false;
			File initialDirectory = Paths.get(FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath(), "statistics").toFile();  // TODO smarter name genereating

			try {
				// TODO check simulation
				IntersectionMenu.pauseSimulation();

				deleteInitialDirectory = initialDirectory.mkdir();

				File statisticsDirectory = getStatisticsSaveDirectory(initialDirectory);
				if (statisticsDirectory == null) {
					return;
				}

				SimulationSaver.saveStatistics(IntersectionScene.getSimulation(), statisticsDirectory);
				deleteInitialDirectory = false;

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
			} finally {
				if (deleteInitialDirectory) {
					initialDirectory.delete();
				}
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
	 * TODO javadoc
	 *
	 * @return
	 */
	private static File getStatisticsSaveDirectory(File initialDirectory) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Statistics data save directory"); // FIXME get rid of this stoopid name
		directoryChooser.setInitialDirectory(initialDirectory);
		return directoryChooser.showDialog(null); // FIXME replace null
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
		IntersectionMenu.playing = playing;
		// TODO extract constants
		String text = playing ? "Pause" : "Play";// TODO
		if (Platform.isFxApplicationThread()) {
			PLAY_BUTTON.setText(text); // TODO
			SimulationMenuTab3.getPlayButton().setText(text);
		} else {
			Platform.runLater(() -> {
				PLAY_BUTTON.setText(text); // TODO
				SimulationMenuTab3.getPlayButton().setText(text);
			});
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
	 * TODO
	 */
	public static boolean decreaseSpeed() {
		if (SPEED_SLIDER.getValue() <= SPEED_SLIDER.getMin()) {
			return false;
		}

		SPEED_SLIDER.decrement();
		System.out.println(SPEED_SLIDER.getValue());
		return true;
	}

	public static boolean decreaseSpeedToThreeFourths() {
		if (SPEED_SLIDER.getValue() <= SPEED_SLIDER.getMin()) {
			return false;
		}

		SPEED_SLIDER.setValue(SPEED_SLIDER.getValue() * 3 / 4);
		System.out.println(SPEED_SLIDER.getValue());
		return true;
	}

	/**
	 * TODO
	 */
	public static boolean increaseSpeed() {
		if (SPEED_SLIDER.getValue() >= SPEED_SLIDER.getMax()) {
			return false;
		}

		SPEED_SLIDER.increment();
		System.out.println(SPEED_SLIDER.getValue());
		return true;
	}

	/**
	 * @return Speed set on slider
	 */
	public static double getSpeed() {
		return SPEED_SLIDER.getValue();
	}

	private static void timelineDaemonTask() {
		while (true) {
			try {
				Thread.sleep(Long.MAX_VALUE);
			} catch (InterruptedException ignored) {
			}
			TIMELINE_SLIDER.setMax(timelineMaximum);
			TIMELINE_SLIDER.setValue(timelineValue);
		}
	}

	/**
	 * TODO
	 *
	 * @param maximum
	 */
	public static void setTimelineMaximum(double value, double maximum) {
		synchronized (TIMELINE_SLIDER) {
			timelineValue = value;
			timelineMaximum = maximum;
		}
		if (timelineInUse) {
			return;
		}
		timelineInUse = true;
		Platform.runLater(() -> {
			updateTimelineMaximumTask();
			timelineInUse = false;
		});
	}

	public static void forceTimelineMaximum(double value, double maximum) {
		synchronized (TIMELINE_SLIDER) {
			timelineValue = value;
			timelineMaximum = maximum;
		}
		Platform.runLater(IntersectionMenu::updateTimelineMaximumTask);
	}

	private static void updateTimelineMaximumTask() {
		synchronized (TIMELINE_SLIDER) {
			TIMELINE_SLIDER.setMax(timelineMaximum);
			if (!TIMELINE_SLIDER.isFocused()) {
				TIMELINE_SLIDER.setValue(timelineValue);
			}
		}
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

	public static boolean playSimulationInBackground() {
		return PLAY_IN_BACKGROUND.isSelected();
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

	private static void updateStatisticsTask() {
		if (statisticsUpdateRunning) {
			return;
		}
		statisticsUpdateRunning = true;
		statisticsLock.lock();
		String agentsFormatted = String.format("%,d", agentsValue);
		String stepFormatted = String.format("%,.2f", stepValue);  // TODO extract constant
		String delayFormatted = String.format("%,d", delayValue);
		String rejectionsFormatted = String.format("%,d", rejectionsValue);
		String collisionsFormatted = String.valueOf(collisionsValue);
		statisticsLock.unlock();

		Platform.runLater(() -> {
			updateStatisticsTask(agentsFormatted, stepFormatted, delayFormatted, rejectionsFormatted, collisionsFormatted);
			statisticsUpdateRunning = false;
		});
	}

	public static void forceUpdateStatistics(long agents, double step, long delays, long rejections, long collisions) {
		statisticsLock.lock();
		String agentsFormatted = String.format("%,d", agentsValue = agents);
		String stepFormatted = String.format("%,.2f", stepValue = step);  // TODO extract constant
		String delayFormatted = String.format("%,d", delayValue = delays);
		String rejectionsFormatted = String.format("%,d", rejectionsValue = rejections);
		String collisionsFormatted = String.valueOf(collisionsValue = collisions);
		statisticsLock.unlock();

		Platform.runLater(() -> {
			updateStatisticsTask(agentsFormatted, stepFormatted, delayFormatted, rejectionsFormatted, collisionsFormatted);
			statisticsUpdateRunning = false;
		});
	}

	private static void updateStatisticsTask(String agentsFormatted, String stepFormatted, String delayFormatted, String rejectionsFormatted, String collisionsFormatted) {
		STEPS_LABEL.setText(stepFormatted);
		SimulationMenuTab3.getStepsLabel().setText(stepFormatted);
		AGENTS_LABEL.setText(agentsFormatted);
		SimulationMenuTab3.getAgentsLabel().setText(agentsFormatted);
		DELAY_LABEL.setText(delayFormatted);
		SimulationMenuTab3.getDelayLabel().setText(delayFormatted);
		REJECTIONS_LABEL.setText(rejectionsFormatted);
		SimulationMenuTab3.getRejectionsLabel().setText(rejectionsFormatted);
		COLLISIONS_LABEL.setText(collisionsFormatted);
		SimulationMenuTab3.getCollisionsLabel().setText(collisionsFormatted);
	}

	/**
	 * Set value of both agent labels. Should be total number of agents that arrived.
	 *
	 * @param agents Value to be shown on agent labels
	 */
	public static void setAgents(long agents) {
		statisticsLock.lock();
		agentsValue = agents;
		statisticsLock.unlock();
		updateStatisticsTask();
	}

	/**
	 * Set value of both step labels.
	 *
	 * @param step Value to be shown on labels
	 */
	public static void setStep(double step) {
		statisticsLock.lock();
		stepValue = step;
		statisticsLock.unlock();
		updateStatisticsTask();
	}

	/**
	 * Set value of both delay labels.
	 *
	 * @param delay Value to be shown on labels
	 */
	public static void setDelay(long delay) {
		statisticsLock.lock();
		delayValue = delay;
		statisticsLock.unlock();
		updateStatisticsTask();
	}

	/**
	 * Set value of both rejections labels.
	 *
	 * @param rejections Value to be shown on labels
	 */
	public static void setRejections(long rejections) {
		statisticsLock.lock();
		rejectionsValue = rejections;
		statisticsLock.unlock();
		updateStatisticsTask();
	}

	/**
	 * Set value of both collision labels.
	 *
	 * @param collisions Value to be shown on labels
	 */
	public static void setCollisions(long collisions) {
		statisticsLock.lock();
		collisionsValue = collisions;
		statisticsLock.unlock();
		updateStatisticsTask();
	}

	public static void setAgentsDelayRejections(long agents, long delay, long rejections) {
		statisticsLock.lock();
		agentsValue = agents;
		delayValue = delay;
		rejectionsValue = rejections;
		statisticsLock.unlock();
		updateStatisticsTask();
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
