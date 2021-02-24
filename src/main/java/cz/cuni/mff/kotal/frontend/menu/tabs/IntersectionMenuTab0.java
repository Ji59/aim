package cz.cuni.mff.kotal.frontend.menu.tabs;


import cz.cuni.mff.kotal.frontend.intersection.IntersectionModel;
import cz.cuni.mff.kotal.frontend.intersection.IntersectionScene;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MyComboBox;
import cz.cuni.mff.kotal.frontend.menu.tabs.myNodes.MySlider;
import cz.cuni.mff.kotal.frontend.simulation.SimulationGraph;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


public class IntersectionMenuTab0 extends MyTabTemplate {

	private static final MyComboBox model = new MyComboBox(Arrays.stream(Parameters.Models.values()).map(Parameters.Models::getText).collect(Collectors.toList())),
		restriction = new MyComboBox(Arrays.stream(Parameters.Restrictions.values()).map(Parameters.Restrictions::getText).collect(Collectors.toList()));
	private static final MySlider granularity = new MySlider(2, 65, 4),
		entries = new MySlider(1, granularity.getValue() - 1, 1),
		exits = new MySlider(1, granularity.getValue() - entries.getValue(), 1);
	private static final Button nextButton = new Button("Next"),
		previousButton = new Button("previous");
	private static final HBox history = new HBox(20, previousButton, nextButton);

	private static long roads = 4;
	private static int granularityDifference = 0;

	public IntersectionMenuTab0() {
		super(Tabs.T0.getText());

		addGranularityActions();
		addEntriesActions();
		addExitsActions();
		addHistoryButtonsActions();
		addModelActions();

		Iterator<Parameters> iterator = Arrays.stream(Parameters.values()).iterator();
		for (int index = 0; iterator.hasNext(); index++) {
			Parameters parameter = iterator.next();
			addRow(index, new MenuLabel(parameter.text), parameter.parameter);
		}

		// TODO pridat tlacitko na ulozeni konfigurace
	}

	private void addModelActions() {
		model.valueProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue.equals(newValue)) {
				return;
			}

			Parameters.Models selected = null;
			for (Parameters.Models model : Parameters.Models.values()) {
				if (model.getText().equals(newValue)) {
					selected = model;
				}
			}
			assert selected != null;

			setSlidersDisable(true);

			roads = selected.getDirections() == null ? 4 : selected.getDirections().size();

			// TODO fix issue when changed from square model to octagonal, there are added entries and exits

			if (selected.equals(Parameters.Models.OCTAGONAL)) {
				granularityDifference = 2;
			} else if (selected.equals(Parameters.Models.HEXAGONAL)) {
				granularityDifference = 1;
			} else {
				granularityDifference = 0;
			}
			granularity.setMin(2 + granularityDifference);
			entries.setMax(granularity.getValue() - granularityDifference - 1);
			exits.setMax(granularity.getValue() - granularityDifference - 1);

			IntersectionScene.getIntersectionGraph().redraw();

			AgentsMenuTab1.createDirectionsMenuAndAddActions(selected);

			AgentsMenuTab1.getNewAgentsMinimum().setMax(roads * entries.getValue());
			AgentsMenuTab1.getNewAgentsMaximum().setMax(roads * entries.getValue());

			setSlidersDisable(false);
		});
	}

	private void addGranularityActions() {
		granularity.addAction((observable, oldValue, newValue) -> {
			setSlidersDisable(true);

			long newVal = newValue.longValue();
			entries.setMax(newVal - granularityDifference - 1);
			exits.setMax(newVal - granularityDifference - 1);
			if (entries.getValue() + exits.getValue() + granularityDifference > newVal) {
				exits.setValue(newVal - entries.getValue() - granularityDifference);
			}

			IntersectionScene.getIntersectionGraph().redraw();

			AgentParametersMenuTab4.getMaximalSizeLength().setMax(newVal - 1);
			AgentParametersMenuTab4.getMinimalSizeLength().setMax(newVal - 1);
			setSlidersDisable(false);
		});
	}

	private void addEntriesActions() {
		entries.addAction((observable, oldValue, newValue) -> {
			adjustAgentsSize(newValue, exits);
			AgentsMenuTab1.getNewAgentsMaximum().setMax(roads * newValue.longValue());
			AgentsMenuTab1.getNewAgentsMinimum().setMax(roads * newValue.longValue());
		});
	}

	private void addExitsActions() {
		exits.addAction((observable, oldValue, newValue) -> adjustAgentsSize(newValue, entries));
	}

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

	private void setValuesFromGraph() {
		SimulationGraph graph = IntersectionModel.getGraph();
		model.setValue(graph.getModel().text);
		granularity.setValue(graph.getGranularity());
		entries.setValue(graph.getEntries());
		exits.setValue(graph.getExits());
	}

	private void setSlidersDisable(boolean b) {
		granularity.setDisable(b);
		entries.setDisable(b);
		exits.setDisable(b);
	}

	private void adjustAgentsSize(Number newValue, MySlider slider) {
		setSlidersDisable(true);

		if (newValue.longValue() + slider.getValue() + granularityDifference > granularity.getValue()) {
			slider.setValue(granularity.getValue() - newValue.longValue() - granularityDifference);
		}

		IntersectionScene.getIntersectionGraph().redraw();

		AgentParametersMenuTab4.getMinimalSizeWidth().setMax(Math.min(newValue.longValue(), exits.getValue()));
		AgentParametersMenuTab4.getMaximalSizeWidth().setMax(Math.min(newValue.longValue(), exits.getValue()));

		setSlidersDisable(false);
	}

	public static Parameters.Models getModel() {
		for (Parameters.Models model : Parameters.Models.values()) {
			if (model.getText().equals(IntersectionMenuTab0.model.getValue())) {
				return model;
			}
		}
		return Parameters.Models.SQUARE;
	}

	public static MyComboBox getRestriction() {
		return restriction;
	}

	public static MySlider getGranularity() {
		return granularity;
	}

	public static MySlider getEntries() {
		return entries;
	}

	public static MySlider getExits() {
		return exits;
	}

	public static long getRoads() {
		return roads;
	}

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

		public enum Models {
			SQUARE("Square grid", Arrays.asList('N', 'S', 'W', 'E')),
			HEXAGONAL("Hexagonal grid", Arrays.asList('A', 'B', 'C', 'D', 'E', 'F')),
			OCTAGONAL("Octagonal grid", Arrays.asList('N', 'S', 'W', 'E')),
			CUSTOM("Custom intersection", new ArrayList<>()),
			;

			private final String text;
			private final List<Character> directions;

			Models(String text, List<Character> directions) {
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
