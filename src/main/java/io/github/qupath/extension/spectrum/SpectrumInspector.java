package io.github.qupath.extension.spectrum;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.gui.viewer.tools.PathTool;

import java.util.Locale;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reusable, non-modal UI for channel navigation and spectrum inspection.
 */
final class SpectrumInspector {

    private static final String CUSTOM_SLIDER_STYLE = "channel-slider-custom";

    private final QuPathGUI qupath;
    private final ResourceBundle resources;
    private final PathTool probeTool;
    private final Runnable activateProbe;
    private final Runnable windowHidden;
    private final SpectrumSampler sampler = new SpectrumSampler();
    private final AtomicLong sampleSequence = new AtomicLong();

    private final Stage stage = new Stage();
    private final Slider channelSlider = new Slider();
    private final Label channelValueLabel = new Label();
    private final Label imageLabel = new Label();
    private final Label coordinateLabel = new Label();
    private final Label statusLabel = new Label();
    private final Label toolStatusLabel = new Label();
    private final Button activateButton = new Button();
    private final CategoryAxis channelAxis = new CategoryAxis();
    private final NumberAxis intensityAxis = new NumberAxis();
    private final LineChart<String, Number> spectrumChart = new LineChart<>(channelAxis, intensityAxis);
    private final TableView<ChannelValueRow> table = new TableView<>();

    private ChannelDisplayController channelController;
    private boolean synchronizingSlider;
    private int lastAppliedSliderValue = Integer.MIN_VALUE;

    SpectrumInspector(
            QuPathGUI qupath,
            ResourceBundle resources,
            PathTool probeTool,
            Runnable activateProbe,
            Runnable windowHidden) {
        this.qupath = qupath;
        this.resources = resources;
        this.probeTool = probeTool;
        this.activateProbe = activateProbe;
        this.windowHidden = windowHidden;
        initializeStage();
        qupath.viewerProperty().addListener((observable, oldViewer, newViewer) -> refreshForViewer());
        qupath.imageDataProperty().addListener((observable, oldImage, newImage) -> refreshForViewer());
        qupath.getToolManager().selectedToolProperty().addListener((observable, oldTool, newTool) -> refreshToolState());
        refreshForViewer();
        refreshToolState();
    }

    void showAndActivate() {
        if (!stage.isShowing())
            stage.show();
        stage.toFront();
        stage.requestFocus();
        activateProbe.run();
        refreshForViewer();
    }

    void requestSample(QuPathViewer viewer, int x, int y) {
        long requestId = sampleSequence.incrementAndGet();
        if (viewer == null || viewer.getServer() == null) {
            setStatus(resources.getString("status.noImage"), true);
            return;
        }
        var server = viewer.getServer();
        if (x < 0 || y < 0 || x >= server.getWidth() || y >= server.getHeight()) {
            setStatus(resources.getString("status.outside"), true);
            return;
        }

        int z = viewer.getZPosition();
        int t = viewer.getTPosition();
        String imageName = qupath.getDisplayedImageName(viewer.getImageData());
        setStatus(resources.getString("status.reading"), false);

        sampler.sample(server, imageName, x, y, z, t).whenComplete((sample, error) ->
                Platform.runLater(() -> {
                    if (requestId != sampleSequence.get())
                        return;
                    var activeViewer = qupath.getViewer();
                    if (activeViewer != viewer || activeViewer == null || activeViewer.getServer() == null
                            || !server.getPath().equals(activeViewer.getServer().getPath())
                            || activeViewer.getZPosition() != z || activeViewer.getTPosition() != t)
                        return;
                    if (error != null) {
                        Throwable cause = unwrap(error);
                        setStatus(resources.getString("status.readError") + ": " + cause.getMessage(), true);
                    } else {
                        displaySample(sample);
                    }
                }));
    }

