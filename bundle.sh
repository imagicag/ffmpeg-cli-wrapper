#!/usr/bin/env bash
set -euo pipefail

VERSION=${1:?Usage: $0 <version>}
ARTIFACT_ID=ffmpeg-java
PROJECT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
TARGET_DIR="$PROJECT_DIR/target"
DEPLOY_DIR="$PROJECT_DIR/deploy/ch/imagic/$ARTIFACT_ID/$VERSION"

for suffix in .pom .jar -sources.jar -javadoc.jar; do
    artifact="$ARTIFACT_ID-$VERSION$suffix"
    for file in "$TARGET_DIR/$artifact" "$TARGET_DIR/$artifact.asc"; do
        if [[ ! -f "$file" ]]; then
            printf 'Missing release artifact: %s\nRun ./mvnw verify before creating the bundle.\n' "$file" >&2
            exit 1
        fi
    done
    md5sum "$TARGET_DIR/$artifact" | awk '{print $1}' > "$TARGET_DIR/$artifact.md5"
    sha1sum "$TARGET_DIR/$artifact" | awk '{print $1}' > "$TARGET_DIR/$artifact.sha1"
done

rm -rf "$PROJECT_DIR/deploy"
mkdir -p "$DEPLOY_DIR"
cp "$TARGET_DIR/$ARTIFACT_ID-$VERSION.pom"* "$DEPLOY_DIR/"
cp "$TARGET_DIR/$ARTIFACT_ID-$VERSION.jar"* "$DEPLOY_DIR/"
cp "$TARGET_DIR/$ARTIFACT_ID-$VERSION-sources.jar"* "$DEPLOY_DIR/"
cp "$TARGET_DIR/$ARTIFACT_ID-$VERSION-javadoc.jar"* "$DEPLOY_DIR/"

(
    cd "$PROJECT_DIR/deploy"
    zip -r b.zip ch
)
