package filesystem;

import java.io.IOException;


public class FileSystem {
    private Disk diskDevice;

    private int iNodeNumber;
    private int fileDescriptor;
    private INode iNodeForFile;
    // gives you a list of all the free blocks for allocation
    private FreeBlockList freeBlockList;
    public FileSystem() throws IOException {
        diskDevice = new Disk();
        diskDevice.format();
        freeBlockList = new FreeBlockList();
    }

    /***
     * Create a file with the name <code>fileName</code>
     *
     * @param fileName - name of the file to create
     * @throws IOException
     */
    public int create(String fileName) throws IOException {
        INode tmpINode = null;
        boolean isCreated = false;

        for (int i = 0; i < Disk.NUM_INODES && !isCreated; i++) {
            tmpINode = diskDevice.readInode(i);
            String name = tmpINode.getFileName();

            // Check if the name is null before trimming
            if (name != null && name.trim().equals(fileName)) {
                throw new IOException("FileSystem::create: " + fileName + " already exists");
            } else if (name == null) { // Unused inode found
                this.iNodeForFile = new INode();
                this.iNodeForFile.setFileName(fileName);
                this.iNodeNumber = i;
                this.fileDescriptor = i;
                isCreated = true;
            }
        }

        if (!isCreated) {
            throw new IOException("FileSystem::create: Unable to create file");
        }

        return fileDescriptor;
    }


    /**
     * Removes the file
     *
     * @param fileName
     * @throws IOException
     */
    public void delete(String fileName) throws IOException {
        INode tmpINode = null;
        boolean isFound = false;
        int inodeNumForDeletion = -1;

        /**
         * Find the non-null named inode that matches,
         * If you find it, set its file name to null
         * to indicate it is unused
         */
        for (int i = 0; i < Disk.NUM_INODES && !isFound; i++) {
            tmpINode = diskDevice.readInode(i);

            String fName = tmpINode.getFileName();

            if (fName != null && fName.trim().compareTo(fileName.trim()) == 0) {
                isFound = true;
                inodeNumForDeletion = i;
                break;
            }
        }

        /***
         * If file found, go ahead and deallocate its
         * blocks and null out the filename.
         */
        if (isFound) {
            deallocateBlocksForFile(inodeNumForDeletion);
            tmpINode.setFileName(null);
            diskDevice.writeInode(tmpINode, inodeNumForDeletion);
            this.iNodeForFile = null;
            this.fileDescriptor = -1;
            this.iNodeNumber = -1;
        }
    }


    /***
     * Makes the file available for reading/writing
     *
     * @return
     * @throws IOException
     */
    public int open(String fileName) throws IOException {
        this.fileDescriptor = -1;
        this.iNodeNumber = -1;
        INode tmpINode = null;
        boolean isFound = false;
        int iNodeContainingName = -1;

        for (int i = 0; i < Disk.NUM_INODES && !isFound; i++) {
            tmpINode = diskDevice.readInode(i);
            String fName = tmpINode.getFileName();
            if (fName != null) {
                if (fName.trim().compareTo(fileName.trim()) == 0) {
                    isFound = true;
                    iNodeContainingName = i;
                    this.iNodeForFile = tmpINode;
                }
            }
        }

        if (isFound) {
            this.fileDescriptor = iNodeContainingName;
            this.iNodeNumber = fileDescriptor;
        }

        return this.fileDescriptor;
    }


    /***
     * Closes the file
     *
     * @throws IOException If disk is not accessible for writing
     */
    public void close(int fileDescriptor) throws IOException {
        if (fileDescriptor != this.iNodeNumber){
            throw new IOException("FileSystem::close: file descriptor, "+
                    fileDescriptor + " does not match file descriptor " +
                    "of open file");
        }
        diskDevice.writeInode(this.iNodeForFile, this.iNodeNumber);
        this.iNodeForFile = null;
        this.fileDescriptor = -1;
        this.iNodeNumber = -1;
    }


    /**
     * Add your Javadoc documentation for this method
     */
    /**
     * Reads data from the file specified by the file descriptor.
     * @param fileDescriptor The descriptor of the file to be read.
     * @return A string containing the contents of the file.
     * @throws IOException If there are issues reading data from the disk.
     */
    public String read(int fileDescriptor) throws IOException {
        // Retrieve inode
        // Retrieves the file's inode using the fileDescriptor, which contains block pointers to locate the file's data
        INode inode = diskDevice.readInode(fileDescriptor);
        String fileData = "";

        // Read Data Blocks
        // Goes through each block pointer in the inode and reads the data stored in each block on the disk
        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            int blockPointer = inode.getBlockPointer(i);
            if (blockPointer >= 0) {
                byte[] blockData = diskDevice.readDataBlock(blockPointer);

                // Combine Data
                // Adds the data from each block together into one string to rebuild the file's content
                String data = new String(blockData).trim();
                fileData += data;
            }
        }

