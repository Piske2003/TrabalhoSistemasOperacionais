package com.minifs.service;

import com.minifs.model.FsNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VirtualFileSystemTest {
    @Test void createsFileAndAllocatesBlocks() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        FsNode file = fs.createFile(fs.getRoot(), "a.txt", 1500);
        assertEquals(2, file.getBlocks().size());
        assertEquals(2, fs.getUsedBlocks());
        assertEquals(6, fs.getFreeBlocks());
    }
    @Test void deletingFileFreesBlocks() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        FsNode file = fs.createFile(fs.getRoot(), "a.txt", 1500);
        fs.delete(file);
        assertEquals(0, fs.getUsedBlocks());
        assertEquals(8, fs.getFreeBlocks());
    }
    @Test void rejectsDuplicateNames() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        fs.createDirectory(fs.getRoot(), "docs");
        assertThrows(IllegalArgumentException.class, () -> fs.createDirectory(fs.getRoot(), "docs"));
    }
}
