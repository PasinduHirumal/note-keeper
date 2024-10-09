import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class NoteKeeper {
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/notepad";
    static final String USER = "root";
    static final String PASS = "2003Pasindu@";
    private static final Logger LOGGER = Logger.getLogger(NoteKeeper.class.getName());

    static {
        try {
            // Configure logger to use FileHandler
            FileHandler fileHandler = new FileHandler("notepad.log");
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void retrieveNotes(Connection conn) throws SQLException {
        LOGGER.info("Retrieving notes...");
        String sql = "SELECT * FROM notes";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String content = rs.getString("content");
                String date = rs.getString("date");
                renumberIDs(conn);
                Note note = new Note(id, title, content, date);
                System.out.println(note);
            }
            if (!found) {
                LOGGER.info("No notes found.");
            }
            renumberIDs(conn);
        }
    }

    private static void addNote(Connection conn, Note note) throws SQLException {
        LOGGER.info("Adding new note...");
        String sql = "INSERT INTO notes (title, content, date) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, note.getTitle());
            preparedStatement.setString(2, note.getContent());
            preparedStatement.setString(3, note.getDate());
            preparedStatement.executeUpdate();
            renumberIDs(conn);
            System.out.println("Note added successfully!");
        }
    }

    private static void searchNoteByIdOrTitle(Connection conn, String idOrTitle) throws SQLException {
        LOGGER.info("Searching note by ID or Title...");
        if (idOrTitle.matches("\\d+")) {
            int id = Integer.parseInt(idOrTitle);
            String sql = "SELECT * FROM notes WHERE id = ?";
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setInt(1, id);
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    if (rs.next()) {
                        String title = rs.getString("title");
                        String content = rs.getString("content");
                        String date = rs.getString("date");
                        Note note = new Note(id, title, content, date);
                        System.out.println(note);
                    } else {
                        System.out.println("There is no note related to this ID.");
                    }
                }
            }
        } else {
            String sql = "SELECT * FROM notes WHERE title LIKE ?";
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, "%" + idOrTitle + "%");
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    boolean found = false;
                    while (rs.next()) {
                        found = true;
                        int id = rs.getInt("id");
                        String title = rs.getString("title");
                        String content = rs.getString("content");
                        String date = rs.getString("date");
                        Note note = new Note(id, title, content, date);
                        System.out.println(note);
                    }
                    if (!found) {
                        System.out.println("There is no note related to this title.");
                    }
                }
            }
        }
    }

    private static void editNoteByIdOrTitle(Connection conn, String idOrTitle) throws SQLException {
        LOGGER.info("Editing note by ID or Title...");
        if (idOrTitle.matches("\\d+")) {
            int id = Integer.parseInt(idOrTitle);
            String sql = "SELECT * FROM notes WHERE id = ?";
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                preparedStatement.setInt(1, id);
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    if (rs.next()) {
                        Scanner scanner = new Scanner(System.in);
                        System.out.print("Enter new title: ");
                        String newTitle = scanner.nextLine();
                        System.out.print("Enter new content: ");
                        String newContent = scanner.nextLine();

                        LocalDateTime now = LocalDateTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String formattedDate = now.format(formatter);
                        rs.updateString("title", newTitle);
                        rs.updateString("content", newContent);
                        rs.updateString("date", formattedDate); // Update date
                        rs.updateRow();
                        System.out.println("Note updated successfully!");
                    } else {
                        System.out.println("No such note here.");
                    }
                }
            }
        } else {
            String sql = "SELECT * FROM notes WHERE title LIKE ?";
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                preparedStatement.setString(1, "%" + idOrTitle + "%");
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    if (rs.next()) {
                        Scanner scanner = new Scanner(System.in);
                        System.out.print("Enter new title: ");
                        String newTitle = scanner.nextLine();
                        System.out.print("Enter new content: ");
                        String newContent = scanner.nextLine();

                        LocalDateTime now = LocalDateTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String formattedDate = now.format(formatter);
                        rs.updateString("title", newTitle);
                        rs.updateString("content", newContent);
                        rs.updateString("date", formattedDate);
                        rs.updateRow();
                        System.out.println("Note updated successfully!");
                    } else {
                        System.out.println("No such note here.");
                    }
                }
            }
        }
    }

    private static void filterNotesByIdOrDate(Connection conn, String firstFilter, String secondFilter) throws SQLException {
        LOGGER.info("Filtering notes by ID or Date...");
        String sql;
        if (firstFilter.matches("\\d+") && secondFilter.matches("\\d+")) {
            int firstId = Integer.parseInt(firstFilter);
            int secondId = Integer.parseInt(secondFilter);
            sql = "SELECT * FROM notes WHERE id BETWEEN ? AND ?";
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setInt(1, Math.min(firstId, secondId));
                preparedStatement.setInt(2, Math.max(firstId, secondId));
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    displayFilteredNotes(rs);
                }
            }
        } else {
            sql = "SELECT * FROM notes WHERE date BETWEEN ? AND ?";
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, firstFilter);
                preparedStatement.setString(2, secondFilter);
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    displayFilteredNotes(rs);
                }
            }
        }
    }

    private static void displayFilteredNotes(ResultSet rs) throws SQLException {
        LOGGER.info("Displaying filtered notes...");
        boolean found = false;
        while (rs.next()) {
            found = true;
            int id = rs.getInt("id");
            String title = rs.getString("title");
            String content = rs.getString("content");
            String date = rs.getString("date");
            Note note = new Note(id, title, content, date);
            System.out.println(note);
        }
        if (!found) {
            System.out.println("No notes found within the specified range.");
        }
    }

    private static void deleteNotesByIds(Connection conn, int[] idsToDelete) throws SQLException {
        LOGGER.info("Deleting notes by IDs...");
        String sql = "DELETE FROM notes WHERE id = ?";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            for (int id : idsToDelete) {
                preparedStatement.setInt(1, id);
                preparedStatement.addBatch();
            }
            int[] rowsAffected = preparedStatement.executeBatch();

            int totalDeleted = 0;
            for (int deleted : rowsAffected) {
                totalDeleted += deleted;
            }

            if (totalDeleted > 0) {
                System.out.println("Successfully deleted " + totalDeleted + " notes!");
            } else {
                System.out.println("No notes were deleted!");
            }
            renumberIDs(conn);
        }
    }

    private static void renumberIDs(Connection conn) throws SQLException {
        LOGGER.info("Renumbering note IDs...");
        String sql = "SET @counter = 0";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
        }
        sql = "UPDATE notes SET id = @counter := @counter + 1";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
        }
    }

    public static void main(String[] args) {
        try {
            Class.forName(JDBC_DRIVER);
            LOGGER.info("Connecting to database...");
            System.out.println("Connecting to database...");
            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                 Statement stmt = conn.createStatement();
                 Scanner scanner = new Scanner(System.in)) {

                LOGGER.info("Creating table if not exists...");
                System.out.println("Creating table if not exists...");
                String sql = "CREATE TABLE IF NOT EXISTS notes " +
                        "(id INTEGER NOT NULL AUTO_INCREMENT, " +
                        " title VARCHAR(50), " +
                        " content TEXT, " +
                        " date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);

                boolean exit = false;
                while (!exit) {
                    System.out.println("\nMenu:");
                    System.out.println("1. View Notes");
                    System.out.println("2. Add Note");
                    System.out.println("3. Search Note");
                    System.out.println("4. Voice Assistant");
                    System.out.println("5. Filter Search");
                    System.out.println("6. Edit Note");
                    System.out.println("7. Delete Note");
                    System.out.println("8. Exit");
                    System.out.print("Enter your choice: ");
                    int choice = scanner.nextInt();

                    switch (choice) {
                        case 1:
                            retrieveNotes(conn);
                            break;
                        case 2:
                            scanner.nextLine();
                            System.out.print("Enter note title: ");
                            String newNoteTitle = scanner.nextLine();
                            System.out.print("Enter your new note: ");
                            String newNoteContent = scanner.nextLine();
                            LocalDateTime now = LocalDateTime.now();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            String formattedDate = now.format(formatter);
                            Note newNote = new Note();
                            newNote.setTitle(newNoteTitle);
                            newNote.setContent(newNoteContent);
                            newNote.setDate(formattedDate);
                            addNote(conn, newNote);
                            break;
                        case 3:
                            scanner.nextLine();
                            System.out.print("Enter the ID or title of the note you want to search: ");
                            String idOrTitleToSearch = scanner.nextLine();
                            searchNoteByIdOrTitle(conn, idOrTitleToSearch);
                            break;
                        case 4:
                            VoiceAssistant.voiceAssistant(conn);
                            break;
                        case 5:
                            scanner.nextLine();
                            System.out.print("Enter the first ID or date (yyyy-mm-dd): ");
                            String firstFilter = scanner.nextLine();
                            System.out.print("Enter the second ID or date (yyyy-mm-dd): ");
                            String secondFilter = scanner.nextLine();
                            filterNotesByIdOrDate(conn, firstFilter, secondFilter);
                            break;
                        case 6:
                            scanner.nextLine();
                            System.out.print("Enter the ID or title of the note you want to edit: ");
                            String idOrTitleToEdit = scanner.nextLine();
                            editNoteByIdOrTitle(conn, idOrTitleToEdit);
                            break;
                        case 7:
                            scanner.nextLine();
                            System.out.print("Enter the IDs of the notes you want to delete (comma-separated): ");
                            String input = scanner.nextLine();
                            String[] deleteInputs = input.split(",");
                            int[] noteIdsToDelete = new int[deleteInputs.length];
                            for (int i = 0; i < deleteInputs.length; i++) {
                                try {
                                    noteIdsToDelete[i] = Integer.parseInt(deleteInputs[i].trim());
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid input. Please enter numeric IDs only.");
                                    return;
                                }
                            }
                            deleteNotesByIds(conn, noteIdsToDelete);
                            break;
                        case 8:
                            exit = true;
                            System.out.println("Exiting...");
                            break;
                        default:
                            System.out.println("Invalid choice. Please enter a number between 1 and 8.");
                    }
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.log(Level.SEVERE, "An error occurred", e);
            e.printStackTrace();
        }
    }
}
