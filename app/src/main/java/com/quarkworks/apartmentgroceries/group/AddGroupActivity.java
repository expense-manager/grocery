package com.quarkworks.apartmentgroceries.group;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.quarkworks.apartmentgroceries.MyApplication;
import com.quarkworks.apartmentgroceries.R;
import com.quarkworks.apartmentgroceries.service.SyncGroup;
import com.quarkworks.apartmentgroceries.service.models.RGroup;

import bolts.Continuation;
import bolts.Task;

public class AddGroupActivity extends AppCompatActivity {
    private static final String TAG = AddGroupActivity.class.getSimpleName();

    /*
        References
     */
    private Toolbar toolbar;
    private TextView titleTextView;
    private EditText groupNameEditText;
    private Button addGroupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_group_activity);

        /**
         * Get view references
         */
        toolbar = (Toolbar) findViewById(R.id.main_toolbar_id);
        titleTextView = (TextView) toolbar.findViewById(R.id.toolbar_title_id);
        groupNameEditText = (EditText) findViewById(R.id.add_group_name_id);
        addGroupButton = (Button) findViewById(R.id.add_group_add_button_id);

        /**
         * Set view data
         */
        titleTextView.setText(getString(R.string.title_activity_add_group));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        addGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupItemName = groupNameEditText.getText().toString();

                if (!groupItemName.isEmpty()) {

                    RGroup groupItem = new RGroup();
                    groupItem.setName(groupItemName);
                    SyncGroup.add(groupItem).onSuccess(new Continuation<Boolean, Void>() {
                        @Override
                        public Void then(Task<Boolean> task) throws Exception {
                            if (task.getResult()) {
                                Intent intent = new Intent(MyApplication.getContext(), GroupActivity.class);
                                startActivity(intent);
                                Toast.makeText(getApplicationContext(), getString(R.string.add_group_success),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        getString(R.string.add_group_failure), Toast.LENGTH_SHORT).show();
                            }
                            return null;
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.grocery_item_name_empty), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
