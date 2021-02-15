package cz.cuni.mff.kotal.frontend.menu;


import cz.cuni.mff.kotal.frontend.menu.scenes.MenuScene;
import javafx.stage.Stage;


public class MenuStage extends Stage {
	public static final String STAGE_NAME = "Settings";

	/**
	 * Create stage and menu scene.
	 *
	 * @param width  Desired width of the scene.
	 * @param height Desired height of the scene.
	 * @param x      Stage x coordination on the monitor.
	 * @param y      Stage y coordination on the monitor.
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