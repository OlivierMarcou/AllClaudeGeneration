package net.arkaine;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        javafx.application.Application.launch(GPT4AllChatApp.class, args);
    }
}