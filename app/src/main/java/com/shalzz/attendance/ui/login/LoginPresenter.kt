/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
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

package com.shalzz.attendance.ui.login

import android.content.Context
import com.shalzz.attendance.R
import com.shalzz.attendance.data.DataManager
import com.shalzz.attendance.data.remote.RetrofitException
import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.injection.ConfigPersistent
import com.shalzz.attendance.ui.base.BasePresenter
import com.shalzz.attendance.utils.NetworkUtil
import com.shalzz.attendance.utils.RxUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@ConfigPersistent
class LoginPresenter @Inject
internal constructor(private val mDataManager: DataManager,
                     @param:ApplicationContext private val mContext: Context
) : BasePresenter<LoginMvpView>() {

    private var mDisposable: Disposable? = null

    @Suppress("RedundantOverride")
    override fun attachView(mvpView: LoginMvpView) {
        super.attachView(mvpView)
    }

    override fun detachView() {
        super.detachView()
        RxUtil.dispose(mDisposable)
    }

    fun login(username: String, password: String, college: String, captcha: String = "default",
              cookie: String = "") {
        checkViewAttached()
        if (!NetworkUtil.isNetworkConnected(mContext)) {
            Timber.i("Login canceled, connection not available")
            mvpView.showError(mContext.getString(R.string.no_internet))
            return
        }

        val onError = { error : Throwable ->
            if (error !is RetrofitException) {
                Timber.e(error)
            }
            else if (isViewAttached) {
                if (error.kind != RetrofitException.Kind.HTTP) {
                    Timber.e(error)
                    mvpView.showError(error.message)
                } else {
                    val res = JSONObject(error.response.errorBody()!!.string())

                    when {
                        !res.isNull("checkcaptcha")
                                && res.getString("checkcaptcha") == "1" -> {
                            when {
                                captcha == "default" -> mvpView.showCaptchaDialog()
                                res.has("error") -> mvpView.showError(res.getString("error"))
                                else -> mvpView.showError(error.message)
                            }
                        }
                        else -> mvpView.showError(error.message)
                    }
                }
            }
        }

        mvpView.showProgressDialog()
        RxUtil.dispose(mDisposable)

       mDisposable = mDataManager.login(username, password, college, captcha, cookie)
                .doOnNext { token ->
                    mvpView.saveToken(username, college, token.token) }
                .flatMap { mDataManager.syncUser() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { user ->
                            mvpView.showMainActivity(user, password)
                        }, onError)
    }

    fun loadColleges() {
        checkViewAttached()
        mvpView.showProgressDialog("Loading...")
        mDisposable = mDataManager.colleges()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( {
                    mvpView.updateCollegeList(it)
                    mvpView.dismissProgressDialog()
                }, {error ->
                    if (error !is RetrofitException) {
                        mvpView.showError(null)
                        Timber.e(error)
                    }
                    else if (isViewAttached) {
                        mvpView.showError(error.message)
                        if (error.kind != RetrofitException.Kind.HTTP) {
                            Timber.e(error)
                        }
                    }
                } )
    }
}
