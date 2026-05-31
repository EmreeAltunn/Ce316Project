===============================================================================
 IAE - Integrated Assignment Environment
 Version 1.0.0
 CE316 Group Project
===============================================================================

ABOUT
-------------------------------------------------------------------------------
IAE is a lightweight desktop application for managing programming assignments.
It lets a lecturer define language configurations (compile / run steps), create
projects, process student submissions delivered as ZIP files, compile and run
each submission, compare the program output against the expected output, and
view a per-student results report.


SYSTEM REQUIREMENTS
-------------------------------------------------------------------------------
- Windows 10 / 11 (64-bit)
- Approx. 250 MB free disk space
- A Java Runtime is BUNDLED with the application, so you do NOT need to install
  Java or JavaFX separately.
- To actually compile/run student code, the relevant compiler/interpreter must
  be available on the system PATH (e.g. "javac"/"java" for Java, "gcc" for C,
  "python" for Python). The IAE itself does not install these.


INSTALLATION
-------------------------------------------------------------------------------
1. Double-click "IAESetup.exe".
2. Follow the setup wizard.
3. On the "Select Additional Tasks" step, keep "Create a desktop shortcut"
   checked if you want a shortcut on your desktop.
4. Click Install, then Finish. You may launch the application directly from the
   last screen of the wizard.

By default the application is installed under:
   C:\Program Files\IAE        (or the folder you choose during setup)


RUNNING THE APPLICATION
-------------------------------------------------------------------------------
- Use the "IAE" shortcut created on the Desktop, OR
- Run "IAE.exe" from the installation folder, OR
- Find "IAE" in the Windows Start menu.

On first launch a Welcome screen appears with three actions:
   New Project  /  Open Project  /  Manage Configurations


QUICK START
-------------------------------------------------------------------------------
1. Configuration > Manage Configurations > New
   Define how a language is compiled and run (e.g. Java, C, Python), then Save.
2. File > New Project (or "New Project" on the Welcome screen)
   - Enter a project name and pick a configuration.
   - Submissions Directory: the folder that contains the student ZIP files.
   - Working Directory: an (empty) folder where the ZIPs will be extracted.
3. Add one or more Test Cases (optional input arguments and an expected-output
   file for comparison).
4. Save Project, then click "Run Assignment".
5. When the run finishes, click "View Results" to see the per-student report.
   Results can be exported with "Export CSV...".

A full user manual is available inside the application from:
   Help > Manual


DATA LOCATION
-------------------------------------------------------------------------------
All projects, configurations and results are stored in a local SQLite database
under your user home directory:
   C:\Users\<you>\.iae

No server or internet connection is required; the application is fully
self-contained.


UNINSTALLING
-------------------------------------------------------------------------------
Use "Add or remove programs" in Windows Settings, or run "unins000.exe" from the
installation folder. (Your data folder ".iae" is not removed automatically; you
may delete it manually if desired.)


BUILDING FROM SOURCE (for developers)
-------------------------------------------------------------------------------
Requirements: JDK 21.

   cd IAE-Project
   .\mvnw clean package
   -> target\IAE-Project-1.0-SNAPSHOT.jar  (runnable fat JAR)

To produce the self-contained app image (bundled runtime + IAE.exe):

   jpackage -t app-image -n IAE -i <folder-with-the-jar> ^
            --main-jar IAE-Project-1.0-SNAPSHOT.jar ^
            --main-class com.iae.Launcher -d dist-image

Then copy the contents of dist-image\IAE\ into installer\dist\ and compile
installer\setup.iss with Inno Setup 6 to produce installer\Output\IAESetup.exe.


===============================================================================
 (c) 2026 CE316 Group Project. For educational use.
===============================================================================
