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

package com.shalzz.attendance.data.remote;

import android.content.Context;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.DisposableObserver;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.List;

public class RxJava2ErrorCallAdapterFactory extends CallAdapter.Factory {
    private final RxJava2CallAdapterFactory original;
    private final Context mContext;

    private RxJava2ErrorCallAdapterFactory(Context context) {
        mContext = context;
        original = RxJava2CallAdapterFactory.create();
    }

    public static CallAdapter.Factory create(Context context) {
        return new RxJava2ErrorCallAdapterFactory(context);
    }

    @Override
    public CallAdapter get(final Type returnType, final Annotation[] annotations, final Retrofit retrofit) {
        return new RxCallAdapterWrapper(retrofit,
                original.get(returnType, annotations,
                retrofit), mContext);
    }

    private static class RxCallAdapterWrapper implements CallAdapter {
        private final Retrofit retrofit;
        private final CallAdapter wrapped;
        private final Context context;

        RxCallAdapterWrapper(Retrofit retrofit, final CallAdapter wrapped, Context context) {
            this.retrofit = retrofit;
            this.wrapped = wrapped;
            this.context = context;
        }

        @Override
        public Type responseType() {
            return wrapped.responseType();
        }

        @SuppressWarnings({"unchecked", "NullableProblems"})
        @Override
        public Object adapt(Call call)
        {
            Object adaptedCall = wrapped.adapt(call);

            if (adaptedCall instanceof Completable) {
                return ((Completable) adaptedCall).onErrorResumeNext(
                        throwable -> Completable.error(asRetrofitException(throwable)));
            }

            if (adaptedCall instanceof Single) {
                return ((Single) adaptedCall).onErrorResumeNext(
                        throwable -> Single.error(asRetrofitException((Throwable) throwable)));
            }

            if (adaptedCall instanceof Observable) {
                return Observable.create(source -> {
                    if (source.isDisposed()) return;

                    ((Observable) adaptedCall)
                            .subscribeWith(new DisposableObserver() {
                                @Override
                                public void onNext(Object object) {
                                    if (object instanceof List && ((List)object).isEmpty() ) {
                                        source.tryOnError(RetrofitException.
                                                        emptyResponseError(retrofit, context));
                                    } else {
                                        source.onNext(object);
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {
                                    source.tryOnError(asRetrofitException(e));
                                }

                                @Override
                                public void onComplete() {
                                    source.onComplete();
                                }
                            });

                });
            }

            throw new RuntimeException("Observable Type not supported");
        }

        private RetrofitException asRetrofitException(Throwable throwable) {
            // We had non-200 http error
            if (throwable instanceof HttpException) {
                HttpException httpException = (HttpException) throwable;
                Response response = httpException.response();
                return RetrofitException.httpError(response.raw().request().url().toString(),
                        response, retrofit, context);
            }
            // A network error happened
            if (throwable instanceof SocketTimeoutException) {
                return RetrofitException.timeoutError((IOException) throwable, context);
            }

            if (throwable instanceof IOException) {
                return RetrofitException.networkError((IOException) throwable, context);
            }

            // We don't know what happened. We need to simply convert to an unknown error

            return RetrofitException.unexpectedError(throwable, context);
        }
    }
}

