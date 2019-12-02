module ToDoList {

    requires javafx.fxml;
    requires javafx.controls;

    opens Controller;
    opens Model;
    opens View;

}