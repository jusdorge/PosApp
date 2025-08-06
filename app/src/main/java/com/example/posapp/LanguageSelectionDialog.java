package com.example.posapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.posapp.utils.DialogUtils;
import com.example.posapp.utils.LanguageManager;

/**
 * حوار اختيار اللغة
 */
public class LanguageSelectionDialog extends DialogFragment {
    
    private LanguageManager languageManager;
    private OnLanguageSelectedListener listener;
    
    public interface OnLanguageSelectedListener {
        void onLanguageSelected(String languageCode);
    }
    
    public static LanguageSelectionDialog newInstance() {
        return new LanguageSelectionDialog();
    }
    
    public void setOnLanguageSelectedListener(OnLanguageSelectedListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        languageManager = LanguageManager.getInstance(requireContext());
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String[] languageCodes = languageManager.getSupportedLanguages();
        String[] languageNames = languageManager.getSupportedLanguageNames();
        
        // تحديد اللغة المختارة حالياً
        String currentLanguage = languageManager.getLanguage();
        int selectedIndex = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                selectedIndex = i;
                break;
            }
        }
        
        return new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.select_language))
                .setSingleChoiceItems(languageNames, selectedIndex, null)
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    int selected = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selected >= 0 && selected < languageCodes.length) {
                        String selectedLanguage = languageCodes[selected];
                        
                        android.util.Log.d("LanguageDialog", "تم اختيار اللغة: " + selectedLanguage);
                        
                        // حفظ اللغة المختارة
                        languageManager.setLanguage(selectedLanguage);
                        
                        // إشعار المستمع
                        if (listener != null) {
                            listener.onLanguageSelected(selectedLanguage);
                        }
                        
                        // عرض رسالة تأكيد
                        DialogUtils.showToastSafely(this, getString(R.string.language_changed_restart_required));
                    }
                    
                    DialogUtils.dismissSafely(this);
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    DialogUtils.dismissSafely(this);
                })
                .create();
    }
    
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        android.util.Log.d("LanguageDialog", "تم إغلاق حوار اختيار اللغة");
    }
}