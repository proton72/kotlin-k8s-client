# Publishing Guide

This document explains how to publish releases of the Kotlin K8s Client library to GitHub Packages and Sonatype Central.

## Prerequisites

### 1. Generate GPG Key (if you don't have one)

```bash
gpg --full-generate-key
```

Select RSA and RSA, 4096 bits, and provide your details.

### 2. Export Your GPG Key

```bash
# List your keys to find the key ID
gpg --list-secret-keys --keyid-format=long

# Export the private key (replace KEY_ID with your key ID)
gpg --armor --export-secret-keys KEY_ID > private-key.asc

# Base64 encode for GitHub Secrets
cat private-key.asc | base64 -w 0 > private-key-base64.txt
```

### 3. Set Up GitHub Secrets

Add the following secrets to your GitHub repository (Settings → Secrets and variables → Actions):

#### For Sonatype Central:
- `SONATYPE_USERNAME`: Your Sonatype account username
- `SONATYPE_PASSWORD`: Your Sonatype account password or token
- `GPG_PRIVATE_KEY_BASE64`: The base64-encoded GPG private key (content of private-key-base64.txt)
- `GPG_PASSPHRASE`: Your GPG key passphrase

#### For GitHub Packages:
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions (no setup needed)

## Publishing Process

The release workflow is organized into separate jobs for better control and visibility:

1. **version-and-build**: Updates version, runs tests, and builds artifacts
2. **publish-github-packages**: Publishes to GitHub Packages
3. **publish-sonatype-central**: Publishes to Sonatype Central (only for stable releases)
4. **release-summary**: Generates a detailed summary of the release

### Automatic Publishing (via GitHub Release)

When you create a new release on GitHub:
1. The release workflow automatically triggers
2. Version is extracted from the release tag (e.g., `v1.2.3` → `1.2.3`)
3. `gradle.properties` is updated with the version
4. Tests are run and library is built
5. Artifacts are published based on release type:
   - **Stable releases** (not marked as prerelease): Published to both GitHub Packages and Sonatype Central
   - **Prereleases** (beta, alpha, RC): Published only to GitHub Packages

### Manual Publishing (via GitHub Actions)

You can trigger a manual release from the GitHub Actions UI:

1. Go to **Actions** → **Release** → **Run workflow**
2. Fill in the parameters:
   - **version**: Version number (e.g., `1.2.3`)
   - **publish_github**: Whether to publish to GitHub Packages
   - **publish_sonatype**: Whether to publish to Sonatype Central
3. Click **Run workflow**

This allows you to:
- Test publishing to individual repositories
- Re-publish a version if needed
- Skip specific repositories during testing

### Local Manual Publishing

You can also publish manually from your local machine:

```bash
# Set environment variables
export SONATYPE_USERNAME="your-username"
export SONATYPE_PASSWORD="your-password"
export GPG_PRIVATE_KEY_BASE64="your-base64-encoded-key"
export GPG_PASSPHRASE="your-gpg-passphrase"

# Publish to Sonatype Central
./gradlew publishMavenPublicationToSonatypeCentralRepository

# Or publish to both repositories
./gradlew publish
```

## Verifying Publications

### GitHub Packages
Visit: https://github.com/proton72/kotlin-k8s-client/packages

### Sonatype Central
Visit: https://central.sonatype.com/publishing

### Workflow Summary

After each release, the workflow generates a detailed summary showing:
- Version published
- Whether it's a prerelease
- Publication status for each repository (success/skipped/failed)
- Links to relevant pages

View the summary in the **Actions** tab after the workflow completes.

## Workflow Features

### Separate Publishing Jobs
- **Isolation**: Each repository has its own job
- **Parallel execution**: GitHub and Sonatype publications run in parallel
- **Independent failure**: One repository failing doesn't affect the other

### Validation Steps
- Credential verification before publishing
- GPG signing configuration check
- Test execution before publishing
- Clear error messages if configuration is missing

### Smart Prerelease Handling
- Prereleases (alpha, beta, RC) are published only to GitHub Packages
- Stable releases go to both GitHub Packages and Sonatype Central
- Prevents cluttering Maven Central with unstable versions

## Troubleshooting

### Signing Issues
- Ensure your GPG key is properly base64-encoded without line breaks
- Verify the passphrase matches your GPG key
- Check that the key hasn't expired: `gpg --list-keys`

### Sonatype Central Issues
- Verify your credentials at https://central.sonatype.com/
- Ensure your account has publishing permissions
- Check that all required POM metadata is present (name, description, URL, licenses, developers, SCM)

### Common Commands

```bash
# Test signing locally (without publishing)
./gradlew signMavenPublication

# View all publishing tasks
./gradlew tasks --group=publishing

# Publish with debug output
./gradlew publish --stacktrace --info
```