        // Return File Content
        // After reading and combining all the blocks, the method returns the full content of the file as a string
        return fileData;
    }

    /**
     * Writes data to the file specified by the file descriptor.
     * @param fileDescriptor The descriptor of the file to write to.
     * @param data The string data to be written.
     * @throws IOException If there are issues writing to the file.
     */
    public int write(int fileDescriptor, String data) throws IOException {
        if (fileDescriptor != this.iNodeNumber) {
            throw new IOException("Filesystem:write: file descriptor," + fileDescriptor +
                    " does not match file descriptor of open file");
        }

        // Convert data to bytes
        byte[] dataBytes = data.getBytes();
        int dataSize = dataBytes.length;

        // Allocate blocks if not already allocated
        int[] blockNumbers = new int[INode.NUM_BLOCK_POINTERS];
        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            blockNumbers[i] = this.iNodeForFile.getBlockPointer(i);
        }

        if (blockNumbers[0] == -1) { // No blocks allocated yet
            int[] newBlockNumbers = allocateBlocksForFile(this.iNodeNumber, dataSize);
            for (int i = 0; i < newBlockNumbers.length; i++) {
                this.iNodeForFile.setBlockPointer(i, newBlockNumbers[i]);
            }
            diskDevice.writeInode(this.iNodeForFile, this.iNodeNumber);
            blockNumbers = newBlockNumbers;
        }

        // Write data to allocated blocks
        int currentBlock = 0;
        int currentByte = 0;

        while (currentByte < dataSize) {
            // Create a 512-byte buffer
            byte[] buffer = new byte[Disk.BLOCK_SIZE];
            int bytesToWrite = Math.min(Disk.BLOCK_SIZE, dataSize - currentByte);

            // Copy data to the buffer
            System.arraycopy(dataBytes, currentByte, buffer, 0, bytesToWrite);

            // Write the buffer to the disk
            diskDevice.writeDataBlock(buffer, blockNumbers[currentBlock]);

            // Move to the next block
            currentBlock++;
            currentByte += Disk.BLOCK_SIZE;
        }

        // Update the inode size
        this.iNodeForFile.setSize(dataSize);
        diskDevice.writeInode(this.iNodeForFile, this.iNodeNumber);

        return fileDescriptor;
    }



/**
 * Allocates the necessary blocks for a file based on its size.
 * @param iNodeNumber The inode number for the file.
 * @param numBytes The size of the file in bytes.
 * @return An array of allocated block numbers.
 * @throws IOException If there are no enough free blocks available.
 */

    /**
     * It calculates how many blocks are need to be allocated.
     * It uses a forloop to keep track of what blocks were allocated.
     *
     */
    private int[] allocateBlocksForFile(int iNodeNumber, int numBytes) throws IOException {
        // Step 1: Calculate required blocks
        int numBlocksNeeded = (numBytes + Disk.BLOCK_SIZE - 1) / Disk.BLOCK_SIZE;
        if (numBlocksNeeded > INode.NUM_BLOCK_POINTERS) {
            throw new IOException("File size exceeds maximum supported size.");
        }

        // Step 2: Read the free block list
        byte[] freeBlockList = diskDevice.readFreeBlockList();
        int[] allocatedBlocks = new int[numBlocksNeeded];
        int foundBlocks = 0;

        // Step 3: Find free blocks
        for (int i = 0; foundBlocks < numBlocksNeeded && i < Disk.NUM_BLOCKS; i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;

            if ((freeBlockList[byteIndex] & (1 << bitIndex)) == 0) { // Block is free
                freeBlockList[byteIndex] |= (1 << bitIndex); // Mark as allocated
                allocatedBlocks[foundBlocks++] = i;
            }
        }

        // Step 4: Handle insufficient blocks
        if (foundBlocks < numBlocksNeeded) {
            throw new IOException("Not enough free blocks available.");
        }

        // Step 5: Write updated free block list
        diskDevice.writeFreeBlockList(freeBlockList);

        // Step 6: Update the inode
        INode inode = diskDevice.readInode(iNodeNumber);
        for (int i = 0; i < numBlocksNeeded; i++) {
            inode.setBlockPointer(i, allocatedBlocks[i]);
        }
        inode.setSize(numBytes);
        diskDevice.writeInode(inode, iNodeNumber);

        // Step 7: Return allocated block numbers
        return allocatedBlocks;
    }

    /**
     * Deallocates the blocks used by a file.
     * @param iNodeNumber The inode number of the file whose blocks are to be deallocated.
     */


    /*
     * Loops through all the blocks connected to the Inode
     * deallocates each block then it severs the connection
     * between the Inode and the block. Then it writes the
     * updated information to the disk.
     */

    private void deallocateBlocksForFile(int iNodeNumber) throws IOException {
        INode inode = diskDevice.readInode(iNodeNumber);

        freeBlockList.setFreeBlockList(diskDevice.readFreeBlockList());

        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            int blockNumber = inode.getBlockPointer(i);
            if (blockNumber == -1) break;

            freeBlockList.deallocateBlock(blockNumber);
            inode.setBlockPointer(i, -1);
        }

        diskDevice.writeFreeBlockList(freeBlockList.getFreeBlockList());
        inode.setSize(-1);
        diskDevice.writeInode(inode, iNodeNumber);
    }

}