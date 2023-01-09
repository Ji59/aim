package cz.cuni.mff.kotal.frontend.menu.tabs;


import cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes.DoubleSlider;
import cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes.IntSlider;
import cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes.MenuLabel;
import cz.cuni.mff.kotal.frontend.menu.tabs.my_nodes.MyComboBox;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Arrays;


/**
 * Tab in menu window for setting agent parameters.
 */
public class AgentParametersMenuTab4 extends MyTabTemplate {

	public static final double DEFAULT_AGENT_LENGTH = 0.56;
	public static final double DEFAULT_AGENT_WIDTH = 0.35;

	// TODO don't use static, use Component
	// TODO rename constants
	// TODO redesign size properties
	private static final DoubleSlider MINIMAL_LENGTH_SLIDER = new DoubleSlider(0.1, IntersectionMenuTab0.getGranularity().getIntValue() - 1., DEFAULT_AGENT_LENGTH);
	private static final DoubleSlider MINIMAL_WIDTH_SLIDER = new DoubleSlider(0.1, Math.min(IntersectionMenuTab0.getEntries().getIntValue(), IntersectionMenuTab0.getExits().getIntValue()), DEFAULT_AGENT_WIDTH);
	private static final DoubleSlider MAXIMAL_LENGTH_SLIDER = new DoubleSlider(0.1, IntersectionMenuTab0.getGranularity().getIntValue() - 1., DEFAULT_AGENT_LENGTH);
	private static final DoubleSlider MAXIMAL_WIDTH_SLIDER = new DoubleSlider(0.1, Math.min(IntersectionMenuTab0.getEntries().getIntValue(), IntersectionMenuTab0.getExits().getIntValue()), DEFAULT_AGENT_WIDTH);
	private static final IntSlider MINIMAl_SPEED_SLIDER = new IntSlider(1, IntersectionMenuTab0.getGranularity().getIntValue(), 1);
	private static final IntSlider MAXIMAL_SPEED_SLIDER = new IntSlider(1, IntersectionMenuTab0.getGranularity().getIntValue(), 1);
	private static final Slider SPEED_DEVIATION_SLIDER = new Slider(0, 1, 0);

	/**
	 * Create new tab with nodes, add actions.
	 */
	public AgentParametersMenuTab4() {
		super(Tabs.T4.getText());

		createSizeSlidersAndAddActions();

		createSpeedSlidersAndAddActions();

		// TODO pridat akce

		int row = 0;
		for (@NotNull Parameters parameter : Parameters.values()) {
			addRow(row++, new MenuLabel(parameter.getText()), parameter.getParameter());
		}
	}

	/**
	 * Create size sliders and assign actions to them.
	 */
	private void createSizeSlidersAndAddActions() {
		addSliderValueChanges(MINIMAL_LENGTH_SLIDER, MAXIMAL_LENGTH_SLIDER);
		addSliderValueChanges(MINIMAL_WIDTH_SLIDER, MAXIMAL_WIDTH_SLIDER);

		GridPane grid = (GridPane) Parameters.SIZE.getParameter();
		// TODO pryc s tim
		@NotNull Label l0 = new Label("L");
		@NotNull Label l1 = new Label("L");
		@NotNull Label w0 = new Label("W");
		@NotNull Label w1 = new Label("W");
		l0.setPrefWidth(20);
		l1.setPrefWidth(20);
		w0.setPrefWidth(20);
		w1.setPrefWidth(20);
		// TODO odebrat konstanty
		grid.addRow(0, new Label("Minimum:"), new VBox(new HBox(10, l0, MINIMAL_LENGTH_SLIDER), new HBox(10, w0, MINIMAL_WIDTH_SLIDER)));
		grid.addRow(1, new Label("Maximum:"), new VBox(new HBox(10, l1, MAXIMAL_LENGTH_SLIDER), new HBox(10, w1, MAXIMAL_WIDTH_SLIDER)));
		grid.setHgap(20);
		grid.setVgap(20);
	}

	private void addSliderValueChanges(final DoubleSlider minimalSizeLength, final DoubleSlider maximalSizeLength) {
		minimalSizeLength.addAction((observable, oldValue, newValue) -> {
			final double newVal = newValue.doubleValue();
			if (maximalSizeLength.getValue() < newVal) {
				maximalSizeLength.setValue(newVal);
			}
		});
		maximalSizeLength.addAction((observable, oldValue, newValue) -> {
			final double newVal = newValue.doubleValue();
			if (minimalSizeLength.getValue() > newVal) {
				minimalSizeLength.setValue(newVal);
			}
		});
	}

