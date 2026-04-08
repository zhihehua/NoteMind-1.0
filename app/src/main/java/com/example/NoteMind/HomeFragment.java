package com.example.NoteMind;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class HomeFragment extends Fragment {

    private ChipGroup chipGroupTags;
    private String currentSelectedTag = "";
    private HorizontalScrollView tagScroll;
    private QuestionDao questionDao;
    private NoteMindAgent agent;
    private CameraUtils cameraUtils;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        questionDao = activity.getQuestionDao();
        agent = activity.getAgent();
        cameraUtils = activity.getCameraUtils();

        chipGroupTags = view.findViewById(R.id.chip_group_tags);
        tagScroll = view.findViewById(R.id.tag_scroll);

        view.findViewById(R.id.btn_input_text_container).setOnClickListener(v -> v.postDelayed(this::showInputTextDialog, 50));
        view.findViewById(R.id.btn_take_photo_container).setOnClickListener(v -> v.postDelayed(cameraUtils::openCamera, 50));
        view.findViewById(R.id.btn_gallery_container).setOnClickListener(v -> v.postDelayed(cameraUtils::openGallery, 50));

        refreshTagChips();
    }

    public void refreshTagChips() {
        if (chipGroupTags == null || getContext() == null) return;
        chipGroupTags.removeAllViews();

        Chip addBtn = new Chip(getContext());
        addBtn.setText("新标签");
        addBtn.setTextSize(15f);
        addBtn.setChipIcon(ContextCompat.getDrawable(getContext(), android.R.drawable.ic_input_add));
        addBtn.setCheckable(false);
        addBtn.setOnClickListener(v -> showNewTagInput());
        chipGroupTags.addView(addBtn);

        List<String> tags = questionDao.getAllUniqueTags();
        for (String tag : tags) {
            Chip chip = new Chip(getContext());
            chip.setText(tag);
            chip.setTextSize(15f);
            chip.setCheckable(true);
            if (tag.equals(currentSelectedTag)) {
                chip.setChecked(true);
            }
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) currentSelectedTag = tag;
                else if (currentSelectedTag.equals(tag)) currentSelectedTag = "";
            });
            chipGroupTags.addView(chip);
        }
    }

    private void showNewTagInput() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_dialog, null);
        TextView tvTitle = v.findViewById(R.id.dialog_title);
        EditText etInput = v.findViewById(R.id.dialog_input);
        Button btnConfirm = v.findViewById(R.id.dialog_btn_confirm);

        tvTitle.setText("定义新场景: (如：高数、菜谱)");
        etInput.setHint("");
        etInput.setBackgroundResource(R.drawable.edittext_bg);

        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setView(v)
                .setCancelable(true)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnConfirm.setOnClickListener(view -> {
            String val = etInput.getText().toString().trim();
            if(!val.isEmpty()) {
                currentSelectedTag = val;
                refreshTagChips();
                dialog.dismiss();
            }
        });
    }

    private void showInputTextDialog() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_dialog, null);
        TextView tvTitle = v.findViewById(R.id.dialog_title);
        EditText etInput = v.findViewById(R.id.dialog_input);
        Button btnConfirm = v.findViewById(R.id.dialog_btn_confirm);

        String label = "智能问答: (请在此输入问题)";
        if(!currentSelectedTag.isEmpty()) {
            label = "智能问答: (针对 [" + currentSelectedTag + "] 提问)";
        }
        tvTitle.setText(label);
        etInput.setHint("");
        etInput.setBackgroundResource(R.drawable.edittext_bg);

        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setView(v)
                .setCancelable(true)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnConfirm.setOnClickListener(view -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                dialog.dismiss();
                String wrappedPrompt = currentSelectedTag.isEmpty() ? text :
                        "【场景标签：" + currentSelectedTag + "】用户问题：" + text;
                
                MainActivity activity = (MainActivity) getActivity();
                if (activity != null) {
                    activity.setCurrentSelectedTag(currentSelectedTag);
                    agent.requestAiText(wrappedPrompt, activity.createAgentCallback());
                }
            }
        });
    }
}
