package com.cerner.cura.ui.elements;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cerner.cura.datamodel.common.PatientListPatient;
import com.cerner.cura.demographics.utils.DemogUtils;
import com.cerner.cura.utils.AppUtils;
import com.cerner.cura.utils.TimeCalculator;
import com.cerner.nursing.nursing.R;

/**
 * The patient item for the patient list
 *
 * @author Mark Lear (ML015922)
 * @author Brad Barnhill (BB024928)
 */
public class PatientListItem extends BaseListItem<PatientListPatient> {
    private boolean mIsNameAlert;

    public PatientListItem(@NonNull final PatientListPatient patient) {
        super(patient.nameFullFormatted, patient);
    }

    public void setNameAlert(final boolean value) {
        mIsNameAlert = value;
    }

    @Override
    public IItem.ViewHolder createViewHolder(final LayoutInflater inflater, final ViewGroup parent) throws IllegalArgumentException, NullPointerException {
        return new ViewHolder(inflater.inflate(R.layout.patient_list_patient, parent, false));
    }

    @Override
    public void bindViewHolder(final IItem.ViewHolder holder) {
        final Context context = holder.itemView.getContext();
        if (context == null) {
            return;
        }

        final ViewHolder viewHolder = (ViewHolder) holder;
        final PatientListPatient patient = getData();

        viewHolder.mAlertView.setVisibility(mIsNameAlert ? View.VISIBLE : View.GONE);

        viewHolder.mNameView.setText(AppUtils.defaultField(context, mIsNameAlert ? context.getString(R.string.name_alert_ind, mTitle) : mTitle));

        viewHolder.mNoRelationshipView.setVisibility(patient.relationshipInd ? View.GONE : View.VISIBLE);

        if (patient.relationshipInd && !TextUtils.isEmpty(patient.ageDisplay)) {
            //TODO bb024928: (CURA-1082) also the age should be a structure passed back and call into TimeCalculator.buildPatientAgeString (currently limited by services that only return an age string)
            viewHolder.mAgeView.setText(AppUtils.formatTextFromHTML(DemogUtils.formatAge(context, patient.ageDisplay)));
            viewHolder.mAgeView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mAgeView.setVisibility(View.GONE);
        }

        if (patient.relationshipInd && !TextUtils.isEmpty(patient.sexAbbr)) {
            viewHolder.mSexView.setText(patient.sexAbbr);
            viewHolder.mSexView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mSexView.setVisibility(View.GONE);
        }

        viewHolder.mDobAbrevView.setVisibility(patient.relationshipInd ? View.VISIBLE : View.GONE);

        final String dateString;
        if (patient.dateOfBirth == null) {
            dateString = "";
        } else {
            dateString = TimeCalculator.getDateString(context, patient.dateOfBirth.dateTime, patient.dateOfBirth.originalTimeZone, patient.dateOfBirth.precision);
        }
        viewHolder.mDobView.setText(AppUtils.defaultField(context, dateString));
        viewHolder.mDobView.setVisibility(patient.relationshipInd ? View.VISIBLE : View.GONE);

        String roomBedText = "";    //Neither room nor bed present
        if (patient.roomLocationDisplay != null && !patient.roomLocationDisplay.trim().isEmpty()) {
            if (patient.bedLocationDisplay != null && !patient.bedLocationDisplay.trim().isEmpty()) {
                //both room and bed present
                roomBedText = context.getString(R.string.patientlist_room_bed_format, patient.roomLocationDisplay, patient.bedLocationDisplay);
            } else {
                //only room present
                roomBedText = patient.roomLocationDisplay;
            }
        } else if (patient.bedLocationDisplay != null && !patient.bedLocationDisplay.trim().isEmpty()) {
            //only bed present
            roomBedText = patient.bedLocationDisplay;
        }

        viewHolder.mRoomBedView.setText(roomBedText);

        switch (PatientListPatient.REVIEW_INDICATOR.values()[patient.reviewIndicator]) {
            case CRITICAL:
                viewHolder.mNotificationImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.icon_critical_alert));
                break;
            case NON_CRITICAL:
                viewHolder.mNotificationImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.icon_non_critical_alert));
                break;
            default:
                viewHolder.mNotificationImageView.setImageDrawable(null);
        }

        viewHolder.mChevron.setVisibility(isItemClickable() && mShowChevron ? View.VISIBLE : View.GONE);

        super.bindViewHolder(viewHolder);
    }

    static class ViewHolder extends IItem.ViewHolder {
        final TextView mNameView;
        final TextView mAlertView;
        final TextView mNoRelationshipView;
        final TextView mAgeView;
        final TextView mSexView;
        final TextView mDobAbrevView;
        final TextView mDobView;
        final TextView mRoomBedView;
        final ImageView mNotificationImageView;
        final ImageView mChevron;

        public ViewHolder(final View root) throws IllegalArgumentException, NullPointerException {
            super(root);

            mNameView = root.findViewById(R.id.nameView);
            mAlertView = root.findViewById(R.id.alertView);
            mNoRelationshipView = root.findViewById(R.id.noRelationshipView);
            mAgeView = root.findViewById(R.id.ageView);
            mSexView = root.findViewById(R.id.sexView);
            mDobAbrevView = root.findViewById(R.id.dobAbrevView);
            mDobView = root.findViewById(R.id.dobView);
            mRoomBedView = root.findViewById(R.id.roomBedView);
            mNotificationImageView = root.findViewById(R.id.patientNotificationImageView);
            mChevron = root.findViewById(R.id.chevron);

            if (mNameView == null || mAlertView == null || mNoRelationshipView == null || mAgeView == null
                || mSexView == null || mDobAbrevView == null || mDobView == null || mRoomBedView == null
                || mNotificationImageView == null || mChevron == null) {
                throw new NullPointerException("ViewHolder failed to find all views");
            }
        }
    }
}
