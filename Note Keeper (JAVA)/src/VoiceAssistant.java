import java.util.Scanner;
import java.sql.*;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class VoiceAssistant extends NoteKeeper{
    private static void readNoteByIdOrTitle(Connection conn, Voice voice, String idOrTitle) throws SQLException {
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
                        String textToSpeak = "Note with ID " + id + ". Title: " + title + ". Content: " + content;
                        voice.speak(textToSpeak);
                    } else {
                        voice.speak("There is no note related to this ID.");
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
                        String textToSpeak = "Note with ID " + id + ". Title: " + title + ". Content: " + content;
                        voice.speak(textToSpeak);
                    }
                    if (!found) {
                        voice.speak("There is no note related to this title.");
                    }
                }
            }
        }
    }

    public static void voiceAssistant(Connection conn) {
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
        VoiceManager voiceManager = VoiceManager.getInstance();
        Voice voice = voiceManager.getVoice("kevin16");
        voice.allocate();
        Scanner scanner = new Scanner(System.in);
        try {
            boolean exit = false;
            while (!exit) {
                System.out.println("\nVoice Assistant Menu:");
                System.out.println("1. Select and read a note by ID or title");
                System.out.println("2. Exit Voice Assistant");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        retrieveNotes(conn);
                        System.out.print("\nEnter the ID or title of the note you want to read: ");
                        String idOrTitle = scanner.nextLine();
                        System.out.println();
                        try {
                            readNoteByIdOrTitle(conn, voice, idOrTitle);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 2:
                        exit = true;
                        System.out.println("Exiting Voice Assistant...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 1 and 2.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            voice.deallocate();
        }
    }
}
