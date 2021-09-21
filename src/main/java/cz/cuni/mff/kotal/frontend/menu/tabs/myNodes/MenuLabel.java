package cz.cuni.mff.kotal.frontend.menu.tabs.myNodes;


import javafx.geometry.Pos;
import javafx.scene.control.Label;


/**
 * Custom Label for menu scene.
 */
public class MenuLabel extends Label {
	/**
	 * Create new label with provided text and set position alignment to baseline left.
	 *
	 * @param text Text the label should contain
	 */
	public MenuLabel(String text) {
		super(text);
		setAlignment(Pos.BASELINE_LEFT);
	}
}
