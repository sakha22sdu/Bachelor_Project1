import re
from collections import Counter

def analyze_commit_messages(file_path):
    try:
        # Read the entire file
        with open(file_path, 'r', encoding='utf-8') as file:
            text = file.read()

        # Remove punctuation and numbers, convert to lowercase
        text = re.sub(r'[^a-zA-Z\s]', '', text).lower()

        # Split into words
        words = text.split()

        # Filter out very short words (optional)
        words = [word for word in words if len(word) > 2]

        # Count word frequencies
        counter = Counter(words)

        # Get 10 most common words
        most_common = counter.most_common(10)

        print(" Top most used words in commit messages:")
        print("------------------------------------------")
        for word, count in most_common:
            print(f"{word}: {count}")

        # Optionally save results
        output_file = "commit_word_frequency.txt"
        with open(output_file, 'w', encoding='utf-8') as out:
            out.write("Top 10 most used words in commit messages:\n")
            out.write("------------------------------------------\n")
            for word, count in most_common:
                out.write(f"{word}: {count}\n")

        print(f"\n Results saved to {output_file}")

    except FileNotFoundError:
        print(f" File '{file_path}' not found. Make sure commit_messages.txt exists.")
    except Exception as e:
        print(f" Error: {e}")


if __name__ == "__main__":
    # Path to your commit messages file (same folder)
    analyze_commit_messages("commit_messages.txt")
