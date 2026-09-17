#!/usr/bin/env python3
import base64
import os
import re
import uuid


def decode_secret(env_var_name: str) -> str:
    raw = os.environ.get(env_var_name, "")
    if not raw:
        return ""
    # MIME Base64: ignore non-base64 chars (newlines, carriage returns, spaces, etc.)
    cleaned = re.sub(r"[^A-Za-z0-9+/=]", "", raw)
    if not cleaned:
        return ""
    return base64.b64decode(cleaned).decode("utf-8").strip()


def main():
    github_env = os.environ.get("GITHUB_ENV")
    if not github_env:
        return

    env_updates = {}

    # 1. Maven Central Password
    maven_central_password = decode_secret("NEXUS_PASS64")
    if maven_central_password:
        env_updates["ORG_GRADLE_PROJECT_mavenCentralPassword"] = maven_central_password

    # 2. GPG Signing Key
    gpg_key = decode_secret("GPG_KEY64")
    if gpg_key:
        env_updates["ORG_GRADLE_PROJECT_signingInMemoryKey"] = gpg_key
        env_updates["ORG_GRADLE_PROJECT_signingInMemoryKeyId"] = "0x4272C851"

    # 3. GPG Passphrase
    gpg_passphrase = os.environ.get("GPG_PASSPHRASE", "")
    if gpg_passphrase:
        env_updates["ORG_GRADLE_PROJECT_signingInMemoryKeyPassword"] = gpg_passphrase

    with open(github_env, "a", encoding="utf-8") as f:
        for key, value in env_updates.items():
            if "\n" in value or "\r" in value:
                delimiter = f"EOF_{uuid.uuid4().hex}"
                f.write(f"{key}<<{delimiter}\n{value}\n{delimiter}\n")
            else:
                f.write(f"{key}={value}\n")

    if "ORG_GRADLE_PROJECT_mavenCentralPassword" in env_updates:
        print("Configured ORG_GRADLE_PROJECT_mavenCentralPassword=***")
    if "ORG_GRADLE_PROJECT_signingInMemoryKey" in env_updates:
        print("Configured ORG_GRADLE_PROJECT_signingInMemoryKey=***")
    if "ORG_GRADLE_PROJECT_signingInMemoryKeyPassword" in env_updates:
        print("Configured ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=***")


if __name__ == "__main__":
    main()
