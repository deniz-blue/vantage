package blue.deniz.vantage.storage

import androidx.room3.RenameColumn
import androidx.room3.migration.AutoMigrationSpec

@RenameColumn(tableName = "events", fromColumnName = "addedAt", toColumnName = "createdAt")
class RenameEventAddedAtToCreatedAt : AutoMigrationSpec
