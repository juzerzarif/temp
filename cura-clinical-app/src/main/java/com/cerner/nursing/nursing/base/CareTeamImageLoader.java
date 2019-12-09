package com.cerner.nursing.nursing.base;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cerner.careaware.connect.contacts.requesters.AvatarImageLoader;
import com.cerner.cura.base.CuraAuthnActivity;
import com.cerner.cura.datamodel.CareTeamPhotoRequest;
import com.cerner.cura.datamodel.CareTeamPhotoResponse;
import com.cerner.cura.requestor.CuraResponseListener;
import com.cerner.cura.requestor.IDataRetriever;
import com.cerner.cura.requestor.JsonRequestor;
import com.cerner.cura.utils.RequestorUtils;
import com.cerner.ion.request.CernRequest;
import com.cerner.ion.request.CernResponse;

/**
 * ImageLoader that uses the application's IonRequestQueue, and the application's instance of AvatarCache.
 *
 * @author Lam Tran (lt028506)
 */
public class CareTeamImageLoader extends AvatarImageLoader {
    private final CuraAuthnActivity mCuraAuthnActivity;
    private static final int IMAGE_CACHE_TIMEOUT = 86400;
    private static final String CHECKPOINT_CARETEAM_AVATAR_READ = "CURA_NURSING_CARETEAM_AVATAR_READ";
    private final String TAG = CareTeamImageLoader.class.getSimpleName();

    public CareTeamImageLoader(final CuraAuthnActivity curaAuthnActivity) {
        super(new CareTeamRequestQueue(curaAuthnActivity));
        mCuraAuthnActivity = curaAuthnActivity;
    }

    /**
     * Create the request to retrieve the avatar URL.
     */
    @Override
    public Request<?> createRequest(final String requestUrl, final Response.Listener<Bitmap> responseListener, final int maxWidth, final int maxHeight, final Bitmap.Config bitmapConfig,
                                    final Response.ErrorListener errorListener) {
        final IDataRetriever dataRetriever = new IDataRetriever() {
            @Override
            public void getData(final DataArgs dataArgs) {
            }

            @Override
            public void onResponse(@NonNull final CernResponse response) {
                responseListener.onResponse(((CareTeamPhotoResponse) response.data).image);
            }

            @Override
            public void onNoContentResponse(@NonNull final CernResponse cernResponse, final Class clazz) {
            }

            @Override
            public void onErrorResponse(@NonNull final VolleyError volleyError, final Class clazz) {
                errorListener.onErrorResponse(volleyError);
            }

            @Override
            public void onFailedResponse(final Class clazz, final boolean cacheReturned) {
            }

            @Override
            public void setActionBarWaitCursor(final boolean isVisible) {
            }

            @Override
            public boolean cancelRequest(@NonNull final CernRequest cernRequest) {
                return false;
            }

            @Override
            public boolean backgroundAfterCancel(@NonNull final CernRequest request) {
                return true;
            }

            @Override
            public void setRequestMethod(final int requestMethod, final String tag) {
            }
        };
        final CuraResponseListener<CareTeamPhotoResponse> curaResponseListener = new CuraResponseListener<>(
                null, TAG, CHECKPOINT_CARETEAM_AVATAR_READ, CareTeamPhotoResponse.class, dataRetriever, mCuraAuthnActivity, true);

        // Prepare the request but not adding to the queue yet
        return JsonRequestor.createRequest(RequestorUtils.getCustomGson(), curaResponseListener, mCuraAuthnActivity, IMAGE_CACHE_TIMEOUT, false,
                                           null, new CareTeamPhotoRequest(requestUrl), true);
    }
}
