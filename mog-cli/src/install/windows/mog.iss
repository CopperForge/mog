#define MyAppName "@APP_NAME@"
#define MyAppVersion "@APP_VERSION@"
#define MyDefaultInstallDir "@DEFAULT_INSTALL_DIR@"
#define MyOutputBaseFilename "@OUTPUT_BASE_FILENAME@"

[Setup]
AppId={{0E9F3A63-7D5A-4C12-A1B7-8D8C2B57A7A4}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
DefaultDirName={code:GetInstallDir}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
LicenseFile=..\stage\docs\LICENSE
OutputDir=..\..\distributions
OutputBaseFilename={#MyOutputBaseFilename}
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
WizardStyle=modern
PrivilegesRequired=admin
UninstallDisplayIcon={app}\bin\mog.cmd

[Files]
Source: "..\stage\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}\MOG CLI"; Filename: "{app}\bin\mog.cmd"; WorkingDir: "{app}"

[Run]
Filename: "{app}\bin\mog.cmd"; Description: "Run MOG CLI"; Flags: postinstall nowait skipifsilent

[Code]
function GetInstallDir(Default: string): string;
var
  MogHome: string;
begin
  MogHome := Trim(GetEnv('MOG_HOME'));
  if MogHome <> '' then
    Result := RemoveBackslashUnlessRoot(MogHome)
  else
    Result := ExpandConstant('{#MyDefaultInstallDir}');
end;
