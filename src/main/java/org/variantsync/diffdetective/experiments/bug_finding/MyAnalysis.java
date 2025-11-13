package org.variantsync.diffdetective.experiments.bug_finding;

import org.tinylog.Logger;
import org.variantsync.diffdetective.analysis.Analysis;
import org.variantsync.diffdetective.datasets.Repository;
import org.variantsync.diffdetective.editclass.EditClass;
import org.variantsync.diffdetective.editclass.proposed.ProposedEditClasses;
import org.variantsync.diffdetective.variation.diff.VariationDiff;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.List;

public class MyAnalysis implements Analysis.Hooks {

    private int commits = 0;
    private BufferedWriter writer;
    private BufferedWriter messageWriter; // new writer for messages
    private static Path outputDirectory;

    @Override
    public void initializeResults(Analysis analysis) {
        try {
            // create output directory if missing
            Files.createDirectories(outputDirectory);

            // main commit log
            Path logFile = outputDirectory.resolve("commit_log.txt");
            writer = new BufferedWriter(new FileWriter(logFile.toFile(), false));
            writer.write("Commit ID\tClassification\tBug-related\tMessage\n");
            writer.write("------------------------------------------------------------\n");
            writer.flush();

            // create commit_messages.txt in repository root
            Path messageFile = Paths.get(System.getProperty("user.dir"), "commit_messages.txt");
            Files.deleteIfExists(messageFile);
            Files.createFile(messageFile);
            messageWriter = new BufferedWriter(new FileWriter(messageFile.toFile(), true));
            messageWriter.write("==== Commit Messages ====\n\n");
            messageWriter.flush();

            Logger.info("Created log files: {} and {}", logFile, messageFile);
        } catch (IOException e) {
            Logger.error("Failed to initialize commit log file: {}", e.getMessage());
        }
    }

    @Override
    public boolean beginCommit(Analysis analysis) throws Exception {
        ++commits;
        return true;
    }

    @Override
    public boolean analyzeVariationDiff(Analysis analysis) {
        VariationDiff<?> diff = analysis.getCurrentVariationDiff();
        RevCommit commit = analysis.getCurrentCommit();
        String message = commit.getFullMessage();

        if (classifyDiff(diff)) {
            try {
                // diff.anyMatch(node -> {
                // if (node.isArtifact()) {
                // EditClass editClass = ProposedEditClasses.Instance.match(diff);
                // if (editClass.equals(ProposedEditClasses.Refactoring) ||
                // editClass.equals(ProposedEditClasses.Reconfiguration)
                // ) {
                // return editClass;
                // }
                // }
                // };

                // EditClass classification = diff.anyMatch( n -> {
                // if (n.isArtifact()) ProposedEditClasses.Instance.match(n);
                // return null;
                // }
                // );
                EditClass classification = null;
                // String classification = classifyDiff(diff);
                boolean isBug = isBugRelated(message);

                // Log to console
                Logger.info("Commit: {} | Type: {} | Bug-related: {} | Message: {}",
                        commit.getId().getName(),
                        classification,
                        isBug,
                        message.replace("\n", " "));

                // write to main log
                if (writer != null) {
                    writer.write(commit.getId().getName() + "\t" +
                            classification + "\t" +
                            isBug + "\t" +
                            message.replace("\n", " ") + "\n");
                    writer.flush();
                }

                // write commit message to commit_messages.txt
                if (messageWriter != null) {
                    messageWriter.write("Commit ID: " + commit.getId().getName() + "\n");
                    messageWriter.write(message.trim() + "\n\n");
                    messageWriter.flush();
                }

            } catch (Exception e) {
                Logger.error("Error analyzing commit: {}", e.getMessage());
            }
        }
        return true;
    }

    private boolean classifyDiff(VariationDiff<?> diff) {
        // try {
        // boolean hasArtifact = diff.computeAllNodesThat(node ->
        // node.isArtifact()).size() > 0;
        // boolean hasChanges = diff.computeAllNodesThat(node -> node.isAdd() ||
        // node.isRem()).size() > 0;

        // if (hasArtifact && hasChanges) {
        // return "Reconfiguration";
        // } else if (hasChanges) {
        // return "Refactoring";
        // } else {
        // return "Unchanged";
        // }
        // } catch (Exception e) {
        // return "Unknown";
        // }

        diff.anyMatch(
                node -> {
                    if (node.isArtifact()) {
                        EditClass editClass2 = ProposedEditClasses.Instance.match(node);
                        if (editClass2.equals(ProposedEditClasses.Refactoring) ||
                                editClass2.equals(ProposedEditClasses.Reconfiguration)) {
                            return true;
                        }
                    }
                    return false;
                });
        // editClass = ProposedEditClasses.Instance.match(diff.get);
        // return editClass;
        return false;
    }

    private boolean isBugRelated(String message) {
        if (message == null)
            return false;
        String lower = message.toLowerCase();
        return lower.contains("bug") ||
                lower.contains("fix") ||
                lower.contains("error") ||
                lower.contains("issue") ||
                lower.contains("defect");
    }

    @Override
    public void endBatch(Analysis analysis) throws Exception {
        Logger.info("Batch done: {} commits analyzed", commits);
        if (writer != null)
            writer.close();
        if (messageWriter != null)
            messageWriter.close();
    }

    public static Analysis Create(Repository repo, Path outputDir) {
        outputDirectory = outputDir;
        return new Analysis(
                "MyAnalysis",
                List.of(new MyAnalysis()),
                repo,
                outputDir);
    }
}
