package com.minifs.controller;

import com.minifs.model.FsNode;
import com.minifs.model.AllocationStrategy;
import com.minifs.model.Permission;
import com.minifs.model.User;
import com.minifs.service.VirtualFileSystem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.EnumSet;
import java.util.List;

public class FileSystemController {
    private final VirtualFileSystem fs = new VirtualFileSystem(64, 1024);
    private final TreeView<FsNode> tree = new TreeView<>();
    private final GridPane diskGrid = new GridPane();
    private final Label status = new Label(">>> Sistema Operacional Virtual pronto. Aguardando comandos...");
    private final Label summary = new Label();
    private final TextArea details = new TextArea();
    private final ComboBox<AllocationStrategy> allocationStrategy = new ComboBox<>();
    private AllocationStrategy selectedAllocationStrategy = AllocationStrategy.LINKED;

    public Parent getMainView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #0d1117;");

        // Cabeçalho Tecnológico com o novo título solicitado
        VBox header = new VBox(4);
        Label title = new Label("MINI GERENCIADOR DE ARQUIVOS - GERENCIAMENTO DE ESPACO EM KERNEL");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        title.setStyle("-fx-text-fill: #00ff66;");
        
        Label subtitle = new Label("Simulador de Alocação de Blocos, Estruturas de Diretórios e Slack Space");
        subtitle.setFont(Font.font("Consolas", 12));
        subtitle.setStyle("-fx-text-fill: #8b949e;");
        header.getChildren().addAll(title, subtitle);
        root.setTop(header);

        HBox main = new HBox(14);
        main.setPadding(new Insets(14, 0, 10, 0));

        // Painel Esquerdo (Árvore e Controles)
        VBox left = new VBox(10);
        left.setPadding(new Insets(14));
        left.setPrefWidth(330);
        left.setStyle(panelStyle());
        
        Label treeTitle = new Label("ESTRUTURA DE DIRETÓRIOS");
        treeTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        treeTitle.setStyle("-fx-text-fill: #c9d1d9;");
        
        tree.setShowRoot(true);
        tree.setRoot(makeTree(fs.getRoot()));
        tree.getRoot().setExpanded(true);
        tree.setStyle("-fx-background-color: #161b22; -fx-control-inner-background: #161b22; -fx-text-fill: #c9d1d9;");
        VBox.setVgrow(tree, Priority.ALWAYS);

        Button user = createTechButton("Definir Usuário Atual");
        Button folder = createTechButton("+ Nova Pasta");
        Button file = createTechButton("+ Novo Arquivo");
        Button permissions = createTechButton("Atribuir Permissões");
        Button delete = createTechButton("Excluir Selecionado");
        Button defragment = createTechButton("Desfragmentar Blocos");

        allocationStrategy.getItems().addAll(AllocationStrategy.values());
        allocationStrategy.setValue(AllocationStrategy.LINKED);
        allocationStrategy.setMaxWidth(Double.MAX_VALUE);
        allocationStrategy.setStyle("-fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-font-family: 'Consolas';");
        
        allocationStrategy.setOnAction(e -> {
            changeAllocationStrategy();
            updateDefragmentationVisibility(defragment);
        });

        user.setOnAction(e -> setCurrentUser());
        folder.setOnAction(e -> createDirectory());
        file.setOnAction(e -> createFile());
        delete.setOnAction(e -> deleteSelected());
        defragment.setOnAction(e -> defragment());
        permissions.setOnAction(e -> assignPermissions());

        Label stratLabel = new Label("ESTRATÉGIA DE ALOCAÇÃO:");
        stratLabel.setFont(Font.font("Consolas", 11));
        stratLabel.setStyle("-fx-text-fill: #8b949e;");

        left.getChildren().addAll(treeTitle, tree, stratLabel, allocationStrategy, user, folder, file, permissions, delete, defragment);

        // Painel Direito (Grid de Blocos do Disco e Logs/Detalhes)
        VBox right = new VBox(12);
        right.setPadding(new Insets(14));
        right.setStyle(panelStyle());
        HBox.setHgrow(right, Priority.ALWAYS);

        Label diskTitle = new Label("MAPA DO DISCO VIRTUAL [64 Blocos x 1 KiB]");
        diskTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        diskTitle.setStyle("-fx-text-fill: #00ff66;");

