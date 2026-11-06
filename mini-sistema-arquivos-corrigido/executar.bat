@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ================================================
echo   Mini Sistema de Arquivos - inicializacao
echo ================================================
echo.
where java >nul 2>nul
if errorlevel 1 (
  echo ERRO: Java nao encontrado. Instale o JDK 17 ou superior.
  pause
  exit /b 1
)
where mvn >nul 2>nul
if errorlevel 1 (
  echo ERRO: Maven nao encontrado. Instale o Maven e adicione-o ao PATH.
  echo Alternativa: abra esta pasta no IntelliJ IDEA e execute MainApp.
  pause
  exit /b 1
)
echo Iniciando o JavaFX...
echo.
call mvn javafx:run
if errorlevel 1 (
  echo.
  echo A execucao falhou.
  echo Se houver outra instancia aberta, feche-a e tente novamente.
  echo Confira as mensagens acima para mais detalhes.
  pause
  exit /b 1
)
pause
