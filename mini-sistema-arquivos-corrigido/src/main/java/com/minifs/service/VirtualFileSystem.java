package com.minifs.service;

import com.minifs.model.FsNode;
import java.util.ArrayList;
import java.util.List;

/** In-memory virtual disk. It does not modify the host computer's files. */
public class VirtualFileSystem {
    private final int blockSize;
    private final boolean[] bitmap;
    private final FsNode root = new FsNode("root", true, 0);

    public VirtualFileSystem(int blockCount, int blockSize) {
        if (blockCount < 8 || blockSize < 1) throw new IllegalArgumentException("Use pelo menos 8 blocos e tamanho positivo.");
        this.bitmap = new boolean[blockCount]; this.blockSize = blockSize;
    }
    public FsNode getRoot() { return root; }
    public int getBlockSize() { return blockSize; }
    public int getBlockCount() { return bitmap.length; }
    public boolean isUsed(int i) { return bitmap[i]; }
    public int getUsedBlocks() { int n=0; for (boolean b:bitmap) if (b) n++; return n; }
    public int getFreeBlocks() { return bitmap.length-getUsedBlocks(); }
    public int blocksFor(int bytes) { return Math.max(1, (int)Math.ceil(bytes/(double)blockSize)); }

    public FsNode createDirectory(FsNode parent, String name) {
        validateParentAndName(parent,name);
        if (findChild(parent,name)!=null) throw new IllegalArgumentException("Já existe um item com esse nome.");
        FsNode node = new FsNode(name,true,0); parent.addChild(node); return node;
    }
    public FsNode createFile(FsNode parent, String name, int bytes) {
        validateParentAndName(parent,name);
        if (parent==null || !parent.isDirectory()) throw new IllegalArgumentException("Selecione uma pasta de destino.");
        if (findChild(parent,name)!=null) throw new IllegalArgumentException("Já existe um item com esse nome.");
        if (bytes < 1) throw new IllegalArgumentException("O tamanho deve ser maior que zero.");
        int needed=blocksFor(bytes); List<Integer> free=findFree(needed);
        if (free.size()<needed) throw new IllegalStateException("Espaço insuficiente no disco virtual.");
        FsNode node=new FsNode(name,false,bytes);
        for(int b:free){bitmap[b]=true;node.addBlock(b);} parent.addChild(node); return node;
    }
    public void delete(FsNode node) {
        if(node==null || node==root || node.getParent()==null) throw new IllegalArgumentException("Não é possível excluir a raiz.");
        for(FsNode child:new ArrayList<>(node.getChildren())) delete(child);
        for(int b:node.getBlocks()) bitmap[b]=false;
        node.getParent().removeChild(node);
    }
    public List<Integer> getUsedBlockNumbers(FsNode node) { return node.getBlocks(); }
    public int countFiles(FsNode node) { int n=node.isDirectory()?0:1; for(FsNode c:node.getChildren()) n+=countFiles(c); return n; }
    public int countDirectories(FsNode node) { int n=node.isDirectory()?1:0; for(FsNode c:node.getChildren()) n+=countDirectories(c); return n; }
    public String pathOf(FsNode node) { if(node==null)return ""; if(node==root)return "/"; List<String> parts=new ArrayList<>(); for(FsNode n=node;n!=null&&n!=root;n=n.getParent())parts.add(n.getName()); StringBuilder s=new StringBuilder(); for(int i=parts.size()-1;i>=0;i--)s.append('/').append(parts.get(i)); return s.toString(); }
    private List<Integer> findFree(int needed) { List<Integer> out=new ArrayList<>(); for(int i=0;i<bitmap.length&&out.size()<needed;i++)if(!bitmap[i])out.add(i); return out; }
    private FsNode findChild(FsNode parent,String name){for(FsNode n:parent.getChildren())if(n.getName().equalsIgnoreCase(name))return n;return null;}
    private void validateParentAndName(FsNode parent,String name){if(parent==null||!parent.isDirectory())throw new IllegalArgumentException("Selecione uma pasta de destino.");if(name==null||name.isBlank()||name.contains("/")||name.contains("\\"))throw new IllegalArgumentException("Informe um nome válido, sem barras.");}
}
