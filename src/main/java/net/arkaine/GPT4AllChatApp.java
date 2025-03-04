package net.arkaine;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import okhttp3.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GPT4AllChatApp extends Application {
    private static final String API_URL = "http://localhost:4891/v1/chat/completions";
    private static final String API_MODELS_URL = "http://localhost:4891/v1/models";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    private ComboBox<String> modelComboBox;

    @Override
    public void start(Stage primaryStage) {
        // Création des composants
        TextArea inputArea = new TextArea();
        inputArea.setPromptText("Entrez votre prompt ici...");
        inputArea.setWrapText(true);
        inputArea.setPrefHeight(100);

        TextArea responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.setWrapText(true);
        responseArea.setPrefHeight(300);

        // Combo box pour les modèles
        modelComboBox = new ComboBox<>();
        modelComboBox.setPromptText("Sélectionnez un modèle");
        modelComboBox.setPrefWidth(500);

        // Charger les modèles disponibles
        loadAvailableModels(modelComboBox, responseArea);

        Button sendButton = new Button("Envoyer");
        sendButton.setOnAction(e -> {
            if (modelComboBox.getValue() == null) {
                showAlert("Erreur", "Veuillez sélectionner un modèle.");
                return;
            }
            sendPrompt(inputArea, responseArea, modelComboBox.getValue());
        });

        // Layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.getChildren().addAll(
                new Label("Modèle:"),
                modelComboBox,
                new Label("Prompt:"),
                inputArea,
                sendButton,
                new Label("Réponse:"),
                responseArea
        );

        // Scène
        Scene scene = new Scene(layout, 500, 700);
        primaryStage.setTitle("GPT4All Chat");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadAvailableModels(ComboBox<String> modelComboBox, TextArea responseArea) {
        Request request = new Request.Builder()
                .url(API_MODELS_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> {
                    responseArea.setText("Erreur de chargement des modèles : " + e.getMessage());
                    // Modèles par défaut en cas d'échec
                    modelComboBox.getItems().addAll(
                            "TheBloke/deepseek-coder-6.7B-instruct-GGUF",
                            "mistralai/Mistral-7B-Instruct-v0.1",
                            "gpt4all-7b-chat"
                    );
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Platform.runLater(() -> {
                    try {
                        if (!response.isSuccessful()) {
                            responseArea.setText("Erreur : " + response.code() + " " + response.message());
                            return;
                        }

                        // Parser la réponse
                        String responseBody = response.body().string();
                        Type listType = new TypeToken<List<ModelInfo>>(){}.getType();
                        List<ModelInfo> models = gson.fromJson(responseBody, listType);

                        // Mettre à jour la combo box
                        List<String> modelNames = models.stream()
                                .map(ModelInfo::getId)
                                .collect(Collectors.toList());

                        modelComboBox.getItems().addAll(modelNames);
                    } catch (Exception e) {
                        responseArea.setText("Erreur de traitement : " + e.getMessage());
                        // Modèles par défaut en cas d'erreur
                        modelComboBox.getItems().addAll(
                                "TheBloke/deepseek-coder-6.7B-instruct-GGUF",
                                "mistralai/Mistral-7B-Instruct-v0.1",
                                "gpt4all-7b-chat"
                        );
                    }
                });
            }
        });
    }

    private void sendPrompt(TextArea inputArea, TextArea responseArea, String selectedModel) {
        String prompt = inputArea.getText().trim();
        if (prompt.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer un prompt.");
            return;
        }

        // Désactiver le bouton pendant le traitement
        Button sendButton = (Button) inputArea.getScene().lookup(".button");
        sendButton.setDisable(true);
        responseArea.setText("Chargement...");

        // Préparer la requête
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", selectedModel);
        JsonObject messageObj = new JsonObject();
        messageObj.addProperty("role", "user");
        messageObj.addProperty("content", prompt);
        requestBody.add("messages", gson.toJsonTree(new JsonObject[]{messageObj}));

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(
                        gson.toJson(requestBody),
                        MediaType.parse("application/json")
                ))
                .build();

        // Appel asynchrone
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Platform.runLater(() -> {
                    responseArea.setText("Erreur de connexion : " + e.getMessage());
                    sendButton.setDisable(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Platform.runLater(() -> {
                    try {
                        if (!response.isSuccessful()) {
                            responseArea.setText("Erreur : " + response.code() + " " + response.message());
                            return;
                        }

                        // Parser la réponse
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        String assistantResponse = jsonResponse
                                .getAsJsonArray("choices")
                                .get(0)
                                .getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content")
                                .getAsString();

                        responseArea.setText(assistantResponse);
                    } catch (Exception e) {
                        responseArea.setText("Erreur de traitement : " + e.getMessage());
                    } finally {
                        sendButton.setDisable(false);
                    }
                });
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Classe pour représenter les informations du modèle
    private static class ModelInfo {
        private String id;
        private String object;
        private long created;

        public String getId() {
            return id;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}