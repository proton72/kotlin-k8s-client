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

### Automatic Publishing (via GitHub Release)

When you create a new release on GitHub:
1. The release workflow automatically triggers
2. Version is extracted from the release tag (e.g., `v1.2.3` → `1.2.3`)
3. `gradle.properties` is updated with the version
4. Library is built and published to both:
   - GitHub Packages
   - Sonatype Central

### Manual Publishing

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
