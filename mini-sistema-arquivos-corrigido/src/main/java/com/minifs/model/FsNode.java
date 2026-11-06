package com.minifs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FsNode {
    private final String name;
    private final boolean directory;
    private final int sizeBytes;
    private final List<Integer> blocks = new ArrayList<>();
    private final List<FsNode> children = new ArrayList<>();
    private final User owner;
    private final AllocationStrategy allocationStrategy;
    private Integer indexBlock;
    private final Map<String, EnumSet<Permission>> permissions = new LinkedHashMap<>();
    private FsNode parent;

    public FsNode(String name, boolean directory, int sizeBytes) {
        this(name, directory, sizeBytes, new User("admin"), AllocationStrategy.LINKED);
    }

    public FsNode(String name, boolean directory, int sizeBytes, User owner) {
        this(name, directory, sizeBytes, owner, AllocationStrategy.LINKED);
    }

    public FsNode(String name, boolean directory, int sizeBytes, User owner, AllocationStrategy allocationStrategy) {
        this.name = name; this.directory = directory; this.sizeBytes = sizeBytes;
        this.owner = owner;
        this.allocationStrategy = allocationStrategy;
        permissions.put(owner.username(), EnumSet.allOf(Permission.class));
    }
    public String getName() { return name; }

    public boolean isDirectory() { return directory; }

    public int getSizeBytes() { return sizeBytes; }

    public List<Integer> getBlocks() { return Collections.unmodifiableList(blocks); }

    public List<FsNode> getChildren() { return Collections.unmodifiableList(children); }

    public FsNode getParent() { return parent; }

    public User getOwner() { return owner; }

    public AllocationStrategy getAllocationStrategy() { return allocationStrategy; }

    public Integer getIndexBlock() { return indexBlock; }

    public void setIndexBlock(Integer indexBlock) { this.indexBlock = indexBlock; }

    public Map<String, EnumSet<Permission>> getPermissions() {
        Map<String, EnumSet<Permission>> copy = new LinkedHashMap<>();
        permissions.forEach((user, granted) -> copy.put(user, EnumSet.copyOf(granted)));
        return Collections.unmodifiableMap(copy);
    }
    public void setPermissions(User user, java.util.Set<Permission> granted) {
        if (user == null || granted == null) throw new IllegalArgumentException("Usuário e permissões são obrigatórios.");
        permissions.put(user.username(), granted.isEmpty()
                ? EnumSet.noneOf(Permission.class)
                : EnumSet.copyOf(granted));
    }
    public boolean hasPermission(User user, Permission permission) {
        return user != null && permission != null
                && permissions.getOrDefault(user.username(), EnumSet.noneOf(Permission.class)).contains(permission);
    }
    public void addChild(FsNode child) { child.parent = this; children.add(child); }

    public void removeChild(FsNode child) { children.remove(child); child.parent = null; }

    public void addBlock(int block) { blocks.add(block); }
    
    public void clearBlocks() { blocks.clear(); }

    public void replaceBlocks(List<Integer> newBlocks) {
        blocks.clear();
        blocks.addAll(newBlocks);
    }
    @Override public String toString() { return directory ? name + "/" : name + " (" + sizeBytes + " B)"; }
}
