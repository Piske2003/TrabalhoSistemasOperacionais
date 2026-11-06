package com.minifs.service;

import com.minifs.model.FsNode;
import com.minifs.model.AllocationStrategy;
import com.minifs.model.Permission;
import com.minifs.model.User;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VirtualFileSystemTest {
    @Test void createsFileAndAllocatesBlocks() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        FsNode file = fs.createFile(fs.getRoot(), "a.txt", 1500);
        assertEquals(2, file.getBlocks().size());
        assertEquals(2, fs.getUsedBlocks());
        assertEquals(6, fs.getFreeBlocks());
    }
    @Test void contiguousAllocationUsesAdjacentBlocks() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        FsNode file = fs.createFile(fs.getRoot(), "a.txt", 2500, AllocationStrategy.CONTIGUOUS);

        assertEquals(List.of(0, 1, 2), file.getBlocks());
        assertEquals(3, fs.getUsedBlocks());
    }
    @Test void indexedAllocationReservesAnIndexBlock() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        FsNode file = fs.createFile(fs.getRoot(), "a.txt", 1500, AllocationStrategy.INDEXED);

        assertEquals(2, file.getBlocks().size());
        assertNotNull(file.getIndexBlock());
        assertEquals(3, fs.getUsedBlocks());
    }
    @Test void resetClearsFilesAndUsers() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        fs.createFile(fs.getRoot(), "a.txt", 1024);
        fs.createUser("convidado");

        fs.reset();

        assertEquals(0, fs.countFiles(fs.getRoot()));
        assertEquals(1, fs.getUsers().size());
        assertEquals("admin", fs.getCurrentUser().username());
        assertEquals(8, fs.getFreeBlocks());
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
    @Test void defragmentationCompactsBlocksAndPreservesFileSizes() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        FsNode first = fs.createFile(fs.getRoot(), "a.txt", 1024);
        FsNode second = fs.createFile(fs.getRoot(), "b.txt", 2048);
        fs.delete(first);

        assertEquals(List.of(1, 2), second.getBlocks());
        assertEquals(2, fs.defragment());
        assertEquals(List.of(0, 1), second.getBlocks());
        assertTrue(fs.isUsed(0));
        assertTrue(fs.isUsed(1));
        assertFalse(fs.isUsed(2));
    }
    @Test void assignsPermissionsToFileByUsername() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        User owner = fs.getCurrentUser();
        FsNode file = fs.createFile(fs.getRoot(), "a.txt", 10);
        User guest = fs.createUser("convidado");

        assertTrue(file.hasPermission(guest, Permission.DELETE));
        fs.assignPermissions(file, guest, java.util.Set.of(Permission.READ));

        assertEquals(owner, file.getOwner());
        assertTrue(file.hasPermission(guest, Permission.READ));
        assertFalse(file.hasPermission(guest, Permission.WRITE));
        assertFalse(file.hasPermission(guest, Permission.DELETE));
        assertTrue(file.hasPermission(owner, Permission.EXECUTE));
    }
    @Test void onlyFileCreatorCanChangePermissions() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        FsNode file = fs.createFile(fs.getRoot(), "a.txt", 10);
        User guest = fs.createUser("convidado");
        fs.setCurrentUser(guest);

        assertThrows(IllegalStateException.class,
                () -> fs.assignPermissions(file, guest, java.util.Set.of(Permission.READ)));
    }
    @Test void deletesOnlyWhenCurrentUserHasDeletePermission() {
        VirtualFileSystem fs = new VirtualFileSystem(8, 1024);
        FsNode file = fs.createFile(fs.getRoot(), "a.txt", 10);
        User guest = fs.createUser("convidado");
        fs.assignPermissions(file, guest, java.util.Set.of(Permission.READ));
        fs.setCurrentUser(guest);

        assertThrows(IllegalStateException.class, () -> fs.delete(file));

        fs.setCurrentUser(file.getOwner());
        fs.delete(file);
        assertEquals(0, fs.countFiles(fs.getRoot()));
    }
}
