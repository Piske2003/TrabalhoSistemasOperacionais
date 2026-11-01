package com.minifs.controller;

import com.minifs.model.FsNode;
import com.minifs.service.VirtualFileSystem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.HashMap;
import java.util.Map;

public class FileSystemController {
    private final VirtualFileSystem fs = new VirtualFileSystem(64, 1024);
    private final TreeView<FsNode> tree = new TreeView<>();
    private final GridPane diskGrid = new GridPane();
    private final Label status = new Label("Pronto. O disco virtual começa vazio.");
    private final Label summary = new Label();
    private final TextArea details = new TextArea();
    private final Map<Integer, Label> blockCells = new HashMap<>();

    public Parent getMainView() {
        BorderPane root = new BorderPane(); root.setPadding(new Insets(18)); root.setStyle("-fx-background-color: #f4f6fa;");
        VBox header = new VBox(5); Label title=new Label("Mini Sistema de Arquivos"); title.setFont(Font.font("System",26)); title.setStyle("-fx-font-weight: bold; -fx-text-fill: #172554;");
        Label subtitle=new Label("Simulação de diretórios, alocação de arquivos e bitmap de blocos"); subtitle.setStyle("-fx-text-fill: #526079;"); header.getChildren().addAll(title,subtitle); root.setTop(header);
        HBox main=new HBox(16); main.setPadding(new Insets(18,0,12,0));
        VBox left=new VBox(10); left.setPadding(new Insets(14)); left.setPrefWidth(330); left.setStyle(panelStyle());
        Label treeTitle=new Label("Estrutura de diretórios"); treeTitle.setFont(Font.font(16)); tree.setShowRoot(true); tree.setRoot(makeTree(fs.getRoot())); tree.getRoot().setExpanded(true); VBox.setVgrow(tree,Priority.ALWAYS);
        Button folder=new Button("+ Nova pasta"); Button file=new Button("+ Novo arquivo"); Button delete=new Button("Excluir selecionado");
        folder.setMaxWidth(Double.MAX_VALUE); file.setMaxWidth(Double.MAX_VALUE); delete.setMaxWidth(Double.MAX_VALUE);
        folder.setOnAction(e->createDirectory()); file.setOnAction(e->createFile()); delete.setOnAction(e->deleteSelected());
        left.getChildren().addAll(treeTitle,tree,folder,file,delete);
        VBox right=new VBox(14); right.setPadding(new Insets(14)); right.setStyle(panelStyle()); HBox.setHgrow(right,Priority.ALWAYS);
        Label diskTitle=new Label("Disco virtual — 64 blocos de 1 KiB"); diskTitle.setFont(Font.font(16));
        summary.setStyle("-fx-text-fill: #334155; -fx-font-size: 13px;");
        diskGrid.setHgap(7); diskGrid.setVgap(7); diskGrid.setPadding(new Insets(8,0,8,0));
        for(int i=0;i<fs.getBlockCount();i++){Label cell=new Label(String.format("%02d",i));cell.setAlignment(Pos.CENTER);cell.setMinSize(43,34);cell.setMaxSize(43,34);cell.setStyle(freeStyle());blockCells.put(i,cell);diskGrid.add(cell,i%8,i/8);}
        Label legend=new Label("Livre: cinza   |   Ocupado: azul"); legend.setStyle("-fx-text-fill: #64748b;");
        Label detailTitle=new Label("Detalhes do item selecionado"); detailTitle.setFont(Font.font(15)); details.setEditable(false); details.setWrapText(true); details.setPrefRowCount(5); details.setText("Selecione um item na árvore para ver seu caminho e os blocos usados.");
        VBox.setVgrow(details,Priority.ALWAYS); tree.getSelectionModel().selectedItemProperty().addListener((obs,old,item)->showDetails(item==null?null:item.getValue()));
        right.getChildren().addAll(diskTitle,summary,diskGrid,legend,new Separator(),detailTitle,details);
        main.getChildren().addAll(left,right); root.setCenter(main); status.setPadding(new Insets(8,0,0,0)); status.setStyle("-fx-text-fill: #475569;"); root.setBottom(status);
        refreshDisk(); return root;
    }
    private String panelStyle(){return "-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12;";}
    private String freeStyle(){return "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-background-radius: 5; -fx-font-size: 11px;";}
    private String usedStyle(){return "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 5; -fx-font-size: 11px; -fx-font-weight: bold;";}
    private TreeItem<FsNode> makeTree(FsNode node){TreeItem<FsNode> item=new TreeItem<>(node);for(FsNode child:node.getChildren())item.getChildren().add(makeTree(child));return item;}
    private FsNode selectedDirectory(){TreeItem<FsNode> item=tree.getSelectionModel().getSelectedItem();if(item==null)return fs.getRoot();FsNode n=item.getValue();if(n.isDirectory())return n;if(n.getParent()!=null)return n.getParent();return fs.getRoot();}
    private void createDirectory(){TextInputDialog d=new TextInputDialog("nova-pasta");d.setTitle("Criar pasta");d.setHeaderText("Nova pasta");d.setContentText("Nome:");d.showAndWait().ifPresent(name->{try{FsNode parent=selectedDirectory();fs.createDirectory(parent,name);refreshTree(parent);status.setText("Pasta criada: "+fs.pathOf(parent)+"/"+name);}catch(Exception ex){error(ex.getMessage());}});}
    private void createFile(){Dialog<FileInput> d=new Dialog<>();d.setTitle("Criar arquivo");d.setHeaderText("Alocar arquivo no disco virtual");ButtonType create=new ButtonType("Criar",ButtonBar.ButtonData.OK_DONE);d.getDialogPane().getButtonTypes().addAll(create,ButtonType.CANCEL);
        TextField name=new TextField("documento.txt");TextField size=new TextField("1500");GridPane form=new GridPane();form.setHgap(10);form.setVgap(10);form.addRow(0,new Label("Nome:"),name);form.addRow(1,new Label("Tamanho em bytes:"),size);d.getDialogPane().setContent(form);
        d.setResultConverter(btn->{if(btn==create){try{return new FileInput(name.getText(),Integer.parseInt(size.getText()));}catch(Exception ex){error("Informe um tamanho inteiro válido.");}}return null;});d.showAndWait().ifPresent(input->{try{FsNode parent=selectedDirectory();fs.createFile(parent,input.name,input.size);refreshTree(parent);refreshDisk();status.setText("Arquivo criado e blocos alocados: "+input.name);}catch(Exception ex){error(ex.getMessage());}});}
    private void deleteSelected(){TreeItem<FsNode> item=tree.getSelectionModel().getSelectedItem();if(item==null||item.getValue()==fs.getRoot()){error("Selecione um arquivo ou pasta para excluir.");return;}FsNode n=item.getValue();String name=n.getName();FsNode parent=n.getParent();Alert confirm=new Alert(Alert.AlertType.CONFIRMATION,"Excluir '"+name+"' e liberar seus blocos?",ButtonType.YES,ButtonType.NO);confirm.setHeaderText("Confirmar exclusão");confirm.showAndWait().filter(b->b==ButtonType.YES).ifPresent(b->{fs.delete(n);refreshTree(parent);refreshDisk();status.setText("Item excluído: "+name+". Blocos liberados.");});}
    private void refreshTree(FsNode select){TreeItem<FsNode> root=makeTree(fs.getRoot());root.setExpanded(true);expandAll(root);tree.setRoot(root);TreeItem<FsNode> target=findItem(root,select);if(target!=null)tree.getSelectionModel().select(target);refreshDisk();}
    private void expandAll(TreeItem<FsNode> item){item.setExpanded(true);for(TreeItem<FsNode> child:item.getChildren())expandAll(child);}
    private TreeItem<FsNode> findItem(TreeItem<FsNode> item,FsNode node){if(item.getValue()==node)return item;for(TreeItem<FsNode> c:item.getChildren()){TreeItem<FsNode> found=findItem(c,node);if(found!=null)return found;}return null;}
    private void refreshDisk(){for(int i=0;i<fs.getBlockCount();i++)blockCells.get(i).setStyle(fs.isUsed(i)?usedStyle():freeStyle());summary.setText("Blocos ocupados: "+fs.getUsedBlocks()+" / "+fs.getBlockCount()+"    |    Livres: "+fs.getFreeBlocks()+"    |    Arquivos: "+fs.countFiles(fs.getRoot())+"    |    Diretórios: "+(fs.countDirectories(fs.getRoot())-1));}
    private void showDetails(FsNode n){if(n==null)return;StringBuilder s=new StringBuilder();s.append("Nome: ").append(n.getName()).append('\n').append("Tipo: ").append(n.isDirectory()?"Diretório":"Arquivo").append('\n').append("Caminho: ").append(fs.pathOf(n)).append('\n');if(!n.isDirectory()){s.append("Tamanho: ").append(n.getSizeBytes()).append(" bytes\n").append("Blocos necessários: ").append(n.getBlocks().size()).append("\nBlocos alocados: ").append(n.getBlocks());}else{s.append("Itens nesta pasta: ").append(n.getChildren().size());}details.setText(s.toString());}
    private void error(String message){Alert a=new Alert(Alert.AlertType.ERROR);a.setTitle("Não foi possível concluir");a.setHeaderText("Operação não realizada");a.setContentText(message==null?"Erro inesperado.":message);a.showAndWait();}
    private record FileInput(String name,int size){}
}
