# Word import and persistence

PR2 imports `app/src/main/assets/words.json` once into Room and treats Room as the source of truth after that point. `WordImporter` reads the DataStore `words_imported` flag and also verifies the Room row count before returning `AlreadyImported`, so a stale preference flag cannot hide an incomplete database.

The importer parses the whole JSON array with Kotlin Serialization, validates the complete list, inserts all words in a single Room transaction, verifies the final count, and only then writes `words_imported = true`. Validation errors are represented by `WordValidationError` values so callers and tests can inspect the exact failure.

Word usage is intentionally modeled through `GameSelectedWordEntity(gameId, wordId)` with a composite primary key. There is no global `Word.used` field because usage belongs to a specific game session.

`WordRepository.getAvailableWords()` currently returns words filtered by difficulty and theme. Excluding words from completed games is intentionally deferred until the game-history flow is implemented on top of the prepared history tables.
