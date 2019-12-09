package com.cerner.cura.ui.elements;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cerner.cura.base.UserContext;
import com.cerner.cura.demographics.datamodel.PatientDemogBanner;
import com.cerner.cura.demographics.utils.DemogUtils;
import com.cerner.cura.utils.AppUtils;
import com.cerner.cura.utils.TimeCalculator;
import com.cerner.nursing.nursing.R;

/**
 * The patient item for the drawer
 *
 * @author Brad Barnhill (BB024928)
 * @author Mark Lear (ML015922)
 */
public class DrawerPatientListItem extends BaseListItem<PatientDemogBanner> implements IDrawerParent {
    private static final String PATIENT_MODULES_EXPANDED_STORAGE_ID = "PATIENT_MODULES_EXPANDED_STORAGE_ID";
    private boolean mExpanded = true;

    public DrawerPatientListItem(final PatientDemogBanner patient) {
        super(patient == null ? null : patient.name_full_formatted, patient);

        if (UserContext.isContextStorageObjectSet(PATIENT_MODULES_EXPANDED_STORAGE_ID, boolean.class)) {
            setExpanded(UserContext.getContextStorageObject(PATIENT_MODULES_EXPANDED_STORAGE_ID, boolean.class));
        }
    }

    @Override
    public void setData(final PatientDemogBanner model) {
        if (model != null) {
            super.setTitle(model.name_full_formatted);
        } else {
            super.setTitle(null);
        }

        super.setData(model);
    }

    @Override
    public IItem.ViewHolder createViewHolder(final LayoutInflater inflater, final ViewGroup parent) throws IllegalArgumentException, NullPointerException {
        final View view = inflater.inflate(R.layout.drawer_patient, parent, false);

        if (view == null) {
            throw new NullPointerException("View was not inflated");
        }

        return new ViewHolder(view);
    }

    @Override
    public void bindViewHolder(final IItem.ViewHolder holder) {
        final Context context = holder.itemView.getContext();
        if (context == null) {
            return;
        }

        final ViewHolder viewHolder = (ViewHolder) holder;

        //No patient in context
        if (getData() == null) {
            viewHolder.mNameView.setText(context.getString(R.string.patient_not_selected));
            viewHolder.mNameView.setAlpha(0.5f);
            viewHolder.mPatientInfoView.setVisibility(View.GONE);
            return;
        }

        //Patient in context
        final PatientDemogBanner patient = getData();

        viewHolder.mNameView.setText(getTitle());
        viewHolder.mNameView.setAlpha(1.0f);

        viewHolder.mPatientInfoView.setVisibility(View.VISIBLE);
        viewHolder.mAgeView.setText(TextUtils.isEmpty(patient.age_display) ? AppUtils.defaultField(context, null) : AppUtils.formatTextFromHTML(DemogUtils.formatAge(context, patient.age_display)));
        viewHolder.mSexView.setText(patient.gender_display);

        final String dateString;
        if (patient.birth_date == null) {
            dateString = "";
        } else {
            dateString = TimeCalculator.getDateString(context, patient.birth_date.dateTime, patient.birth_date.originalTimeZone, patient.birth_date.precision);
        }
        viewHolder.mDobView.setText(AppUtils.defaultField(context, dateString));

        //change the drop down indicator based on expansion
        viewHolder.mDropDownInd.setImageDrawable(ContextCompat.getDrawable(context, (mExpanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down)));

        super.bindViewHolder(holder);
    }

    @Override
    public boolean isExpanded() {
        return mExpanded;
    }

    public void setExpanded(final boolean expanded) {
        mExpanded = expanded;
    }

    @Override
    public void onClick() {
        mExpanded = !mExpanded;
        UserContext.putContextStorageObject(PATIENT_MODULES_EXPANDED_STORAGE_ID, mExpanded);
    }

    static class ViewHolder extends IItem.ViewHolder {
        final TextView mNameView;
        final TextView mAgeView;
        final TextView mSexView;
        final TextView mDobView;
        final LinearLayout mPatientInfoView;
        final ImageView mDropDownInd;

        public ViewHolder(final View root) throws IllegalArgumentException, NullPointerException {
            super(root);

            mNameView = root.findViewById(R.id.nameView);
            mAgeView = root.findViewById(R.id.ageView);
            mSexView = root.findViewById(R.id.sexView);
            mDobView = root.findViewById(R.id.dobView);
            mPatientInfoView = root.findViewById(R.id.patientInfoView);
            mDropDownInd = root.findViewById(R.id.dropDownInd);

            if (mNameView == null || mAgeView == null || mSexView == null || mDobView == null || mPatientInfoView == null || mDropDownInd == null) {
                throw new NullPointerException("ViewHolder failed to find all views");
            }
        }
    }
}
