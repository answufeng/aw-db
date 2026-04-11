# aw-db

Room database utility library for Android. Provides DSL-style database builder, generic DAO, transaction helpers, type converters, and result wrapper.

## Installation

Add the dependency in your module-level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.answufeng:aw-db:1.0.0")
}
```

Make sure you have the JitPack repository in your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

## Features

- DSL-style Room database builder with migration/callback support
- Generic `BaseDao<T>` with CRUD + upsert operations
- Transaction helpers: `runInTransaction`, `safeTransaction`, `batchInsert`
- Room type converters: Date, List<String>, List<Long>, Map<String, String>
- `DbResult< T>` sealed class for reactive data observation (Loading/Success/Failure)
- Simplified Migration/Callback creation with Kotlin DSL

## Usage

```kotlin
// Build database
val db = BrickDatabase.build<AppDatabase>(context, "app.db") {
    addMigrations(migration(1, 2) { executeSql("ALTER TABLE User ADD COLUMN email TEXT") })
    addCallback(onCreateCallback { /* seed data */ })
    fallbackToDestructiveMigration()
}

// Generic DAO
@Dao
abstract class UserDao : BaseDao<User>() {
    @Query("SELECT * FROM User")
    abstract suspend fun getAll(): List<User>
}

// Transaction
val result = db.safeTransaction { userDao().insert(User(name = "test")) }

// DbResult
db.userDao().observeAll().asDbResult().collect { result ->
    result.onLoading { showLoading() }
        .onSuccess { showData(it) }
        .onFailure { showError(it) }
}
```

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.
