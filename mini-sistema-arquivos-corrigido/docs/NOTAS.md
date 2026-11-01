# Notas técnicas

- **Disco virtual:** vetor booleano representa o bitmap; `false` indica bloco livre e `true` indica ocupado.
- **Tamanho do bloco:** 1024 bytes.
- **Cálculo de blocos:** `ceil(tamanhoArquivo / tamanhoBloco)`, com no mínimo um bloco por arquivo.
- **Diretórios:** árvore de nós; diretórios não consomem blocos neste modelo.
- **Exclusão:** percorre filhos, libera os blocos dos arquivos e remove o nó do diretório pai.
- **Persistência:** não implementada; reiniciar a aplicação recria o disco vazio.
