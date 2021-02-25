package cz.cuni.mff.kotal.frontend.intersection;


import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;


public class IntersectionScene extends Scene {
	public static final double PADDING = 15;

	private static final VBox MENU = new IntersectionMenu(PADDING);
	private static final IntersectionModel GRAPH = new IntersectionModel(Screen.getPrimary().getVisualBounds().getHeight());
	private static final SimulationAgents AGENTS = new SimulationAgents(Screen.getPrimary().getVisualBounds().getHeight());
	private static final HBox ROOT = new HBox(MENU, new StackPane(GRAPH, AGENTS));


	/**
	 * Create intersection scene object.
	 * Create actions on window resize, add intersection menu and graph objects.
	 *
	 * @param width  Desired width of the scene.
	 * @param height Desired height of the scene.
	 */
	public IntersectionScene(double width, double height) {
		super(ROOT, width, height);

		heightProperty().addListener((observable, oldValue, newValue) -> {
			double newHeight = Math.min(newValue.doubleValue(), getWidth() - MENU.getMinWidth());
			IntersectionModel.setPreferredHeight(newHeight);
			MENU.setPrefWidth(getWidth() - newHeight);
			GRAPH.redraw();
		});

		widthProperty().addListener((observable, oldValue, newValue) -> {
			double newHeight = Math.min(getHeight(), newValue.doubleValue() - MENU.getMinWidth());
			IntersectionModel.setPreferredHeight(newHeight);
			MENU.setPrefWidth(newValue.doubleValue() - newHeight - 1);
			GRAPH.redraw();
		});


		double menuWidth = width - height;
		setMenuProperties(menuWidth);

		setButtonsProperties(menuWidth);
	}

	/**
	 * Set minimum width for play and restart button.
	 *
	 * @param menuWidth Minimal acceptable width for buttons.
	 */
	private void setButtonsProperties(double menuWidth) {
		//      IntersectionMenu.getPlayButton().setPrefWidth(menuWidth / 2 - PADDING / 2);
		IntersectionMenu.getPlayButton().setMinWidth(menuWidth / 2 - PADDING);
		IntersectionMenu.getPlayButton().setPrefWidth(Double.MAX_VALUE);
//      IntersectionMenu.getRestartButton().setPrefWidth(menuWidth / 2 - PADDING / 2);
		IntersectionMenu.getRestartButton().setMinWidth(menuWidth / 2 - PADDING);
		IntersectionMenu.getRestartButton().setPrefWidth(Double.MAX_VALUE);
	}

	/**
	 * Set menu VBox width sizes and add padding.
	 *
	 * @param menuWidth Minimal acceptable width for menu.
	 */
	private void setMenuProperties(double menuWidth) {
		MENU.setPrefWidth(menuWidth);
		MENU.setMinWidth(menuWidth);
		MENU.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));
	}

	/**
	 * Get Intersection graph object.
	 *
	 * @return Graph constant.
	 */
	public static IntersectionModel getIntersectionGraph() {
		return GRAPH;
	}

	/**
	 * Get Intersection menu object.
	 *
	 * @return Menu VBox.
	 */
	public static VBox getMENU() {
		return MENU;
	}
}
