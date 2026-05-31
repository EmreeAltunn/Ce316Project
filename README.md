# IAE - Integrated Assignment Environment

Version 1.0.0  
CE316 Group Project

## About

IAE is a lightweight desktop application for managing programming assignments.
It lets a lecturer define language configurations, create projects, process
student submissions delivered as ZIP files, compile and run each submission,
compare program output against expected output, and view a per-student results
report.

## Features

- Define language configurations with compile and run commands.
- Create assignment projects with submission and working directories.
- Process student submissions submitted as ZIP files.
- Compile and run each student submission.
- Compare actual program output with expected output files.
- View per-student compile, run, and test results.
- Export results to CSV.
- Access the user manual from `Help > Manual`.

## System Requirements

- Windows 10 or Windows 11, 64-bit.
- Approximately 250 MB of free disk space.
- No separate Java or JavaFX installation is required for the Windows installer,
  because a Java runtime is bundled with the application.
- To compile and run student code, the relevant compiler or interpreter must be
  available on the system `PATH`, such as `javac`/`java` for Java, `gcc` for C,
  or `python` for Python. IAE does not install these tools.

## Installation

1. Double-click `IAESetup.exe`.
2. Follow the setup wizard.
3. On the **Select Additional Tasks** step, keep **Create a desktop shortcut**
   checked if you want a desktop shortcut.
4. Click **Install**, then **Finish**. The application can also be launched
   directly from the final setup screen.

By default, IAE is installed under:

```text
C:\Program Files\IAE
```

You may choose a different installation folder during setup.

## Running the Application

Start IAE using one of these options:

- Use the `IAE` desktop shortcut.
- Run `IAE.exe` from the installation folder.
- Find `IAE` in the Windows Start menu.

On first launch, the Welcome screen provides three main actions:

- **New Project**
- **Open Project**
- **Manage Configurations**

## Quick Start

1. Open **Configuration > Manage Configurations > New**.
2. Define how a programming language is compiled and run, then save the
   configuration.
3. Open **File > New Project**, or click **New Project** on the Welcome screen.
4. Enter a project name and choose a configuration.
5. Select the **Submissions Directory**, which contains the student ZIP files.
6. Select the **Working Directory**, where ZIP files will be extracted.
7. Add one or more test cases. Each test case may include input arguments and an
   expected-output file for comparison.
8. Click **Save Project**, then **Run Assignment**.
9. When the run finishes, click **View Results** to see the per-student report.
10. Use **Export CSV...** if you need to save the results outside the
    application.

The full user manual is available inside the application from:

```text
Help > Manual
```

## Data Location

Projects, configurations, and results are stored in a local SQLite database under
the user's home directory:

```text
C:\Users\<you>\.iae
```

No server or internet connection is required. The application is fully
self-contained.

## Uninstalling

Uninstall IAE using **Add or remove programs** in Windows Settings, or run
`unins000.exe` from the installation folder.

The `.iae` data folder is not removed automatically. Delete it manually if you
also want to remove saved projects, configurations, and results.

## Building From Source

Developer requirement:

- JDK 21

Build the runnable fat JAR:

```powershell
cd IAE-Project
.\mvnw clean package
```

The build produces:

```text
target\IAE-Project-1.0-SNAPSHOT.jar
```

To produce a self-contained application image with a bundled runtime and
`IAE.exe`, use `jpackage`:

```powershell
jpackage -t app-image -n IAE -i <folder-with-the-jar> `
  --main-jar IAE-Project-1.0-SNAPSHOT.jar `
  --main-class com.iae.Launcher -d dist-image
```

Then copy the contents of `dist-image\IAE\` into `installer\dist\` and compile
`installer\setup.iss` with Inno Setup 6 to produce:

```text
installer\Output\IAESetup.exe
```

## License

Copyright (c) 2026 CE316 Group Project.

This project is for educational use.
