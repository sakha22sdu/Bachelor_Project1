package org.variantsync.diffdetective.experiments.views;

import org.variantsync.diffdetective.analysis.Analysis;
import org.variantsync.diffdetective.datasets.Repository;
import org.variantsync.diffdetective.variation.diff.parse.VariationDiffParseOptions;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Starts analysis of a repository.
 * See {@link org.variantsync.diffdetective.analysis.Analysis.MyAnalysis}
 */
public class Main {
    public static final VariationDiffParseOptions VARIATION_DIFF_PARSE_OPTIONS = new VariationDiffParseOptions(true,
            false);

    public static void main(String[] args) {
        Path repoPath = Paths.get("repositories/gimp");
        Path outputDir = Paths.get("output");

        System.out.println("Running MyAnalysis on gimp...");

        Repository repo = Repository.fromDirectory(repoPath, repoPath.getFileName().toString());

        Analysis.forEachCommit(() -> MyAnalysis.Create(repo, outputDir));
    }

}



