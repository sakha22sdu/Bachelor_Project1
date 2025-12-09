from collections import Counter
from pathlib import Path
import argparse
import re

# stopwords, noise

STOPWORDS = {
    "the", "and", "or", "a", "an", "to", "of", "in", "for", "on", "with",
    "is", "are", "was", "were", "it", "this", "that", "as", "by", "be",
    "at", "from", "into", "about", "we", "you", "i", "our",
    # commit-log noise
    "git", "svn", "id", "http", "https", "org", "com",
    "patch", "file", "files",
}


# parsing help

def find_refac_reconf_commits(analysis_path: Path) -> set[str]:
    """
    Parse commit_analysis_log.txt and return IDs whose Type is
    Refactoring or Reconfiguration.
    """
    refac_types = {"Refactoring", "Reconfiguration"}
    interesting_ids: set[str] = set()

    with analysis_path.open("r", encoding="utf-8", errors="ignore") as f:
        for line in f:
            if not line.startswith("Commit:"):
                continue

            # Expected format:
            # Commit: <id> | Type: <type> | Bug-related: ... | Message: ...
            parts = [p.strip() for p in line.split("|")]
            if len(parts) < 2:
                continue

            # Commit ID
            commit_part = parts[0]  # "Commit: <id>"
            _, cid = commit_part.split("Commit:", 1)
            cid = cid.strip()

            # Type
            type_part = parts[1]  # "Type: <type>"
            _, ctype = type_part.split("Type:", 1)
            ctype = ctype.strip()

            if ctype in refac_types:
                interesting_ids.add(cid)

    return interesting_ids


def load_messages_for_ids(messages_path: Path, wanted_ids: set[str]) -> list[str]:
    """
    Parse commit_messages.txt and return a list of messages whose
    Commit ID is in wanted_ids.

    Format created by MyAnalysis
        Commit ID: <hash>
        <full message possibly multi-line>
    """
    messages: list[str] = []

    current_id: str | None = None
    current_lines: list[str] = []

    def flush_current():
        if current_id and current_id in wanted_ids and current_lines:
            messages.append("\n".join(current_lines).strip())

    with messages_path.open("r", encoding="utf-8", errors="ignore") as f:
        for raw in f:
            line = raw.rstrip("\n")

            if line.startswith("Commit ID:"):
                # New block begins → flush previous
                flush_current()
                current_id = line.split("Commit ID:", 1)[1].strip()
                current_lines = []
            elif line.strip() == "":
                # Blank line - end of current block
                flush_current()
                current_id = None
                current_lines = []
            else:
                # Part of current message
                if current_id is not None:
                    current_lines.append(line)

    # In case file doesn't end with a blank line
    flush_current()
    return messages


def tokenize(text: str) -> list[str]:
    """
    Split text into lowercase “words”.
    """
    return re.findall(r"[a-zA-Z0-9_']+", text.lower())


def analyze_keywords(messages: list[str]) -> Counter:
    """
    Given a list of commit messages, return a Counter of keyword frequencies.
    """
    counter = Counter()
    for msg in messages:
        words = tokenize(msg)
        keywords = [w for w in words if w not in STOPWORDS and len(w) > 2]
        counter.update(keywords)
    return counter


def write_report(output_path: Path,
                 wanted_ids: set[str],
                 messages: list[str],
                 keyword_freq: Counter,
                 top_n: int = 50):
    with output_path.open("w", encoding="utf-8") as out:
        out.write("==== Topic Analysis: Refactoring / Reconfiguration ====\n\n")
        out.write(f"Number of matching commits: {len(wanted_ids)}\n")
        out.write(f"Number of messages loaded : {len(messages)}\n\n")

        out.write("## Top {} keywords\n".format(top_n))
        for word, count in keyword_freq.most_common(top_n):
            out.write(f"{word:20s} {count}\n")

        out.write("\n\n## Messages used for analysis\n\n")
        for i, msg in enumerate(messages, 1):
            out.write(f"--- Message {i} ---\n")
            out.write(msg + "\n\n")


#main entry point

def main():
    parser = argparse.ArgumentParser(
        description="Topic analysis for Refactoring/Reconfiguration commits."
    )
    parser.add_argument(
        "--analysis",
        type=Path,
        default=Path("commit_analysis_log.txt"),
        help="Path to commit_analysis_log.txt",
    )
    parser.add_argument(
        "--messages",
        type=Path,
        default=Path("commit_messages.txt"),
        help="Path to commit_messages.txt",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("topic_analysis_refac_reconf.txt"),
        help="Output report file",
    )
    parser.add_argument(
        "--top",
        type=int,
        default=50,
        help="How many top keywords to show",
    )

    args = parser.parse_args()

    if not args.analysis.exists():
        raise SystemExit(f"Analysis file not found: {args.analysis}")
    if not args.messages.exists():
        raise SystemExit(f"Messages file not found: {args.messages}")

    # 1) Find interesting commit IDs
    wanted_ids = find_refac_reconf_commits(args.analysis)

    # 2) Load their messages
    messages = load_messages_for_ids(args.messages, wanted_ids)

    # 3) Keyword analysis
    keyword_freq = analyze_keywords(messages)

    # 4) Write report
    write_report(args.output, wanted_ids, messages, keyword_freq, top_n=args.top)
    print(f"[topic-analysis] Wrote report to {args.output}")


if __name__ == "__main__":
    main()

"""
Topic analysis for Refactoring / Reconfiguration commits.

Steps:
1. Read commit_analysis_log.txt to find commits whose Type is
   Refactoring or Reconfiguration.
2. Read commit_messages.txt and collect the messages for those commits.
3. Tokenize messages, remove stopwords, and count word frequencies.
4. Write a report to topic_analysis_refac_reconf.txt (default).
"""