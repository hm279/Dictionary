package com.dict.hm.dictionary;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by hm on 15-4-7.
 */
public class NotificationDialog extends DialogFragment{
    private static String TXT = "text";

    static NotificationDialog newInstance(String text) {
        NotificationDialog dialogFragment = new NotificationDialog();
        Bundle bundle = new Bundle();
        bundle.putString(TXT, text);
        dialogFragment.setArguments(bundle);

        return dialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        String text = bundle.getString(TXT);
        if (text == null || text.equals("")) {
            text = "HAVE A GOOD DAY!";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setMessage(text);
        TextView view =(TextView) getActivity().getLayoutInflater().inflate(R.layout.snackbar, null);
        view.setText(text);
        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        //set dialog's style
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.gravity = Gravity.BOTTOM | Gravity.START;
        layoutParams.dimAmount = 0.0f;
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(layoutParams);
//        window.setLayout();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (fab != null) {
            fab.animate().translationYBy(y);
        }
        super.onDismiss(dialog);
    }

    public void setFab(ImageView fab, float y) {
        this.fab = fab;
        this.y = y;
    }
    ImageView fab = null;
    float y = 0;

}
