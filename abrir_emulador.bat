@echo off
echo ===================================================
echo A preparar o Emulador Android Vira...
echo ===================================================
:: Limpar ficheiros .lock residuais se existirem
if exist "%USERPROFILE%\.android\avd\Medium_Phone.avd\hardware-qemu.ini.lock" (
    rd /s /q "%USERPROFILE%\.android\avd\Medium_Phone.avd\hardware-qemu.ini.lock" 2>nul
)
if exist "%USERPROFILE%\.android\avd\Medium_Phone.avd\multiinstance.lock" (
    del /f /q "%USERPROFILE%\.android\avd\Medium_Phone.avd\multiinstance.lock" 2>nul
)

echo A iniciar o Emulador Medium_Phone_API_36.0...
start "" "%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_36.0 -gpu host
echo Emulador iniciado com sucesso com janela visivel!

