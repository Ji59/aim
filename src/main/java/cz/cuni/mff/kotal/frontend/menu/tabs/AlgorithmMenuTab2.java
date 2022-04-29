package cz.cuni.mff.kotal.frontend.menu.tabs;


import cz.cuni.mff.kotal.backend.algorithm.*;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MyComboBox;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * Tab in menu window for algorithm.
 */
public class AlgorithmMenuTab2 extends MyTabTemplate {

	// TODO don't use static, use Component
	private static final MyComboBox ALGORITHM = new MyComboBox(Arrays.stream(Parameters.Algorithm.values()).map(Parameters.Algorithm::getName).toList());
	private static final GridPane PARAMETERS = new GridPane();

	/**
	 * Create new tab with nodes, add actions.
	 */
	public AlgorithmMenuTab2() {
		super(Tabs.T2.getText());

		Parameters.Algorithm algorithm = Parameters.Algorithm.nameOf(ALGORITHM.getValue());

		addRow(0, new MenuLabel(Parameters.ALGORITHM_NAME.getText()), ALGORITHM);

		TextField description = (TextField) Parameters.ALGORITHM_DESCRIPTION.getParameter();

		setAlgorithmActions(description);

		description.setText(algorithm.getDescription());
		description.setEditable(false);
		description.prefWidthProperty().bind(getGrid().widthProperty());
		description.setAlignment(Pos.BASELINE_LEFT);
		addRowNode(1, description);

		setParameters(algorithm);
		PARAMETERS.prefWidthProperty().bind(getGrid().widthProperty());
		PARAMETERS.setHgap(20);
		PARAMETERS.setVgap(50);
		addRow(2, new MenuLabel(Parameters.ALGORITHM_PARAMETERS.getText()), PARAMETERS);

	}

	private void setAlgorithmActions(TextField description) {
		ALGORITHM.valueProperty().addListener((observable, oldValue, newValue) -> {
			Parameters.Algorithm algorithm = Parameters.Algorithm.nameOf(newValue);
			description.setText(algorithm.getDescription());
			setParameters(algorithm);
		});
	}

	private void setParameters(Parameters.Algorithm algorithm) {
		Map<String, Integer> parameters;
		try {
			parameters = (Map<String, Integer>) algorithm.getAlgorithmClass().getField("PARAMETERS").get(null);
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			// TODO
			throw new RuntimeException(e);
		}
		PARAMETERS.getChildren().clear();

		Iterator<Map.Entry<String, Integer>> iterator = parameters.entrySet().iterator();
		for (int i = 0; iterator.hasNext(); i++) {
			Map.Entry<String, Integer> entry = iterator.next();
			String name = entry.getKey();
			Integer value = entry.getValue();
			MenuLabel nameLabel = new MenuLabel(name);
			TextField valueField = new TextField(value.toString());
			valueField.setPrefWidth(69);
			valueField.setId(name);
			PARAMETERS.addRow(i, nameLabel, valueField);
		}
	}

	/**
	 * @return Selected algorithm
	 */
	public static Parameters.Algorithm getAlgorithm() {
		for (Parameters.Algorithm algorithm : Parameters.Algorithm.values()) {
			if (AlgorithmMenuTab2.ALGORITHM.getValue().equals(algorithm.name)) {
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
		ALGORITHM_NAME("Algorithm:", ALGORITHM),
		ALGORITHM_DESCRIPTION(null, new TextField()),
		ALGORITHM_PARAMETERS("Parameters:", PARAMETERS),
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
			SAFE_LINES("Safe lines", "Every agent follows its line, but checks for collisions.", SafeLines.class),
			BFS("Breadth First Search", "Finds the shortest path for every agent.", BreadthFirstSearch.class),
			LINES("Follow lines", "Every agent follows line from start to end.", Lines.class),
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

			public static Algorithm nameOf(String name) {
				for (Algorithm algorithm : values()) {
					if (algorithm.name.equals(name)) {
						return algorithm;
					}
				}
				return null;
			}
		}
	}
}
