// Creates the least-privilege application user for stringpro-backend.
//
// IMPORTANT: the mongo entrypoint runs this script ONLY on first initialization
// of an empty data volume. It does NOT re-run against an existing mongo-data
// volume. If MONGO_APP_PASSWORD is rotated later, update the user manually:
//   docker compose exec mongodb mongosh -u root -p "$MONGO_ROOT_PASSWORD" \
//     --authenticationDatabase admin --eval 'db.getSiblingDB("stringpro").updateUser(...)'
//
// Runs under mongosh (mongo:7), so process.env is available.
const username = process.env.MONGO_APP_USERNAME;
const password = process.env.MONGO_APP_PASSWORD;

// Create the app user IN the stringpro database, so Spring authenticates against
// `stringpro` directly (no authSource=admin needed). Scoped to readWrite on its
// own database only — the root account stays reserved for admin.
db.getSiblingDB("stringpro").createUser({
  user: username,
  pwd: password,
  roles: [{ role: "readWrite", db: "stringpro" }],
});
