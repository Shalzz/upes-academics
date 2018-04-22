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

package com.shalzz.attendance.data.remote;

import com.shalzz.attendance.data.model.Period;
import com.shalzz.attendance.data.model.Subject;
import com.shalzz.attendance.data.model.User;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DataAPI {

    String ENDPOINT = "https://academics.8bitlabs.in/api/v1/";
//    String ENDPOINT = "http://192.168.1.160:8080/api/v1/";

    @GET("me")
    Observable<User> getUser(@Header("Authorization") String authorization);

    @GET("me/attendance")
    Observable<List<Subject>> getAttendance();

    @GET("me/timetable/{date}")
    Observable<List<Period>> getTimetable(@Path("date") String date);

    @GET("verify")
    Observable<Boolean> verifyValidSignature(@Query("data") String signedData,
                                             @Query("sig") String signature);
}
