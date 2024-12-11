package filesystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class WriteTest {

    private YourClass instance; // Replace 'YourClass' with the class containing the write method.

    @BeforeEach
    void setUp() {
        // Initialize your class or set up any required dependencies here.
        instance = new YourClass();
        // Example: instance.openFile(1); // Simulate opening a file with descriptor 1.
    }

    @Test
    void testWriteWithMatchingFileDescriptor() {
        int fileDescriptor = 1; // Use a valid file descriptor.
        String data = "Sample data";

        int bytesWritten;
        bytesWritten = instance.write(fileDescriptor, data);
        assertEquals(data.length(), bytesWritten, "Bytes written should match data length.");
    }

    @Test
    void testWriteWithNonMatchingFileDescriptor() {
        int invalidFileDescriptor = 999; // Use an invalid file descriptor.
        String data = "Sample data";

        Exception exception = assertThrows(IOException.class, () -> {
            instance.write(invalidFileDescriptor, data);
        });

        String expectedMessage = "Filesystem:write: file descriptor," + invalidFileDescriptor +
                " does not match file descriptor to open file";
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testWriteWithNoBlocksAllocated() {
        int fileDescriptor = 1; // Use a valid file descriptor.
        String data = "Data that needs new blocks allocated.";

        int bytesWritten = instance.write(fileDescriptor, data);
        assertEquals(data.length(), bytesWritten, "Bytes written should match data length.");
        // Additional assertions to verify block allocation can go here.
    }
}