        summary.setStyle("-fx-text-fill: #c9d1d9; -fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        
        diskGrid.setHgap(8);
        diskGrid.setVgap(8);
        diskGrid.setPadding(new Insets(6, 0, 6, 0));

        Label legend = new Label("■ [Livre: Cinza Escuro]   |   ■ [Dados Úteis: Verde Terminal]   |   ■ [Slack Space: Laranja Alerta]");
        legend.setStyle("-fx-text-fill: #8b949e; -fx-font-family: 'Consolas'; -fx-font-size: 11px;");

        Label detailTitle = new Label("LOGS & METADADOS DO ITEM SELECIONADO");
        detailTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        detailTitle.setStyle("-fx-text-fill: #c9d1d9;");

        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(5);
        details.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #00ff66; -fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        details.setText(">>> Selecione um nó na árvore lateral para inspecionar os detalhes.");
        VBox.setVgrow(details, Priority.ALWAYS);

        tree.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            FsNode selected = item == null ? null : item.getValue();
            showDetails(selected);
            refreshDisk(selected);
        });

        right.getChildren().addAll(diskTitle, summary, diskGrid, legend, new Separator(), detailTitle, details);
        main.getChildren().addAll(left, right);
        root.setCenter(main);

        status.setPadding(new Insets(6, 0, 0, 0));
        status.setStyle("-fx-text-fill: #00ff66; -fx-font-family: 'Consolas'; -fx-font-size: 11px;");
        root.setBottom(status);

        refreshDisk();
        return root;
    }

    private void updateDefragmentationVisibility(Button defragment) {
        boolean available = allocationStrategy.getValue() != AllocationStrategy.INDEXED;
        defragment.setVisible(available);
        defragment.setManaged(available);
    }

    private String panelStyle() {
        return "-fx-background-color: #161b22; -fx-background-radius: 8; -fx-border-color: #30363d; -fx-border-radius: 8;";
    }

    private Button createTechButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        btn.setTextFill(Color.web("#c9d1d9"));
        btn.setStyle("-fx-background-color: #21262d; -fx-background-radius: 5; -fx-cursor: hand; -fx-border-color: #30363d; -fx-border-radius: 5;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #30363d; -fx-text-fill: #00ff66; -fx-background-radius: 5; -fx-border-color: #00ff66; -fx-border-radius: 5;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #21262d; -fx-text-fill: #c9d1d9; -fx-background-radius: 5; -fx-border-color: #30363d; -fx-border-radius: 5;"));
        return btn;
    }

    private TreeItem<FsNode> makeTree(FsNode node) {
        TreeItem<FsNode> item = new TreeItem<>(node);
        for (FsNode child : node.getChildren()) item.getChildren().add(makeTree(child));
        return item;
    }

    private FsNode selectedDirectory() {
        TreeItem<FsNode> item = tree.getSelectionModel().getSelectedItem();
        if (item == null) return fs.getRoot();
        FsNode n = item.getValue();
        if (n.isDirectory()) return n;
        if (n.getParent() != null) return n.getParent();
        return fs.getRoot();
    }

    private void createDirectory() {
        TextInputDialog d = new TextInputDialog("nova-pasta");
        d.setTitle("Criar Pasta");
        d.setHeaderText("Novo Diretório Virtual");
        d.setContentText("Nome:");
        d.showAndWait().ifPresent(name -> {
            try {
                FsNode parent = selectedDirectory();
                fs.createDirectory(parent, name);
                refreshTree(parent);
                status.setText(">>> Diretório criado: " + fs.pathOf(parent) + "/" + name);
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });
    }

    private void createFile() {
        Dialog<FileInput> d = new Dialog<>();
        d.setTitle("Criar Arquivo");
        d.setHeaderText("Alocar Arquivo (" + allocationStrategy.getValue() + ")");
        ButtonType create = new ButtonType("Alocar", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);

        TextField name = new TextField("documento.txt");
        TextField size = new TextField("1500");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Nome:"), name);
        form.addRow(1, new Label("Tamanho (bytes):"), size);
        d.getDialogPane().setContent(form);

        d.setResultConverter(btn -> {
            if (btn == create) {
                try {
                    return new FileInput(name.getText(), Integer.parseInt(size.getText()));
                } catch (Exception ex) {
                    error("Informe um tamanho inteiro válido em bytes.");
                }
            }
            return null;
        });
        d.showAndWait().ifPresent(input -> {
            try {
                FsNode parent = selectedDirectory();
                fs.createFile(parent, input.name, input.size, allocationStrategy.getValue());
                refreshTree(parent);
                refreshDisk();
                status.setText(">>> Arquivo alocado com sucesso: " + input.name);
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });
    }

    private void changeAllocationStrategy() {
        AllocationStrategy newValue = allocationStrategy.getValue();
        if (newValue == null || newValue == selectedAllocationStrategy) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "A alteração da estratégia reiniciará o disco virtual. Continuar?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Alerta de Kernel");
        confirm.setHeaderText("Reinicialização do Sistema de Arquivos");
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            allocationStrategy.setValue(selectedAllocationStrategy);
            return;
        }
        selectedAllocationStrategy = newValue;
        fs.reset();
        tree.setRoot(makeTree(fs.getRoot()));
        tree.getRoot().setExpanded(true);
        details.setText(">>> Sistema reiniciado. Selecione um item.");
        refreshDisk();
        status.setText(">>> Estratégia alterada para: " + newValue);
    }

    private void deleteSelected() {
        TreeItem<FsNode> item = tree.getSelectionModel().getSelectedItem();
        if (item == null || item.getValue() == fs.getRoot()) {
            error("Selecione um arquivo ou pasta válida para exclusão.");
            return;
        }
        FsNode n = item.getValue();
        String name = n.getName();
        FsNode parent = n.getParent();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remover '" + name + "' e liberar blocos?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmação de Exclusão");
        confirm.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            try {
                fs.delete(n);
                refreshTree(parent);
                refreshDisk();
                status.setText(">>> Removido: " + name + ". Blocos liberados no bitmap.");
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });
    }

    private void defragment() {
        FsNode selected = tree.getSelectionModel().getSelectedItem() == null ? null : tree.getSelectionModel().getSelectedItem().getValue();
        int movedBlocks = fs.defragment();
        refreshDisk(selected);
        showDetails(selected);
        status.setText(movedBlocks == 0 ? ">>> O disco já está totalmente desfragmentado." : ">>> Desfragmentação concluída: " + movedBlocks + " bloco(s) realocado(s).");
    }

    private void refreshTree(FsNode select) {
        TreeItem<FsNode> root = makeTree(fs.getRoot());
        root.setExpanded(true);
        expandAll(root);
        tree.setRoot(root);
        TreeItem<FsNode> target = findItem(root, select);
        if (target != null) tree.getSelectionModel().select(target);
        refreshDisk();
    }

    private void expandAll(TreeItem<FsNode> item) {
        item.setExpanded(true);
        for (TreeItem<FsNode> child : item.getChildren()) expandAll(child);
    }

    private TreeItem<FsNode> findItem(TreeItem<FsNode> item, FsNode node) {
        if (item.getValue() == node) return item;
        for (TreeItem<FsNode> c : item.getChildren()) {
            TreeItem<FsNode> found = findItem(c, node);
            if (found != null) return found;
        }
        return null;
    }

    private void refreshDisk() {
        refreshDisk(null);
    }

    // --- RENDERIZAÇÃO DARK TECH COM NOME DE BLOCOS ATUALIZADO PARA "BLOCO" ---
    private void refreshDisk(FsNode selected) {
        diskGrid.getChildren().clear();
        int blockSize = fs.getBlockSize();
        int columns = 8;

        for (int i = 0; i < fs.getBlockCount(); i++) {
            boolean used = fs.isUsed(i);
            FileBlockInfo info = used ? getBlockInfo(fs.getRoot(), i, blockSize) : null;

            StackPane cell = new StackPane();
            double w = 110;
            double h = 80;
            cell.setPrefSize(w, h);
            cell.setAlignment(Pos.CENTER_LEFT);

            if (used) {
                int usedSpace = info != null ? info.usedSpace : blockSize;
                int slackSpace = info != null ? info.slackSpace : 0;
                String ownerName = info != null ? info.owner : "Alocado";

                // Fundo do bloco representa o Slack Space (Laranja escuro/alerta tecnológico)
                cell.setStyle("-fx-background-color: #2d1a0e; -fx-border-color: #f97316; -fx-border-radius: 5; -fx-background-radius: 5;");

                // Barra proporcional verde brilhante para o espaço útil preenchido
                double ratio = (blockSize > 0) ? (double) usedSpace / blockSize : 1.0;
                Region fill = new Region();
                fill.setPrefSize(w * ratio, h);
                fill.setMaxSize(w * ratio, h);
                fill.setStyle("-fx-background-color: #065f46; -fx-background-radius: 4 0 0 4;");
                cell.getChildren().add(fill);

                // Overlay de texto no bloco
                VBox text = new VBox(2);
                text.setAlignment(Pos.CENTER);
                text.setPadding(new Insets(4));

                boolean isSelectedBlock = selected != null && !selected.isDirectory() && selected.getBlocks().contains(i);
                String idColor = isSelectedBlock ? "#34d399" : "#ffffff";

                Label lblId = new Label(String.format("BLOCO #%02d", i));
                lblId.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
                lblId.setTextFill(Color.web(idColor));

                Label lblOwner = new Label(ownerName);
                lblOwner.setFont(Font.font("Consolas", 10));
                lblOwner.setTextFill(Color.web("#e2e8f0"));

                Label lblSlack = new Label(slackSpace > 0 ? "Slack: " + slackSpace + "B" : "Full");
                lblSlack.setFont(Font.font("Consolas", FontWeight.BOLD, 9));
                lblSlack.setTextFill(Color.web("#fb923c"));

                text.getChildren().addAll(lblId, lblOwner, lblSlack);
                cell.getChildren().add(text);
            } else {
                // Bloco livre (Cinza escuro tecnológico)
                cell.setStyle("-fx-background-color: #21262d; -fx-border-color: #30363d; -fx-border-radius: 5; -fx-background-radius: 5;");

                VBox text = new VBox(2);
                text.setAlignment(Pos.CENTER);

                Label lblId = new Label(String.format("BLOCO #%02d", i));
                lblId.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
                lblId.setTextFill(Color.web("#8b949e"));

                Label lblFree = new Label("LIVRE");
                lblFree.setFont(Font.font("Consolas", 10));
                lblFree.setTextFill(Color.web("#484f58"));

                text.getChildren().addAll(lblId, lblFree);
                cell.getChildren().add(text);
            }

            diskGrid.add(cell, i % columns, i / columns);
        }

        summary.setText(String.format("USER: %s  |  OCUPADOS: %d/%d  |  LIVRES: %d  |  ARQUIVOS: %d  |  PASTAS: %d",
                fs.getCurrentUser().username().toUpperCase(),
                fs.getUsedBlocks(), fs.getBlockCount(),
                fs.getFreeBlocks(),
                fs.countFiles(fs.getRoot()),
                fs.countDirectories(fs.getRoot()) - 1));
    }

    private FileBlockInfo getBlockInfo(FsNode node, int blockIndex, int blockSize) {
        if (node == null) return null;
        if (!node.isDirectory()) {
            List<Integer> blocks = node.getBlocks();
            if (blocks.contains(blockIndex)) {
                int lastBlock = blocks.get(blocks.size() - 1);
                int slack = 0;
                int used = blockSize;
                if (blockIndex == lastBlock) {
                    int remainder = node.getSizeBytes() % blockSize;
                    slack = (remainder == 0) ? 0 : (blockSize - remainder);
                    used = blockSize - slack;
                }
                return new FileBlockInfo(node.getName(), used, slack);
            }
            if (node.getIndexBlock() != null && node.getIndexBlock() == blockIndex) {
                return new FileBlockInfo(node.getName() + " [Index]", blockSize, 0);
            }
        } else {
            for (FsNode child : node.getChildren()) {
                FileBlockInfo info = getBlockInfo(child, blockIndex, blockSize);
                if (info != null) return info;
            }
        }
        return null;
    }

    private record FileBlockInfo(String owner, int usedSpace, int slackSpace) {}

    private void setCurrentUser() {
        TextInputDialog dialog = new TextInputDialog(fs.getCurrentUser().username());
        dialog.setTitle("Identidade");
        dialog.setHeaderText("Alterar Usuário Ativo do Kernel");
        dialog.setContentText("Username:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                fs.setCurrentUser(name);
                refreshDisk();
                status.setText(">>> Usuário ativo alterado para: " + fs.getCurrentUser().username());
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });
    }

    private void assignPermissions() {
        TreeItem<FsNode> item = tree.getSelectionModel().getSelectedItem();
        if (item == null || item.getValue().isDirectory()) {
            error("Selecione um arquivo válido para gerenciar permissões.");
            return;
        }
        FsNode file = item.getValue();
        if (!fs.getCurrentUser().equals(file.getOwner())) {
            error("Acesso negado: Apenas o criador (" + file.getOwner().username() + ") pode alterar permissões.");
            return;
        }
        Dialog<PermissionInput> dialog = new Dialog<>();
        dialog.setTitle("Controle de Acessos");
        dialog.setHeaderText("Permissões para o arquivo: " + file.getName());
        ButtonType save = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        ComboBox<User> users = new ComboBox<>();
        users.getItems().addAll(fs.getUsers());
        users.setValue(fs.getCurrentUser());
        
        CheckBox read = new CheckBox("Leitura (r)");
        CheckBox write = new CheckBox("Escrita (w)");
        CheckBox execute = new CheckBox("Execução (x)");
        CheckBox delete = new CheckBox("Excluir (d)");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Usuário:"), users);
        form.addRow(1, read);
        form.addRow(2, write);
        form.addRow(3, execute);
        form.addRow(4, delete);
        dialog.getDialogPane().setContent(form);

        Runnable load = () -> {
            User selected = users.getValue();
            java.util.Set<Permission> granted = selected == null ? java.util.Set.of() : file.getPermissions().getOrDefault(selected.username(), EnumSet.noneOf(Permission.class));
            read.setSelected(granted.contains(Permission.READ));
            write.setSelected(granted.contains(Permission.WRITE));
            execute.setSelected(granted.contains(Permission.EXECUTE));
            delete.setSelected(granted.contains(Permission.DELETE));
        };
        users.valueProperty().addListener((obs, old, value) -> load.run());
        load.run();

        dialog.setResultConverter(button -> {
            if (button == save) {
                EnumSet<Permission> granted = EnumSet.noneOf(Permission.class);
                if (read.isSelected()) granted.add(Permission.READ);
                if (write.isSelected()) granted.add(Permission.WRITE);
                if (execute.isSelected()) granted.add(Permission.EXECUTE);
                if (delete.isSelected()) granted.add(Permission.DELETE);
                return new PermissionInput(users.getValue(), granted);
            }
            return null;
        });
        dialog.showAndWait().ifPresent(input -> {
            try {
                fs.assignPermissions(file, input.user, input.permissions);
                showDetails(file);
                status.setText(">>> Permissões atualizadas para o usuário: " + input.user.username());
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });
    }

    private void showDetails(FsNode n) {
        if (n == null) return;
        StringBuilder s = new StringBuilder();
        s.append("[NOME]: ").append(n.getName()).append('\n')
         .append("[TIPO]: ").append(n.isDirectory() ? "Diretório" : "Arquivo").append('\n')
         .append("[PATH]: ").append(fs.pathOf(n)).append('\n');
        
        if (!n.isDirectory()) {
            s.append("[OWNER]: ").append(n.getOwner().username()).append('\n')
             .append("[ESTRATÉGIA]: ").append(n.getAllocationStrategy()).append('\n')
             .append("[PERMISSÕES]: ").append(formatPermissions(n)).append('\n')
             .append("[TAMANHO]: ").append(n.getSizeBytes()).append(" bytes\n")
             .append("[BLOCOS ALOCADOS]: ").append(n.getBlocks());
            if (n.getIndexBlock() != null) s.append("\n[BLOCO ÍNDICE]: ").append(n.getIndexBlock());
        } else {
            s.append("[ITENS NA PASTA]: ").append(n.getChildren().size());
        }
        details.setText(s.toString());
    }

    private String formatPermissions(FsNode file) {
        StringBuilder result = new StringBuilder();
        file.getPermissions().forEach((user, granted) -> {
            if (result.length() > 0) result.append(", ");
            result.append(user).append(" [");
            for (Permission permission : Permission.values()) {
                result.append(granted.contains(permission) ? permission.getSymbol() : "-");
            }
            result.append("]");
        });
        return result.toString();
    }

    private void error(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erro no Kernel");
        a.setHeaderText("Falha na Operação");
        a.setContentText(message == null ? "Erro desconhecido." : message);
        a.showAndWait();
    }

    private record FileInput(String name, int size) {}
    private record PermissionInput(User user, java.util.Set<Permission> permissions) {}
}

