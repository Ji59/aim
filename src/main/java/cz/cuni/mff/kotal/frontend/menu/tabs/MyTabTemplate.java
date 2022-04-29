package cz.cuni.mff.kotal.frontend.menu.tabs;


import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;

import java.util.Arrays;
import java.util.Iterator;


/**
 * Abstract class implementing common methods.
 */
public abstract class MyTabTemplate extends Tab {
	private final GridPane grid = new GridPane();

	/**
	 * Create new tab with provided name, set node grid parameters.
	 *
	 * @param text Name og the tab
	 */
	protected MyTabTemplate(String text) {
		super(text);

		// TODO zavest konstanty
		grid.setHgap(20);
		grid.setVgap(50);
		grid.setPadding(new Insets(30, 30, 30, 30));

		setContent(grid);
	}

	/**
	 * TODO
	 *
	 * @param index
	 * @param node
	 * @return
	 */
	public MyTabTemplate addRowNode(int index, Node node) {
		grid.add(node, 0, index, grid.getColumnCount(), 1);
		return this;
	}

	/**
	 * Add row of nodes to the grid.
	 *
	 * @param index    Index of row
	 * @param children List of nodes to add
	 * @return This object
	 */
	public MyTabTemplate addRow(int index, Node... children) {
		grid.addRow(index, children);
		Iterator<Node> iterator = Arrays.stream(children).iterator();
		for (int i = 0; iterator.hasNext(); i++) {
			GridPane.setConstraints(iterator.next(), i, index);
		}
		return this;
	}

	/**
	 * Add row of nodes to the grid and set their visibility to false.
	 *
	 * @param index    Index of row
	 * @param children List of nodes to add
	 * @return This object
	 */
	public MyTabTemplate addInvisibleRow(int index, Node... children) {
		grid.addRow(index, children);
		Iterator<Node> iterator = Arrays.stream(children).iterator();
		for (int i = 0; iterator.hasNext(); i++) {
			Node child = iterator.next();
			GridPane.setConstraints(child, i, index);
			child.setVisible(false);
		}
		return this;
	}

	/**
	 * @return Node grid for elements
	 */
	public GridPane getGrid() {
		return grid;
	}
}
