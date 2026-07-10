# PR2 data layer

Initial words are loaded from `app/src/main/assets/words.json` by a data source, parsed with Kotlin Serialization into DTOs, validated as a complete list, and only then written to Room. Room is the source of truth after import; repository APIs map Room entities to domain models instead of exposing persistence classes.

Canonical integrity is checked by comparing the full persisted word ID set with the asset ID set. A count-only check is insufficient because a database can contain 2025 rows while still missing canonical IDs or containing unexpected IDs.

Application startup creates an `AppContainer`, starts `AppInitializer` from `Application.onCreate()`, and keeps database/import work outside Composables. The importer is idempotent and protected by a process-level mutex. It does not trust the DataStore `words_imported` flag alone: if the flag is false but IDs are valid it repairs the flag, and if the flag is true but Room is empty or incomplete it repairs Room first. The flag is written only after persistence is verified.

Canonical word import uses `OnConflictStrategy.IGNORE` plus explicit updates. SQLite `REPLACE` is forbidden for canonical words because it deletes and recreates rows, which can break selected-word, turn-result, history, and future statistics relationships.

Repair happens in a Room transaction; final canonical verification throws an internal rollback exception if it fails, so partial repair changes are not committed. Unexpected unreferenced word rows can be safely removed during repair. Unexpected rows referenced by history produce a structured consistency failure instead of deleting history. Word usage belongs to relationships such as `gameId + wordId`; no global mutable `Word.used` state exists, so the same canonical word can appear in different games.

Validation failures are returned as structured `WordValidationError` values. Database repair failures are returned as structured `WordDatabaseConsistencyError` values. Gameplay, selection exclusion for completed games, scoring, timers, history UI, statistics, settings, sound, vibration, and later PR UI are intentionally deferred.
