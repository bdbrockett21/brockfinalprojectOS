package filesystem;

public class YourClass {
    private String data;

    public int write(int fileDescriptor, String data) {
        this.data = data;
        return fileDescriptor;
    }
}
