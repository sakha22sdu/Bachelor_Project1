package org.variantsync.diffdetective.experiments.views;

import org.tinylog.Logger;
import org.variantsync.diffdetective.analysis.Analysis;
import org.variantsync.diffdetective.datasets.Repository;
import org.variantsync.diffdetective.variation.diff.VariationDiff;
import org.variantsync.diffdetective.editclass.EditClass;
import org.variantsync.diffdetective.editclass.proposed.ProposedEditClasses;

import org.eclipse.jgit.revwalk.RevCommit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyAnalysis implements Analysis.Hooks {

    private static Path outputDirectory;
    private static final Set<String> writtenCommits = new HashSet<>();

    private BufferedWriter analysisWriter;
    private BufferedWriter messageWriter;
    
    @Override
    public void initializeResults(Analysis analysis) {
        try {
            Files.createDirectories(outputDirectory);

            // Clean old logs
            Path analysisFile = Paths.get(System.getProperty("user.dir"), "commit_analysis_log.txt");
            Files.deleteIfExists(analysisFile);
            Files.createFile(analysisFile);
            analysisWriter = new BufferedWriter(new FileWriter(analysisFile.toFile(), true));
            analysisWriter.write("==== Commit Analysis Log ====\n\n");

            Path messageFile = Paths.get(System.getProperty("user.dir"), "commit_messages.txt");
            Files.deleteIfExists(messageFile);
            Files.createFile(messageFile);
            messageWriter = new BufferedWriter(new FileWriter(messageFile.toFile(), true));
            messageWriter.write("==== Commit Messages ====\n\n");

            writtenCommits.clear();

            Logger.info("Initialized output files: {}, {}", analysisFile, messageFile);

        } catch (IOException e) {
            Logger.error("Failed to initialize log files: {}", e.getMessage());
        }
    }

    @Override
    public boolean beginCommit(Analysis analysis) {
        return true;
    }

    @Override
    public boolean analyzeVariationDiff(Analysis analysis) {
        try {
            RevCommit commit = analysis.getCurrentCommit();
            VariationDiff<?> diff = analysis.getCurrentVariationDiff();
            String commitId = commit.getId().getName();
            
            if (!writtenCommits.contains(commitId)) {
                writtenCommits.add(commitId);
                
                String message = commit.getFullMessage().trim();
                
                EditClass classification = classifyDiff(diff);
                boolean isBug = isBugRelated(message);

                // Write simple analysis line
                if (analysisWriter != null) {
                    analysisWriter.write(
                        "Commit: " + commitId +
                        " | Type: " + classification +
                        " | Bug-related: " + isBug +
                        " | Message: " + message.replace("\n", " ") +
                        "\n"
                    );
                    analysisWriter.flush();
                }

                // Store plain message
                if (messageWriter != null) {
                    messageWriter.write("Commit ID: " + commitId + "\n");
                    messageWriter.write(message + "\n\n");
                    messageWriter.flush();
                }
            }

        } catch (Exception e) {
            Logger.error("Error analyzing commit: {}", e.getMessage());
        }

        return true;
    }

    private EditClass classifyDiff(VariationDiff<?> diff) {
        EditClass editClass = null;

        try {
            for (org.variantsync.diffdetective.variation.diff.DiffNode<?> node :
                    diff.computeAllNodesThat(n -> n.isArtifact())) {

                editClass = ProposedEditClasses.Instance.match(node);
                if (editClass == null) continue;

                if (editClass.equals(ProposedEditClasses.Refactoring)
                        || editClass.equals(ProposedEditClasses.Reconfiguration)) {
                    return editClass;
                }
            }
        } catch (Exception e) {
            Logger.error("Error classifying diff: {}", e.getMessage());
        }

        return editClass;
    }

    private boolean isBugRelated(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("bug")
                || lower.contains("fix")
                || lower.contains("error")
                || lower.contains("issue")
                || lower.contains("defect");
    }

    @Override
    public void endBatch(Analysis analysis) throws Exception {
        Logger.info("Analysis complete — {} unique commits analyzed.", writtenCommits.size());
        if (analysisWriter != null) analysisWriter.close();
        if (messageWriter != null) messageWriter.close();
    }

    public static Analysis Create(Repository repo, Path outputDir) {
        outputDirectory = outputDir;
        return new Analysis(
                "MyAnalysis",
                List.of(new MyAnalysis()),
                repo,
                outputDir
        );
    }
}
