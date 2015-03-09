package de.dreier.mytargets.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.text.DateFormat;

import de.dreier.mytargets.R;
import de.dreier.mytargets.adapters.RoundsAdapter;
import de.dreier.mytargets.models.Training;

/**
 * Shows all rounds of one training day
 */
public class TrainingActivity extends NowListActivity {

    private long mTraining;

    @Override
    protected void init(Intent intent, Bundle savedInstanceState) {
        itemTypeRes = R.plurals.round;
        mEditable = true;

        if(intent!=null && intent.hasExtra(TRAINING_ID)) {
            mTraining = intent.getLongExtra(TRAINING_ID,-1);
        }
        if(savedInstanceState!=null) {
            mTraining = savedInstanceState.getLong(TRAINING_ID,-1);
        }
        if(mTraining==-1)
            finish();

        Training tr = db.getTraining(mTraining);
        getSupportActionBar().setTitle(tr.title);
        getSupportActionBar().setSubtitle(DateFormat.getDateInstance().format(tr.date));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onEdit(long id) {
        Intent i = new Intent(this, EditRoundActivity.class);
        i.putExtra(EditRoundActivity.TRAINING_ID, mTraining);
        i.putExtra(EditRoundActivity.ROUND_ID, id);
        startActivity(i);
    }

    @Override
    protected void onDelete(long[] ids) {
        db.deleteRounds(ids);
    }

    @Override
    public boolean onItemClick(Intent i, int pos, long id) {
        if(pos==0) {
            i.setClass(this, EditRoundActivity.class);
            i.putExtra(EditRoundActivity.TRAINING_ID, mTraining);
        } else {
            i.setClass(this, RoundActivity.class);
            i.putExtra(RoundActivity.ROUND_ID, getListAdapter().getItemId(pos));
            i.putExtra(RoundActivity.TRAINING_ID, mTraining);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter = new RoundsAdapter(this,mTraining);
        setListAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(TRAINING_ID,mTraining);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.training, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean hasPasses = adapter.getCount() > 1;
        menu.findItem(R.id.action_statistics).setVisible(hasPasses);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_statistics:
                Intent i = new Intent(this, StatisticsActivity.class);
                i.putExtra(StatisticsActivity.TRAINING_ID, mTraining);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}