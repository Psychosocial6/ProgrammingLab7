package Server.managers;

import Common.collectionElements.Vehicle;
import Server.exceptions.ScriptExecutionException;
import Server.utils.Invoker;
import Server.utils.ScriptExecutor;
import Server.utils.User;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Класс управляющий коллекцией
 * @author Андрей
 * */
public class CollectionManager {
    private ConcurrentHashMap<String, Vehicle> collection;
    private final ZonedDateTime initializationDate;
    private DatabaseManager databaseManager;
    //нужно периодически обновлять
    private ConcurrentHashMap<Integer, User> users;


    public CollectionManager(DatabaseManager databaseManager) {
        initializationDate = ZonedDateTime.now();
        this.databaseManager = databaseManager;
        collection = databaseManager.getCollection().getValue();
        users = databaseManager.getCollection().getKey();
    }

    /**
     * Метод выводящий информацию о коллекции
     */
    public String info() {
        return String.format("----------\n Дата создания: %s\n Размер коллекции: %d\n Тип коллекции: %s\n ----------", initializationDate, collection.size(), collection.getClass());
    }

    /**
     * Метод выводящий содержимое коллекции
     */
    public String show() {
        if (collection.isEmpty()) {
            return "Collection is empty";
        }
        String ret = collection.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
        return ret;
    }

    /**
     * Метод вставки элемента по ключу
     * @param key - ключ
     * @param element - элемент
     */
    public String insert(String key, Vehicle element) {
        if (databaseManager.insertNewElement(key, element)) {
            collection = databaseManager.getCollection().getValue();
            return "Объект успешно вставлен в коллекцию";
        }
        return "Ошибка";
    }

    /**
     * Метод замены элемента с заданным id на данный
     * @param id - id
     * @param element - элемент на который заменяется существующий
     */
    public String updateById(int id, Vehicle element) {
        if (databaseManager.updateElementByID(id, element)) {
            for (String key : collection.keySet()) {
                if (collection.get(key).getId() == id) {
                    element.setId(collection.get(key).getId());
                    element.setCreationDate(collection.get(key).getCreationDate());
                    collection.put(key, element);
                }
            }
            return String.format("Объект с id {%d} успешно обновлен", id);
        }
        return "Ошибка";
    }

    /**
     * Метод удаления элемента по ключу
     * @param key - ключ
     */
    public String removeByKey(String key, String login) {
        if (databaseManager.deleteElementByKey(key, login)) {
            collection.remove(key);
            return String.format("Элемент с ключом {%s} удален", key);
        }
        return "Ошибка";
    }

    /**
     * Метод очистки коллекции
     */
    public String clear(String userName) {
        if (databaseManager.clearCollection(userName)) {
            collection = databaseManager.getCollection().getValue();
            return "Коллекция успешно очищена";
        }
        return "Ошибка!";
    }

    /**
     * Метод для сохранения коллекции в файл
     * @param file - файл для сохранения
     */
    public String save(File file) {
        return "";
    }

    /**
     * Метод для исполнения скрипта из заданного файла
     * @param file - файл со скриптом
     * @param invoker - исполнитель команд
     */
    public String executeScript(String file, Invoker invoker) {
        String msg = "";
        ScriptExecutor executor = new ScriptExecutor(invoker);
        try {
            msg = executor.executeScript(file);
        } catch (ScriptExecutionException e) {
            System.out.println(e.getMessage());
        }
        return msg;
    }

    /**
     * Метод для завершения работы программы
     */
    public String exit() {
        System.exit(0);
        return "Работа завершена";
    }

    /**
     * Метод для удаления всех элементов коллекции меньше данного
     * @param element - элемент с которым сравнивается
     */
    public String removeIfLower(Vehicle element, String login) {
        String msg = "Ничего не было удалено";
        for (String key : collection.keySet()) {
            if (collection.get(key).compareTo(element) < 0) {
                databaseManager.deleteElementByKey(key, login);
                collection.remove(key);
            }
        }
        return msg;
    }

    /**
     * Метод для замены элемента по ключу, если новый элемент больше
     * @param key - ключ
     * @param element - новое значение
     */
    public String replaceIfLower(String key, Vehicle element) {
        String msg = "Ничего не было заменено";
        if (collection.get(key).compareTo(element) < 0) {
            databaseManager.updateByKey(key, element);
            collection.put(key, element);
            msg = String.format("Объект с ключом {%s} был заменен", key);
        }
        return msg;
    }

    /**
     * Метод для замены элемента по ключу, если новый элемент меньше
     * @param key - ключ
     * @param element - новое значение
     */
    public String replaceIfGreater(String key, Vehicle element) {
        String msg = "Ничего не было заменено";
        if (collection.get(key).compareTo(element) > 0) {
            databaseManager.updateByKey(key, element);
            collection.put(key, element);
            msg = String.format("Объект с ключом {%s} был заменен", key);
        }
        return msg;
    }

    /**
     * Вывод элементов имя которых содержит заданную подстроку
     * @param name - подстрока
     */
    public String filterContainsName(String name) {
        String msg = String.format("Name contains %s:\n", name);
        for (String key: collection.keySet()) {
            if (collection.get(key).getName().contains(name)) {
                msg += collection.get(key) + "\n";
            }
        }
        return msg;
    }

    /**
     * Вывод элементов имя которых начинается на заданную подстроку
     * @param name - подстрока
     */
    public String filterStartsWithName(String name) {
        String msg = String.format("Name starts with %s:\n", name);
        for (String key: collection.keySet()) {
            if (collection.get(key).getName().startsWith(name)) {
                msg += collection.get(key) + "\n";
            }
        }
        return msg;
    }

    /**
     * Метод для вывода полей distanceTravelled в порядке возрастания
     */
    public String printFieldAscendingDistanceTravelled() {
        String msg = "";
        ArrayList<Long> distances = new ArrayList<>();
        for (String key : collection.keySet()) {
            distances.add(collection.get(key).getDistanceTravelled());
        }
        Collections.sort(distances);
        System.out.println("Field distanceTravelled:");
        for (Long dist : distances) {
            msg += dist + "\n";
        }
        return msg;
    }

    public String register(String name, String password) {
        ConcurrentHashMap<Integer, User> users = databaseManager.getUsers();
        for (Integer id : users.keySet()) {
            if (users.get(id).getName().equals(name)) {
                if (users.get(id).getPassword().equals(password)) {
                    return String.format("Пользователь успешно заргеистрирован c id = %d", databaseManager.getOwnerID(name));
                }
                else {
                    return "Неверный пароль";
                }
            }

        }
        databaseManager.insertUser(name, password);
        return String.format("Новый пользователь %s зарегистрирован c id = %d", name, databaseManager.getOwnerID(name));
    }

    /**
     * Метод возвращающий коллекцию элементов
     * @return collection - коллекция
     * */
    public ConcurrentHashMap<String, Vehicle> getCollection() {
        return collection;
    }

    /**
     * Метод устанавливающий значение коллекции
     * @param collection
     */
    public void setCollection(ConcurrentHashMap<String, Vehicle> collection) {
        this.collection = collection;
    }

    /**
     * Метод возвращающий дату создания коллекции
     * @return initializationDate - дата и время
     * */
    public ZonedDateTime getInitializationDate() {
        return initializationDate;
    }
}