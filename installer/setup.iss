; ─────────────────────────────────────────────────────────────────────────
;  IAE - Integrated Assignment Environment — Inno Setup 6 kurulum betigi
;
;  Kullanim:
;    1. fat JAR'i uretin:   cd IAE-Project && mvn clean package
;    2. dist\ klasorunu hazirlayin:
;         - IAE-Project\target\IAE-Project-1.0-SNAPSHOT.jar  -> dist\IAE.jar
;         - bir JRE 21 (javafx dahil) -> dist\runtime\   (opsiyonel)
;         - calistirici  dist\IAE.exe  (jar'i baslatan launcher)
;    3. Inno Setup ile bu betigi derleyin -> installer\Output\IAESetup.exe
; ─────────────────────────────────────────────────────────────────────────

[Setup]
AppName=IAE - Integrated Assignment Environment
AppVersion=1.0.0
AppPublisher=CE316 Group Project
DefaultDirName={autopf}\IAE
DefaultGroupName=IAE
OutputBaseFilename=IAESetup
Compression=lzma2
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64compatible
DisableProgramGroupPage=yes
; SetupIconFile=..\IAE-Project\src\main\resources\iae.ico

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional icons:"

[Files]
; dist\ klasoru altindaki her seyi (jar, runtime, exe) {app}'e kopyalar
Source: "dist\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[Icons]
Name: "{group}\IAE"; Filename: "{app}\IAE.exe"
Name: "{group}\Uninstall IAE"; Filename: "{uninstallexe}"
Name: "{autodesktop}\IAE"; Filename: "{app}\IAE.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\IAE.exe"; Description: "Launch IAE"; Flags: nowait postinstall skipifsilent
