#!/bin/sh

# Decrypt the file
# --batch to prevent interactive command
# --yes to assume "yes" for questions
gpg --quiet --batch --yes --decrypt --passphrase="$SECRET_PASSPHRASE" \
--output "$SECRET_NAME" "deploy_secrets/$SECRET_NAME.gpg"