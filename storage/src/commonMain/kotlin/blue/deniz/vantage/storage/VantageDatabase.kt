package blue.deniz.vantage.storage

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

@Database(
    entities = [
        EventEntity::class,
        EventCacheEntity::class,
        EventTimeRangeEntity::class,
        TagEntity::class,
        TagParentEntity::class,
        EventTagEntity::class,
        CredentialEntity::class,
        SeenEventVersionEntity::class,
        MarkerEntity::class,
        PeriodEntity::class,
        HiddenCalendarEventEntity::class,
        SyncStoreEntity::class,
        SyncedRecordEntity::class,
        PendingUploadEntity::class,
    ],
    version = 9,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = RebuildEventTimeRanges::class),
        AutoMigration(from = 5, to = 6, spec = RenameEventAddedAtToCreatedAt::class),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
    ],
)
@ColumnTypeConverters(ColumnConverters::class)
@ConstructedBy(VantageDatabaseConstructor::class)
abstract class VantageDatabase : RoomDatabase() {
    abstract fun events(): EventDao

    abstract fun tags(): TagDao

    abstract fun credentials(): CredentialDao

    abstract fun markers(): MarkerDao

    abstract fun periods(): PeriodDao

    abstract fun hiddenCalendarEvents(): HiddenCalendarEventDao

    abstract fun sync(): SyncDao

    abstract fun pendingUploads(): PendingUploadDao
}

@Suppress("KotlinNoActualForExpect")
expect object VantageDatabaseConstructor : RoomDatabaseConstructor<VantageDatabase> {
    override fun initialize(): VantageDatabase
}
