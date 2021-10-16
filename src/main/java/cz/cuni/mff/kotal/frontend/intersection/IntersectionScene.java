package cz.cuni.mff.kotal.frontend.intersection;


import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.simulation.Simulation;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;


/**
 * Scene for intersection map.
 */
public class IntersectionScene extends Scene {
	public static final double PADDING = 15;

	// TODO don't use static, use Component
	private static final IntersectionMenu MENU = new IntersectionMenu(PADDING);
	private static final IntersectionModel GRAPH = new IntersectionModel(Screen.getPrimary().getVisualBounds().getHeight());
	private static final SimulationAgents AGENTS = new SimulationAgents(Screen.getPrimary().getVisualBounds().getHeight());
	private static final HBox ROOT = new HBox(MENU, new StackPane(GRAPH, AGENTS));

	private static Simulation simulation;

	/**
	 * Create intersection scene object.
	 * Create actions on window resize, add intersection menu and graph objects.
	 *
	 * @param width  Desired width of the scene
	 * @param height Desired height of the scene
	 */
	public IntersectionScene(double width, double height) {
		super(ROOT, width, height);

		heightProperty().addListener((observable, oldValue, newValue) -> {
			double newHeight = Math.min(newValue.doubleValue(), getWidth() - MENU.getMinWidth());
			IntersectionModel.setPreferredHeight(newHeight);
			MENU.setPrefWidth(getWidth() - newHeight);
			GRAPH.redraw(true);
		});

		widthProperty().addListener((observable, oldValue, newValue) -> {
			double newHeight = Math.min(getHeight(), newValue.doubleValue() - MENU.getMinWidth());
			IntersectionModel.setPreferredHeight(newHeight);
			MENU.setPrefWidth(newValue.doubleValue() - newHeight - 1);
			GRAPH.redraw(true);
		});


		double menuWidth = width - height;
		setMenuProperties(menuWidth);

		setButtonsProperties(menuWidth);
	}

	/**
	 * Set minimum width for play and restart button.
	 *
	 * @param menuWidth Minimal acceptable width for buttons
	 */
	private void setButtonsProperties(double menuWidth) {
		IntersectionMenu.getPlayButton().setMinWidth(menuWidth / 2 - PADDING);
		IntersectionMenu.getPlayButton().setPrefWidth(Double.MAX_VALUE);
		IntersectionMenu.getRestartButton().setMinWidth(menuWidth / 2 - PADDING);
		IntersectionMenu.getRestartButton().setPrefWidth(Double.MAX_VALUE);
	}

	/**
	 * Set menu VBox width sizes and add padding.
	 *
	 * @param menuWidth Minimal acceptable width for menu
	 */
	private void setMenuProperties(double menuWidth) {
		MENU.setPrefWidth(menuWidth);
		MENU.setMinWidth(menuWidth);
		MENU.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));
	}

	/**
	 * Start the simulation.
	 *
	 * @param algorithm Algorithm to use in simulation path finding
	 */
	public static void startSimulation(Algorithm algorithm) {
		if (simulation == null) {
			simulation = new Simulation(IntersectionModel.getGraph(), algorithm, AGENTS);
			AGENTS.setSimulation(simulation);
			simulation.start((long) getPeriod());
		} else {
			resumeSimulation();
		}
	}

	/**
	 * Resume already started simulation.
	 */
	private static void resumeSimulation() {
		assert simulation != null;
		long period = (long) getPeriod();
		simulation.start(period);
	}

	/**
	 * Compute delay between steps based on speed slider value.
	 *
	 * @return Delay between steps in milliseconds
	 */
	public static double getPeriod() {
		double speed = IntersectionMenu.getSpeed();
		// TODO do speed properly
		return (2500 - Math.sqrt(speed) * 78.8);
	}

	/**
	 * Stop running simulation.
	 */
	public static void stopSimulation() {
		assert simulation != null;
		simulation.stop();
	}

	/**
	 * Restart simulation and other parts to initial state.
	 */
	public static void resetSimulation() {
		if (simulation == null) {
			return;
		}
		IntersectionMenu.setPlayButtonPlaying(false);

		simulation.reset();
		simulation = null;

		AGENTS.resetSimulation();
	}

	/**
	 * Apply changes to running simulation.
	 */
	public static void changeSimulation() {
		if (simulation != null && simulation.isRunning()) {
			stopSimulation();
			resumeSimulation();
		}
	}

	/**
	 * Get Intersection graph object.
	 *
	 * @return Graph constant
	 */
	public static IntersectionModel getIntersectionGraph() {
		return GRAPH;
	}

	/**
	 * Get Intersection menu object.
	 *
	 * @return Menu VBox
	 */
	public static VBox getMENU() {
		return MENU;
	}
}
