package sample;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import sample.datamodel.TodoData;
import sample.datamodel.TodoItem;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Controller {
    private List<TodoItem> todoItems;
    @FXML
    private ListView<TodoItem> todoListView;
    @FXML
    private TextArea itemDetailsTextArea;
    @FXML
    private Label deadlineLabel;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private ContextMenu listContextMenu;
    @FXML
    private ToggleButton filterToggleButton;

    private FilteredList<TodoItem> filteredList;
    private Predicate<TodoItem> wantAllItems;
    private Predicate<TodoItem> wantTodayItems;


    public void initialize(){
        listContextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TodoItem item = todoListView.getSelectionModel().getSelectedItem();
                deleteItem(item);
            }
        });
        listContextMenu.getItems().addAll(deleteMenuItem);
//        TodoItem item1 = new TodoItem("Mail birthday card", "Buy a 30th birthday card for John",
//                LocalDate.of(2019, Month.DECEMBER,23));
//        TodoItem item2 = new TodoItem("Doctor'a Appointment", "See Dr. Smith at 123 Main Street. Bring paperwork",
//                LocalDate.of(2019, Month.MAY,24));
//        TodoItem item3 = new TodoItem("Finish design proposal for client", "I promised Mike I'd email website mockups by Friday 22nd April",
//                LocalDate.of(2019, Month.JULY,12));
//        TodoItem item4 = new TodoItem("Pickup Doug at the train station", "Doug's arriving on March 23 on the 5:00 train",
//                LocalDate.of(2019, Month.MARCH,23));
//        TodoItem item5 = new TodoItem("Pick up dry cleaning", "The clothes should be ready by Wednesday",
//                LocalDate.of(2019, Month.APRIL,22));
//
//        todoItems = new ArrayList<TodoItem>();
//        todoItems.add(item1);
//        todoItems.add(item2);
//        todoItems.add(item3);
//        todoItems.add(item4);
//        todoItems.add(item5);
//
//        TodoData.getInstance().setTodoItems(todoItems);
        todoListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TodoItem>() {
            @Override
            public void changed(ObservableValue<? extends TodoItem> observableValue, TodoItem todoItem, TodoItem t1) {
                if(t1 != null){
                    TodoItem item = todoListView.getSelectionModel().getSelectedItem();
                    itemDetailsTextArea.setText(item.getDetails());
                    DateTimeFormatter df = DateTimeFormatter.ofPattern("d MMMM yy");
                    deadlineLabel.setText(df.format(item.getDeadLine()));
                }
            }
        });
        wantAllItems = new Predicate<TodoItem>() {
            @Override
            public boolean test(TodoItem todoItem) {
                return true;
            }
        };
        wantTodayItems = new Predicate<TodoItem>() {
            @Override
            public boolean test(TodoItem todoItem) {
                return (todoItem.getDeadLine().equals(LocalDate.now()));
            }
        };
        filteredList = new FilteredList<TodoItem>(TodoData.getInstance().getTodoItems(), wantAllItems);
        SortedList<TodoItem> sortedList = new SortedList<>(filteredList, new Comparator<TodoItem>() {
            @Override
            public int compare(TodoItem o1, TodoItem o2) {
                return o1.getDeadLine().compareTo(o2.getDeadLine());
            }
        });

        todoListView.setItems(sortedList);
        //todoListView.setItems(TodoData.getInstance().getTodoItems());
        todoListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        todoListView.getSelectionModel().selectFirst();

        todoListView.setCellFactory(new Callback<ListView<TodoItem>, ListCell<TodoItem>>() {
            @Override
            public ListCell<TodoItem> call(ListView<TodoItem> todoItemListView) {
                ListCell<TodoItem> cell = new ListCell<>(){
                    @Override
                    protected void updateItem(TodoItem todoItem, boolean empty) {
                        super.updateItem(todoItem, empty);
                        if(empty){
                            setText(null);
                        }else {
                            setText(todoItem.getShortDescription());
                            if(todoItem.getDeadLine().isBefore(LocalDate.now().plusDays(1))){
                                setTextFill(Color.RED);
                            }else if(todoItem.getDeadLine().equals(LocalDate.now().plusDays(1))){
                                setTextFill(Color.BROWN);
                            }
                        }
                    }
                };
                cell.emptyProperty().addListener(
                        (obs,wasEmpty,isNowEmpty)->{
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
    @FXML
    public void showNewItemDialog(){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainBorderPane.getScene().getWindow());
        dialog.setHeaderText("Use this dialog to create a new todo item");
        dialog.setTitle("Add New Todo Item");
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("todoItemDialog.fxml"));
        try {
            dialog.getDialogPane().setContent(fxmlLoader.load());
        }catch (IOException e){
            System.out.println("Couldn't load the dialog");
            e.printStackTrace();
            return;
        }

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK){
            TodoItemDialog  controller = fxmlLoader.getController();
            TodoItem newItem = controller.processResults();
            //todoListView.getItems().setAll(TodoData.getInstance().getTodoItems());
            todoListView.getSelectionModel().select(newItem);
            //System.out.println("Ok Pressed");
        }
    }
    @FXML
    public void handleKeyPressed(KeyEvent keyEvent){
        TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();
        if(selectedItem != null){
            if(keyEvent.getCode().equals(KeyCode.DELETE)){
                deleteItem(selectedItem);
            }
        }
    }

    @FXML
    public void handleClickListView(){
        TodoItem item =  todoListView.getSelectionModel().getSelectedItem();
        itemDetailsTextArea.setText(item.getDetails());
        DateTimeFormatter df = DateTimeFormatter.ofPattern("d MMMM yy");
        deadlineLabel.setText(df.format(item.getDeadLine()));
        //System.out.println("The selected items is "+item);
//        StringBuilder sb = new StringBuilder(item.getDetails());
//        sb.append("\n\n\n\n\n").append("Due: ");
//        sb.append(item.getDeadLine()).toString();
//        itemDetailsTextArea.setText(sb.toString());
    }

    public void deleteItem(TodoItem item){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Todo Item");
        alert.setHeaderText("Delete item: "+item.getShortDescription());
        alert.setContentText("Are you sure? Press OK to confirm, or cancel to Back out.");
        Optional<ButtonType> result = alert.showAndWait();
        if(result.isPresent() && (result.get() == ButtonType.OK)){
            TodoData.getInstance().deleteTodoItem(item);
        }
    }
    @FXML
    public void handleFilterButton(){
        TodoItem selectedItem = todoListView.getSelectionModel().getSelectedItem();
        if(filterToggleButton.isSelected()){
            filteredList.setPredicate(wantTodayItems);
            if(filteredList.isEmpty()){
                itemDetailsTextArea.clear();
                deadlineLabel.setText("");
            }else if(filteredList.contains(selectedItem)){
                todoListView.getSelectionModel().select(selectedItem);
            }else{
                todoListView.getSelectionModel().selectFirst();
            }
        }else{
            filteredList.setPredicate(wantAllItems);
            todoListView.getSelectionModel().select(selectedItem);
        }
    }
    @FXML
    public void handleExit(){
        Platform.exit();
    }
}
