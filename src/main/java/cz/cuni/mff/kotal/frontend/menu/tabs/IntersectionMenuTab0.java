package cz.cuni.mff.kotal.frontend.menu.tabs;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes.MyComboBox;
import cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes.IntSlider;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * Tab in menu window for setting intersection parameters.
 */
public class IntersectionMenuTab0 extends MyTabTemplate {

	// TODO don't use static, use Component
	private static final MyComboBox model = new MyComboBox(Arrays.stream(Parameters.GraphType.values()).map(Parameters.GraphType::getText).toList());
	private static final MyComboBox restriction = new MyComboBox(Arrays.stream(Parameters.Restrictions.values()).map(Parameters.Restrictions::getText).toList());
	private static final IntSlider granularity = new IntSlider(2, 33, 4);
	private static final IntSlider entries = new IntSlider(1, granularity.getIntValue() - 1, 1);
	private static final IntSlider exits = new IntSlider(1, granularity.getIntValue() - entries.getIntValue(), 1);
	private static final Button nextButton = new Button("Next");
	private static final Button previousButton = new Button("previous");
	private static final HBox history = new HBox(20, previousButton, nextButton);

	private static int roads = 4;
	private static int granularityDifference = 0;

	/**
	 * Create new tab with nodes, add actions.
	 */
	public IntersectionMenuTab0() {
		super(Tabs.T0.getText());

		addGranularityActions();
		addEntriesActions();
		addExitsActions();
		addHistoryButtonsActions();
		addModelActions();

		@NotNull Iterator<Parameters> iterator = Arrays.stream(Parameters.values()).iterator();
		for (int index = 0; iterator.hasNext(); index++) {
			Parameters parameter = iterator.next();
			addRow(index, new MenuLabel(parameter.text), parameter.parameter);
		}

		// TODO pridat tlacitko na ulozeni konfigurace
	}

	/**
	 * Add action to model combo box.
	 */
	private void addModelActions() {
		model.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue.equals(newValue)) {
				return;
			}

			Parameters.@Nullable GraphType selected = null;
			for (Parameters.@NotNull GraphType modelType : Parameters.GraphType.values()) {
				if (modelType.getText().equals(newValue)) {
					selected = modelType;
				}
			}
			assert selected != null;

			setSlidersDisable(true);

			roads = selected.getDirections() == null ? 4 : selected.getDirections().size();

			// TODO fix issue when changed from square model to octagonal, there are added entries and exits

			if (selected.equals(Parameters.GraphType.OCTAGONAL)) {
				granularityDifference = 2;
			} else if (selected.equals(Parameters.GraphType.HEXAGONAL)) {
				granularityDifference = 1;
			} else {
				granularityDifference = 0;
			}
			granularity.setMin(2 + granularityDifference);
			correctEntriesExitsValues(granularity.getIntValue());

			SimulationGraph graph = IntersectionScene.getIntersectionGraph().redraw();
			@NotNull Thread thread = new Thread(() -> graph.createDistances(true));
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();

			AgentsMenuTab1.createDirectionsMenuAndAddActions(selected);

			AgentsMenuTab1.getNewAgentsMinimum().setMax(roads * entries.getIntValue());
			AgentsMenuTab1.getNewAgentsMaximum().setMax(roads * entries.getIntValue());