/*package com.minifs.controller;

import com.minifs.model.FsNode;
import com.minifs.model.AllocationStrategy;
import com.minifs.model.Permission;
import com.minifs.model.User;
import com.minifs.service.VirtualFileSystem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import java.util.HashMap;
import java.util.EnumSet;
import java.util.Map;

public class FileSystemController {
    private final VirtualFileSystem fs = new VirtualFileSystem(64, 1024);
    private final TreeView<FsNode> tree = new TreeView<>();
    private final GridPane diskGrid = new GridPane();
    private final Label status = new Label("Pronto. O disco virtual começa vazio.");
    private final Label summary = new Label();
    private final TextArea details = new TextArea();
    private final Map<Integer, Label> blockCells = new HashMap<>();
    private final ComboBox<AllocationStrategy> allocationStrategy = new ComboBox<>();
    private AllocationStrategy selectedAllocationStrategy = AllocationStrategy.LINKED;

    public Parent getMainView() {
        BorderPane root = new BorderPane(); root.setPadding(new Insets(18)); root.setStyle("-fx-background-color: #f4f6fa;");
        VBox header = new VBox(5); Label title=new Label("Mini Sistema de Arquivos"); title.setFont(Font.font("System",26)); title.setStyle("-fx-font-weight: bold; -fx-text-fill: #172554;");
        Label subtitle=new Label("Simulação de diretórios, alocação de arquivos e bitmap de blocos"); subtitle.setStyle("-fx-text-fill: #526079;"); header.getChildren().addAll(title,subtitle); root.setTop(header);
        HBox main=new HBox(16); main.setPadding(new Insets(18,0,12,0));
        VBox left=new VBox(10); left.setPadding(new Insets(14)); left.setPrefWidth(330); left.setStyle(panelStyle());
        Label treeTitle=new Label("Estrutura de diretórios"); treeTitle.setFont(Font.font(16)); tree.setShowRoot(true); tree.setRoot(makeTree(fs.getRoot())); tree.getRoot().setExpanded(true); VBox.setVgrow(tree,Priority.ALWAYS);
        Button user=new Button("Definir usuário atual"); Button folder=new Button("+ Nova pasta"); Button file=new Button("+ Novo arquivo"); Button permissions=new Button("Atribuir permissões"); Button delete=new Button("Excluir selecionado"); Button defragment=new Button("Desfragmentar blocos");
        allocationStrategy.getItems().addAll(AllocationStrategy.values()); allocationStrategy.setValue(AllocationStrategy.LINKED); allocationStrategy.setMaxWidth(Double.MAX_VALUE);
        allocationStrategy.setOnAction(e -> {
            changeAllocationStrategy();
            updateDefragmentationVisibility(defragment);
        });
        user.setMaxWidth(Double.MAX_VALUE); folder.setMaxWidth(Double.MAX_VALUE); file.setMaxWidth(Double.MAX_VALUE); permissions.setMaxWidth(Double.MAX_VALUE); delete.setMaxWidth(Double.MAX_VALUE); defragment.setMaxWidth(Double.MAX_VALUE);
        user.setOnAction(e->setCurrentUser());
        folder.setOnAction(e->createDirectory()); file.setOnAction(e->createFile()); delete.setOnAction(e->deleteSelected()); defragment.setOnAction(e->defragment());
        permissions.setOnAction(e->assignPermissions());
        left.getChildren().addAll(treeTitle,tree,new Label("Estratégia para novos arquivos:"),allocationStrategy,user,folder,file,permissions,delete,defragment);
        VBox right=new VBox(14); right.setPadding(new Insets(14)); right.setStyle(panelStyle()); HBox.setHgrow(right,Priority.ALWAYS);
        Label diskTitle=new Label("Disco virtual — 64 blocos de 1 KiB"); diskTitle.setFont(Font.font(16));
        summary.setStyle("-fx-text-fill: #334155; -fx-font-size: 13px;");
        diskGrid.setHgap(7); diskGrid.setVgap(7); diskGrid.setPadding(new Insets(8,0,8,0));
        for(int i=0;i<fs.getBlockCount();i++){Label cell=new Label(String.format("%02d",i));cell.setAlignment(Pos.CENTER);cell.setMinSize(43,34);cell.setMaxSize(43,34);cell.setStyle(freeStyle());blockCells.put(i,cell);diskGrid.add(cell,i%8,i/8);}
        Label legend=new Label("Livre: cinza   |   Ocupado: azul   |   Arquivo selecionado: verde"); legend.setStyle("-fx-text-fill: #64748b;");
        Label detailTitle=new Label("Detalhes do item selecionado"); detailTitle.setFont(Font.font(15)); details.setEditable(false); details.setWrapText(true); details.setPrefRowCount(5); details.setText("Selecione um item na árvore para ver seu caminho e os blocos usados.");
        VBox.setVgrow(details,Priority.ALWAYS); tree.getSelectionModel().selectedItemProperty().addListener((obs,old,item)->{FsNode selected=item==null?null:item.getValue();showDetails(selected);refreshDisk(selected);});
        right.getChildren().addAll(diskTitle,summary,diskGrid,legend,new Separator(),detailTitle,details);
        main.getChildren().addAll(left,right); root.setCenter(main); status.setPadding(new Insets(8,0,0,0)); status.setStyle("-fx-text-fill: #475569;"); root.setBottom(status);
        refreshDisk(); return root;
    }
    private void updateDefragmentationVisibility(Button defragment) {
        boolean available = allocationStrategy.getValue() != AllocationStrategy.INDEXED;
        defragment.setVisible(available);
        defragment.setManaged(available);
    }
    private String panelStyle(){return "-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12;";}
    private String freeStyle(){return "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-background-radius: 5; -fx-font-size: 11px;";}
    private String usedStyle(){return "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-size: 11px; -fx-font-weight: bold;";}
    private String selectedFileStyle(){return "-fx-background-color: #16a34a; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-size: 11px; -fx-font-weight: bold;";}
    private TreeItem<FsNode> makeTree(FsNode node){TreeItem<FsNode> item=new TreeItem<>(node);for(FsNode child:node.getChildren())item.getChildren().add(makeTree(child));return item;}
    private FsNode selectedDirectory(){TreeItem<FsNode> item=tree.getSelectionModel().getSelectedItem();if(item==null)return fs.getRoot();FsNode n=item.getValue();if(n.isDirectory())return n;if(n.getParent()!=null)return n.getParent();return fs.getRoot();}
    private void createDirectory(){TextInputDialog d=new TextInputDialog("nova-pasta");d.setTitle("Criar pasta");d.setHeaderText("Nova pasta");d.setContentText("Nome:");d.showAndWait().ifPresent(name->{try{FsNode parent=selectedDirectory();fs.createDirectory(parent,name);refreshTree(parent);status.setText("Pasta criada: "+fs.pathOf(parent)+"/"+name);}catch(Exception ex){error(ex.getMessage());}});}
    private void createFile(){Dialog<FileInput> d=new Dialog<>();d.setTitle("Criar arquivo");d.setHeaderText("Alocar arquivo no disco virtual ("+allocationStrategy.getValue()+")");ButtonType create=new ButtonType("Criar",ButtonBar.ButtonData.OK_DONE);d.getDialogPane().getButtonTypes().addAll(create,ButtonType.CANCEL);
        TextField name=new TextField("documento.txt");TextField size=new TextField("1500");GridPane form=new GridPane();form.setHgap(10);form.setVgap(10);form.addRow(0,new Label("Nome:"),name);form.addRow(1,new Label("Tamanho em bytes:"),size);d.getDialogPane().setContent(form);
        d.setResultConverter(btn->{if(btn==create){try{return new FileInput(name.getText(),Integer.parseInt(size.getText()));}catch(Exception ex){error("Informe um tamanho inteiro válido.");}}return null;});d.showAndWait().ifPresent(input->{try{FsNode parent=selectedDirectory();fs.createFile(parent,input.name,input.size,allocationStrategy.getValue());refreshTree(parent);refreshDisk();status.setText("Arquivo criado com alocação "+allocationStrategy.getValue().toString().toLowerCase()+": "+input.name);}catch(Exception ex){error(ex.getMessage());}});}
    private void changeAllocationStrategy() {
        AllocationStrategy newValue = allocationStrategy.getValue();
        if (newValue == null || newValue == selectedAllocationStrategy) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Trocar a estratégia apagará todas as pastas, arquivos e usuários. Deseja continuar?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Trocar estratégia de alocação");
        confirm.setHeaderText("O disco virtual será reiniciado");
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            allocationStrategy.setValue(selectedAllocationStrategy);
            return;
        }
        selectedAllocationStrategy = newValue;
        fs.reset();
        tree.setRoot(makeTree(fs.getRoot()));
        tree.getRoot().setExpanded(true);
        details.setText("Selecione um item na árvore para ver seu caminho e os blocos usados.");
        refreshDisk();
        status.setText("Estratégia alterada para "+newValue+"; disco, arquivos e usuários reiniciados.");
    }
    private void deleteSelected(){TreeItem<FsNode> item=tree.getSelectionModel().getSelectedItem();if(item==null||item.getValue()==fs.getRoot()){error("Selecione um arquivo ou pasta para excluir.");return;}FsNode n=item.getValue();String name=n.getName();FsNode parent=n.getParent();Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Excluir '"+name+"' e liberar seus blocos?",ButtonType.YES,ButtonType.NO);confirm.setHeaderText("Confirmar exclusão");confirm.showAndWait().filter(b->b==ButtonType.YES).ifPresent(b->{try{fs.delete(n);refreshTree(parent);refreshDisk();status.setText("Item excluído: "+name+". Blocos liberados.");}catch(Exception ex){error(ex.getMessage());}});}
    private void defragment(){FsNode selected=tree.getSelectionModel().getSelectedItem()==null?null:tree.getSelectionModel().getSelectedItem().getValue();int movedBlocks=fs.defragment();refreshDisk(selected);showDetails(selected);status.setText(movedBlocks==0?"O disco já estava desfragmentado.":"Desfragmentação concluída: "+movedBlocks+" bloco(s) reposicionado(s).");}
    private void refreshTree(FsNode select){TreeItem<FsNode> root=makeTree(fs.getRoot());root.setExpanded(true);expandAll(root);tree.setRoot(root);TreeItem<FsNode> target=findItem(root,select);if(target!=null)tree.getSelectionModel().select(target);refreshDisk();}
    private void expandAll(TreeItem<FsNode> item){item.setExpanded(true);for(TreeItem<FsNode> child:item.getChildren())expandAll(child);}
    private TreeItem<FsNode> findItem(TreeItem<FsNode> item,FsNode node){if(item.getValue()==node)return item;for(TreeItem<FsNode> c:item.getChildren()){TreeItem<FsNode> found=findItem(c,node);if(found!=null)return found;}return null;}
    private void refreshDisk(){refreshDisk(null);}
    private void refreshDisk(FsNode selected){for(int i=0;i<fs.getBlockCount();i++){boolean selectedBlock=selected!=null&&!selected.isDirectory()&&selected.getBlocks().contains(i);blockCells.get(i).setStyle(selectedBlock?selectedFileStyle():(fs.isUsed(i)?usedStyle():freeStyle()));}summary.setText("Usuário: "+fs.getCurrentUser().username()+"    |    Blocos ocupados: "+fs.getUsedBlocks()+" / "+fs.getBlockCount()+"    |    Livres: "+fs.getFreeBlocks()+"    |    Arquivos: "+fs.countFiles(fs.getRoot())+"    |    Diretórios: "+(fs.countDirectories(fs.getRoot())-1));}
    private void setCurrentUser(){TextInputDialog dialog=new TextInputDialog(fs.getCurrentUser().username());dialog.setTitle("Usuário atual");dialog.setHeaderText("Informe somente o nome do usuário");dialog.setContentText("Usuário:");dialog.showAndWait().ifPresent(name->{try{fs.setCurrentUser(name);refreshDisk();status.setText("Usuário atual: "+fs.getCurrentUser().username());}catch(Exception ex){error(ex.getMessage());}});}
    private void assignPermissions(){TreeItem<FsNode> item=tree.getSelectionModel().getSelectedItem();if(item==null||item.getValue().isDirectory()){error("Selecione um arquivo para atribuir permissões.");return;}FsNode file=item.getValue();if(!fs.getCurrentUser().equals(file.getOwner())){error("Somente o criador do arquivo ("+file.getOwner().username()+") pode alterar suas permissões.");return;}Dialog<PermissionInput> dialog=new Dialog<>();dialog.setTitle("Permissões do arquivo");dialog.setHeaderText("Arquivo: "+file.getName());ButtonType save=new ButtonType("Salvar",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(save,ButtonType.CANCEL);
        ComboBox<User> users=new ComboBox<>();users.getItems().addAll(fs.getUsers());users.setValue(fs.getCurrentUser());CheckBox read=new CheckBox("Leitura (r)");CheckBox write=new CheckBox("Escrita (w)");CheckBox execute=new CheckBox("Execução (x)");CheckBox delete=new CheckBox("Excluir (d)");GridPane form=new GridPane();form.setHgap(10);form.setVgap(8);form.addRow(0,new Label("Usuário:"),users);form.addRow(1,read);form.addRow(2,write);form.addRow(3,execute);form.addRow(4,delete);dialog.getDialogPane().setContent(form);
        Runnable load=()->{User selected=users.getValue();java.util.Set<Permission> granted=selected==null?java.util.Set.of():file.getPermissions().getOrDefault(selected.username(),EnumSet.noneOf(Permission.class));read.setSelected(granted.contains(Permission.READ));write.setSelected(granted.contains(Permission.WRITE));execute.setSelected(granted.contains(Permission.EXECUTE));delete.setSelected(granted.contains(Permission.DELETE));};users.valueProperty().addListener((obs,old,value)->load.run());load.run();
        dialog.setResultConverter(button->{if(button==save){EnumSet<Permission> granted=EnumSet.noneOf(Permission.class);if(read.isSelected())granted.add(Permission.READ);if(write.isSelected())granted.add(Permission.WRITE);if(execute.isSelected())granted.add(Permission.EXECUTE);if(delete.isSelected())granted.add(Permission.DELETE);return new PermissionInput(users.getValue(),granted);}return null;});dialog.showAndWait().ifPresent(input->{try{fs.assignPermissions(file,input.user,input.permissions);showDetails(file);status.setText("Permissões atualizadas para "+input.user.username()+".");}catch(Exception ex){error(ex.getMessage());}});}

    private void showDetails(FsNode n){
        if(n==null)return;
        StringBuilder s=new StringBuilder();
        s.append("Nome: ").append(n.getName()).append('\n').append("Tipo: ").append(n.isDirectory()?"Diretório":"Arquivo").append('\n').append("Caminho: ").append(fs.pathOf(n)).append('\n');
        if(!n.isDirectory()){
            if(n.getAllocationStrategy()==AllocationStrategy.CONTIGUOUS){
                s.append("User criador: ").append(n.getOwner().username()).append('\n').append("Alocação: ")
                .append(n.getAllocationStrategy()).append('\n').append("Permissões: ").append(formatPermissions(n))
                .append('\n').append("Tamanho: ").append(n.getSizeBytes()).append(" bytes\n")
                .append("Blocos de dados: ").append(n.getBlocks().size()).append("\nBlocos alocados: [").append(n.getBlocks().get(0) + ",").append(n.getBlocks().size()+"]");
            }
            else{
                s.append("User criador: ").append(n.getOwner().username()).append('\n').append("Alocação: ")
                .append(n.getAllocationStrategy()).append('\n').append("Permissões: ").append(formatPermissions(n))
                .append('\n').append("Tamanho: ").append(n.getSizeBytes()).append(" bytes\n")
                .append("Blocos de dados: ").append(n.getBlocks().size()).append("\nBlocos alocados: ").append(n.getBlocks());
            }
            if(n.getIndexBlock()!=null)s.append("\nBloco índice: ").append(n.getIndexBlock());
        }
        else{s.append("Itens nesta pasta: ").append(n.getChildren().size());}details.setText(s.toString());
    }


    
    private String formatPermissions(FsNode file){StringBuilder result=new StringBuilder();file.getPermissions().forEach((user,granted)->{if(result.length()>0)result.append(", ");result.append(user).append(" (");for(Permission permission:Permission.values())result.append(granted.contains(permission)?permission.getSymbol():"-");result.append(")");});return result.toString();}
    private void error(String message){Alert a=new Alert(Alert.AlertType.ERROR);a.setTitle("Não foi possível concluir");a.setHeaderText("Operação não realizada");a.setContentText(message==null?"Erro inesperado.":message);a.showAndWait();}
    private record FileInput(String name,int size){}
    private record PermissionInput(User user,java.util.Set<Permission> permissions){}
}*/
