package cz.cuni.mff.kotal.frontend.menu.scenes;


import cz.cuni.mff.kotal.frontend.menu.tabs.*;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;


/**
 * Window for setting up the intersection and simulation.
 */
public class MenuScene extends Scene {

	/**
	 * Create menu scene, create tabs and add them.
	 *
	 * @param width  Desired width of the scene
	 * @param height Desired height of the scene
	 */
	public MenuScene(double width, double height) {
		super(new TabPane(), width, height);
		TabPane tabPane = (TabPane) getRoot();

		tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		tabPane.getTabs().addAll(
			new IntersectionMenuTab0(),
			new AgentsMenuTab1(),
			new AlgorithmMenuTab2(),
			new SimulationMenuTab3(),
			new AgentParametersMenuTab4()
		);
	}
}
