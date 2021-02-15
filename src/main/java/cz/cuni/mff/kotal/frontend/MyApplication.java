package cz.cuni.mff.kotal.frontend;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.menu.MenuStage;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;


public class MyApplication extends Application {
	private static final String STAGE_NAME = "Autonomous Intersection Management";
	private static final int INTERSECTION_MENU_WIDTH = 250;

	private static Window menuStage;


	/**
	 * Start application by creating 2 windows, menu and intersection.
	 *
	 * @param primaryStage Stage where the intersection scene will be drawn.
	 */
	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle(STAGE_NAME);

		Rectangle2D primaryDisplayBounds = Screen.getPrimary().getVisualBounds();
		double width = primaryDisplayBounds.getWidth(),
			height = primaryDisplayBounds.getHeight(),
			x = primaryDisplayBounds.getMinX(),
			y = primaryDisplayBounds.getMinY();

		double menuStageWidth = width - height - INTERSECTION_MENU_WIDTH;

		menuStage = new MenuStage(menuStageWidth, height, x, y);

		createIntersectionStage(primaryStage, height, x + menuStageWidth, y);
	}

	/**
	 * Process stage parameters and create scene with intersection.
	 * Before return call redraw on the intersection to draw the intersection for the first time.
	 *
	 * @param primaryStage Stage where to assign intersection scene.
	 * @param height       Desired height of the stage.
	 * @param x            Stage x coordination on the monitor.
	 * @param y            Stage y coordination on the monitor.
	 */
	private void createIntersectionStage(Stage primaryStage, double height, double x, double y) {
		Scene scene = new IntersectionScene(height + INTERSECTION_MENU_WIDTH, height);
		primaryStage.setScene(scene);

		primaryStage.setX(x);
		primaryStage.setY(y);

		// TODO extract constants
		primaryStage.setMinWidth(250 + IntersectionScene.PADDING * 2.5);
		primaryStage.setMinHeight(IntersectionScene.PADDING * 2 + 37);

		primaryStage.setMaximized(false);
		primaryStage.show();

		IntersectionScene.getIntersectionGraph().redraw();
	}

	/**
	 * @return Window containing menu settings.
	 */
	public static Window getMenuStage() {
		return menuStage;
	}
}