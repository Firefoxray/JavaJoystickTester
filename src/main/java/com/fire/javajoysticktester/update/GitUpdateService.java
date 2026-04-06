package com.fire.javajoysticktester.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handles git-based update check and update execution.
 */
public class GitUpdateService {
    private static final String MAIN_CLASS = "com.fire.javajoysticktester.Main";

    public enum UpdateStatus {
        ALREADY_UP_TO_DATE,
        UPDATE_AVAILABLE,
        WORKING_TREE_DIRTY,
        UNABLE_TO_CHECK
    }

    public record CheckResult(
            UpdateStatus status,
            String branch,
            String shortCommit,
            String message
    ) {
    }

    public record UpdateResult(boolean success, String message) {
    }

    public CheckResult checkForUpdates() {
        Path repoRoot;
        try {
            repoRoot = resolveRepoRoot();
        } catch (IOException ex) {
            return new CheckResult(UpdateStatus.UNABLE_TO_CHECK, "unknown", "unknown", ex.getMessage());
        }

        try {
            String branch = readNonEmpty(runGit(repoRoot, "symbolic-ref", "--short", "-q", "HEAD"), "detached");
            String shortCommit = readNonEmpty(runGit(repoRoot, "rev-parse", "--short", "HEAD"), "unknown");

            if ("detached".equals(branch)) {
                return new CheckResult(
                        UpdateStatus.UNABLE_TO_CHECK,
                        branch,
                        shortCommit,
                        "Detached HEAD state: checkout a branch before using in-app updates."
                );
            }

            String dirty = runGit(repoRoot, "status", "--porcelain").stdout().trim();
            if (!dirty.isEmpty()) {
                return new CheckResult(
                        UpdateStatus.WORKING_TREE_DIRTY,
                        branch,
                        shortCommit,
                        "Local changes detected. Commit or stash changes before updating."
                );
            }

            CommandResult upstream = runGitAllowFailure(repoRoot, "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}");
            if (!upstream.success()) {
                return new CheckResult(
                        UpdateStatus.UNABLE_TO_CHECK,
                        branch,
                        shortCommit,
                        "No upstream tracking branch configured for '" + branch + "'."
                );
            }

            CommandResult fetch = runGitAllowFailure(repoRoot, "fetch", "--quiet");
            if (!fetch.success()) {
                return new CheckResult(
                        UpdateStatus.UNABLE_TO_CHECK,
                        branch,
                        shortCommit,
                        "Unable to fetch remote updates: " + errorSummary(fetch)
                );
            }

            CommandResult counts = runGitAllowFailure(repoRoot, "rev-list", "--left-right", "--count", "HEAD...@{u}");
            if (!counts.success()) {
                return new CheckResult(
                        UpdateStatus.UNABLE_TO_CHECK,
                        branch,
                        shortCommit,
                        "Unable to compare with upstream: " + errorSummary(counts)
                );
            }

            String[] parts = counts.stdout().trim().split("\\s+");
            if (parts.length < 2) {
                return new CheckResult(
                        UpdateStatus.UNABLE_TO_CHECK,
                        branch,
                        shortCommit,
                        "Unexpected git compare output: '" + counts.stdout().trim() + "'."
                );
            }

            int behind = Integer.parseInt(parts[1]);
            if (behind > 0) {
                return new CheckResult(
                        UpdateStatus.UPDATE_AVAILABLE,
                        branch,
                        shortCommit,
                        "Update available: local branch is behind upstream by " + behind + " commit(s)."
                );
            }

            return new CheckResult(
                    UpdateStatus.ALREADY_UP_TO_DATE,
                    branch,
                    shortCommit,
                    "Already up to date with upstream branch."
            );
        } catch (IOException | NumberFormatException ex) {
            return new CheckResult(UpdateStatus.UNABLE_TO_CHECK, "unknown", "unknown", ex.getMessage());
        }
    }

    public UpdateResult updateAndRestart() {
        CheckResult check = checkForUpdates();
        if (check.status() == UpdateStatus.WORKING_TREE_DIRTY) {
            return new UpdateResult(false, check.message());
        }
        if (check.status() == UpdateStatus.UNABLE_TO_CHECK) {
            return new UpdateResult(false, check.message());
        }
        if (check.status() == UpdateStatus.ALREADY_UP_TO_DATE) {
            return new UpdateResult(false, "Already up to date. Nothing to pull.");
        }

        Path repoRoot;
        try {
            repoRoot = resolveRepoRoot();
        } catch (IOException ex) {
            return new UpdateResult(false, ex.getMessage());
        }

        try {
            CommandResult pull = runGitAllowFailure(repoRoot, "pull", "--ff-only");
            if (!pull.success()) {
                return new UpdateResult(false, "Update failed while pulling latest changes: " + errorSummary(pull));
            }

            UpdateResult restart = restartApplication(repoRoot);
            if (!restart.success()) {
                return restart;
            }

            return new UpdateResult(true, "Update pulled successfully. Restarting now...");
        } catch (IOException ex) {
            return new UpdateResult(false, ex.getMessage());
        }
    }

    private UpdateResult restartApplication(Path repoRoot) {
        String javaHome = System.getProperty("java.home", "");
        String javaBin = Path.of(javaHome, "bin", isWindows() ? "java.exe" : "java").toString();
        String classpath = System.getProperty("java.class.path", "");

        List<String> command = new ArrayList<>();
        command.add(javaBin);

        String libraryPath = System.getProperty("java.library.path", "");
        if (!libraryPath.isBlank()) {
            command.add("-Djava.library.path=" + libraryPath);
        }

        command.add("-cp");
        command.add(classpath);
        command.add(MAIN_CLASS);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(repoRoot.toFile());

        try {
            processBuilder.start();
            return new UpdateResult(true, "Restart process started.");
        } catch (IOException ex) {
            return new UpdateResult(false, "Updated successfully, but failed to restart automatically. Please relaunch manually. " + ex.getMessage());
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private Path resolveRepoRoot() throws IOException {
        Path workingDir = Path.of(System.getProperty("user.dir", "."));
        CommandResult result = runCommand(workingDir, List.of("git", "rev-parse", "--show-toplevel"));
        if (!result.success()) {
            throw new IOException("Unable to locate Git repository root from current working directory.");
        }
        return Path.of(result.stdout().trim());
    }

    private CommandResult runGit(Path repoRoot, String... args) throws IOException {
        CommandResult result = runGitAllowFailure(repoRoot, args);
        if (!result.success()) {
            throw new IOException(errorSummary(result));
        }
        return result;
    }

    private CommandResult runGitAllowFailure(Path repoRoot, String... args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        for (String arg : args) {
            cmd.add(arg);
        }
        return runCommand(repoRoot, cmd);
    }

    private CommandResult runCommand(Path workingDirectory, List<String> command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory.toFile());

        Process process = processBuilder.start();
        String stdout = readAll(process.getInputStream());
        String stderr = readAll(process.getErrorStream());

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Command interrupted: " + String.join(" ", command), ex);
        }

        return new CommandResult(exitCode == 0, exitCode, stdout, stderr);
    }

    private static String readAll(java.io.InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static String readNonEmpty(CommandResult result, String fallback) {
        String text = result.stdout().trim();
        return text.isEmpty() ? fallback : text;
    }

    private static String errorSummary(CommandResult result) {
        String err = result.stderr().trim();
        if (!err.isEmpty()) {
            return err;
        }
        String out = result.stdout().trim();
        if (!out.isEmpty()) {
            return out;
        }
        return "git exited with code " + result.exitCode();
    }

    private record CommandResult(boolean success, int exitCode, String stdout, String stderr) {
    }
}
