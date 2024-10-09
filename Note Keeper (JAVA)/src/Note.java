public class Note{
    private int id;
    private String title;
    private String content;
    private String date;

    public Note() {
    }

    public Note(int id, String title, String content, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String toString() {
        return "id= " + id +
                ", title= '" + title + '\'' +
                ", content= '" + content + '\'' +
                ", date= '" + date + '\'';
    }
}