	/**
	 * Create speed sliders and assign actions to them.
	 */
	private void createSpeedSlidersAndAddActions() {
		@NotNull Label speedDeviationLabel = new Label("0%");

		MINIMAl_SPEED_SLIDER.addAction((observable, oldValue, newValue) -> {
			if (MAXIMAL_SPEED_SLIDER.getIntValue() < newValue.intValue()) {
				MAXIMAL_SPEED_SLIDER.setValue(newValue.intValue());
			}
		});
		MAXIMAL_SPEED_SLIDER.addAction((observable, oldValue, newValue) -> {
			if (MINIMAl_SPEED_SLIDER.getIntValue() > newValue.intValue()) {
				MINIMAl_SPEED_SLIDER.setValue(newValue.intValue());
			}
		});
		SPEED_DEVIATION_SLIDER.valueProperty().addListener((observable, oldValue, newValue) -> {
			speedDeviationLabel.setText(new DecimalFormat("###.##%").format(newValue));
		});
		SPEED_DEVIATION_SLIDER.setBlockIncrement(0.05);
		SPEED_DEVIATION_SLIDER.setMajorTickUnit(0.2);
		SPEED_DEVIATION_SLIDER.setMinorTickCount(3);
		SPEED_DEVIATION_SLIDER.setShowTickLabels(true);
		SPEED_DEVIATION_SLIDER.setShowTickMarks(true);

		// TODO odstranit konstanty
		GridPane grid = (GridPane) Parameters.SPEED.getParameter();
		grid.addRow(0, new Label("Minimum:"), MINIMAl_SPEED_SLIDER);
		grid.addRow(1, new Label("Maximum:"), MAXIMAL_SPEED_SLIDER);
		grid.addRow(2, new Label("Deviation:"), SPEED_DEVIATION_SLIDER, speedDeviationLabel);
		grid.setHgap(20);
		grid.setVgap(20);
	}

	/**
	 * @return Minimal size length slider
	 */
	public static @NotNull DoubleSlider getMinimalLengthSlider() {
		return MINIMAL_LENGTH_SLIDER;
	}

	/**
	 *
	 * @return
	 */
	public static double getMinimalLength() {
		return MINIMAL_LENGTH_SLIDER.getValue();
	}

	/**
	 * @return Minimal size width slider
	 */
	public static @NotNull DoubleSlider getMinimalWidthSlider() {
		return MINIMAL_WIDTH_SLIDER;
	}

	public static double getMinimalWidth() {
		return MINIMAL_WIDTH_SLIDER.getValue();
	}

	/**
	 * @return Maximal size length slider
	 */
	public static @NotNull DoubleSlider getMaximalLengthSlider() {
		return MAXIMAL_LENGTH_SLIDER;
	}
	
	public static double getMaximalLength() {
		return MAXIMAL_LENGTH_SLIDER.getValue();
	}

	/**
	 * @return Maximal size width slider
	 */
	public static @NotNull DoubleSlider getMaximalWidthSlider() {
		return MAXIMAL_WIDTH_SLIDER;
	}

	public static double getMaximalWidth() {
		return MAXIMAL_WIDTH_SLIDER.getValue();
	}

	/**
	 * @return Minimal speed slider
	 */
	public static @NotNull IntSlider getMinimalSpeedSlider() {
		return MINIMAl_SPEED_SLIDER;
	}

	public static int getMinimalSpeed() {
		return MINIMAl_SPEED_SLIDER.getIntValue();
	}

	/**
	 * @return Maximal speed slider
	 */
	public static @NotNull IntSlider getMaximalSpeedSlider() {
		return MAXIMAL_SPEED_SLIDER;
	}

	public static int getMaximalSpeed() {
		return MAXIMAL_SPEED_SLIDER.getIntValue();
	}

	/**
	 * @return Speed deviation slider
	 */
	public static @NotNull Slider getSpeedDeviationSlider() {
		return SPEED_DEVIATION_SLIDER;
	}

	public static double getSpeedDeviation() {
		return SPEED_DEVIATION_SLIDER.getValue();
	}

	public static Parameters.Random getRandomDistribution() {
		return Parameters.Random.value(((MyComboBox)Parameters.RNG.getParameter()).getValue());
	}

	/**
	 * Parameters shown in this tab.
	 */
	public enum Parameters {
		SIZE("Size:", new GridPane()),
		SPEED("Speed:", new GridPane()),
		RNG("RNG distribution:", new MyComboBox(Arrays.stream(Random.values()).map(Random::getText).toList())),
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
		 * Supported random distributions to show in parameter combo box.
		 */
		public enum Random {
			// TODO pridat funkcni predpis
			GAUSS("Gauss"),
			LINEAR("Linear"),
			;

			private final String text;

			Random(String text) {
				this.text = text;
			}

			public String getText() {
				return text;
			}

			public static @Nullable Random value(String name) {
				for (Random random : Random.values()) {
					if (random.getText().equals(name)) {
						return random;
					}
				}
				return null;
			}
		}
	}
}
