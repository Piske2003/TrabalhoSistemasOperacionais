package com.minifs.service;

import com.minifs.model.AllocationStrategy;
import com.minifs.model.FsNode;
import com.minifs.model.Permission;
import com.minifs.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Sistema de arquivos didático mantido inteiramente em memória. */
public final class VirtualFileSystem {
    private final boolean[] bitmap;
    private final int blockSize;
    private final List<User> users = new ArrayList<>();
    private FsNode root;
    private User currentUser;

    public VirtualFileSystem(int blockCount, int blockSize) {
        if (blockCount <= 0 || blockSize <= 0) throw new IllegalArgumentException("Quantidade e tamanho dos blocos devem ser positivos.");
        bitmap = new boolean[blockCount];
        this.blockSize = blockSize;
        reset();
    }

    public FsNode getRoot() { return root; }
    public int getBlockCount() { return bitmap.length; }
    public int getBlockSize() { return blockSize; }
    public boolean isUsed(int block) { return block >= 0 && block < bitmap.length && bitmap[block]; }
    public int getUsedBlocks() { return bitmap.length - getFreeBlocks(); }
    public int getFreeBlocks() { int free = 0; for (boolean used : bitmap) if (!used) free++; return free; }
    public User getCurrentUser() { return currentUser; }
    public List<User> getUsers() { return Collections.unmodifiableList(users); }

    public void reset() {
        Arrays.fill(bitmap, false);
        users.clear();
        currentUser = new User("admin");
        users.add(currentUser);
        root = new FsNode("raiz", true, 0, currentUser);
    }

    public User createUser(String username) {
        User candidate = new User(username);
        User existing = findUser(candidate.username());
        if (existing != null) return existing;
        users.add(candidate);
        grantDefaultPermissions(root, candidate);
        return candidate;
    }

    public void setCurrentUser(String username) { currentUser = createUser(username); }
    public void setCurrentUser(User user) {
        if (user == null) throw new IllegalArgumentException("Usuário é obrigatório.");
        User existing = findUser(user.username());
        if (existing == null) throw new IllegalArgumentException("Usuário não cadastrado.");
        currentUser = existing;
    }

    public FsNode createDirectory(FsNode parent, String name) {
        validateParentAndName(parent, name);
        FsNode directory = new FsNode(name.trim(), true, 0, currentUser);
        parent.addChild(directory);
        return directory;
    }

    public FsNode createFile(FsNode parent, String name, int sizeBytes) {
        return createFile(parent, name, sizeBytes, AllocationStrategy.LINKED);
    }

    public FsNode createFile(FsNode parent, String name, int sizeBytes, AllocationStrategy strategy) {
        validateParentAndName(parent, name);
        if (sizeBytes < 0) throw new IllegalArgumentException("O tamanho do arquivo não pode ser negativo.");
        if (strategy == null) throw new IllegalArgumentException("Selecione uma estratégia de alocação.");
        long dataCountLong = sizeBytes == 0 ? 0 : ((long) sizeBytes + blockSize - 1) / blockSize;
        long requiredLong = dataCountLong + (strategy == AllocationStrategy.INDEXED ? 1 : 0);
        if (requiredLong > getFreeBlocks()) throw new IllegalStateException("Não há blocos livres suficientes.");
        int dataCount = (int) dataCountLong;
        int required = (int) requiredLong;

        List<Integer> allocated = strategy == AllocationStrategy.CONTIGUOUS ? findContiguousBlocks(dataCount) : findFreeBlocks(required);
        if (allocated == null) throw new IllegalStateException("Não existe uma sequência contígua de blocos livres suficiente.");
        FsNode file = new FsNode(name.trim(), false, sizeBytes, currentUser, strategy);
        if (strategy == AllocationStrategy.INDEXED) {
            file.setIndexBlock(allocated.remove(0));
            bitmap[file.getIndexBlock()] = true;
        }
        for (int block : allocated) { bitmap[block] = true; file.addBlock(block); }
        for (User user : users) file.setPermissions(user, EnumSet.allOf(Permission.class));
        parent.addChild(file);
        return file;
    }

    public void assignPermissions(FsNode file, User user, Set<Permission> permissions) {
        if (file == null || file.isDirectory()) throw new IllegalArgumentException("Selecione um arquivo válido.");
        if (!currentUser.equals(file.getOwner())) throw new IllegalStateException("Apenas o criador pode alterar as permissões.");
        if (user == null || findUser(user.username()) == null) throw new IllegalArgumentException("Usuário não cadastrado.");
        file.setPermissions(user, permissions);
    }

    public void resizeFile(FsNode file, int newSizeBytes) {
        if (file == null || file.isDirectory()) throw new IllegalArgumentException("Selecione um arquivo válido.");
        if (newSizeBytes < 0) throw new IllegalArgumentException("O tamanho do arquivo não pode ser negativo.");
        if (!file.hasPermission(currentUser, Permission.WRITE)) {
            throw new IllegalStateException("O usuário atual não possui permissão de escrita neste arquivo.");
        }
        if (file.getSizeBytes() == newSizeBytes) return;

        boolean[] previousBitmap = bitmap.clone();
        List<Integer> previousBlocks = new ArrayList<>(file.getBlocks());
        Integer previousIndexBlock = file.getIndexBlock();

        try {
            releaseFileBlocks(file);
            allocateBlocks(file, newSizeBytes, file.getAllocationStrategy());
            file.setSizeBytes(newSizeBytes);
        } catch (RuntimeException exception) {
            System.arraycopy(previousBitmap, 0, bitmap, 0, bitmap.length);
            file.replaceBlocks(previousBlocks);
            file.setIndexBlock(previousIndexBlock);
            throw exception;
        }
    }

