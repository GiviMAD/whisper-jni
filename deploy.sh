set -xe
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git tag v$VERSION
git push origin v$VERSION