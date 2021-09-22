package cz.cuni.mff.kotal.frontend.menu;


import cz.cuni.mff.kotal.frontend.menu.scenes.MenuScene;
import javafx.stage.Stage;


/**
 * Window containing menu tabs with settings.
 */
public class MenuStage extends Stage {
	public static final String STAGE_NAME = "Settings";

	/**
	 * Create stage and menu scene.
	 *
	 * @param width  Desired width of the scene.
	 * @param height Desired height of the scene.
	 * @param x      Stage X coordinate on the monitor.
	 * @param y      Stage Y coordinate on the monitor.
	 */
	public MenuStage(double width, double height, double x, double y) {
		setTitle(STAGE_NAME);
		setMaximized(false);

		MenuScene menuScene = new MenuScene(width, height);
		setScene(menuScene);

		setX(x);
		setY(y);

		show();
	}
}