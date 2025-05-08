import os
import fnmatch

# Directories and file patterns to ignore
IGNORE_DIRS = {"env", "__pycache__", "data"}
IGNORE_PATERS = [
    "test_*.py", "*_test.py",
    "dev.py", "debug.py", "sandbox.py"
]

# Scoring weights (0-5 scale approximation)
SCORING_WEIGHTS = {
    'blueprints': 0,
    'env_config': 2,        # assume moderate
    'dependencies': 2,
    'deployment': 2,
    'logging': 1,
    'auth': 2
}

EMPTY_INIT_FILES = []

# Name of this script to avoid self-analysis
def should_ignore(file_name):
    return any(fnmatch.fnmatch(file_name, pattern) for pattern in IGNORE_PATERS)

def is_empty_init(file_path):
    return (
        os.path.basename(file_path) == "__init__.py" and
        os.path.getsize(file_path) == 0
    )

def analyze_files(root_dir):
    ignored_files = []
    file_line_report = []
    total_py_files = 0
    total_lines = 0
    script_name = os.path.basename(__file__)

    for dirpath, dirnames, filenames in os.walk(root_dir, topdown=True):
        # Filter out ignored directories
        dirnames[:] = [d for d in dirnames if d not in IGNORE_DIRS]

        for filename in filenames:
            # Skip non-Python files and this script itself
            if not filename.endswith('.py') or filename == script_name:
                continue
            total_py_files += 1
            full_path = os.path.join(dirpath, filename)

            # Check ignore patterns and empty __init__.py
            if should_ignore(filename) or is_empty_init(full_path):
                ignored_files.append(full_path)
                continue

            try:
                with open(full_path, 'r', encoding='utf-8') as f:
                    lines = f.readlines()
                    num_lines = len(lines)
                    total_lines += num_lines
                    file_line_report.append(f"{full_path}: {num_lines} lines")
            except Exception as e:
                file_line_report.append(f"{full_path}: ERROR - {str(e)}")

    return ignored_files, file_line_report, total_py_files, total_lines


def estimate_blueprint_complexity(root_dir):
    blueprint_count = 0
    for dirpath, dirnames, filenames in os.walk(root_dir):
        # Skip ignored directories
        dirnames[:] = [d for d in dirnames if d not in IGNORE_DIRS]
        for filename in filenames:
            if not filename.endswith('.py'):
                continue
            full_path = os.path.join(dirpath, filename)
            try:
                with open(full_path, 'r', encoding='utf-8') as f:
                    if 'Blueprint(' in f.read():
                        blueprint_count += 1
            except:
                continue
    return blueprint_count


def estimate_overall_complexity(blueprint_count):
    # Adjust blueprint weight
    if blueprint_count <= 2:
        SCORING_WEIGHTS['blueprints'] = 1
    elif blueprint_count <= 4:
        SCORING_WEIGHTS['blueprints'] = 2
    else:
        SCORING_WEIGHTS['blueprints'] = 4
    return sum(SCORING_WEIGHTS.values())


def main():
    root = os.getcwd()
    ignored, file_lines, py_file_count, total_lines = analyze_files(root)
    blueprint_count = estimate_blueprint_complexity(root)
    total_score = estimate_overall_complexity(blueprint_count)

    
    for f in ignored:
        print(f"  - {f}")
    print("\n===== Deployment Complexity Report =====")
    print(f"Blueprints Detected: {blueprint_count}")
    print(f"Python Files (analyzed): {py_file_count}")
    print(f"Total Lines of Code: {total_lines}")
    print(f"Deployment Complexity Score (0–20): {total_score}")
    print("\nIgnored Files:")

    # Write line count report to file
    with open("file_line_report.txt", "w", encoding='utf-8') as f:
        f.write("Lines of Code Per File:\n")
        f.write("\n".join(file_lines))

if __name__ == "__main__":
    main()
