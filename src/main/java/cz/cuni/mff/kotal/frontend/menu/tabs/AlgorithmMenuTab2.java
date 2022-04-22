package cz.cuni.mff.kotal.frontend.menu.tabs;


import cz.cuni.mff.kotal.backend.algorithm.*;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MyComboBox;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tab in menu window for algorithm.
 */
public class AlgorithmMenuTab2 extends MyTabTemplate {

	// TODO don't use static, use Component
	private static final MyComboBox algorithm = new MyComboBox(Arrays.stream(Parameters.Algorithm.values()).map(Parameters.Algorithm::getName).collect(Collectors.toList()));

	/**
	 * Create new tab with nodes, add actions.
	 */
	public AlgorithmMenuTab2() {
		super(Tabs.T2.getText());

		addRow(0, new MenuLabel(Parameters.ALGORITHM_NAME.getText()), algorithm);

		TextField description = (TextField) Parameters.ALGORITHM_DESCRIPTION.getParameter();

		algorithm.valueProperty().addListener((observable, oldValue, newValue) -> {
			for (Parameters.Algorithm alg : Parameters.Algorithm.values()) {
				if (alg.name.equals(newValue)) {
					description.setText(alg.getDescription());
					return;
				}
			}
		});

		description.setEditable(false);
		description.prefWidthProperty().bind(getGrid().widthProperty());
		description.setAlignment(Pos.BASELINE_LEFT);
		getGrid().getChildren().add(description);
		GridPane.setConstraints(description, 0, 1, 3, 1);
	}

	/**
	 * @return Selected algorithm
	 */
	public static Parameters.Algorithm getAlgorithm() {
		for (Parameters.Algorithm algorithm : Parameters.Algorithm.values()) {
			if (AlgorithmMenuTab2.algorithm.getValue().equals(algorithm.name)) {
				return algorithm;
			}
		}
		// TODO throw exception
		return null;
	}

	/**
	 * Parameters shown in this tab.
	 */
	public enum Parameters {
		ALGORITHM_NAME("Algorithm:", algorithm),
		ALGORITHM_DESCRIPTION(null, new TextField(Algorithm.BFS.getDescription())),
		;

		private final String text;
		private final javafx.scene.Node parameter;

		Parameters(String text, Node parameter) {
			this.text = text;
			this.parameter = parameter;
		}

		public String getText() {
			return text;
		}

		public Node getParameter() {
			return parameter;
		}

		/**
		 * All algorithms to be shown in combo box.
		 */
		public enum Algorithm {
			BFS("Breadth First Search", "Finds the shortest path for every agent.", BreadthFirstSearch.class),
			LINES("Follow lines", "Every agent follows line from start to end.", Lines.class),
			SAFE_LINES("Safe lines", "Every agent follows its line, but checks for collisions.", SafeLines.class),
			SEMAPHORE("Semaphore", "Standard semaphore with direction limit.", Semaphore.class),
			ROUNDABOUT("Roundabout", "Standard one way one line roundabout.", Roundabout.class),
			BI_ROUNDABOUT("Bidirectional Roundabout", "One line roundabout where agents can travel both directions.", BidirectionalRoundabout.class),
			A_STAR("A Star", "This algorithm computes A star algorithm for each agent in non-collision way.", AStar.class),
			;

			private final String name;
			private final String description;
			private final Class<? extends cz.cuni.mff.kotal.backend.algorithm.Algorithm> algorithmClass;


			Algorithm(String name, String description, Class<? extends cz.cuni.mff.kotal.backend.algorithm.Algorithm> algorithmClass) {
				this.name = name;
				this.description = description;
				this.algorithmClass = algorithmClass;
			}

			public String getName() {
				return name;
			}

			public String getDescription() {
				return description;
			}

			public Class<? extends cz.cuni.mff.kotal.backend.algorithm.Algorithm> getAlgorithmClass() {
				return algorithmClass;
			}
		}
	}
}
