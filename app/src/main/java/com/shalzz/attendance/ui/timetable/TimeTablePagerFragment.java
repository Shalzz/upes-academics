/*
 * Copyright (c) 2013-2016 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of UPES Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shalzz.attendance.ui.timetable;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bugsnag.android.Bugsnag;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.shalzz.attendance.R;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.utils.RxEventBus;
import com.shalzz.attendance.wrapper.DateHelper;

import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnPageChange;
import butterknife.Unbinder;

public class TimeTablePagerFragment extends Fragment implements TimeTableMvpView {

    @BindView(R.id.pager)
    public ViewPager mViewPager;

    @Inject @Named("app")
    FirebaseAnalytics mTracker;

    @Inject
    TimeTablePresenter mTimeTablePresenter;

    @Inject
    AppCompatActivity mActivity;

    @Inject
    RxEventBus eventBus;

    private int mPreviousPosition = 15;
    private TimeTablePagerAdapter mAdapter;
    private Context mContext;
    private ActionBar actionbar;
    private Unbinder unbinder;

    @Override
    public void onStart() {
        super.onStart();
        mTracker.setCurrentScreen(mActivity, getClass().getSimpleName(), getClass().getSimpleName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null)
            return null;
        mContext = getActivity();
        Bugsnag.setContext("Timetable");
        ((MainActivity) getActivity()).activityComponent().inject(this);

        setHasOptionsMenu(true);
        setRetainInstance(false);
        final View view = inflater.inflate(R.layout.fragment_viewpager, container, false);
        unbinder = ButterKnife.bind(this, view);
        mTimeTablePresenter.attachView(this);

        actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        mAdapter = new TimeTablePagerAdapter(getChildFragmentManager(),
                mActivity,
                position -> mViewPager.setCurrentItem(position, true),
                eventBus);

        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter.scrollToToday();
    }

    @Override
    public void onResume() {
        super.onResume();
        showcaseView();
    }

    public void showcaseView() {
        final ShowcaseView sv = new ShowcaseView.Builder(getActivity())
                .setStyle(R.style.ShowcaseTheme)
                .setTarget(Target.NONE)
                .singleShot(3333)
                .doNotBlockTouches()
                .setContentTitle(getString(R.string.sv_timetable_title))
                .setContentText(getString(R.string.sv_timetable_content))
                .build();

        sv.overrideButtonClick(v -> sv.hide());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.time_table, menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateTitle(-1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         if (item.getItemId() == R.id.menu_date) {
            Calendar today = Calendar.getInstance();
            today.setTime(new Date());
            DatePickerDialog mDatePickerDialog = new DatePickerDialog(mContext, onDateSetListener()
                    , today.get(Calendar.YEAR)
                    , today.get(Calendar.MONTH)
                    , today.get(Calendar.DAY_OF_MONTH));
            mDatePickerDialog.show();

             Bundle bundle = new Bundle();
             bundle.putString(FirebaseAnalytics.Param.ITEM_ID, item.getTitle().toString());
             bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Scroll to Date");
             bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "menu");
             mTracker.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
             return true;
        } else if (item.getItemId() == R.id.menu_today) {
            setDate(new Date());

             Bundle bundle = new Bundle();
             bundle.putString(FirebaseAnalytics.Param.ITEM_ID, item.getTitle().toString());
             bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Scroll to Today");
             bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "menu");
             mTracker.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
             return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update action bar title and subtitle
     *
     * @param position to update for, -1 for current page
     */
    @OnPageChange(R.id.pager)
    public void updateTitle(int position) {
        if (position != -1)
            mPreviousPosition = position;
        Date mDate = mAdapter.getDateForPosition(mPreviousPosition);
        if (mDate != null) {
            actionbar.setTitle(DateHelper.getProperWeekday(mDate));
            actionbar.setSubtitle(DateHelper.toProperFormat(mDate));
        }
    }

    DatePickerDialog.OnDateSetListener onDateSetListener() {
        return (view, year, monthOfYear, dayOfMonth) -> {
            Calendar date = Calendar.getInstance();
            date.set(year, monthOfYear, dayOfMonth);
            setDate(date.getTime());
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        mTimeTablePresenter.detachView();
        mAdapter.destroy();
    }

    /******* MVP View methods implementation *****/

    @Override
    public void setDate(Date date) {
        mAdapter.setDate(date);
        mAdapter.scrollToDate(date);
        updateTitle(-1);
    }

}
