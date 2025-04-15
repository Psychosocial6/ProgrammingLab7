package Server.managers;

import Common.collectionElements.Coordinates;
import Common.collectionElements.FuelType;
import Common.collectionElements.Vehicle;
import Server.utils.Pair;
import Server.utils.User;
import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {
    private Connection connection;
    private ResultSet resultSet;
    private ScriptRunner scriptRunner;

    public DatabaseManager() {
        try {
            this.connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/studs", "s465993", System.getenv("PSWD"));
            this.scriptRunner = new ScriptRunner(connection);
            setScriptRunnerConfig();
            System.out.println("connected");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void create() {
        try {
            FileReader reader = new FileReader("src/main/java/Server/sql/create.sql", StandardCharsets.UTF_8);
            scriptRunner.runScript(reader);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Pair<ConcurrentHashMap<Integer, User>, ConcurrentHashMap<String, Vehicle>> getCollection() {
        ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Vehicle> collection = new ConcurrentHashMap<>();
        try {
            Statement statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM collection JOIN users ON collection.owner_id = users.id JOIN elements ON collection.element_id = elements.id JOIN coordinates ON elements.coordinates_id = coordinates.id");
            while (resultSet.next()) {
                Integer userID = resultSet.getInt("id");
                String userName = resultSet.getString("login");
                String password = resultSet.getString("password");
                User user = new User(userName, password);
                users.put(userID, user);

                Long id = resultSet.getLong("id");
                String name = resultSet.getString("name");
                Integer x = resultSet.getInt("x");
                Long y = resultSet.getLong("y");
                ZonedDateTime creationDate = resultSet.getTimestamp("creation_date").toInstant().atZone(ZoneId.of("UTC"));
                Long enginePower = resultSet.getLong("engine_power");
                double capacity = resultSet.getDouble("capacity");
                Long distanceTravelled = resultSet.getLong("distance_travelled");
                FuelType fuelType = FuelType.valueOf(resultSet.getString("fuel_type"));
                int ownerID = resultSet.getInt("owner_id");
                Vehicle vehicle = new Vehicle(id, name, new Coordinates(x, y), creationDate, enginePower, capacity, distanceTravelled, fuelType, ownerID);
                String key = resultSet.getString("key");
                collection.put(key, vehicle);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return new Pair<>(users, collection);
    }

    public ConcurrentHashMap<Integer, User> getUsers() {
        ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();
        try {
            Statement statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM users");
            while (resultSet.next()) {
                Integer userID = resultSet.getInt("id");
                String userName = resultSet.getString("login");
                String password = resultSet.getString("password");
                User user = new User(userName, password);
                users.put(userID, user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    public void insertUser(String name, String password) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO users (login, password) VALUES (?, ?)");
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, password);
            preparedStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public boolean insertNewElement(String key, Vehicle vehicle) {
        boolean result = false;
        try {
            connection.setAutoCommit(false);
            PreparedStatement preparedStatementCoordinates = connection.prepareStatement("INSERT INTO coordinates (x, y) VALUES (?, ?)");
            preparedStatementCoordinates.setInt(1, vehicle.getCoordinates().getX());
            preparedStatementCoordinates.setLong(2, vehicle.getCoordinates().getY());

            preparedStatementCoordinates.execute();

            Statement statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT id FROM coordinates ORDER BY id DESC LIMIT 1");
            long maxID = 1;
            while (resultSet.next()) {
                maxID = resultSet.getInt(1);
            }

            PreparedStatement preparedStatementElement = connection.prepareStatement("INSERT INTO elements (name, coordinates_id, creation_date, engine_power, capacity, distance_travelled, fuel_type) VALUES (?, ?, ?, ?, ?, ?, ?)");
            preparedStatementElement.setString(1, vehicle.getName());
            preparedStatementElement.setLong(2, maxID);
            preparedStatementElement.setTimestamp(3, Timestamp.from(vehicle.getCreationDate().toInstant()));
            preparedStatementElement.setLong(4, vehicle.getEnginePower());
            preparedStatementElement.setDouble(5, vehicle.getCapacity());
            preparedStatementElement.setLong(6, vehicle.getDistanceTravelled());
            preparedStatementElement.setString(7, vehicle.getFuelType().name());

            preparedStatementElement.execute();

            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT id FROM elements ORDER BY id DESC LIMIT 1");
            maxID = 1;
            while (resultSet.next()) {
                maxID = resultSet.getInt("id");
            }

            PreparedStatement preparedStatementCollection = connection.prepareStatement("INSERT INTO collection (key, element_id, owner_id) VALUES (?, ?, ?)");
            preparedStatementCollection.setString(1, key);
            preparedStatementCollection.setInt(2, (int) maxID);
            preparedStatementCollection.setInt(3, vehicle.getOwnerID());

            preparedStatementCollection.execute();

            connection.commit();
            result = true;
        }
        catch (SQLException e) {
            try {
                e.printStackTrace();
                connection.rollback();
            } catch (SQLException ex) {
                e.printStackTrace();
            }
        }
        finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean updateElementByID(int id, Vehicle vehicle) {
        boolean result = false;
        try {
            connection.setAutoCommit(false);
            PreparedStatement preparedStatementOwnerID = connection.prepareStatement("SELECT owner_id FROM collection WHERE element_id = ?");
            preparedStatementOwnerID.setInt(1, id);

            resultSet = preparedStatementOwnerID.executeQuery();
            Integer ownID = null;
            while (resultSet.next()) {
                ownID = resultSet.getInt("owner_id");
            }

            PreparedStatement preparedStatementElement = connection.prepareStatement("UPDATE elements SET name = ?, creation_date = ?, engine_power = ?, capacity = ?, distance_travelled = ?, fuel_type = ? WHERE id = ? AND ? = ?");
            preparedStatementElement.setString(1, vehicle.getName());
            preparedStatementElement.setTimestamp(2, Timestamp.from(vehicle.getCreationDate().toInstant()));
            preparedStatementElement.setLong(3, vehicle.getEnginePower());
            preparedStatementElement.setDouble(4, vehicle.getCapacity());
            preparedStatementElement.setLong(5, vehicle.getDistanceTravelled());
            preparedStatementElement.setString(6, vehicle.getFuelType().name());
            preparedStatementElement.setInt(7, id);
            preparedStatementElement.setInt(8, ownID);
            preparedStatementElement.setInt(9, vehicle.getOwnerID());

            preparedStatementElement.execute();

            connection.commit();
            result = true;
        }
        catch (SQLException e) {
            try {
                e.printStackTrace();
                connection.rollback();
            } catch (SQLException ex) {
                e.printStackTrace();
            }
        }
        finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean deleteElementByKey(String login, String key) {
        boolean result = false;
        try {
            connection.setAutoCommit(false);

            PreparedStatement preparedStatementDelete = connection.prepareStatement("DELETE FROM coordinates WHERE id = (SELECT coordinates_id FROM elements WHERE id = (SELECT element_id FROM collection WHERE key = ? AND owner_id = ?))");
            preparedStatementDelete.setString(1, key);
            preparedStatementDelete.setInt(2, getOwnerID(login));

            preparedStatementDelete.execute();
            connection.commit();
            result = true;
        }
        catch (SQLException e) {
            try {
                e.printStackTrace();
                connection.rollback();
            } catch (SQLException ex) {
                e.printStackTrace();
            }
        }
        finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean clearCollection(String login) {
        boolean result = false;
        try {
            connection.setAutoCommit(false);

            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM coordinates WHERE id = (SELECT coordinates_id FROM elements WHERE id = (SELECT element_id FROM collection WHERE owner_id = (SELECT id FROM users WHERE login = ?)))");
            preparedStatement.setString(1, login);
            preparedStatement.execute();
            connection.commit();
            result = true;
        }
        catch (SQLException e) {
            try {
                e.printStackTrace();
                connection.rollback();
            } catch (SQLException ex) {
                e.printStackTrace();
            }
        }
        finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean updateByKey(String key, Vehicle vehicle) {
        boolean result = false;
        try {
            connection.setAutoCommit(false);
            PreparedStatement preparedStatementOwnerID = connection.prepareStatement("SELECT owner_id FROM collection WHERE key = ?");
            preparedStatementOwnerID.setString(1, key);

            resultSet = preparedStatementOwnerID.executeQuery();
            Integer ownID = null;
            while (resultSet.next()) {
                ownID = resultSet.getInt("owner_id");
            }

            PreparedStatement preparedStatementElement = connection.prepareStatement("UPDATE element SET name = ?, creation_date = ?, engine_power = ?, capacity = ?, distance_travelled = ?, fuel_type = ? WHERE id = (SELECT element_id FROM collection WHERE key = ?) AND ? = ?");
            preparedStatementElement.setString(1, vehicle.getName());
            preparedStatementElement.setTimestamp(2, Timestamp.from(vehicle.getCreationDate().toInstant()));
            preparedStatementElement.setLong(3, vehicle.getEnginePower());
            preparedStatementElement.setDouble(4, vehicle.getCapacity());
            preparedStatementElement.setLong(5, vehicle.getDistanceTravelled());
            preparedStatementElement.setString(6, vehicle.getFuelType().name());
            preparedStatementElement.setString(7, key);
            preparedStatementElement.setInt(8, ownID);
            preparedStatementElement.setInt(9, vehicle.getOwnerID());

            preparedStatementElement.execute();

            connection.commit();
            result = true;
        }
        catch (SQLException e) {
            try {
                e.printStackTrace();
                connection.rollback();
            } catch (SQLException ex) {
                e.printStackTrace();
            }
        }
        finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public int getOwnerID(String login) {
        int id = 0;
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT id FROM users WHERE login = ?");
            preparedStatement.setString(1, login);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                id = resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    private void setScriptRunnerConfig() {
        scriptRunner.setStopOnError(true);
        scriptRunner.setAutoCommit(false);
        scriptRunner.setLogWriter(null);
        scriptRunner.setSendFullScript(false);
    }

}