    public void delete(FsNode node) {
        if (node == null || node == root || node.getParent() == null) throw new IllegalArgumentException("Não é possível excluir esse item.");
        ensureDeletePermission(node);
        releaseRecursively(node);
        node.getParent().removeChild(node);
    }

    public int defragment() {
        List<FsNode> files = new ArrayList<>();
        collectFiles(root, files);
        boolean[] compacted = new boolean[bitmap.length];
        int next = 0, moved = 0;
        for (FsNode file : files) {
            if (file.getIndexBlock() != null) {
                if (file.getIndexBlock() != next) moved++;
                file.setIndexBlock(next); compacted[next++] = true;
            }
            List<Integer> blocks = new ArrayList<>();
            for (int old : file.getBlocks()) { if (old != next) moved++; blocks.add(next); compacted[next++] = true; }
            file.replaceBlocks(blocks);
        }
        System.arraycopy(compacted, 0, bitmap, 0, bitmap.length);
        return moved;
    }

    public int countFiles(FsNode node) {
        if (node == null) return 0;
        if (!node.isDirectory()) return 1;
        return node.getChildren().stream().mapToInt(this::countFiles).sum();
    }
    public int countDirectories(FsNode node) {
        if (node == null || !node.isDirectory()) return 0;
        return 1 + node.getChildren().stream().mapToInt(this::countDirectories).sum();
    }
    public String pathOf(FsNode node) {
        if (node == null) throw new IllegalArgumentException("Item inválido.");
        List<String> parts = new ArrayList<>();
        for (FsNode current = node; current != null && current != root; current = current.getParent()) parts.add(current.getName());
        Collections.reverse(parts);
        return parts.isEmpty() ? "/" : "/" + String.join("/", parts);
    }

    private void validateParentAndName(FsNode parent, String name) {
        if (parent == null || !parent.isDirectory()) throw new IllegalArgumentException("Selecione um diretório válido.");
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\")) throw new IllegalArgumentException("Informe um nome válido, sem barras.");
        String normalized = name.trim();
        if (parent.getChildren().stream().anyMatch(child -> child.getName().equalsIgnoreCase(normalized))) throw new IllegalArgumentException("Já existe um item com esse nome na pasta.");
    }
    private List<Integer> findFreeBlocks(int count) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < bitmap.length && result.size() < count; i++) if (!bitmap[i]) result.add(i);
        return result.size() == count ? result : null;
    }
    private List<Integer> findContiguousBlocks(int count) {
        if (count == 0) return new ArrayList<>();
        int run = 0;
        for (int i = 0; i < bitmap.length; i++) {
            run = bitmap[i] ? 0 : run + 1;
            if (run == count) { List<Integer> result = new ArrayList<>(); for (int b = i - count + 1; b <= i; b++) result.add(b); return result; }
        }
        return null;
    }
    private User findUser(String username) {
        if (username == null) return null;
        return users.stream().filter(user -> user.username().equalsIgnoreCase(username.trim())).findFirst().orElse(null);
    }
    private void grantDefaultPermissions(FsNode node, User user) {
        if (!node.isDirectory()) node.setPermissions(user, EnumSet.allOf(Permission.class));
        for (FsNode child : node.getChildren()) grantDefaultPermissions(child, user);
    }
    private void ensureDeletePermission(FsNode node) {
        if (!node.isDirectory() && !node.hasPermission(currentUser, Permission.DELETE)) throw new IllegalStateException("O usuário atual não possui permissão para excluir " + node.getName() + ".");
        for (FsNode child : node.getChildren()) ensureDeletePermission(child);
    }
    private void releaseRecursively(FsNode node) {
        for (FsNode child : new ArrayList<>(node.getChildren())) releaseRecursively(child);
        for (int block : node.getBlocks()) bitmap[block] = false;
        if (node.getIndexBlock() != null) bitmap[node.getIndexBlock()] = false;
    }

    private void releaseFileBlocks(FsNode file) {
        for (int block : file.getBlocks()) bitmap[block] = false;
        if (file.getIndexBlock() != null) bitmap[file.getIndexBlock()] = false;
        file.clearBlocks();
        file.setIndexBlock(null);
    }

    private void allocateBlocks(FsNode file, int sizeBytes, AllocationStrategy strategy) {
        long dataCountLong = sizeBytes == 0 ? 0 : ((long) sizeBytes + blockSize - 1) / blockSize;
        long requiredLong = dataCountLong + (strategy == AllocationStrategy.INDEXED ? 1 : 0);
        if (requiredLong > getFreeBlocks()) throw new IllegalStateException("Não há blocos livres suficientes.");
        int dataCount = (int) dataCountLong;
        int required = (int) requiredLong;
        List<Integer> allocated = strategy == AllocationStrategy.CONTIGUOUS
                ? findContiguousBlocks(dataCount)
                : findFreeBlocks(required);
        if (allocated == null) throw new IllegalStateException("Não existe uma sequência contígua de blocos livres suficiente.");
        if (strategy == AllocationStrategy.INDEXED) {
            file.setIndexBlock(allocated.remove(0));
            bitmap[file.getIndexBlock()] = true;
        }
        for (int block : allocated) { bitmap[block] = true; file.addBlock(block); }
    }
    private void collectFiles(FsNode node, List<FsNode> files) {
        if (!node.isDirectory()) { files.add(node); return; }
        for (FsNode child : node.getChildren()) collectFiles(child, files);
    }
}
