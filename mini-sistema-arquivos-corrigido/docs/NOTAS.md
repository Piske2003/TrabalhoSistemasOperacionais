# Notas técnicas

- **Disco virtual:** vetor booleano representa o bitmap; `false` indica bloco livre e `true` indica ocupado.
- **Tamanho do bloco:** 1024 bytes.
- **Cálculo de blocos:** `ceil(tamanhoArquivo / tamanhoBloco)`, com no mínimo um bloco por arquivo.
- **Diretórios:** árvore de nós; diretórios não consomem blocos neste modelo.
- **Exclusão:** percorre filhos, libera os blocos dos arquivos e remove o nó do diretório pai.
- **Desfragmentação:** percorre os arquivos em ordem na árvore e compacta seus blocos a partir do bloco zero, preservando a quantidade de blocos de cada arquivo.
- **Persistência:** não implementada; reiniciar a aplicação recria o disco vazio.
- **Usuários e permissões:** cada usuário possui somente um nome. Arquivos registram o proprietário e permissões independentes de leitura (`r`), escrita (`w`), execução (`x`) e exclusão (`d`) para cada usuário. Usuários novos recebem todas as permissões nos arquivos existentes; a exclusão exige a permissão `d`.
