package Controller;

import Model.TodoData;
import Model.TodoItem;
import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Controller {

    private List<TodoItem> todoItems;

    @FXML
    private ListView<TodoItem> todoListView;
    @FXML
    private TextArea textArea;
    @FXML
    private Label deadlineLabel;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private ContextMenu listContextMenu;
    @FXML
    private ToggleButton filterToggleButton;

    private FilteredList<TodoItem> filteredList;

    private Predicate<TodoItem> findAllItems;
    private Predicate<TodoItem> findDueItems;


    public void initialize(){

        listContextMenu = new ContextMenu();


        MenuItem editMenuItem = new MenuItem("Edit");
        editMenuItem.setOnAction(actionEvent -> {
            TodoItem item = todoListView.getSelectionModel().getSelectedItem();
            showEditItemDialog(item);
        });


        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(actionEvent -> {
            TodoItem item = todoListView.getSelectionModel().getSelectedItem();
            deleteItem(item);
        });

        listContextMenu.getItems().addAll(editMenuItem);
        listContextMenu.getItems().addAll(deleteMenuItem);

        todoListView.getSelectionModel().selectedItemProperty().addListener((observableValue, todoItem, t1) -> {
            if (t1 != null) {
                TodoItem item = todoListView.getSelectionModel().getSelectedItem();
                textArea.setText(item.getDetails());
                DateTimeFormatter df = DateTimeFormatter.ofPattern("d MMMM, yyyy");
                deadlineLabel.setText(df.format(item.getDeadline()));
            }
        });

        //Predicate to show all items
        findAllItems = todoItem -> true;

        //Predicate to only show items due today.
        findDueItems = todoItem -> todoItem.getDeadline().equals(LocalDate.now());

        //filter the list based on which predicate it is passed.
        filteredList = new FilteredList<>(TodoData.getInstance().getTodoItems(),
                findAllItems);

        //Sort the list based on when they are due
        SortedList<TodoItem> sortedList = new SortedList<>(filteredList, Comparator.comparing(TodoItem::getDeadline));


        //Populate the list with the items, and select the first one by defualt
        todoListView.setItems(sortedList);
        todoListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        todoListView.getSelectionModel().selectFirst();


        //Colour the different TodoItems based on when they are due.
        todoListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<TodoItem> call(ListView<TodoItem> todoItemListView) {
                ListCell<TodoItem> cell = new ListCell<>(){
                    @Override
                    protected void updateItem(TodoItem todoItem, boolean empty) {
                        super.updateItem(todoItem, empty);
                        if(empty) {
                            setText(null);
                        }else{
                            setText(todoItem.getShortDescription());
                            if(todoItem.getDeadline().isBefore(LocalDate.now().plusDays(1))){
                                setTextFill(Color.RED);
                            }else if(todoItem.getDeadline().equals(LocalDate.now().plusDays(1))){
                                setTextFill(Color.ORANGE);
                            }
                        }
                    }
                };
                cell.emptyProperty().addListener(
                        (obs, wasEmpty, isNowEmpty) ->{
                            if(isNowEmpty){
                                cell.setContextMenu(null);
                            }else{
                                cell.setContextMenu(listContextMenu);
                            }
                        });

                return cell;
            }
        });
    }

    //Dialog to add new TodoItems
    @FXML
    public void showNewItemDialog(){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainBorderPane.getScene().getWindow());
        dialog.setTitle("Add new Todo Item");
        dialog.setHeaderText("Use this dialog to create a new todo item");

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/View/todoItemDialog.fxml"));

        try{
            dialog.getDialogPane().setContent(fxmlLoader.load());

        }catch (IOException e){
            System.out.println("Something went wrong: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        DialogController controller = fxmlLoader.getController();

        //Adds a new Todoitem when the okay button is pressed, and the fields are not empty.
        if(result.isPresent() && result.get() == ButtonType.OK && controller.checkValid()){
            TodoItem newItem = controller.processResults();
            todoListView.getSelectionModel().select(newItem);
        }

        //Sends an alert to the user if the fields are empty, and reloads the dialog to add an item
        //Todo gjør det slik at det er det orginale vinduet man blir returnert til, og ikke et nytt ett
        if(result.isPresent() && result.get() == ButtonType.OK && !controller.checkValid()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Please enter all the required information");
            alert.showAndWait();
            showNewItemDialog();
        }
    }

    @FXML
    public void showEditItemDialog(TodoItem item){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainBorderPane.getScene().getWindow());
        dialog.setTitle("Edit an Todo Item");
        dialog.setHeaderText("Use this dialog to edit todo item");

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/View/todoItemDialog.fxml"));

        try{
            dialog.getDialogPane().setContent(fxmlLoader.load());

        }catch (IOException e){
            System.out.println("Something went wrong: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        DialogController controller = fxmlLoader.getController();

        controller.shortDescriptionField.setText(item.getShortDescription());
        controller.detailsArea.setText(item.getDetails());
        controller.deadlinePicker.setValue(item.getDeadline());

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();

        if(result.isPresent() && result.get() == ButtonType.OK && controller.checkValid()){
            controller.setItemData(item);
            filteredList.setPredicate(findAllItems);
            todoListView.getSelectionModel().select(item);
        }

        //Todo: oppdatere vinduet etter at en TodoItem er blir endret.

    }


    @FXML
    public void handleClickListView(){
        TodoItem item = todoListView.getSelectionModel().getSelectedItem();

        textArea.setText(item.getDetails());
        deadlineLabel.setText(item.getDeadline().toString());
    }


    //Alert to handle deletion of TodoItems.
    public void deleteItem(TodoItem item){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Todo Item");
        alert.setHeaderText("Delete Item: " + item.getShortDescription());
        alert.setContentText("Are you sure? Press OK to confirm, or cancel to back out");
        Optional<ButtonType> result = alert.showAndWait();

        if(result.isPresent() && (result.get() == ButtonType.OK)){
            TodoData.getInstance().deleteTodoItem(item);
        }

    }


    //Handle the button to filter items due today and everything else. Also selects the first item in the list after toggle.
    @FXML
    public void handleFilterButton(){
        TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();

        if(filterToggleButton.isSelected()){
            filteredList.setPredicate(findDueItems);
            if(filteredList.isEmpty()){
                textArea.clear();
                deadlineLabel.setText("");
            }else if(filteredList.contains(selectedItem)){
                todoListView.getSelectionModel().select(selectedItem);
            }else{
                todoListView.getSelectionModel().selectFirst();
            }
        }else{
            filteredList.setPredicate(findAllItems);
            todoListView.getSelectionModel().select(selectedItem);
        }
    }

    @FXML
    public void handleExit(){
        Platform.exit();
    }

}
