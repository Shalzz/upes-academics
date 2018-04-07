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

package com.shalzz.attendance.injection.component;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.data.DataManager;
import com.shalzz.attendance.data.local.DatabaseHelper;
import com.shalzz.attendance.data.local.DbOpenHelper;
import com.shalzz.attendance.injection.ApplicationContext;
import com.shalzz.attendance.injection.module.ApplicationModule;
import com.shalzz.attendance.injection.module.NetworkModule;
import com.shalzz.attendance.data.remote.DataAPI;
import com.shalzz.attendance.sync.SyncService;
import com.shalzz.attendance.ui.settings.AboutSettingsFragment;
import com.shalzz.attendance.ui.settings.SettingsFragment;
import com.shalzz.attendance.data.local.PreferencesHelper;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ApplicationModule.class, NetworkModule.class})
public interface ApplicationComponent {

    @ApplicationContext
    Context context();
    Application application();
    @Named("app")
    Tracker tracker();
    DataAPI dataApi();
    PreferencesHelper preferenceManager();
    DbOpenHelper databaseHandler();
    DatabaseHelper databaseHelper();
    DataManager dataManager();

    void inject(SyncService syncService);

    void inject(SettingsFragment settingsFragment);

    void inject(AboutSettingsFragment aboutSettingsFragment);
}