package com.shalzz.attendance

import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.local.DatabaseHelper
import com.shalzz.attendance.data.local.PreferencesHelper
import com.shalzz.attendance.data.model.entity.Period
import com.shalzz.attendance.data.model.entity.Subject
import com.shalzz.attendance.data.model.entity.User
import com.shalzz.attendance.data.remote.DataAPI
import com.shalzz.attendance.util.RxSchedulersOverrideRule
import com.shalzz.attendance.wrapper.DateHelper
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.util.Arrays
import java.util.Date

/**
 * This test class performs local unit tests without dependencies on the Android framework
 * For testing methods in the DataManager follow this approach:
 * 1. Stub mock helper classes that your method relies on. e.g. RetrofitServices or DatabaseHelper
 * 2. Test the Observable using TestSubscriber
 * 3. Optionally write a SEPARATE test that verifies that your method is calling the right helper
 * using Mockito.verify()
 */
@RunWith(MockitoJUnitRunner::class)
class DataManagerTest {

    @Rule
    @JvmField
    val mOverrideSchedulersRule = RxSchedulersOverrideRule()

    @Mock
    internal lateinit var mMockDatabaseHelper: DatabaseHelper
    @Mock
    internal lateinit var mMockPreferencesHelper: PreferencesHelper
    @Mock
    internal lateinit var mMockDataAPI: DataAPI

    private lateinit var mDataManager: DataManager
    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mDataManager = DataManager(mMockDataAPI, mMockDatabaseHelper, mMockPreferencesHelper)
    }

    @Test
    fun syncUserEmitsValues() {
        val user = TestDataFactory.makeUser("u1")
        stubSyncUserHelperCalls(user)

        val result = TestObserver<User>()
        mDataManager.syncUser().subscribe(result)
        result.assertNoErrors()
        result.assertValue(user)
    }

    @Test
    fun syncAttendanceEmitsValues() {
        val subjects = Arrays.asList(TestDataFactory.makeSubject("s1"),
                TestDataFactory.makeSubject("s2"))
        stubSyncAttendanceHelperCalls(subjects)

        val result = TestObserver<Subject>()
        mDataManager.syncAttendance().subscribe(result)
        result.assertNoErrors()
        result.assertValueSequence(subjects)
    }

    @Test
    fun syncDayEmitsValues() {
        val day = Date()
        val periods = Arrays.asList(TestDataFactory.makePeriod("p1", day),
                TestDataFactory.makePeriod("p2", day))
        stubSyncDayHelperCalls(day, periods)

        val result = TestObserver<Period>()
        mDataManager.syncDay(day).subscribe(result)
        result.assertNoErrors()
        result.assertValueSequence(periods)
    }

    @Test
    fun syncUserCallsApiAndDatabase() {
        val user = TestDataFactory.makeUser("u1")
        stubSyncUserHelperCalls(user)

        mDataManager.syncUser().subscribe()
        // Verify right calls to helper methods
        verify<DataAPI>(mMockDataAPI).getUser()
        verify<DatabaseHelper>(mMockDatabaseHelper).setUser(user)
    }

    @Test
    fun syncAttendanceCallsApiAndDatabase() {
        val subjects = Arrays.asList(TestDataFactory.makeSubject("s1"),
                TestDataFactory.makeSubject("s2"))
        stubSyncAttendanceHelperCalls(subjects)

        mDataManager.syncAttendance().subscribe()
        // Verify right calls to helper methods
        verify<DataAPI>(mMockDataAPI).attendance
        verify<DatabaseHelper>(mMockDatabaseHelper).setSubjects(subjects)
    }

    @Test
    fun syncDayCallsApiAndDatabase() {
        val day = Date()
        val periods = Arrays.asList(TestDataFactory.makePeriod("p1", day),
                TestDataFactory.makePeriod("p2", day))
        stubSyncDayHelperCalls(day, periods)

        mDataManager.syncDay(day).subscribe()
        // Verify right calls to helper methods
        verify<DataAPI>(mMockDataAPI).getTimetable(DateHelper.toTechnicalFormat(day))
        verify<DatabaseHelper>(mMockDatabaseHelper).setPeriods(periods)
    }

    @Test
    fun syncUserDoesNotCallDatabaseWhenApiFails() {
        `when`(mMockDataAPI.getUser())
                .thenReturn(Observable.error(RuntimeException()))

        val result = TestObserver<User>()
        mDataManager.syncUser().subscribe(result)
        // Verify right calls to helper methods
        verify<DataAPI>(mMockDataAPI).getUser()
        result.assertNoValues()
        verify<DatabaseHelper>(mMockDatabaseHelper, never()).setUser(any(User::class.java))
    }

    @Test
    fun syncAttendanceDoesNotCallDatabaseWhenApiFails() {
        `when`(mMockDataAPI.attendance)
                .thenReturn(Observable.error(RuntimeException()))

        mDataManager.syncAttendance().subscribe(TestObserver())
        // Verify right calls to helper methods
        verify<DataAPI>(mMockDataAPI).attendance
        verify<DatabaseHelper>(mMockDatabaseHelper, never()).setSubjects(ArgumentMatchers.anyList())
    }

    @Test
    fun syncDayDoesNotCallDatabaseWhenApiFails() {
        val day = Date()
        `when`(mMockDataAPI.getTimetable(DateHelper.toTechnicalFormat(day)))
                .thenReturn(Observable.error(RuntimeException()))

        mDataManager.syncDay(day).subscribe(TestObserver())
        // Verify right calls to helper methods
        verify<DataAPI>(mMockDataAPI).getTimetable(DateHelper.toTechnicalFormat(day))
        verify<DatabaseHelper>(mMockDatabaseHelper, never()).setPeriods(ArgumentMatchers.anyList())
    }

    private fun stubSyncUserHelperCalls(user: User) {
        // Stub calls to the DataAPI service and database helper.
        `when`(mMockDataAPI.getUser())
                .thenReturn(Observable.just(user))
        `when`(mMockDatabaseHelper.setUser(user))
                .thenReturn(Observable.just(user))
    }

    private fun stubSyncAttendanceHelperCalls(subjects: List<Subject>) {
        // Stub calls to the DataAPI service and database helper.
        `when`(mMockDataAPI.attendance)
                .thenReturn(Observable.just(subjects))
        `when`(mMockDatabaseHelper.setSubjects(subjects))
                .thenReturn(Observable.fromIterable(subjects))
    }

    private fun stubSyncDayHelperCalls(date: Date, periods: List<Period>) {
        // Stub calls to the DataAPI service and database helper.
        `when`(mMockDataAPI.getTimetable(DateHelper.toTechnicalFormat(date)))
                .thenReturn(Observable.just(periods))
        `when`(mMockDatabaseHelper.setPeriods(periods))
                .thenReturn(Observable.fromIterable(periods))
    }

}