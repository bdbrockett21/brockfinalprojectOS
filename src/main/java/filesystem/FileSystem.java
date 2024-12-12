package filesystem;

import java.io.IOException;


public class FileSystem {
    private Disk diskDevice;
    // gives you a list of all the free blocks for allocation
    private FreeBlockList freeBlockList;
    private int iNodeNumber;
    private int fileDescriptor;
    private INode iNodeForFile;

    public FileSystem() throws IOException {
        diskDevice = new Disk();
        diskDevice.format();
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
            if (name.trim().equals(fileName)) {
                throw new IOException("FileSystem::create: " + fileName +
                        " already exists");
            } else if (tmpINode.getFileName() == null) {
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
        if (fileDescriptor != this.iNodeNumber) {
            throw new IOException("FileSystem::close: file descriptor, " +
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
    public String read(int fileDescriptor) throws IOException {
        INode inode = diskDevice.readInode(fileDescriptor);
        String fileData = "";
    
        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            int blockPointer = inode.getBlockPointer(i);
            if (blockPointer >= 0) {
                byte[] blockData = diskDevice.readDataBlock(blockPointer);
                String data = new String(blockData).trim();
                fileData += data;
            }
        }
    
        return fileData;
    }


    /**
     * Add your Javadoc documentation for this method
     *
     * @return
     */
    public int write(int fileDescriptor, String data) throws IOException {
        if (fileDescriptor != this.iNodeNumber) {
            throw new IOException("Filesystem:write: file descriptor," + fileDescriptor +
                    " does not match file descriptor to open file");
        }

        int[] blockNumbers = this.iNodeForFile.getBlockNumbers();
        if (blockNumbers == null) {
            int[] newBlockNumbers = allocateBlocksForFile(this.iNodeNumber, data.length());
            this.iNodeForFile.setBlockNumbers(newBlockNumbers);
            diskDevice.writeInode(this.iNodeForFile, this.iNodeNumber);
            blockNumbers = this.iNodeForFile.getBlockNumbers();
        }

        // now we write the data
        int currentBlock = 0;
        int currentByte = 0;

        while (currentByte < data.length()) {
            // Get a chunk of data for this block
            byte[] dataBytes = data.substring(currentByte,
                    Math.min(currentByte + Disk.BLOCK_SIZE, data.length())).getBytes();

            // Write the block
            diskDevice.writeDataBlock(dataBytes, blockNumbers[currentBlock]);

            // Move to next block
            currentBlock++;
            currentByte += Disk.BLOCK_SIZE;
        }

        return fileDescriptor;
    }



    /**
     * Add your Javadoc documentation for this method
     */
    private int[] allocateBlocksForFile(int iNodeNumber, int numBytes)
            throws IOException {

        // TODO: replace this line with your code

        return null;
    }

    /**
     * Add your Javadoc documentation for this method
     */
    /*
     * Loops through all the blocks connected to the Inode
     * deallocates each block then it severs the connection
     * between the Inode and the block. Then it writes the
     * updated information to the disk.
     * Wayleom Did This
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