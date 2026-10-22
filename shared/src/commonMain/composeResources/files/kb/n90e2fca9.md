Room это ORM (object relational mapping) поверх SQLite. Room состоит из трёх основных компонентов: 

1. @Database: непосредственно база данных. Это класс, с аннотацией @Database и который должен: ○ быть абстрактным и наследоваться от класса RoomDatabase; ○ включать список сущностей (таблиц), хранящихся в БД; ○ содержать метод без аргументов, возвращающий @Dao; Его можно получить с помощью фабричных методов Room.databaseBuilder() или Room.inMemoryDatabaseBuilder() - для создания тестов. **Database (@Database) → Связь с SQLite**

```kotlin
@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
```

- ✅ `@Database` → определяет список таблиц и версию БД.
- ✅ `RoomDatabase` → абстрактный класс, управляющий БД.

2. @Entity (сущность): таблица внутри базы данных. Их может быть несколько. (**Entity (@Entity) → Таблица в БД**)

```kotlin
@Entity(tableName = "users")

data class User(

    @PrimaryKey(autoGenerate = true) val id: Int,

    @ColumnInfo(name = "user_name") val name: String,

    val age: Int

)
```

3. @DAO (Data Access Object): интерфейс, содержащий различные методы доступа (**Data Access Object (DAO)** — это интерфейс, через который мы взаимодействуем с БД.)
```kotlin
@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): User?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Delete
    suspend fun deleteUser(user: User)
}
```

- **@PrimaryKey** - аннотация для объявления первичного ключа сущности.
- **@ColumnInfo** - аннотация для настроек конкретного столбца сущности.
- **@Query** - аннотация, которая позволяет выполнить SQL-запрос в методах DAO-интерфейса.
- **@Insert** - аннотация, которая позволяет выполнить вставку в таблицу базы данных.
- **@Update** - аннотация, которая позволяет выполнить обновление некоторых строк в таблице базы данных.
- **@Delete** - аннотация, которая позволяет выполнить удаление некоторых строк в таблице базы данных.
- **@Transaction**

### **Миграции (Обновление БД без потери данных)**

Room автоматически **не обновляет БД** при изменении схемы, но можно **настроить миграции**.

```kotlin
val db = Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
    .addMigrations(MIGRATION_1_2)
    .build()

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE users ADD COLUMN email TEXT")
    }
}
```

**Конвертеры данных (TypeConverters)**

• Если поле содержит **нестандартный тип данных** (например, Date), Room **не может** сохранить его в SQLite.

• Для этого нужно использовать **конвертеры**.

```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Database(entities = [User::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase()
```

> [!tip] Что часто спрашивают про Room
> - **Главное преимущество над голым SQLite** — проверка SQL **на этапе компиляции** (`@Query` валидируется компилятором) и автогенерация бойлерплейта.
> - **`suspend`-функции** в DAO выполняются вне главного потока автоматически (Room сам уводит на свой Executor). Запрос с возвратом `Flow<List<T>>` — **реактивный**: при изменении таблицы эмитит новые данные.
> - Нельзя обращаться к БД с **главного потока** (по умолчанию бросит исключение) — `suspend`/`Flow`/`Rx` или `allowMainThreadQueries()` (только для тестов).
> - **`@Transaction`** — для атомарных групп операций и для запросов с `@Relation` (чтобы данные были согласованы).
> - **Миграции:** при изменении схемы поднимаешь `version` и добавляешь `Migration`; иначе `fallbackToDestructiveMigration()` сотрёт данные. Схемы экспортируются для тестов миграций.

Связано: [[Room or SharedPreferences]], [[SharedFlow vs Flow vs StateFlow]], [[Coroutines]], [[Чистая архитектура]]