			setSlidersDisable(false);
		});
	}

	private void correctEntriesExitsValues(int granularityValue) {
		setSlidersDisable(true);
		int maxValue = granularityValue - granularityDifference - 1;
		entries.setMax(maxValue);
		exits.setMax(maxValue);

		// correct exits value for the entries to fit
		if (entries.getIntValue() + exits.getIntValue() + granularityDifference > granularityValue) {
			exits.setValue(granularityValue - entries.getIntValue() - granularityDifference);
		}
		setSlidersDisable(false);
	}

	/**
	 * Add action to granularity slider.
	 */
	private void addGranularityActions() {
		granularity.addAction(() -> {
			setSlidersDisable(true);

			int newVal = granularity.getIntValue();
			correctEntriesExitsValues(newVal);

			startGraphDistanceComputing();

			AgentParametersMenuTab4.getMaximalSizeLength().setMax(newVal - 1.);
			AgentParametersMenuTab4.getMinimalSizeLength().setMax(newVal - 1.);
			setSlidersDisable(false);
		});
	}

	private void startGraphDistanceComputing() {
		SimulationGraph graph = IntersectionScene.getIntersectionGraph().redraw();

		@NotNull Thread thread = new Thread(() -> graph.createDistances(true));
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	/**
	 * Add action to entries slider.
	 */
	private void addEntriesActions() {
		entries.addAction((observable, oldValue, newValue) -> adjustEntriesExitsValues(newValue, exits));
		entries.addAction(() -> {
			int newValue = entries.getIntValue();
			adjustAgentsSize();
			AgentsMenuTab1.getNewAgentsMaximum().setMax(roads * newValue);
			AgentsMenuTab1.getNewAgentsMinimum().setMax(roads * newValue);
		});
	}

	/**
	 * Add action to exit slider.
	 */
	private void addExitsActions() {
		exits.addAction((observable, oldValue, newValue) -> adjustEntriesExitsValues(newValue, entries));
		exits.addAction(this::adjustAgentsSize);
	}

	/**
	 * Add actions to history buttons.
	 */
	private void addHistoryButtonsActions() {
		nextButton.setOnMouseClicked(event -> {
			IntersectionScene.getIntersectionGraph().drawNextGraph();
			setValuesFromGraph();
		});

		previousButton.setOnMouseClicked(event -> {
			IntersectionScene.getIntersectionGraph().drawPreviousGraph();
			setValuesFromGraph();
		});
	}

	/**
	 * Set nodes values based on selected graph.
	 */
	private void setValuesFromGraph() {
		SimulationGraph graph = IntersectionModel.getGraph();
		model.setValue(graph.getModel().text);
		granularity.setValue(graph.getGranularity());
		entries.setValue(graph.getEntries());
		exits.setValue(graph.getExits());
	}

	/**
	 * Set disability of sliders.
	 *
	 * @param b New value to be set
	 */
	private void setSlidersDisable(boolean b) {
		granularity.setDisable(b);
		entries.setDisable(b);
		exits.setDisable(b);
	}

	/**
	 * Modify agent size slider values.
	 */
	private void adjustAgentsSize() {
		setSlidersDisable(true);

		startGraphDistanceComputing();

		int entriesVal = entries.getIntValue();
		int exitsVal = exits.getIntValue();
		AgentParametersMenuTab4.getMinimalSizeWidth().setMax(Math.min(entriesVal, exitsVal));
		AgentParametersMenuTab4.getMaximalSizeWidth().setMax(Math.min(entriesVal, exitsVal));

		setSlidersDisable(false);
	}

	private void adjustEntriesExitsValues(@NotNull Number newValue, @NotNull IntSlider slider) {
		setSlidersDisable(true);
		int newVal = newValue.intValue();
		if (newVal + slider.getIntValue() + granularityDifference > granularity.getIntValue()) {
			slider.setValue(granularity.getIntValue() - newVal - granularityDifference);
		}
		setSlidersDisable(false);
	}

	/**
	 * @return Selected model
	 */
	public static Parameters.@NotNull GraphType getModel() {
		for (Parameters.@NotNull GraphType model : Parameters.GraphType.values()) {
			if (model.getText().equals(IntersectionMenuTab0.model.getValue())) {
				return model;
			}
		}
		return Parameters.GraphType.SQUARE;
	}

	/**
	 * @return Restriction combo box
	 */
	public static @NotNull MyComboBox getRestriction() {
		return restriction;
	}

	/**
	 * @return Granularity slider
	 */
	public static @NotNull IntSlider getGranularity() {
		return granularity;
	}

	/**
	 * @return Entries slider
	 */
	public static @NotNull IntSlider getEntries() {
		return entries;
	}

	/**
	 * @return Exits slider
	 */
	public static @NotNull IntSlider getExits() {
		return exits;
	}

	/**
	 * @return Number of entry / exit directions
	 */
	public static int getRoads() {
		return roads;
	}

	/**
	 * Parameters shown in this tab.
	 */
	public enum Parameters {
		MODEL("Model: ", model),
		GRANULARITY("Granularity: ", granularity),
		ENTRY("Entries: ", entries),
		EXIT("Exits: ", exits),
		RESTRICTION("Restriction: ", restriction),
		HISTORY("History: ", history),
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
		 * Supported intersection model types.
		 */
		public enum GraphType {
			SQUARE("Square grid", Arrays.asList('N', 'E', 'S', 'W')),
			HEXAGONAL("Hexagonal grid", Arrays.asList('A', 'B', 'C', 'D', 'E', 'F')),
			OCTAGONAL("Octagonal grid", Arrays.asList('N', 'E', 'S', 'W')),
			CUSTOM("Custom intersection", new ArrayList<>()),
			;

			private final String text;
			private final List<Character> directions;

			GraphType(String text, List<Character> directions) {
				this.text = text;
				this.directions = directions;
			}

			public String getText() {
				return text;
			}

			public List<Character> getDirections() {
				return directions;
			}
		}


		/**
		 * Supported restriction types.
		 */
		public enum Restrictions {
			NO_RESTRICTION("Without restriction"),
			ROUNDABOUT("Roundabout"),
			LANES("Lanes"),
			CUSTOM("Custom restriction"),
			;

			private final String text;

			Restrictions(String text) {
				this.text = text;
			}

			public String getText() {
				return text;
			}
		}
	}
}
