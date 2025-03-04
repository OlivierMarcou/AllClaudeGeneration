package net.arkaine;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HostsManager extends Application {
    private static final String HOSTS_FILE = "/etc/hosts";
    private ListView<HostEntry> hostsListView;
    private TextField newEntryField;
    private List<HostEntry> hostEntries;
    private static String sudoPassword = null;  // Stockage statique du mot de passe
    @Override
    public void start(Stage primaryStage) {
        hostEntries = new ArrayList<>();

        // Demander le mot de passe au démarrage
        if (!requestSudoPassword()) {
            System.exit(1);
            return;
        }

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        hostsListView = new ListView<>();
        hostsListView.setPrefHeight(300);

        newEntryField = new TextField();
        newEntryField.setPromptText("Entrez une nouvelle ligne hosts");

        Button addButton = new Button("Ajouter");
        addButton.setOnAction(e -> addNewEntry());

        // Nouveau bouton de rafraîchissement
        Button refreshButton = new Button("Rafraîchir");
        refreshButton.setOnAction(e -> refreshHostsFile());

        HBox buttonBox = new HBox(10, addButton, refreshButton);

        loadHostsFile();

        hostsListView.setCellFactory(lv -> new HostEntryCell(this::saveHostsFile));

        root.getChildren().addAll(
                new Label("Fichier /etc/hosts:"),
                hostsListView,
                newEntryField,
                buttonBox
        );

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Gestionnaire de fichier hosts");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    private void refreshHostsFile() {
        try {
            // Sauvegarde les états actuels des cases
            List<Boolean> previousStates = hostsListView.getItems().stream()
                    .map(HostEntry::isEnabled)
                    .collect(Collectors.toList());

            // Vide la liste actuelle
            hostEntries.clear();
            hostsListView.getItems().clear();

            // Recharge le fichier
            List<String> lines = Files.readAllLines(Paths.get(HOSTS_FILE));

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    HostEntry entry = new HostEntry(line);

                    // Restaure l'état précédent si possible
                    if (i < previousStates.size()) {
                        entry.setEnabled(previousStates.get(i));
                    }

                    hostEntries.add(entry);
                }
            }

            hostsListView.getItems().addAll(hostEntries);

        } catch (IOException e) {
            showError("Erreur lors du rafraîchissement", e.getMessage());
        }
    }


    private boolean requestSudoPassword() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Authentification Sudo");

        VBox dialogVbox = new VBox(10);
        dialogVbox.setAlignment(Pos.CENTER);
        dialogVbox.setPadding(new Insets(20));

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe sudo");

        Label messageLabel = new Label("Veuillez entrer votre mot de passe sudo :");
        Button validateButton = new Button("Valider");

        dialogVbox.getChildren().addAll(messageLabel, passwordField, validateButton);

        Scene dialogScene = new Scene(dialogVbox, 300, 150);
        dialog.setScene(dialogScene);

        final boolean[] result = {false};
        Runnable validatePassword = () -> {
            String password = passwordField.getText();
            if (SudoAuthenticator.authenticate(password)) {
                sudoPassword = password;
                result[0] = true;
                dialog.close();
            } else {
                messageLabel.setText("Mot de passe incorrect ! Réessayez :");
                passwordField.clear();
            }
        };
        validateButton.setOnAction(e -> validatePassword.run());
        passwordField.setOnAction(e -> validatePassword.run());
        dialog.showAndWait();
        return result[0];
    }

    private boolean saveHostsFile(List<String> lines) {
        if (sudoPassword != null) {
            String content = String.join("\n", lines);
            if (SudoAuthenticator.writeToHostsFile(content, sudoPassword)) {
                return true;
            }
        }
        // Si l'écriture échoue, on redemande le mot de passe
        if (requestSudoPassword()) {
            String content = String.join("\n", lines);
            return SudoAuthenticator.writeToHostsFile(content, sudoPassword);
        }
        return false;
    }

    private void loadHostsFile() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(HOSTS_FILE));
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    hostEntries.add(new HostEntry(line));
                }
            }
            hostsListView.getItems().addAll(hostEntries);
        } catch (IOException e) {
            showError("Erreur lors de la lecture du fichier hosts", e.getMessage());
        }
    }

    private void addNewEntry() {
        String newEntry = newEntryField.getText().trim();
        if (!newEntry.isEmpty()) {
            List<String> allLines = new ArrayList<>();
            for (HostEntry entry : hostEntries) {
                allLines.add(entry.toString());
            }
            allLines.add(newEntry);

            if (saveHostsFile(allLines)) {
                HostEntry entry = new HostEntry(newEntry);
                hostEntries.add(entry);
                hostsListView.getItems().add(entry);
                newEntryField.clear();
            }
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}