package com.example.chatviewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatApplication extends Application {
    private String previousFolderPath = null;

    @Override
    public void start(Stage stage) throws IOException {

        TextFlow text_flow = new TextFlow();
        text_flow.setPrefWidth(Region.USE_COMPUTED_SIZE);
        text_flow.setMinWidth(Region.USE_PREF_SIZE);

        ScrollPane scrollPane = new ScrollPane(text_flow);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        /** Default text on startup */
        Text text_1 = new Text("Upload a .msg file using the Upload button and chat history will display here.");
        text_flow.getChildren().add(text_1);

        Button upload = new Button("Upload file");
        Label filePathLabel = new Label("No file uploaded");

        /** Tie the upload button to the file chooser pop-up */
        upload.setOnAction(e -> {
            FileChooser chooser = new FileChooser();

            /** If there's a previous folder path, use it */
            if (previousFolderPath != null)
            {
                File initialPath = new File(previousFolderPath);
                if (initialPath.exists() && initialPath.isDirectory())
                {
                    chooser.setInitialDirectory(initialPath);
                }
            }

            File selected = chooser.showOpenDialog(stage);

            if (selected == null)
            {
                /** do nothing - user cancelled the file chooser */
            }
            /** If the selected file is valid, pass it to the necessary functions to fill the text window */
            else if (IsFileValid(selected))
            {
                previousFolderPath = selected.getParent();  /** save the selected file path to remember for next time */
                filePathLabel.setText(selected.getPath());  /** set the file path label in the corner of the window */
                List<ChatMessage> history = ParseChatFile(selected);
                FillChatWindow(text_flow, history);
            }
            else    /** This only hits if a file of the wrong format is selected */
            {
                Alert alert = new Alert(Alert.AlertType.ERROR, "File selected is an invalid format. Please select a .msg file.");
                alert.showAndWait();
            }
        });

        VBox controlsContainer = new VBox(upload, filePathLabel);
        controlsContainer.setSpacing(10);
        controlsContainer.setAlignment(Pos.BOTTOM_LEFT);

        VBox root = new VBox(text_flow, scrollPane, controlsContainer);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        scrollPane.setStyle("-fx-pref-width: 400px; -fx-pref-height: 99999999999"); // for some reason this fixes a spacing issue

        Scene scene = new Scene(root, 600, 300);
        stage.setTitle("Chat Viewer");
        stage.setScene(scene);
        stage.show();
    }

    private boolean IsFileValid(File selection)
    {
        /** pull out the file extension and check to see if it's valid */
        String extension = "";
        String name = selection.getName();
        int index = name.lastIndexOf('.');
        if (index > 0 && index < name.length() - 1)
        {
            extension = name.substring(index + 1);
        }

        if (extension.equals("msg"))
        {
            return true;
        }
        return false;
    }

    /** Pass in the textflow object and the parsed-out chat history to this function to actually use the chat history to fill the text window */
    private void FillChatWindow(TextFlow flow, List<ChatMessage> history)
    {
        /** Clear out any existing text first */
        flow.getChildren().clear();

        /** Error case */
        if (history.get(0).Nickname.equals("PARSE-ERR"))
        {
            Alert alert = new Alert(Alert.AlertType.ERROR, "The uploaded chat history file is corrupt or broken.");
            alert.showAndWait();
            return;
        }

        for (int i = 0; i < history.size(); i++)
        {

            Text time = new Text(history.get(i).Timestamp);

            Text name;  /** We can't check the very first object in the array against the previous one, so have a special case for the first one*/
            if (i == 0)
            {
                name = new Text(history.get(i).Nickname);
                name.setStyle("-fx-fill: blue;");
            }
            else    /** Here we check to see if a message is sent by the same user as the last one, and avoid printing duplicate names accordingly */
            {
                name = new Text(FormatName(history.get(i - 1).Nickname, history.get(i).Nickname));
                name.setStyle("-fx-fill: blue;");
            }

            /** Add the text objects for timestamp and nickname */
            flow.getChildren().addAll(time, name);

            /** Content is more complicated, call a function dedicated to handling it */
            FormatContentWithEmojis(flow, history.get(i).Content);

            /** Add a newline at the end of each message */
            flow.getChildren().addAll(new Text("\n"));
        }
    }

    private void FormatContentWithEmojis(TextFlow flow, String content)
    {
        /** Assemble the content string character by character, checking for a match against any desired emoji and embedding them accordingly */
        Map<String, String> EmojiMap = createEmojiMap();
        String[] contentCharacters = content.split("\\s+");
        for (String word : contentCharacters)
        {
            Text text = new Text(word + " ");
            if (word.startsWith(":") && EmojiMap.containsKey(word))
            {
                String emojiPath = EmojiMap.get(word);
                Image emoji = new Image(getClass().getResourceAsStream(emojiPath));
                ImageView emojiView = new ImageView(emoji);
                emojiView.setFitWidth(16);
                emojiView.setFitHeight(16);
                flow.getChildren().add(emojiView);
            }
            else {
                /** if there's no emojis, make sure the text still gets bolded */
                text.setStyle("-fx-font-weight: bold;");
                flow.getChildren().add(text);
            }
        }

    }

    /** extendable map to organize desired emojis and their file paths */
    private static Map<String, String> createEmojiMap() {
        Map<String, String> emojiMap = new HashMap<>();
        emojiMap.put(":)", "/smile_happy.gif");
        emojiMap.put(":(", "/smile_sad.gif");
        return emojiMap;
    }

    /** helper function to output ellipses if duplicate names would be printed */
    private String FormatName(String previousName, String currentName)
    {
        if (previousName.equals(currentName))
        {
            return "...";
        }
        return currentName;
    }

    /** Read the chat history file and output a list of ChatMessage data objects that can be parsed by the UI */
    private List<ChatMessage> ParseChatFile(File chatlog)
    {
        List<ChatMessage> history = new ArrayList<ChatMessage>();

        try (BufferedReader reader = new BufferedReader(new FileReader(chatlog.getPath())))
        {
            String line1, line2, line3, buffer;
            buffer = null;
            /** Parse the file 3 lines at a time */
            while ((line1 = reader.readLine()) != null &&
                    (line2 = reader.readLine()) != null &&
                    (line3 = reader.readLine()) != null)
            {

                /** throw an error if any mismatches in formatting/syntax are found*/
                if (!line1.startsWith("Time:") || !line2.startsWith("Name:") || !line3.startsWith("Message:"))
                {
                    throw new Exception();
                }

                ChatMessage newmessage = new ChatMessage();

                newmessage.Timestamp = "[" + line1.substring(5) + "]";
                newmessage.Nickname = line2.substring(5) + ": ";
                newmessage.Content  = line3.substring(8);

                history.add(newmessage);
                buffer = reader.readLine();
            }
            /** if there are extra newlines at the end of the file (which there should not be), buffer will not be null */
            if (buffer != null)
            {
                throw new Exception();
            }

        }
        catch (Exception e)
        {
            /** replace any parsed data with a readable error to pass to the parent function */
            history.clear();
            ChatMessage err = new ChatMessage();
            err.Nickname = "PARSE-ERR";
            history.add(err);
            return history;
        }

        return history;
    }

    public static void main(String[] args) {
        launch();
    }
}