    private void initializeStage() {
        stage.initOwner(qupath.getStage());
        stage.setTitle(resources.getString("window.title"));
        stage.setMinWidth(620);
        stage.setMinHeight(520);

        imageLabel.setStyle("-fx-font-weight: bold;");
        coordinateLabel.setWrapText(true);
        statusLabel.setWrapText(true);
        toolStatusLabel.setWrapText(true);

        activateButton.setText(resources.getString("tool.activate"));
        activateButton.setOnAction(event -> activateProbe.run());

        configureSlider();
        configureChart();
        configureTable();

        var header = new VBox(7,
                imageLabel,
                coordinateLabel,
                new HBox(8, toolStatusLabel, activateButton),
                new Separator(),
                createChannelControls(),
                statusLabel);
        header.setPadding(new Insets(12, 12, 8, 12));

        var splitPane = new SplitPane(spectrumChart, table);
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPositions(0.58);

        var root = new BorderPane(splitPane);
        root.setTop(header);
        var scene = new Scene(root, 780, 680);
        var stylesheet = SpectrumInspector.class.getResource("spectrum.css");
        if (stylesheet != null)
            scene.getStylesheets().add(stylesheet.toExternalForm());
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            event.consume();
            stage.hide();
        });
        stage.setOnHidden(event -> windowHidden.run());
    }

    private HBox createChannelControls() {
        var title = new Label(resources.getString("channel.slider"));
        title.setMinWidth(Label.USE_PREF_SIZE);
        channelValueLabel.setMinWidth(165);
        HBox.setHgrow(channelSlider, Priority.ALWAYS);
        return new HBox(10, title, channelSlider, channelValueLabel);
    }

    private void configureSlider() {
        channelSlider.setMin(0);
        channelSlider.setMax(1);
        channelSlider.setMajorTickUnit(1);
        channelSlider.setMinorTickCount(0);
        channelSlider.setBlockIncrement(1);
        channelSlider.setSnapToTicks(true);
        channelSlider.setShowTickMarks(true);
        channelSlider.setShowTickLabels(true);
        channelSlider.setTooltip(new Tooltip(resources.getString("channel.tooltip")));
        channelSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (synchronizingSlider || channelController == null)
                return;
            int rounded = (int)Math.round(newValue.doubleValue());
            if (rounded == lastAppliedSliderValue)
                return;
            lastAppliedSliderValue = rounded;
            channelController.selectSliderValue(rounded);
        });
    }

    private void configureChart() {
        channelAxis.setLabel(resources.getString("chart.channelAxis"));
        intensityAxis.setLabel(resources.getString("chart.intensityAxis"));
        intensityAxis.setForceZeroInRange(false);
        spectrumChart.setAnimated(false);
        spectrumChart.setCreateSymbols(true);
        spectrumChart.setLegendVisible(false);
        spectrumChart.setTitle(resources.getString("chart.title"));
    }

    private void configureTable() {
        var indexColumn = new TableColumn<ChannelValueRow, Number>(resources.getString("table.index"));
        indexColumn.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().index()));
        indexColumn.setPrefWidth(85);

        var nameColumn = new TableColumn<ChannelValueRow, String>(resources.getString("table.name"));
        nameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        nameColumn.setPrefWidth(300);

        var valueColumn = new TableColumn<ChannelValueRow, Number>(resources.getString("table.value"));
        valueColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().value()));
        valueColumn.setPrefWidth(180);

        table.getColumns().setAll(List.of(indexColumn, nameColumn, valueColumn));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(resources.getString("table.placeholder")));
    }

    private void refreshForViewer() {
        sampleSequence.incrementAndGet();
        spectrumChart.getData().clear();
        table.getItems().clear();
        coordinateLabel.setText("");
        if (channelController != null)
            channelController.close();

        var viewer = qupath.getViewer();
        channelController = viewer == null ? null : new ChannelDisplayController(viewer);
        if (channelController != null)
            channelController.addStateListener(this::synchronizeChannelState);

        if (viewer == null || viewer.getServer() == null) {
            imageLabel.setText(resources.getString("image.none"));
            setStatus(resources.getString("status.noImage"), false);
        } else {
            imageLabel.setText(qupath.getDisplayedImageName(viewer.getImageData()));
            setStatus(resources.getString("status.clickPrompt"), false);
        }
        synchronizeChannelState();
    }

    private void synchronizeChannelState() {
        synchronizingSlider = true;
        try {
            ChannelSelectionState state = channelController == null
                    ? ChannelSelectionState.unsupported()
                    : channelController.getState();
            int count = Math.max(1, state.channelCount());
            channelSlider.setMax(count);
            channelSlider.setDisable(state.kind() == ChannelSelectionState.Kind.UNSUPPORTED);
            channelSlider.getStyleClass().remove(CUSTOM_SLIDER_STYLE);

            switch (state.kind()) {
                case ALL -> {
                    channelSlider.setValue(0);
                    channelValueLabel.setText(resources.getString("channel.all"));
                }
                case SINGLE -> {
                    int sliderValue = state.channelIndex() + 1;
                    channelSlider.setValue(sliderValue);
                    String name = channelController.getChannelNames().get(state.channelIndex());
                    channelValueLabel.setText(String.format(Locale.ROOT, "%d · %s", sliderValue, name));
                }
                case CUSTOM -> {
                    channelSlider.getStyleClass().add(CUSTOM_SLIDER_STYLE);
                    channelValueLabel.setText(resources.getString("channel.custom"));
                }
                case UNSUPPORTED -> {
                    channelSlider.setValue(0);
                    channelValueLabel.setText(resources.getString("channel.unsupported"));
                }
            }
            lastAppliedSliderValue = (int)Math.round(channelSlider.getValue());
        } finally {
            synchronizingSlider = false;
        }
    }

    private void refreshToolState() {
        boolean active = qupath.getToolManager().getSelectedTool() == probeTool;
        toolStatusLabel.setText(resources.getString(active ? "tool.active" : "tool.inactive"));
        activateButton.setDisable(active);
    }

    private void displaySample(SpectrumSample sample) {
        imageLabel.setText(sample.imageName());
        coordinateLabel.setText(String.format(Locale.ROOT,
                resources.getString("sample.coordinates"),
                sample.x(), sample.y(), sample.z(), sample.t(), sample.pixelType(), sample.channelNames().size()));

        double[] intensities = sample.intensities();
        var rows = FXCollections.<ChannelValueRow>observableArrayList();
        var series = new XYChart.Series<String, Number>();
        for (int index = 0; index < intensities.length; index++) {
            String name = sample.channelNames().get(index);
            rows.add(new ChannelValueRow(index + 1, name, intensities[index]));
            String category = (index + 1) + " · " + name;
            series.getData().add(new XYChart.Data<>(category, intensities[index]));
        }
        table.setItems(rows);
        spectrumChart.getData().setAll(List.of(series));
        setStatus(resources.getString("status.ready"), false);
    }

    private void setStatus(String text, boolean error) {
        statusLabel.setText(text == null ? "" : text);
        statusLabel.setStyle(error ? "-fx-text-fill: -fx-text-base-color; -fx-font-weight: bold;" : "");
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable result = throwable;
        while ((result instanceof CompletionException || result instanceof SpectrumSampler.SpectrumReadException)
                && result.getCause() != null)
            result = result.getCause();
        return result;
    }

    private record ChannelValueRow(int index, String name, double value) {
    }
}
