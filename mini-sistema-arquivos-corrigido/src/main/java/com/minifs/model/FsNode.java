package com.minifs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FsNode {
    private final String name;
    private final boolean directory;
    private final int sizeBytes;
    private final List<Integer> blocks = new ArrayList<>();
    private final List<FsNode> children = new ArrayList<>();
    private FsNode parent;

    public FsNode(String name, boolean directory, int sizeBytes) {
        this.name = name; this.directory = directory; this.sizeBytes = sizeBytes;
    }
    public String getName() { return name; }
    public boolean isDirectory() { return directory; }
    public int getSizeBytes() { return sizeBytes; }
    public List<Integer> getBlocks() { return Collections.unmodifiableList(blocks); }
    public List<FsNode> getChildren() { return Collections.unmodifiableList(children); }
    public FsNode getParent() { return parent; }
    public void addChild(FsNode child) { child.parent = this; children.add(child); }
    public void removeChild(FsNode child) { children.remove(child); child.parent = null; }
    public void addBlock(int block) { blocks.add(block); }
    public void clearBlocks() { blocks.clear(); }
    @Override public String toString() { return directory ? name + "/" : name + " (" + sizeBytes + " B)"; }
}
