# DiffDetective Variability Analysis – Bachelor Project

This project extends DiffDetective with a custom analysis (`MyAnalysis`) to study the evolution of variability and presence conditions in configurable software systems. The analysis operates at commit level and is followed by a lightweight topic analysis of commit messages.

---

## Prerequisites

- Java 17 (or compatible with DiffDetective)
- Maven
- Python 3
- Git
- Linux / macOS (tested under WSL Ubuntu)

---

## Project Structure

- `src/main/java/org/variantsync/diffdetective/experiments/views/Main.java`  
  Entry point of the Java-based analysis.

- `repositories/`  
  Contains cloned subject systems (e.g., `xterm`, `libssh`).

- `commit_analysis_log.txt`  
  Generated output file containing per-commit variability classifications.

- `commit_messages.txt`  
  Generated output file containing raw commit messages.

- `topic_analysis.py` (or equivalent Python script)  
  Performs topic analysis on the generated commit messages.

---

## Running the Analysis

### 1. Compile the Project

From the project root directory, run:

```bash
mvn clean compile

mvn exec:java -Dexec.mainClass="org.variantsync diffdetective.experiments.views.Main"

python3 src/java/main/org/variantsync/diffdetective/experiments/views/topic_analysis.py