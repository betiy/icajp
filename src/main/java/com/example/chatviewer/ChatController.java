package com.example.chatviewer;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ChatController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onUploadButtonClick() {
        welcomeText.setText("Make this upload a file");
    }
}