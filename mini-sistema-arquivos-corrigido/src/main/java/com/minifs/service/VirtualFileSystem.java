package com.minifs.service;

import com.minifs.model.FsNode;
import com.minifs.model.AllocationStrategy;
import com.minifs.model.Permission;
import com.minifs.model.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory virtual disk. It does not modify the host computer's files. */
public class VirtualFileSystem {
    private final int blockSize;
    private final boolean[] bitmap;
    private final FsNode root = new FsNode("root", true, 0);
    private final Map<String, User> users = new LinkedHashMap<>();
    private User currentUser;

    public VirtualFileSystem(int blockCount, int blockSize) {
        if (blockCount < 8 || blockSize < 1) throw new IllegalArgumentException("Use pelo menos 8 blocos e tamanho positivo.");
        this.bitmap = new boolean[blockCount]; this.blockSize = blockSize;
        currentUser = new User("admin");
        users.put(currentUser.username().toLowerCase(), currentUser);
    }
    public FsNode getRoot() { return root; }
    public int getBlockSize() { return blockSize; }
    public int getBlockCount() { return bitmap.length; }
    public boolean isUsed(int i) { return bitmap[i]; }
    public int getUsedBlocks() { int n=0; for (boolean b:bitmap) if (b) n++; return n; }
    public int getFreeBlocks() { return bitmap.length-getUsedBlocks(); }
    public int blocksFor(int bytes) { return Math.max(1, (int)Math.ceil(bytes/(double)blockSize)); }
    public User createUser(String username) {
        User user = new User(username);
        User existing = users.get(user.username().toLowerCase());
        if (existing != null) return existing;
        users.put(user.username().toLowerCase(), user);
        assignAllPermissionsToUser(root, user);
        return user;
    }
    public Collection<User> getUsers() { return Collections.unmodifiableCollection(users.values()); }
    public User getCurrentUser() { return currentUser; }
    public void reset() {
        for (FsNode child : new ArrayList<>(root.getChildren())) {
            deleteWithoutPermissionCheck(child);
        }
        java.util.Arrays.fill(bitmap, false);
        users.clear();
        currentUser = new User("admin");
        users.put(currentUser.username().toLowerCase(), currentUser);
    }
    public void setCurrentUser(User user) {
        if (user == null || !users.containsKey(user.username().toLowerCase())) {
            throw new IllegalArgumentException("Usuário não cadastrado.");
        }
        currentUser = users.get(user.username().toLowerCase());
    }
    public void setCurrentUser(String username) { setCurrentUser(createUser(username)); }
    public void assignPermissions(FsNode file, User user, java.util.Set<Permission> granted) {
        if (file == null || file.isDirectory()) throw new IllegalArgumentException("Selecione um arquivo.");
        if (user == null) throw new IllegalArgumentException("Selecione um usuário.");
        if (!currentUser.equals(file.getOwner())) {
            throw new IllegalStateException("Somente o criador do arquivo pode alterar suas permissões.");
        }
        User registered = createUser(user.username());
        file.setPermissions(registered, granted);
    }

