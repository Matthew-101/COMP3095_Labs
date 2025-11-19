/**
 * MongoDB init script (works with Docker's /docker-entrypoint-initdb.d/)
 * - Idempotent: safe to re-run without errors
 *
 * NOTE: In MongoDB, a database "appears" only after a write
 * (e.g., creating a collection). getSiblingDB() just gives you a handle.
 */

print("Mongo Init Script - START");

/**
 * Function: upsertUser
 * ---------------------
 * Create or update a MongoDB user on a given database.
 *
 * @param {string} dbName   - The database name where the user should exist.
 * @param {object} userDoc  - A document describing the user:
 *    {
 *      user: "username",               // string: the username
 *      pwd: "password",                // string: the user’s password
 *      roles: [{ role: "readWrite", db: "product-service" }] // array of role objects
 *    }
 */
function upsertUser(dbName, userDoc) {
    const targetDb = db.getSiblingDB(dbName); // get a handle to the DB (creates lazily)
    const existing = targetDb.getUser(userDoc.user);

    if (existing) {
        targetDb.updateUser(userDoc.user, {
            pwd: userDoc.pwd,
            roles: userDoc.roles,
        });
        print(`🟡 Updated user "${userDoc.user}" on "${dbName}"`);
    } else {
        targetDb.createUser(userDoc);
        print(`✅ Created user "${userDoc.user}" on "${dbName}"`);
    }
}

/**
 * Function: ensureCollection
 * ---------------------------
 * Create a collection only if it does not already exist.
 *
 * @param {string} dbName      - The name of the database to target.
 * @param {string} collName    - The name of the collection to create.
 * @param {object} [options]   - (Optional) Extra options to pass to createCollection().
 */
function ensureCollection(dbName, collName, options = {}) {
    const targetDb = db.getSiblingDB(dbName);
    const exists = targetDb.getCollectionInfos({ name: collName }).length > 0;
    if (!exists) {
        targetDb.createCollection(collName, options);
        print(`✅ Created collection "${dbName}.${collName}"`);
    } else {
        print(`🟡 Collection "${dbName}.${collName}" already exists`);
    }
}

/* ------------------------------------------------------------------ */
/* 1) ADMIN USER (used by tools like mongo-express)                    */
/* ------------------------------------------------------------------ */
upsertUser("admin", {
    user: "admin",
    pwd: "password",
    roles: [{ role: "root", db: "admin" }],
});

/* ------------------------------------------------------------------ */
/* 2) APPLICATION USER FOR product-service                             */
/* ------------------------------------------------------------------ */
upsertUser("product-service", {
    user: "productAdmin",
    pwd: "password",
    roles: [{ role: "readWrite", db: "product-service" }],
});

/* ------------------------------------------------------------------ */
/* 3) MATERIALIZE THE DATABASE (first write)                           */
/* ------------------------------------------------------------------ */
ensureCollection("product-service", "user");

print('✅ Ensured user "productAdmin" has readWrite on "product-service" and base collection exists.');
print("🔵 Mongo Init Script - END");