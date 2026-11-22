\documentclass[12pt]{article}

% 120cm x 90cm
\usepackage[
    paperwidth=90cm,
    paperheight=120cm,
    left=4cm,
    right=4cm,
    top=2cm,
    bottom=2.5cm
]{geometry}

\usepackage[T1]{fontenc}
\usepackage[utf8]{inputenc}
\usepackage[brazilian]{babel}

\usepackage{tgheros}
\renewcommand{\familydefault}{\sfdefault}

\usepackage{graphicx}
\usepackage{xcolor}
\usepackage{tikz}
\usepackage{array}

\usetikzlibrary{calc}

\pagestyle{empty}

\setlength{\parindent}{0pt}
\setlength{\parskip}{0pt}

\definecolor{azulclaro}{HTML}{B9E0E3}
\definecolor{azulescuro}{HTML}{123568}
\definecolor{corpotexto}{HTML}{2B3036}
\newcommand{\FonteCorpo}{\fontsize{40}{49}\selectfont\color{corpotexto}}
\newcommand{\FonteLegenda}{\fontsize{30}{37}\selectfont\color{corpotexto}}
\newcommand{\FonteReferencia}{\fontsize{28}{35}\selectfont\color{corpotexto}}
\newcommand{\FonteTituloFaixa}{\fontsize{50}{58}\selectfont\bfseries\color{azulescuro}}


% Faixa sempre usa exatamente a largura do bloco em que estiver inserida
\newcommand{\Faixa}[1]{%
  \begin{tikzpicture}
    \node[
        fill=azulclaro,
        rounded corners=12pt,
        minimum width=\linewidth,
        minimum height=4.2cm,
        inner sep=0pt,
        anchor=center
    ] {\FonteTituloFaixa #1};
  \end{tikzpicture}%
}

% Bloco de texto alinhado à esquerda, com recuo visual consistente
\newenvironment{TextoSecao}{%
  \begin{minipage}{0.97\linewidth}
  \raggedright
  \FonteCorpo
}{%
  \end{minipage}
}

% Figura provisória. Para inserir uma imagem real, trocar a caixa por
% \includegraphics[width=\linewidth,height=13.5cm,keepaspectratio]{arquivo.png}
\newcommand{\FiguraPoster}[2]{%
    \begin{minipage}[t]{0.305\linewidth}
    \centering
  
    {\FonteLegenda #1}
    \par
    \vspace{0.5cm}
    
    \begin{tikzpicture}
      \node[
        draw=gray!60,
        fill=azulclaro,
        minimum width=\linewidth,
        minimum height=13.5cm,
        inner sep=0pt
    ] {};
    \end{tikzpicture}
    \par
    \vspace{0.35cm}
    
    {\FonteLegenda #2}
  \end{minipage}%
}


% Informações institucionais com a mesma fonte e espaçamento uniforme.
\newcommand{\FonteInstituicao}{\fontsize{37}{45}\selectfont\bfseries}
\newcommand{\EspacoInstituicao}{\vspace{0.55cm}}

\newcommand{\Cabecalho}{%
  \begin{tikzpicture}
    \node[fill=azulclaro,rounded corners=18pt,
      minimum width=\linewidth,minimum height=7.2cm,inner sep=0pt] (barra) {};
    
    \node[align=center,text=azulescuro,
      font=\bfseries\fontsize{52}{60}\selectfont]
      at (barra.center) {SEMINÁRIO\\APRENDIZADO DE MÁQUINA};

  \end{tikzpicture}\par
  \vspace{1.0cm}
  
  {\fontsize{44}{52}\selectfont\bfseries Sistemas de Recomendação}\par
  \vspace{0.35cm}
  
  {\fontsize{50}{58}\selectfont\bfseries Bookmendation}\par
  \vspace{0.9cm}
  
  {\FonteInstituicao Acadêmicos: Fábio Felsky, Lemuel Manske, Thiago H. Russi}\par
  \EspacoInstituicao
  
  {\FonteInstituicao Departamento de Sistemas e Computação, Curso de Ciência da Computação}\par
  \EspacoInstituicao
  
  {\FonteInstituicao Universidade Regional de Blumenau (FURB) -- Blumenau -- SC -- Brasil}\par
}


\begin{document}

\centering

\Cabecalho
\vspace{1.0cm}


\Faixa{INTRODUÇÃO}
\par
\vspace{0.65cm}

\begin{TextoSecao}
Deve iniciar com uma contextualização do estudo realizado, explicando claramente sua origem e motivação, bem como a formulação do problema (pergunta de pesquisa) que foi investigado pelo autor.
\par
\vspace{0.55cm}

Deve-se também apresentar as características deste trabalho, incluindo principais funcionalidades e aplicações, pontos fortes e fracos, bem como os resultados descritos pelo autor, entre outros elementos que permitam compreender o trabalho.
\end{TextoSecao}
\par
\vspace{3.0cm}


\Faixa{OBJETIVOS}
\par
\vspace{0.65cm}

\begin{TextoSecao}
Deve-se colocar os objetivos principais do trabalho.
\end{TextoSecao}
\par
\vspace{3.0cm}


\begin{minipage}[t]{0.487\linewidth}
    \centering
    \Faixa{METODOLOGIA}
    \par
    \vspace{0.75cm}

    \begin{TextoSecao}
        \textbullet\ Metodologia
        \par
        \vspace{0.5cm}
        
        Descreva as etapas, os dados utilizados e as técnicas aplicadas.
    \end{TextoSecao}
\end{minipage}
\hfill
\begin{minipage}[t]{0.487\linewidth}\centering
    \Faixa{RESULTADOS}
    \par
    \vspace{0.75cm}
  
    \begin{TextoSecao}
        \textbullet\ Resultados
        \par
        \vspace{0.5cm}
        
        Apresente os principais achados, indicadores e análises.
    \end{TextoSecao}
\end{minipage}\par
\vspace{13cm}


\FiguraPoster{Figura 1. XXXXXX}{Fonte: Autor (2026).}
\hfill
\FiguraPoster{Figura 2. XXXXXX}{Fonte: Autor (2026).}
\hfill
\FiguraPoster{Figura 3. XXXXXX}{Fonte: Autor (2026).}
\par
\vspace{2.0cm}


\Faixa{CONCLUSÕES}
\par
\vspace{0.65cm}

\begin{TextoSecao}
O projeto... Acrescente aqui uma síntese dos resultados, contribuições e trabalhos futuros.
\end{TextoSecao}
\par
\vspace{2.0cm}


\Faixa{REFERÊNCIAS}
\par
\vspace{0.6cm}

\begin{minipage}{0.97\linewidth}
\raggedright
\FonteReferencia
SOBRENOME, Nome. \textbf{Título da obra}. Cidade: Editora, 2026.
\par
\vspace{0.2cm}

SOBRENOME, Nome. Título do artigo. \textbf{Nome da Revista}, v. 1, n. 1, p. 1--10, 2026.
\par
\vspace{0.2cm}

AUTOR, Nome. \textbf{Título do trabalho}. Disponível em: endereço eletrônico. Acesso em: 29 out. 2026.
\end{minipage}
\par
\vfill

\begin{flushright}
    \includegraphics[
        width=12cm,
        keepaspectratio
    ]{logo_furb.jpg}
\end{flushright}

\end{document}
