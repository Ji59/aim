package cz.cuni.mff.kotal.frontend.menu.tabs.myNodes;


import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;

import java.util.List;


/**
 * Custom combo box used in this project.
 */
public class MyComboBox extends ComboBox<String> {
	/**
	 * Create new combo box with provide items and select the first item.
	 *
	 * @param items List of items to be available to chose
	 */
	public MyComboBox(List<String> items) {
		super(FXCollections.observableList(items));
		getSelectionModel().selectFirst();
	}
}
