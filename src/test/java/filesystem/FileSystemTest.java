package filesystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemTest {

    private FileSystem fileSystem;

    @BeforeEach
    void setUp() throws IOException {
        // Initialize the FileSystem instance before each test
        fileSystem = new FileSystem();
    }

    @Test
    void testCreateFileSuccessfully() throws IOException {
        String fileName = "testFile.txt";
        int fileDescriptor = fileSystem.create(fileName);

        assertTrue(fileDescriptor >= 0, "File descriptor should be non-negative.");
    }

    @Test
    void testCreateDuplicateFile() throws IOException {
        String fileName = "duplicateFile.txt";
        fileSystem.create(fileName);

        Exception exception = assertThrows(IOException.class, () -> fileSystem.create(fileName));
        String expectedMessage = "FileSystem::create: " + fileName + " already exists";
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match expected message.");
    }

    @Test
    void testDeleteFileSuccessfully() throws IOException {
        String fileName = "fileToDelete.txt";
        fileSystem.create(fileName);

        fileSystem.delete(fileName);

        Exception exception = assertThrows(IOException.class, () -> fileSystem.open(fileName));
        String expectedMessage = "FileSystem::open: file not found";
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match expected message.");
    }

    @Test
    void testDeleteNonExistentFile() {
        String fileName = "nonExistentFile.txt";

        Exception exception = assertThrows(IOException.class, () -> fileSystem.delete(fileName));
        String expectedMessage = "FileSystem::delete: file not found";
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match expected message.");
    }

    @Test
    void testOpenFileSuccessfully() throws IOException {
        String fileName = "fileToOpen.txt";
        fileSystem.create(fileName);

        int fileDescriptor = fileSystem.open(fileName);
        assertTrue(fileDescriptor >= 0, "File descriptor should be non-negative.");
    }

    @Test
    void testOpenNonExistentFile() {
        String fileName = "nonExistentFile.txt";

        Exception exception = assertThrows(IOException.class, () -> fileSystem.open(fileName));
        String expectedMessage = "FileSystem::open: file not found";
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match expected message.");
    }

    @Test
    void testWriteFileSuccessfully() throws IOException {
        String fileName = "fileToWrite.txt";
        String data = "Hello, world!";
        int fileDescriptor = fileSystem.create(fileName);

        int bytesWritten = fileSystem.write(fileDescriptor, data);
        assertEquals(data.length(), bytesWritten, "Bytes written should match the length of data.");

        String fileContent = fileSystem.read(fileDescriptor);
        assertEquals(data, fileContent, "File content should match written data.");
    }

    @Test
    void testWriteToInvalidFileDescriptor() {
        int invalidFileDescriptor = 999;
        String data = "Invalid write test";

        Exception exception = assertThrows(IOException.class, () -> fileSystem.write(invalidFileDescriptor, data));
        String expectedMessage = "Filesystem:write: file descriptor," + invalidFileDescriptor +
                " does not match file descriptor to open file";
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match expected message.");
    }

    @Test
    void testReadFileSuccessfully() throws IOException {
        String fileName = "fileToRead.txt";
        String data = "This is test data.";
        int fileDescriptor = fileSystem.create(fileName);
        fileSystem.write(fileDescriptor, data);

        String fileContent = fileSystem.read(fileDescriptor);
        assertEquals(data, fileContent, "File content should match written data.");
    }

    @Test
    void testReadFromInvalidFileDescriptor() {
        int invalidFileDescriptor = 999;

        Exception exception = assertThrows(IOException.class, () -> fileSystem.read(invalidFileDescriptor));
        String expectedMessage = "Filesystem:read: file descriptor," + invalidFileDescriptor + " not found";
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match expected message.");
    }

    @Test
    void testCloseFileSuccessfully() throws IOException {
        String fileName = "fileToClose.txt";
        int fileDescriptor = fileSystem.create(fileName);

        fileSystem.close(fileDescriptor);

        Exception exception = assertThrows(IOException.class, () -> fileSystem.write(fileDescriptor, "Data after close"));
        String expectedMessage = "Filesystem:write: file descriptor," + fileDescriptor +
                " does not match file descriptor to open file";
        assertEquals(expectedMessage, exception.getMessage(), "Exception message should match expected message.");
    }
}
