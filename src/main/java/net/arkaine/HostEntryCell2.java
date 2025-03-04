package net.arkaine;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HostEntryCell2 extends ListCell<HostEntry> {
    private HBox hbox;
    private CheckBox checkBox;
    private Label label;
    private Function<List<String>, Boolean> saveCallback;

    public HostEntryCell2(Function<List<String>, Boolean> saveCallback) {
        this.saveCallback = saveCallback;
        hbox = new HBox(10);
        checkBox = new CheckBox();
        label = new Label();
        hbox.getChildren().addAll(checkBox, label);
    }

    @Override
    protected void updateItem(HostEntry entry, boolean empty) {
        super.updateItem(entry, empty);

        if (empty || entry == null) {
            setGraphic(null);
        } else {
            label.setText(entry.getContent());
            checkBox.setSelected(entry.isEnabled());

            checkBox.selectedProperty().bindBidirectional(entry.enabledProperty());

            checkBox.setOnAction(e -> {
                List<String> lines = getListView().getItems()
                        .stream()
                        .map(HostEntry::toString)
                        .collect(Collectors.toList());

                if (!saveCallback.apply(lines)) {
                    // Si la sauvegarde échoue, on revient à l'état précédent
                    checkBox.setSelected(!checkBox.isSelected());
                }
            });

            setGraphic(hbox);
        }
    }
}