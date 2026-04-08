package com.example.NoteMind;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class AtomFragment extends Fragment {

    private QuestionDao questionDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_atom, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;
        questionDao = activity.getQuestionDao();

        view.findViewById(R.id.btn_knowledge_atom_tab).setOnClickListener(v -> v.postDelayed(this::showAtomTagDialog, 50));
        
        // 默认尝试打开“高数”图谱的逻辑可以放在这里或者由 Activity 控制
        // 这里根据要求，如果存在高数数据，可以做个引导提示或默认逻辑
    }

    private void showAtomTagDialog() {
        if (getContext() == null) return;
        List<String> tags = questionDao.getAllUniqueTags();
        if (tags.isEmpty()) {
            Toast.makeText(getContext(), "暂无数据", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] tagArray = tags.toArray(new String[0]);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        ListView listView = new ListView(getContext());
        listView.setAdapter(new android.widget.ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, tagArray));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(400 * getResources().getDisplayMetrics().density));
        listView.setLayoutParams(params);
        layout.addView(listView);

        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setTitle("选择要探索的原子领域")
                .setView(layout)
                .setNegativeButton("取消", null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(getActivity(), AtomGraphActivity.class);
            intent.putExtra("TARGET_TAG", tagArray[position]);
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();
    }
}
