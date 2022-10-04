package cz.cuni.mff.kotal.frontend;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.menu.MenuStage;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;


/**
 * Main GUI class
 */
public class MyApplication extends Application {
	private static final String STAGE_NAME = "Autonomous Intersection Management";
	private static final int INTERSECTION_MENU_WIDTH = 250;

	private static Window menuStage;


	/**
	 * Start application by creating 2 windows, menu and intersection.
	 *
	 * @param primaryStage Stage where the intersection scene will be drawn
	 */
	@Override
	public void start(@NotNull Stage primaryStage) {
		primaryStage.setTitle(STAGE_NAME);
		primaryStage.setOnCloseRequest(event -> System.exit(0));

		Rectangle2D primaryDisplayBounds = Screen.getPrimary().getVisualBounds();
		double width = primaryDisplayBounds.getWidth();
		double height = primaryDisplayBounds.getHeight();
		double x = primaryDisplayBounds.getMinX();
		double y = primaryDisplayBounds.getMinY();

		double menuStageWidth = width - height - INTERSECTION_MENU_WIDTH;

		menuStage = new MenuStage(menuStageWidth, height, x, y);

		createIntersectionStage(primaryStage, height, x + menuStageWidth, y);
	}

	/**
	 * Process stage parameters and create scene with intersection.
	 * Before return call redraw on the intersection to draw the intersection for the first time.
	 *
	 * @param primaryStage Stage where to assign intersection scene
	 * @param height       Desired height of the stage
	 * @param x            Stage X coordinate on the monitor
	 * @param y            Stage Y coordinate on the monitor
	 */
	private void createIntersectionStage(@NotNull Stage primaryStage, double height, double x, double y) {
		@NotNull Scene scene = new IntersectionScene(height + INTERSECTION_MENU_WIDTH, height);
		primaryStage.setScene(scene);

		primaryStage.setX(x);
		primaryStage.setY(y);

		// TODO extract constants
		primaryStage.setMinWidth(250 + IntersectionScene.PADDING * 2.5);
		primaryStage.setMinHeight(IntersectionScene.PADDING * 2 + 37);

		primaryStage.setMaximized(false);
		primaryStage.show();

		SimulationGraph graph = IntersectionScene.getIntersectionGraph().redraw();
		@NotNull Thread thread = new Thread(() -> graph.createDistances(true));
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	/**
	 * @return Window containing menu settings
	 */
	public static Window getMenuStage() {
		return menuStage;
	}
}