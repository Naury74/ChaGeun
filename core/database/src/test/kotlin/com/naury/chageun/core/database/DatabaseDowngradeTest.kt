package com.naury.chageun.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.database.di.DatabaseModule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 더 새 버전 앱이 만든 DB를 옛 앱이 열면 지우고 새로 만들지 않고 실패해야 한다(CICD §9.2 Downgrade 차단).
 * 실제 앱과 같은 빌더 설정을 쓰려고 DatabaseModule로 연다.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseDowngradeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun openingNewerDatabase_failsAndKeepsData() = runTest {
        val current = DatabaseModule.provideDatabase(context)
        current.vehicleDao().upsert(vehicle())
        current.close()
        val path = context.getDatabasePath(ChageunDatabase.NAME).path
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.version = ChageunDatabase.VERSION + 1
        }

        val older = DatabaseModule.provideDatabase(context)
        val error = assertThrows(IllegalStateException::class.java) { older.openHelper.writableDatabase }
        older.close()

        assertThat(error).hasMessageThat().contains("migration")
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertThat(db.version).isEqualTo(ChageunDatabase.VERSION + 1)
            db.rawQuery("SELECT id FROM vehicle", null).use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("vehicle-1")
            }
        }
    }
}
