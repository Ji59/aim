package cz.cuni.mff.kotal.frontend.menu.tabs;


import cz.cuni.mff.kotal.backend.algorithm.*;
import cz.cuni.mff.kotal.backend.algorithm.astar.*;
import cz.cuni.mff.kotal.backend.algorithm.cbs.CBSSingleGrouped;
import cz.cuni.mff.kotal.backend.algorithm.sat.SATAll;
import cz.cuni.mff.kotal.backend.algorithm.sat.SATRoundabout;
import cz.cuni.mff.kotal.backend.algorithm.sat.SATSingle;
import cz.cuni.mff.kotal.backend.algorithm.sat.SATSingleGrouped;
import cz.cuni.mff.kotal.backend.algorithm.simple.BidirectionalRoundabout;
import cz.cuni.mff.kotal.backend.algorithm.simple.Roundabout;
import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLines;
import cz.cuni.mff.kotal.backend.algorithm.simple.Semaphore;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MyComboBox;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
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
		GridPane.setConstraints(PARAMETERS, 1, 2, 2, 1);

	}

	private void setAlgorithmActions(TextField description) {
		ALGORITHM.valueProperty().addListener((observable, oldValue, newValue) -> {
			Parameters.Algorithm algorithm = Parameters.Algorithm.nameOf(newValue);
			description.setText(algorithm.getDescription());
			setParameters(algorithm);
		});
	}

	private void setParameters(Parameters.Algorithm algorithm) {
		Map<String, Object> parameters;
		try {
			parameters = (Map<String, Object>) algorithm.getAlgorithmClass().getField("PARAMETERS").get(null);
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			// TODO
			throw new RuntimeException(e);
		}
		PARAMETERS.getChildren().clear();

		Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet().iterator();
		for (int i = 0; iterator.hasNext(); i++) {
			Map.Entry<String, Object> entry = iterator.next();
			String name = entry.getKey();
			MenuLabel nameLabel = new MenuLabel(name);
			Node valueNode;
			Object value = entry.getValue();
			if (value instanceof Boolean booleanValue) {
				CheckBox checkBox = new CheckBox();
				checkBox.setSelected(booleanValue);
				valueNode = checkBox;
			} else {
				TextField textField = new TextField(value.toString());
				textField.setPrefWidth(42);
				valueNode = textField;
			}
			valueNode.setId(name);
			PARAMETERS.addRow(i, nameLabel, valueNode);
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
	 * TODO
	 *
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
	public static double getDoubleParameter(String parameterName, double defaultValue) {
		return getParameter(parameterName, defaultValue, Double.class);
	}

	/**
	 * TODO
	 *
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
	public static int getIntegerParameter(String parameterName, int defaultValue) {
		return getParameter(parameterName, defaultValue, Integer.class);
	}

	/**
	 * TODO
	 *
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBooleanParameter(String parameterName, boolean defaultValue) {
		return getParameter(parameterName, defaultValue, Boolean.class);
	}

	/**
	 * TODO
	 *
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
	public static String getStringParameter(String parameterName, String defaultValue) {
		return getParameter(parameterName, defaultValue, String.class);
	}


	/**
	 * TODO
	 *
	 * @param parameterName
	 * @param defaultValue
	 * @return
	 */
	public static <T> T getParameter(String parameterName, T defaultValue, Class<T> tClass) {
		for (Node child : PARAMETERS.getChildren()) {
			if (parameterName.equals(child.getId())) {
				try {
					if (tClass == Boolean.class) {
						return (T) Boolean.valueOf(((CheckBox) child).isSelected());
					} else {
						return tClass.getConstructor(String.class).newInstance(((TextField) child).getText());
					}
				} catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
					// TODO
					((TextField) child).setText(String.valueOf(defaultValue));
					return defaultValue;
				}
			}
		}
		return defaultValue;
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
			ROUNDABOUT("Roundabout", "Standard one way one lane roundabout.", Roundabout.class),
			BI_ROUNDABOUT("Bidirectional Roundabout", "One lane roundabout where agents can travel both directions.", BidirectionalRoundabout.class),
			A_STAR("A* single", "This algorithm computes A star algorithm for each agent in non-collision way.", AStarSingle.class),
			A_STAR_SINGLE_GROUPED("A*", "TODO", AStarSingleGrouped.class),
			A_STAR_ALL("A* all", "TODO", AStarAll.class),
			A_STAR_ROUNDABOUT("A* single roundabout", "Roundabout supporting multiple lanes. Path is found using A* algorithm.", AStarRoundabout.class),
			A_STAR_BI_ROUNDABOUT("A* single bidirectional roundabout", "Bidirectional roundabout supporting multiple lanes. Path is found using A* algorithm.", AStarBidirectionalRoundabout.class),
			CBS_SINGLE_GROUPED("CBS single brouped", "TODO", CBSSingleGrouped.class),
			SAT_REPLAN_SINGLE_GROUPED("SAT planner", "Planning with SAT solver.", SATSingleGrouped.class),
			SAT_REPLAN_SINGLE("SAT planner single", "Planning with SAT solver.", SATSingle.class),
			SAT_REPLAN_ALL("SAT planner all", "Planning with SAT solver.", SATAll.class),
			SAT_ROUNDABOUT("SAT roundabout", "TODO", SATRoundabout.class),
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

			public static @Nullable Algorithm nameOf(String name) {
				for (Algorithm algorithm : values()) {
					if (algorithm.name.equals(name)) {
						return algorithm;
					}
				}
				return null;
			}

			public static @Nullable Algorithm nameOf(cz.cuni.mff.kotal.backend.algorithm.Algorithm algorithm) {
				for (Algorithm alg : values()) {
					if (alg.getAlgorithmClass().equals(algorithm.getClass())) {
						return alg;
					}
				}
				return null;
			}
		}
	}
}
