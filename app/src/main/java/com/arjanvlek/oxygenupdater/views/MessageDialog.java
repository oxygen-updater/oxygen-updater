package com.arjanvlek.oxygenupdater.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;

/**
 * Usage: Title text, Message text, Positive button text, Negative button text.
 */
public class MessageDialog extends DialogFragment {
    private DialogListener dialogListener;

    private String title;
    private String message;
    private String positiveButtonText;
    private String negativeButtonText;
    private boolean closable;


    public interface DialogListener {
        void onDialogPositiveButtonClick(DialogFragment dialogFragment);
        void onDialogNegativeButtonClick(DialogFragment dialogFragment);
    }

    public MessageDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    public MessageDialog setMessage(String message) {
        this.message = message;
        return this;
    }

    public MessageDialog setDialogListener(DialogListener listener) {
        dialogListener = listener;
        return this;
    }

    public MessageDialog setPositiveButtonText(String positiveButtonText) {
        this.positiveButtonText = positiveButtonText;
        return this;
    }

    public MessageDialog setNegativeButtonText(String negativeButtonText) {
        this.negativeButtonText = negativeButtonText;
        return this;
    }

    public MessageDialog setClosable(boolean closable) {
        this.closable = closable;
        return this;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(title);
        builder.setMessage(message);

        if(negativeButtonText != null) {
            builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(dialogListener != null) {
                        dialogListener.onDialogNegativeButtonClick(MessageDialog.this);
                    }
                    dismiss();
                }
            });
        }
        if(positiveButtonText != null) {
            builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(dialogListener != null) {
                        dialogListener.onDialogPositiveButtonClick(MessageDialog.this);
                    }
                    dismiss();
                }
            });
        }

        if(!closable) {
            builder.setCancelable(false);
            builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                    if (i == KeyEvent.KEYCODE_BACK) {
                        getActivity().finish();
                        System.exit(0);
                    }
                    return true;
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        getActivity().finish();
                        System.exit(0);
                    }
                });
            }
        }
        return builder.create();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(!closable) {
            getActivity().finish();
            System.exit(0);
        }
    }
}
