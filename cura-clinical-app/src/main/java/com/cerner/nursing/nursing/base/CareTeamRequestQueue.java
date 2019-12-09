package com.cerner.nursing.nursing.base;

import android.support.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.cerner.cura.requestor.CuraRequest;
import com.cerner.cura.requestor.JsonRequestor;
import com.cerner.cura.requestor.MultiResponseListener;
import com.cerner.ion.log.Logger;
import com.cerner.ion.request.security.IonRequestContext;

/**
 * Cura version of request queue to call into JsonRequestor when adding request to the application queue.
 *
 * @author Lam Tran (lt028506).
 */
public class CareTeamRequestQueue extends RequestQueue {
    private final IonRequestContext mIonRequestContext;
    private static final String TAG = CareTeamRequestQueue.class.getSimpleName();

    public CareTeamRequestQueue(final @NonNull IonRequestContext ionRequestContext) {
        super(null, null);
        mIonRequestContext = ionRequestContext;
    }

    @Override
    public <T> Request<T> add(final Request<T> request) {
        //Don't send a request because we failed to create one or we already sent a cached response.
        if(request == null){
            Logger.d(TAG, "Trying to add a null request to the queue");
            return null;
        }

        if (request instanceof CuraRequest) {
            final CuraRequest<T> curaRequest = (CuraRequest<T>) request;
            //NOTE: request isn't really a Request<T> as it is a CuraRequest<T> which extends Request<CernResponse<T>> so we get away with this due to not caring what the return type is
            //noinspection unchecked
            if (curaRequest.getCernResponseListener() instanceof MultiResponseListener) {
                //noinspection unchecked
                return (Request) JsonRequestor.sendRequest((MultiResponseListener<T>) curaRequest.getCernResponseListener(), curaRequest, mIonRequestContext);
            } else {
                throw new IllegalArgumentException("CuraRequest doesn't contain MultiResponseListener");
            }
        } else {
            throw new IllegalArgumentException("Request is not type CuraRequest");
        }
    }
}
