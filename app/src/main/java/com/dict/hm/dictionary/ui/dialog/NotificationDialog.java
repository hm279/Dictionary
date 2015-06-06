package com.dict.hm.dictionary.ui.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.dict.hm.dictionary.R;

/**
 * Created by hm on 15-4-7.
 */
public class NotificationDialog extends DialogFragment{
    private static String TXT = "text";

    public static NotificationDialog newInstance(String text) {
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
        View view = getActivity().getLayoutInflater().inflate(R.layout.snackbar, null);
        TextView textView =(TextView) view.findViewById(R.id.snackbar_text);
        textView.setText(text);
        builder.setView(view);

        Dialog dialog = builder.create();
        //set dialog's style
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.dimAmount = 0.0f;
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(layoutParams);
        return dialog;
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
