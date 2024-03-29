package cz.cuni.mff.kotal.frontend.menu.tabs;


import cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes.MenuLabel;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


/**
 * Tab in menu window for setting simulation parameters.
 */
public class SimulationMenuTab3 extends MyTabTemplate {

	// TODO don't use static, use Component
	// TODO extract constants and rename constants
	private static final Slider SPEED_SLIDER = new Slider();
	private static final Slider TIMELINE_SLIDER = new Slider(0, 0, 0);
	private static final Label agensLabel = new Label("#n");
	private static final Label stepsLabel = new Label("#n");
	private static final Label delayLabel = new Label("#n");
	private static final Label rejectionsLabel = new Label("#n");
	private static final Label collisionsLabel = new Label("#n");
	private static final Label remainingLabel = new Label();
	private static final Button PLAY_BUTTON = new Button("Play");
	private static final Button RESTART_BUTTON = new Button("Restart");
	private static final Button SAVE_AGENTS_BUTTON = new Button("Save agents");

	/**
	 * Create new tab with nodes, add actions.
	 */
	public SimulationMenuTab3() {
		super(Tabs.T3.getText());

		createStatisticsGrid((GridPane) Parameters.STATISTICS.getParameter());

		int row = 0;
		for (@NotNull Parameters parameter : Parameters.values()) {
			if (parameter != Parameters.BUTTONS) {
				addRow(row++, new MenuLabel(parameter.getText()), parameter.getParameter());
			} else {
				getGrid().getChildren().add(parameter.getParameter());
				GridPane.setConstraints(parameter.getParameter(), 0, row++, 3, 1);
			}
		}
	}

	/**
	 * Add statistics nodes to grid.
	 *
	 * @param grid Grid to add parameter nodes
	 */
	public static void createStatisticsGrid(@NotNull GridPane grid) {
		// TODO remove constants
		grid.setHgap(20);
		int row = 0;
		for (Parameters.@NotNull Statistics statistic : Parameters.Statistics.values()) {
			@NotNull Label label = new Label(statistic.getText());
			Label labelValue = statistic.getValue();
			grid.getChildren().addAll(label, labelValue);
			GridPane.setConstraints(label, 0, row);
			GridPane.setConstraints(labelValue, 1, row++);
		}
	}

	/**
	 * Add statistics nodes to grid.
	 *
	 * @param grid   Grid to add parameter nodes
	 * @param labels Labels for nodes
	 */
	public static void createStatisticsGrid(@NotNull GridPane grid, @NotNull Map<Parameters.Statistics, Label> labels) {
		// TODO remove constants
		grid.setHgap(20);
		int row = 0;
		for (Parameters.@NotNull Statistics statistic : Parameters.Statistics.values()) {
			@NotNull Label label = new Label(statistic.getText());
			Label labelValue = labels.get(statistic);
			grid.getChildren().addAll(label, labelValue);
			GridPane.setConstraints(label, 0, row);
			GridPane.setConstraints(labelValue, 1, row++);
		}
	}

	/**
	 * @return Speed slider
	 */
	public static @NotNull Slider getSpeedSlider() {
		return SPEED_SLIDER;
	}

	/**
	 * @return Timeline slider
	 */
	public static @NotNull Slider getTimelineSlider() {
		return TIMELINE_SLIDER;
	}

	/**
	 * @return Label for number of agents that arrived
	 */
	public static @NotNull Label getAgentsLabel() {
		return agensLabel;
	}

	/**
	 * @return Steps label
	 */
	public static @NotNull Label getStepsLabel() {
		return stepsLabel;
	}

	/**
	 * @return Delay label
	 */
	public static @NotNull Label getDelayLabel() {
		return delayLabel;
	}

	/**
	 * @return Rejections label
	 */
	public static @NotNull Label getRejectionsLabel() {
		return rejectionsLabel;
	}

	/**
	 * @return Collisions label
	 */
	public static @NotNull Label getCollisionsLabel() {
		return collisionsLabel;
	}

	/**
	 * @return Remaining time label
	 */
	public static @NotNull Label getRemainingLabel() {
		return remainingLabel;
	}

	/**
	 * @return Play button
	 */
	public static @NotNull Button getPlayButton() {
		return PLAY_BUTTON;
	}

	/**
	 * @return Restart button
	 */
	public static @NotNull Button getRestartButton() {
		return RESTART_BUTTON;
	}

	/**
	 * @return Save agents button
	 */
	public static @NotNull Button getSaveAgentsButton() {
		return SAVE_AGENTS_BUTTON;
	}

	/**
	 * Parameters shown in this tab.
	 */
	public enum Parameters {
		SPEED("Speed:", SimulationMenuTab3.SPEED_SLIDER),
		TIMELINE("Timeline:", SimulationMenuTab3.TIMELINE_SLIDER),
		// TODO remove constants
		BUTTONS(null, new TilePane(Orientation.HORIZONTAL, 20, 0, PLAY_BUTTON, RESTART_BUTTON, SAVE_AGENTS_BUTTON)),
		STATISTICS("Statistics:", new GridPane()),
		;

		private final String text;
		private final Node parameter;

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
		 * All statistics rows to be shown in statistics section.
		 */
		public enum Statistics {
			AGENTS("Agents:", agensLabel),
			STEPS("Steps:", stepsLabel),
			DELAY("Total delay:", delayLabel),
			REJECTIONS("Rejections:", rejectionsLabel),
			COLLISIONS("Collisions:", collisionsLabel),
//			REMAINS("Remaining time:", remainingLabel),
			;

			private final String text;
			private final Label value;

			Statistics(String text, Label value) {
				this.text = text;
				this.value = value;
			}

			public String getText() {
				return text;
			}

			public Label getValue() {
				return value;
			}
		}
	}
}
