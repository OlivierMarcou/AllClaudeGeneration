package net.arkaine;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HostEntryCell extends ListCell<HostEntry> {
    private HBox hbox;
    private CheckBox checkBox;
    private Label label;
    private Function<List<String>, Boolean> saveCallback;

    public HostEntryCell(Function<List<String>, Boolean> saveCallback) {
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
            return;
        }

        label.setText(entry.getContent());

        // Délie la propriété précédente pour éviter des effets secondaires
        checkBox.selectedProperty().unbindBidirectional(getItem() != null ? getItem().enabledProperty() : null);

        // Mise à jour de l'état de la case à cocher
        checkBox.setSelected(entry.isEnabled());

        // Lie la nouvelle propriété
        checkBox.selectedProperty().bindBidirectional(entry.enabledProperty());

        checkBox.setOnAction(e -> {
            // Crée une copie de la liste à sauvegarder
            List<String> lines = getListView().getItems()
                    .stream()
                    .map(HostEntry::toString)
                    .collect(Collectors.toList());

            if (!saveCallback.apply(lines)) {
                // Si la sauvegarde échoue, annule le changement
                checkBox.setSelected(!checkBox.isSelected());
            }
        });

        setGraphic(hbox);
    }
}