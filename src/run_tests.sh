#!/bin/bash

# --- CONFIGURATION ---
TEST_DIR="./testgames"

# 1. Move RegressionTest to the main source folder so Gradle manages it automatically
if [ -f "./src/RegressionTest.java" ]; then
    echo "Moving RegressionTest.java to src/main/java/ to allow Gradle to manage dependencies..."
    mv "./src/RegressionTest.java" "./src/main/java/RegressionTest.java"
fi

# 2. Compile via Gradle to ensure all dependencies and classes are ready
echo "Compiling project..."
./gradlew classes
if [ $? -ne 0 ]; then
    echo "Compilation failed."
    exit 1
fi

# 3. Loop through files in the shell
echo "Starting Regression Suite (Process Isolation Mode)..."
count=0
passed=0
failed=0

# Find all .rails files and loop
for file in "$TEST_DIR"/*.rails; do
    ((count++))
    filename=$(basename "$file")
    
    echo "------------------------------------------------------------"
    echo "Processing [$count]: $filename"
    
 # --- START FIX ---
    # Use Gradle's run task to automatically resolve the SLF4J classpath and run the test!
    # We pass the file as an argument and enforce headless mode via JAVA_OPTS.
    export JAVA_OPTS="-Djava.awt.headless=true"
    output=$(./gradlew -q run -PmainClass=RegressionTest --args="$file" 2>&1)
    exit_code=$?

    # Print the output so you can still see it
    echo "$output"

    # Fail if Java exited with error OR if keywords leaked to the console
if [ $exit_code -ne 0 ] || echo "$output" | grep -qE "FATAL|CRASH|VALIDATION FAILURE|Exception|RELOAD ERROR|Action not in PossibleActions"; then
        echo "RESULT: FAILED"
        ((failed++))
    else
        echo "RESULT: OK"
        ((passed++))
    fi
    # --- END FIX ---
    
    # Check exit code of the Java process
    # (The previous check block is replaced by the logic above)
done

echo ""
echo "=== SUITE SUMMARY ==="
echo "Total:  $count"
echo "Passed: $passed"
echo "Failed: $failed"

if [ $failed -gt 0 ]; then
    exit 1
fi