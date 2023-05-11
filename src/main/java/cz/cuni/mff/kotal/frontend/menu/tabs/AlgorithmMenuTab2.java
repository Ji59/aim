package cz.cuni.mff.kotal.frontend.menu.tabs;


import cz.cuni.mff.kotal.backend.algorithm.astar.AStarAll;
import cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingle;
import cz.cuni.mff.kotal.backend.algorithm.astar.AStarSingleGrouped;
import cz.cuni.mff.kotal.backend.algorithm.cbs.CBSAll;
import cz.cuni.mff.kotal.backend.algorithm.cbs.CBSSingleGrouped;
import cz.cuni.mff.kotal.backend.algorithm.sat.SATAll;
import cz.cuni.mff.kotal.backend.algorithm.sat.SATSingle;
import cz.cuni.mff.kotal.backend.algorithm.sat.SATSingleGrouped;
import cz.cuni.mff.kotal.backend.algorithm.simple.SafeLanes;
import cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes.MyComboBox;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * Tab in menu window for algorithm.
 */
public class AlgorithmMenuTab2 extends MyTabTemplate {
	private static final MyComboBox ALGORITHM = new MyComboBox(Arrays.stream(Parameters.Algorithm.values()).map(Parameters.Algorithm::getName).toList());
	private static final GridPane PARAMETERS = new GridPane();

	/**
	 * Create new tab with nodes, add actions.
	 */
	public AlgorithmMenuTab2() {
		super(Tabs.T2.getText());

		Parameters.@Nullable Algorithm algorithm = Parameters.Algorithm.nameOf(ALGORITHM.getValue());

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

	/**
	 * @return Selected algorithm
	 */
	public static Parameters.@Nullable Algorithm getAlgorithm() {
		for (Parameters.@NotNull Algorithm algorithm : Parameters.Algorithm.values()) {
			if (AlgorithmMenuTab2.ALGORITHM.getValue().equals(algorithm.name)) {
				return algorithm;
			}
		}
		return null;
	}

	public static double getDoubleParameter(@NotNull String parameterName, double defaultValue) {
		return getParameter(parameterName, defaultValue, Double.class);
	}

	public static int getIntegerParameter(@NotNull String parameterName, int defaultValue) {
		return getParameter(parameterName, defaultValue, Integer.class);
	}

	public static long getLongParameter(@NotNull String parameterName, long defaultValue) {
		return getParameter(parameterName, defaultValue, Long.class);
	}

	public static boolean getBooleanParameter(@NotNull String parameterName, boolean defaultValue) {
		return getParameter(parameterName, defaultValue, Boolean.class);
	}

	public static String getStringParameter(@NotNull String parameterName, String defaultValue) {
		return getParameter(parameterName, defaultValue, String.class);
	}

	public static <T> @Nullable T getParameter(@NotNull String parameterName, T defaultValue, @NotNull Class<T> tClass) {
		for (@NotNull Node child : PARAMETERS.getChildren()) {
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

	private void setAlgorithmActions(@NotNull TextField description) {
		ALGORITHM.valueProperty().addListener((observable, oldValue, newValue) -> {
			Parameters.@Nullable Algorithm algorithm = Parameters.Algorithm.nameOf(newValue);
			description.setText(algorithm.getDescription());
			setParameters(algorithm);
		});
	}

	private void setParameters(Parameters.@NotNull Algorithm algorithm) {
		Map<String, Object> parameters;
		try {
			parameters = (Map<String, Object>) algorithm.getAlgorithmClass().getField("PARAMETERS").get(null);
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			throw new RuntimeException(e);
		}
		PARAMETERS.getChildren().clear();

		@NotNull Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet().iterator();
		for (int i = 0; iterator.hasNext(); i++) {
			Map.Entry<String, Object> entry = iterator.next();
			String name = entry.getKey();
			@NotNull MenuLabel nameLabel = new MenuLabel(name);
			Node valueNode;
			Object value = entry.getValue();
			if (value instanceof Boolean booleanValue) {
				@NotNull CheckBox checkBox = new CheckBox();
				checkBox.setSelected(booleanValue);
				valueNode = checkBox;
			} else {
				@NotNull TextField textField = new TextField(value.toString());
				textField.setPrefWidth(42);
				valueNode = textField;
			}
			valueNode.setId(name);
			PARAMETERS.addRow(i, nameLabel, valueNode);
		}
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
			SAFE_LANES("Safe lanes", "Every agent follows its line, but checks for collisions.", SafeLanes.class),
			A_STAR("A* single", "This algorithm computes A star algorithm for each agent in non-collision way.", AStarSingle.class),
			A_STAR_SINGLE_GROUPED("A*", "This algorithm extends simple A* to replan single grouped strategy with independence detection improvement.", AStarSingleGrouped.class),
			A_STAR_ALL("A* all", "This algorithm extends simple A* to SubOID strategy with independence detection improvement.", AStarAll.class),
			CBS_SINGLE_GROUPED("CBS single grouped", "Conflict-Based Search extends A* replan single strategy to single grouped strategy.", CBSSingleGrouped.class),
			CBS_ALL("CBS all", "Conflict-Based Search extends A* replan single strategy to SubOID strategy.", CBSAll.class),
			SAT_REPLAN_SINGLE_GROUPED("SAT planner", "Planning with SAT solver with replan single grouped strategy.", SATSingleGrouped.class),
			SAT_REPLAN_SINGLE("SAT planner single", "Planning with SAT solver with replan single strategy.", SATSingle.class),
			SAT_REPLAN_ALL("SAT planner all", "Planning with SAT solver with replan all strategy.", SATAll.class),
			;

			private final String name;
			private final String description;
			private final Class<? extends cz.cuni.mff.kotal.backend.algorithm.Algorithm> algorithmClass;


			Algorithm(String name, String description, Class<? extends cz.cuni.mff.kotal.backend.algorithm.Algorithm> algorithmClass) {
				this.name = name;
				this.description = description;
				this.algorithmClass = algorithmClass;
			}

			public static @Nullable Algorithm nameOf(String name) {
				for (@NotNull Algorithm algorithm : values()) {
					if (algorithm.name.equals(name)) {
						return algorithm;
					}
				}
				return null;
			}

			public static @Nullable Algorithm nameOf(cz.cuni.mff.kotal.backend.algorithm.@NotNull Algorithm algorithm) {
				for (@NotNull Algorithm alg : values()) {
					if (alg.getAlgorithmClass().equals(algorithm.getClass())) {
						return alg;
					}
				}
				return null;
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