    public FsNode createDirectory(FsNode parent, String name) {
        validateParentAndName(parent,name);
        if (findChild(parent,name)!=null) throw new IllegalArgumentException("Já existe um item com esse nome.");
        FsNode node = new FsNode(name,true,0); parent.addChild(node); return node;
    }
    public FsNode createFile(FsNode parent, String name, int bytes) {
        return createFile(parent, name, bytes, currentUser, AllocationStrategy.LINKED);
    }
    public FsNode createFile(FsNode parent, String name, int bytes, User owner) {
        return createFile(parent, name, bytes, owner, AllocationStrategy.LINKED);
    }
    public FsNode createFile(FsNode parent, String name, int bytes, AllocationStrategy strategy) {
        return createFile(parent, name, bytes, currentUser, strategy);
    }
    public FsNode createFile(FsNode parent, String name, int bytes, User owner, AllocationStrategy strategy) {
        validateParentAndName(parent,name);
        if (parent==null || !parent.isDirectory()) throw new IllegalArgumentException("Selecione uma pasta de destino.");
        if (owner == null) throw new IllegalArgumentException("Informe o proprietário do arquivo.");
        if (!currentUser.equals(owner)) throw new IllegalArgumentException("O criador do arquivo deve ser o usuário atual.");
        if (findChild(parent,name)!=null) throw new IllegalArgumentException("Já existe um item com esse nome.");
        if (bytes < 1) throw new IllegalArgumentException("O tamanho deve ser maior que zero.");
        if (strategy == null) throw new IllegalArgumentException("Informe uma estratégia de alocação.");
        int dataBlocks = blocksFor(bytes);
        int needed = dataBlocks + (strategy == AllocationStrategy.INDEXED ? 1 : 0);
        List<Integer> free = strategy == AllocationStrategy.CONTIGUOUS
                ? findContiguousFree(dataBlocks)
                : findFree(needed);
        if (free.size()<needed) throw new IllegalStateException("Espaço insuficiente no disco virtual.");
        User registered = createUser(owner.username());
        FsNode node=new FsNode(name,false,bytes,registered, strategy);
        if (strategy == AllocationStrategy.INDEXED) {
            node.setIndexBlock(free.get(0));
            bitmap[free.get(0)] = true;
            free = free.subList(1, free.size());
        }
        for(int b:free){bitmap[b]=true;node.addBlock(b);}
        parent.addChild(node); return node;
    }
    public void delete(FsNode node) {
        if(node==null || node==root || node.getParent()==null) throw new IllegalArgumentException("Não é possível excluir a raiz.");
        if (!canDelete(node, currentUser)) {
            throw new IllegalStateException("O usuário atual não tem permissão para excluir este item.");
        }
        deleteWithoutPermissionCheck(node);
    }
    private void deleteWithoutPermissionCheck(FsNode node) {
        for(FsNode child:new ArrayList<>(node.getChildren())) deleteWithoutPermissionCheck(child);
        for(int b:node.getBlocks()) bitmap[b]=false;
        if (node.getIndexBlock() != null) bitmap[node.getIndexBlock()] = false;
        node.getParent().removeChild(node);
    }
    private boolean canDelete(FsNode node, User user) {
        if (!node.isDirectory() && !node.hasPermission(user, Permission.DELETE)) return false;
        for (FsNode child : node.getChildren()) if (!canDelete(child, user)) return false;
        return true;
    }
    private void assignAllPermissionsToUser(FsNode node, User user) {
        if (!node.isDirectory()) node.setPermissions(user, java.util.EnumSet.allOf(Permission.class));
        for (FsNode child : node.getChildren()) assignAllPermissionsToUser(child, user);
    }
    public List<Integer> getUsedBlockNumbers(FsNode node) { return node.getBlocks(); }
    public int defragment() {
        List<FsNode> files = new ArrayList<>();
        collectFiles(root, files);
        boolean[] newBitmap = new boolean[bitmap.length];
        int nextBlock = 0;
        int movedBlocks = 0;
        for (FsNode file : files) {
            List<Integer> newBlocks = new ArrayList<>();
            if (file.getAllocationStrategy() == AllocationStrategy.INDEXED) {
                file.setIndexBlock(nextBlock);
                newBitmap[nextBlock++] = true;
            }
            for (int oldBlock : file.getBlocks()) {
                if (oldBlock != nextBlock) movedBlocks++;
                newBlocks.add(nextBlock);
                newBitmap[nextBlock] = true;
                nextBlock++;
            }
            file.replaceBlocks(newBlocks);
        }
        System.arraycopy(newBitmap, 0, bitmap, 0, bitmap.length);
        return movedBlocks;
    }
    public int countFiles(FsNode node) { int n=node.isDirectory()?0:1; for(FsNode c:node.getChildren()) n+=countFiles(c); return n; }
    public int countDirectories(FsNode node) { int n=node.isDirectory()?1:0; for(FsNode c:node.getChildren()) n+=countDirectories(c); return n; }
    public String pathOf(FsNode node) { if(node==null)return ""; if(node==root)return "/"; List<String> parts=new ArrayList<>(); for(FsNode n=node;n!=null&&n!=root;n=n.getParent())parts.add(n.getName()); StringBuilder s=new StringBuilder(); for(int i=parts.size()-1;i>=0;i--)s.append('/').append(parts.get(i)); return s.toString(); }
    private List<Integer> findFree(int needed) { List<Integer> out=new ArrayList<>(); for(int i=0;i<bitmap.length&&out.size()<needed;i++)if(!bitmap[i])out.add(i); return out; }
    private List<Integer> findContiguousFree(int needed) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i <= bitmap.length - needed; i++) {
            boolean available = true;
            for (int j = 0; j < needed; j++) {
                if (bitmap[i + j]) {
                    available = false;
                    break;
                }
            }
            if (available) {
                for (int j = 0; j < needed; j++) out.add(i + j);
                return out;
            }
        }
        return out;
    }
    private void collectFiles(FsNode node, List<FsNode> files) {
        if (!node.isDirectory()) {
            files.add(node);
            return;
        }
        for (FsNode child : node.getChildren()) collectFiles(child, files);
    }
    private FsNode findChild(FsNode parent,String name){for(FsNode n:parent.getChildren())if(n.getName().equalsIgnoreCase(name))return n;return null;}
    private void validateParentAndName(FsNode parent,String name){if(parent==null||!parent.isDirectory())throw new IllegalArgumentException("Selecione uma pasta de destino.");if(name==null||name.isBlank()||name.contains("/")||name.contains("\\"))throw new IllegalArgumentException("Informe um nome válido, sem barras.");}
}
