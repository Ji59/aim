package cz.cuni.mff.kotal.frontend.menu.tabs;


import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MyComboBox;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.Arrays;
import java.util.stream.Collectors;


public class AlgorithmMenuTab2 extends MyTabTemplate {

	private static final MyComboBox algorithm = new MyComboBox(Arrays.stream(Parameters.Algorithm.values()).map(Parameters.Algorithm::getName).collect(Collectors.toList()));

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

	public static Parameters.Algorithm getAlgorithm() {
		for (Parameters.Algorithm algorithm : Parameters.Algorithm.values()) {
			if (AlgorithmMenuTab2.algorithm.getValue().equals(algorithm.name)) {
				return algorithm;
			}
		}
		// TODO throw exception
		return null;
	}

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

		public enum Algorithm {
			BFS("Breadth First Search", "Finds the shortest path for every agent."),
			A0("Algorithm0", "Description of alg0"),
			A1("Algorithm1", "Description of alg1"),
			A2("Algorithm2", "Description of alg2"),
			;

			private final String name, description;


			Algorithm(String name, String description) {
				this.name = name;
				this.description = description;
			}

			public String getName() {
				return name;
			}

			public String getDescription() {
				return description;
			}
		}
	}
}
