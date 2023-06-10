module com.example.chatviewer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.example.chatviewer to javafx.fxml;
    exports com.example.chatviewer;
}