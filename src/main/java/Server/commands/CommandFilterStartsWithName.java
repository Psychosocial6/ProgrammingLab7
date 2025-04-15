package Server.commands;

import Server.managers.CollectionManager;

/**
 * Класс команды фильтра по началу имени
 * @author Андрей
 * */
public class CommandFilterStartsWithName extends Command {

    /**
     * Конструктор
     * @param collectionManager - класс управляющий коллекцией
     * */
    public CommandFilterStartsWithName(CollectionManager collectionManager) {
        super(collectionManager);
    }

    /**
     * Метод реализующий исполнение команды
     * @param args - массив аргументов команды
     * */
    @Override
    public String execute(Object[] args) {
        String name = (String) args[0];
        return collectionManager.filterStartsWithName(name);
    }

    /**
     * Метод возвращающий информацию о команде
     * @return String info
     * */
    @Override
    public String getInfo() {
        return "filter_starts_with_name name: вывести элементы, значение поля name которых начинается с заданной подстроки";
    }
}
